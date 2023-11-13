package io.levelops.api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.api.model.RunbookDTO;
import io.levelops.api.model.RunbookDTO.RunbookUiData.UiFieldData;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.databases.services.RunbookNodeTemplateDatabaseService;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.ContentType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookDTOService {

    public static final String START_NODE_ID = "0";
    private final ObjectMapper objectMapper;
    private final RunbookNodeTemplateDatabaseService runbookNodeTemplateDatabaseService;

    @Autowired
    public RunbookDTOService(ObjectMapper objectMapper,
                             RunbookNodeTemplateDatabaseService runbookNodeTemplateDatabaseService) {
        this.objectMapper = objectMapper;
        this.runbookNodeTemplateDatabaseService = runbookNodeTemplateDatabaseService;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookNodeUiData.RunbookNodeUiDataBuilder.class)
    public static class RunbookNodeUiData {
        @JsonProperty("position")
        JsonNode position;

        @JsonProperty("ports")
        JsonNode ports;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookInternalUiData.RunbookInternalUiDataBuilder.class)
    public static class RunbookInternalUiData {
        @JsonProperty("start_node")
        RunbookNode startNode;
    }

    public Runbook parseDTO(String company, RunbookDTO runbookDTO) {
        Validate.notNull(runbookDTO, "runbookDTO cannot be null.");

        List<RunbookNode> nodes = parseNodes(company, runbookDTO.getUiData());
        List<RunbookNode> nodesWithoutStartNode = nodes.stream()
                .filter(n -> !START_NODE_ID.equals(n.getId()))
                .collect(Collectors.toList());
        RunbookNode startNode = nodes.stream()
                .filter(n -> START_NODE_ID.equals(n.getId()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Invalid runbook: no start node"));

        return Runbook.builder()
                .name(runbookDTO.getName())
                .description(runbookDTO.getDescription())
                .enabled(runbookDTO.getEnabled())
                .previousId(runbookDTO.getPreviousId())
                .triggerType(runbookDTO.getTriggerType())
                .triggerTemplateType(runbookDTO.getTriggerTemplateType())
                .triggerData(startNode.getInput())
                .addNodes(nodesWithoutStartNode)
                .uiData(ParsingUtils.toJsonObject(objectMapper, RunbookInternalUiData.builder()
                        .startNode(startNode.toBuilder()
                                .input(null) // this goes in trigger_data
                                .build())
                        .build()))
                .settings(runbookDTO.getSettings())
                .build();
    }

    private Map<String, String> getTemplateTypeToNodeHandlerMapping(String company, Collection<RunbookDTO.RunbookUiData.Node> nodes) {
        var templateTypes = CollectionUtils.emptyIfNull(nodes).stream()
                .map(RunbookDTO.RunbookUiData.Node::getType)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
        return runbookNodeTemplateDatabaseService.stream(company, templateTypes, null, null, null, null)
                .filter(Objects::nonNull)
                .filter(t -> StringUtils.isNotEmpty(t.getType()))
                .collect(Collectors.toMap(
                        RunbookNodeTemplate::getType,
                        t -> StringUtils.firstNonBlank(t.getNodeHandler(), t.getType()),
                        (a, b) -> {
                            log.warn("Runbook DTO: collision between 2 node templates with the same type (keeping later): {} - {}", a, b);
                            return b;
                        }));
    }

    private List<RunbookNode> parseNodes(String company, RunbookDTO.RunbookUiData uiData) {
        if (uiData == null) {
            return Collections.emptyList();
        }

        Collection<RunbookDTO.RunbookUiData.Node> nodes = MapUtils.emptyIfNull(uiData.getNodes()).values();

        Map<String, String> templateTypeToNodeHandlerMapping = getTemplateTypeToNodeHandlerMapping(company, nodes);

        Map<String, RunbookNode.RunbookNodeBuilder> nodeBuilders = nodes.stream()
                .filter(Objects::nonNull)
                .filter(node -> StringUtils.isNotEmpty(node.getId()))
                .collect(Collectors.toMap(
                        RunbookDTO.RunbookUiData.Node::getId,
                        node -> this.parseUiNode(node, templateTypeToNodeHandlerMapping),
                        (a, b) -> {
                            log.warn("Runbook DTO: collision between 2 nodes with the same id (keeping later): {} - {}", a, b);
                            return b;
                        }));

        MapUtils.emptyIfNull(uiData.getLinks()).values().stream()
                .filter(Objects::nonNull)
                .forEach(link -> {
                    if (link.getFrom() == null || link.getTo() == null) {
                        return;
                    }
                    String fromNodeId = link.getFrom().getNodeId();
                    String toNodeId = link.getTo().getNodeId();
                    if (StringUtils.isAnyEmpty(fromNodeId, toNodeId)) {
                        return;
                    }
                    RunbookNode.NodeTransition transition = parseNodeTransition(link);
                    if (!nodeBuilders.containsKey(toNodeId)) {
                        return;
                    }
                    if (!nodeBuilders.containsKey(fromNodeId)) {
                        return;
                    }
                    nodeBuilders.get(fromNodeId).to(toNodeId, transition);
                    nodeBuilders.get(toNodeId).from(fromNodeId, transition);
                });

        return nodeBuilders.values().stream()
                .map(RunbookNode.RunbookNodeBuilder::build)
                .collect(Collectors.toList());
    }

    private RunbookNode.RunbookNodeBuilder parseUiNode(RunbookDTO.RunbookUiData.Node node, Map<String, String> templateTypeToNodeHandler) {
        return RunbookNode.builder()
                .id(node.getId())
                .name(node.getName())
                .type(node.getType())
                .nodeHandler(templateTypeToNodeHandler.getOrDefault(node.getType(), node.getType()))
                .input(parseInputVariables(node.getInput()))
                .uiData(ParsingUtils.toJsonObject(objectMapper, RunbookNodeUiData.builder()
                        .ports(node.getPorts())
                        .position(node.getPosition())
                        .build()));
    }

    private Map<String, RunbookVariable> parseInputVariables(Map<String, UiFieldData> input) {
        return MapUtils.emptyIfNull(input).values().stream()
                .filter(Objects::nonNull)
                .filter(data -> Strings.isNotEmpty(data.getKey()))
                .map(this::parseInputField)
                .collect(Collectors.toMap(
                        RunbookVariable::getName,
                        Function.identity(),
                        (a, b) -> {
                            log.warn("Runbook DTO: collision between 2 variables with the same key (keeping later): {} - {}", a, b);
                            return b;
                        }));
    }

    @VisibleForTesting
    protected RunbookVariable parseInputField(UiFieldData field) {
        Object value = null;
        String contentType = null;
        RunbookVariable.RunbookValueType valueType = RunbookVariable.RunbookValueType.NONE;
        List<UiFieldData.Value> values = ListUtils.emptyIfNull(field.getValues());
        if (values.size() == 1) {
            value = values.get(0).getValue();
            contentType = values.get(0).getType();
            if (value == null || value instanceof String) {
                valueType = RunbookVariable.RunbookValueType.STRING;
            } else {
                valueType = RunbookVariable.RunbookValueType.JSON_BLOB;
            }
        } else if (values.size() > 1) {
            value = values.stream().map(UiFieldData.Value::getValue).collect(Collectors.toList());
            contentType = values.get(0).getType();
            valueType = RunbookVariable.RunbookValueType.JSON_ARRAY;
        }
        return RunbookVariable.builder()
                .name(field.getKey())
                .type(field.getType())
                .contentType(ContentType.fromString(contentType))
                .value(value)
                .valueType(valueType)
                .build();
    }

    private RunbookNode.NodeTransition parseNodeTransition(RunbookDTO.RunbookUiData.Link link) {
        var builder = RunbookNode.NodeTransition.builder();
        if (link.getProperties() != null) {
            builder = builder
                    .wait(link.getProperties().getWait()) // TODO defaults and null checks
                    .option(link.getProperties().getOption());
        }
        return builder.build();
    }

    // region TO DTO ////////////////////////////////////////////////////////////////////////////////

    public RunbookDTO toDTO(Runbook runbook) {
        RunbookDTO.RunbookUiData uiData = convertToDTOUiData(runbook);

        return RunbookDTO.builder()
                .id(runbook.getId())
                .permanentId(runbook.getPermanentId())
                .name(runbook.getName())
                .description(runbook.getDescription())
                .enabled(runbook.getEnabled())
                .previousId(runbook.getPreviousId())
                .triggerType(runbook.getTriggerType())
                .triggerTemplateType(runbook.getTriggerTemplateType())
                .uiData(uiData)
                .settings(runbook.getSettings())
                .build();
    }

    private RunbookDTO.RunbookUiData convertToDTOUiData(Runbook runbook) {
        return RunbookDTO.RunbookUiData.builder()
                .nodes(convertToDTONodes(runbook))
                .links(convertToDTOLinks(runbook))
                .build();
    }

    private Map<String, RunbookDTO.RunbookUiData.Link> convertToDTOLinks(Runbook runbook) {
        return MapUtils.emptyIfNull(runbook.getNodes()).values().stream()
                .flatMap(node -> MapUtils.emptyIfNull(node.getFromNodes()).entrySet().stream()
                        // from = entry.getKey(), to = node.getId(), transition data = entry.getValue()
                        .map(entry -> convertToDTOLink(entry.getKey(), node.getId(), entry.getValue())))
                .collect(Collectors.toMap(
                        RunbookDTO.RunbookUiData.Link::getId,
                        Function.identity(), (a, b) -> {
                            log.warn("Runbook DTO: collision between 2 links with the same id (keeping later): {} - {}", a, b);
                            return b;
                        }));
    }

    private RunbookDTO.RunbookUiData.Link convertToDTOLink(String fromNode, String toNode,
                                                           RunbookNode.NodeTransition nodeTransition) {
        return RunbookDTO.RunbookUiData.Link.builder()
                .id(UUID.randomUUID().toString())
                .from(RunbookDTO.RunbookUiData.NodePort.builder()
                        .nodeId(fromNode)
                        .portId("output")
                        .build())
                .to(RunbookDTO.RunbookUiData.NodePort.builder()
                        .nodeId(toNode)
                        .portId("input")
                        .build())
                .properties(RunbookDTO.RunbookUiData.Link.LinkProperties.builder()
                        .wait(nodeTransition != null ? nodeTransition.getWait() : null)
                        .option(nodeTransition != null ? nodeTransition.getOption() : null)
                        .build())
                .build();
    }

    private Map<String, RunbookDTO.RunbookUiData.Node> convertToDTONodes(Runbook runbook) {
        return Stream.concat(
                MapUtils.emptyIfNull(runbook.getNodes()).values().stream()
                        .map(this::convertToDTONode),
                Stream.of(convertToDTOTriggerNode(runbook))
        ).collect(Collectors.toMap(
                RunbookDTO.RunbookUiData.Node::getId,
                Function.identity(),
                (a, b) -> {
                    log.warn("Runbook DTO: collision between 2 nodes with the same id (keeping later): {} - {}", a, b);
                    return b;
                }));
    }

    private RunbookDTO.RunbookUiData.Node convertToDTOTriggerNode(Runbook rb) {
        if (MapUtils.isEmpty(rb.getUiData())) {
            throw new IllegalArgumentException("Invalid runbook: no UI data");
        }
        RunbookInternalUiData runbookInternalUiData = objectMapper.convertValue(rb.getUiData(), RunbookInternalUiData.class);
        if (runbookInternalUiData == null || runbookInternalUiData.getStartNode() == null) {
            throw new IllegalArgumentException("Invalid runbook: UI data missing start_node");
        }
        RunbookNode startNode = runbookInternalUiData.getStartNode();
        return convertToDTONode(startNode.toBuilder()
                .input(rb.getTriggerData())
                .build());
    }

    private RunbookDTO.RunbookUiData.Node convertToDTONode(RunbookNode node) {
        if (node == null || node.getUiData() == null) {
            throw new IllegalArgumentException("Invalid runbook node with no UI data: " + node);
        }
        RunbookNodeUiData runbookNodeUiData = objectMapper.convertValue(node.getUiData(), RunbookNodeUiData.class);
        return RunbookDTO.RunbookUiData.Node.builder()
                .id(node.getId())
                .type(node.getType())
                .name(node.getName())
                .position(runbookNodeUiData.getPosition())
                .ports(runbookNodeUiData.getPorts())
                .input(convertToDTOFields(node.getInput()))
                .build();
    }

    private Map<String, UiFieldData> convertToDTOFields(Map<String, RunbookVariable> variables) {
        return MapUtils.emptyIfNull(variables).values().stream()
                .map(this::convertToDTOField)
                .filter(field -> field != null && StringUtils.isNotEmpty(field.getKey()))
                .collect(Collectors.toMap(
                        UiFieldData::getKey,
                        Function.identity(),
                        (a, b) -> {
                            log.warn("Runbook DTO: collision between 2 variables with the same key (keeping later): {} - {}", a, b);
                            return b;
                        }));
    }

    private UiFieldData convertToDTOField(RunbookVariable variable) {
        List<UiFieldData.Value> values = new ArrayList<>();

        String contentType = variable.getContentType() != null ? variable.getContentType().toString() : null;
        RunbookVariable.RunbookValueType valueType = MoreObjects.firstNonNull(variable.getValueType(), RunbookVariable.RunbookValueType.NONE);
        switch (valueType) {
            case NONE:
                break;
            case JSON_ARRAY:
                if (variable.getValue() == null) {
                    break;
                }
                List<Object> list = objectMapper.convertValue(variable.getValue(), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, Object.class));
                if (list == null) {
                    break;
                }
                list.forEach(item ->
                        values.add(UiFieldData.Value.builder()
                                .type(contentType)
                                .value(item)
                                .build()));
                break;
            case JSON_BLOB:
                values.add(UiFieldData.Value.builder()
                        .type(contentType)
                        .value(variable.getValue())
                        .build());
                break;
            case STRING:
            default:
                values.add(UiFieldData.Value.builder()
                        .type(contentType)
                        .value(variable.getValue() != null ? variable.getValue().toString() : null)
                        .build());
                break;
        }
        return UiFieldData.builder()
                .key(variable.getName())
                .values(values)
                .type(variable.getType())
                .build();
    }

    // endregion
}
