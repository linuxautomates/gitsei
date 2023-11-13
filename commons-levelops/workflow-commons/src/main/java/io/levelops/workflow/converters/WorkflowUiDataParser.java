package io.levelops.workflow.converters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import io.levelops.workflow.models.Workflow;
import io.levelops.workflow.models.WorkflowPolicy;
import io.levelops.workflow.models.WorkflowPolicy.Action;
import io.levelops.workflow.models.WorkflowPolicy.WorkflowPolicyBuilder;
import io.levelops.workflow.models.ui.WorkflowUiData;
import io.levelops.workflow.models.ui.WorkflowUiData.Link;
import io.levelops.workflow.models.ui.WorkflowUiData.Node;
import io.levelops.workflow.models.ui.WorkflowUiData.NodeType;
import io.levelops.workflow.models.ui.WorkflowUiData.Port;
import io.levelops.workflow.models.ui.configurations.CheckStatusNodeConfiguration;
import io.levelops.workflow.models.ui.configurations.CheckStatusNodeConfiguration.Frequency;
import io.levelops.workflow.models.ui.configurations.ConditionNodeConfiguration;
import io.levelops.workflow.models.ui.configurations.StartNodeConfiguration;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.workflow.models.WorkflowPolicy.WorkflowPolicyType.CHECK_STATUS;
import static io.levelops.workflow.models.WorkflowPolicy.WorkflowPolicyType.POLICY;

@Log4j2
public class WorkflowUiDataParser {

    private final ObjectMapper objectMapper;

    @Builder
    public WorkflowUiDataParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ParsedWorkflowData.ParsedWorkflowDataBuilder.class)
    public static class ParsedWorkflowData {
        @JsonProperty("workflow")
        Workflow workflow;

        @JsonProperty("policies")
        List<WorkflowPolicy> policies;
    }

    public ParsedWorkflowData parse(WorkflowUiData data) {
        // start node will be use as the start of the traversal
        Node startNode = findStartNode(data.getNodes().values().stream())
                .orElseThrow(() -> new IllegalArgumentException("Invalid workflow: could not find START node"));
        Map<String, List<Link>> linksByFromNode = groupLinksByFromNode(data.getLinks().values().stream());

        // stores policies built during traversal
        ArrayList<WorkflowPolicy> builtPolicies = Lists.newArrayList();

        // stores visited nodes to detect cycles in the Workflow graph and prevent infinite loops
        Set<String> visitedNodeIds = new HashSet<>();

        // recursive traversal
        traverse(startNode, null, data.getNodes(), linksByFromNode, visitedNodeIds, builtPolicies, null);

        // workflow metadata
        Workflow workflow = parseWorkflow(data, startNode);

        // add workflow id to policies
        List<WorkflowPolicy> policies = builtPolicies.stream()
                .map(p -> p.toBuilder()
                        .workflowId(workflow.getId())
                        .build())
                .collect(Collectors.toList());

        return ParsedWorkflowData.builder()
                .workflow(workflow)
                .policies(policies)
                .build();
    }

    private void traverse(Node currentNode,
                          Port fromPort,
                          Map<String, Node> nodesById,
                          Map<String, List<Link>> linksByFromNode,
                          Set<String> visitedNodeIds, List<WorkflowPolicy> builtPolicies, WorkflowPolicyBuilder pendingPolicyBuilder) {
        log.info("Visiting   : {} ('{}') of type {}", currentNode.getId(),
                currentNode.getProperties().getName(),
                currentNode.getProperties().getType());

        if (visitedNodeIds.contains(currentNode.getId())) {
            throw newParsingException("Invalid Workflow: detected cycle", currentNode);
        }
        visitedNodeIds.add(currentNode.getId());

        // parse current node
        WorkflowPolicyBuilder newPolicyBuilder = parseNode(currentNode, fromPort, pendingPolicyBuilder);

        // follow links from this node
        List<Link> links = linksByFromNode.getOrDefault(currentNode.getId(), Collections.emptyList());
        for (Link link : links) {
            Node to = nodesById.get(link.getTo().getNodeId());
            String currentNodePortId = link.getFrom().getPortId();
            Port currentNodePort = currentNode.getPorts().get(currentNodePortId);

            log.info("Traversing : {} ('{}') --[{}]--> {} ('{}')", currentNode.getId(), currentNode.getProperties().getName(), currentNodePort.getProperties().getAction(), to.getId(), to.getProperties().getName());
            traverse(to, currentNodePort, nodesById, linksByFromNode, visitedNodeIds, builtPolicies, newPolicyBuilder);
        }

        // if a new policy was created, add it to the list of built policies
        if (newPolicyBuilder != null && pendingPolicyBuilder != newPolicyBuilder) {
            builtPolicies.add(newPolicyBuilder.build());
        }
    }

    // region node parsing

    private Workflow parseWorkflow(WorkflowUiData data, Node startNode) {
        StartNodeConfiguration config = startNode.getConfiguration(objectMapper, StartNodeConfiguration.class)
                .orElseThrow(() -> newParsingException("Got malformed node with no configuration", startNode));
        return Workflow.builder()
                .id(data.getId())
                .name(data.getName())
                .productIds(config.getProductIds())
                .releaseIds(config.getReleaseIds())
                .stageIds(config.getStageIds())
                .severity("low")
                .build();
    }

    private WorkflowPolicyBuilder parseNode(Node currentNode,
                                            Port fromPort,
                                            WorkflowPolicyBuilder pendingPolicyBuilder) {
        NodeType nodeType = NodeType.fromString(currentNode.getProperties().getType());
        if (nodeType == null) {
            throw newParsingException("Got node with invalid type: " + currentNode.getProperties().getType(), currentNode);
        }
        // if no pending policy then we are at the start
        if (pendingPolicyBuilder == null) {
            if (nodeType == NodeType.START) {
                return null;
            }
            // parent was start node, we expect a condition node only
            if (nodeType != NodeType.CONDITION) {
                throw newParsingException("Expected a condition after Workflow start. Got: " + nodeType, currentNode);
            }
            return parseConditionNode(currentNode);
        }
        // case ACTION_*:
        if (nodeType.isAction()) {
            int actionId = pendingPolicyBuilder.getActionsSize();
            pendingPolicyBuilder.action(parseAction(actionId, currentNode, fromPort));
            return pendingPolicyBuilder;
        }
        switch (nodeType) {
            case WAIT:
                WorkflowPolicy parentPolicy = pendingPolicyBuilder.build();
                return parseCheckStatusNode(currentNode, parentPolicy);
            case START:
                throw newParsingException("Unexpected node of type 'start': only 1 allowed per workflow", currentNode);
            case CONDITION:
                throw newParsingException("Unexpected node of type 'condition': only allowed after Start node", currentNode);
            default:
                throw newParsingException("Unsupported node of type: " + nodeType, currentNode);
        }
    }

    private WorkflowPolicyBuilder parseConditionNode(Node currentNode) {
        ConditionNodeConfiguration config = currentNode.getConfiguration(objectMapper, ConditionNodeConfiguration.class)
                .orElseThrow(() -> newParsingException("Got malformed node with no configuration", currentNode));
        return WorkflowPolicy.builder()
                .id(currentNode.getId())
                .name(currentNode.getProperties().getName())
                .type(POLICY)
                .integrationType(config.getIntegrationType())
                .integrationIds(config.getIntegrationIds())
                .condition(WorkflowPolicy.Condition.builder()
                        .conditionType(config.getConditionType())
                        .signatureCondition(config.getSignatureCondition())
                        .aggregateCondition(config.getAggregateCondition())
                        .build());
    }

    private WorkflowPolicyBuilder parseCheckStatusNode(Node currentNode, WorkflowPolicy parentPolicy) {
        CheckStatusNodeConfiguration config = currentNode.getConfiguration(objectMapper, CheckStatusNodeConfiguration.class)
                .orElseThrow(() -> newParsingException("Got malformed node with no configuration", currentNode));
        String nodeId = config.getNodeId();

        String cron = Optional.of(config)
                .map(CheckStatusNodeConfiguration::getFrequency)
                .map(Frequency::getCron)
                .orElse(null);

        String actionId = null;
        if (Strings.isNotEmpty(nodeId) && CollectionUtils.isNotEmpty(parentPolicy.getActions())) {
            actionId = parentPolicy.getActions()
                    .stream()
                    .filter(a -> nodeId.equals(a.getNodeId()))
                    .map(Action::getId)
                    .findFirst()
                    .orElse(null);
        }

        return WorkflowPolicy.builder()
                .id(currentNode.getId())
                .name(currentNode.getProperties().getName())
                .type(CHECK_STATUS)
                .parentId(parentPolicy.getId())
                .actionId(actionId)
                .exitStatus(config.getExitStatus())
                .cron(cron);
    }

    private Action parseAction(int actionId, Node currentNode, Port fromPort) {
        return Action.builder()
                .id(String.valueOf(actionId))
                .onStatus(fromPort.getProperties().getAction())
                .type(currentNode.getType())
                .name(currentNode.getProperties().getName())
                .nodeId(currentNode.getId())
                .payload(currentNode.getProperties().getConfigurations())
                .build();
    }

    // endregion

    // region utility
    private Optional<Node> findStartNode(Stream<Node> nodeStream) {
        return nodeStream
                .filter(n -> NodeType.START.equals(NodeType.fromString(n.getProperties().getType())))
                .findFirst();
    }

    private Map<String, List<Node>> groupNodesByType(Stream<Node> nodeStream) {
        return nodeStream
                .collect(Collectors.groupingBy(
                        n -> n.getProperties().getType()));
    }

    private Map<String, List<Link>> groupLinksByFromNode(Stream<Link> linkStream) {
        return linkStream.collect(Collectors.groupingBy(
                l -> l.getFrom().getNodeId()));
    }

    private IllegalArgumentException newParsingException(String message, Node currentNode) {
        if (currentNode != null) {
            return new IllegalArgumentException(message + " - " + currentNode.toString());
        } else {
            return new IllegalArgumentException(message);
        }
    }
    // endregion
}
