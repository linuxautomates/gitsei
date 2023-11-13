package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.UITriggerSchema;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.EventType.EventTypeCategory;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triggers.clients.TriggersRESTClient;
import io.levelops.triggers.clients.TriggersRESTClientException;
import io.levelops.web.util.SpringUtils;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RestController
@RequestMapping("/v1/playbooks/triggers")
public class TriggersController {

    private final TriggersRESTClient triggersClient;

    public TriggersController(final TriggersRESTClient triggersClient) {
        this.triggersClient = triggersClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping(value = "/schemas/{type}", produces = "application/json")
    public DeferredResult<ResponseEntity<TriggerSchema>> getTriggerSchema(@SessionAttribute("company") String company,
                                                                          @PathVariable("type") String type) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(searchByType(company, type)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(value = "/schemas/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<UITriggerSchema>>> listTriggerSchema(
            @SessionAttribute("company") String company, @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> {
            // check for the special 'type' search field
            String type = search.getFilter() != null ? (String) search.getFilter().get("type") : null;
            log.debug("Type: {}", type);
            if (Strings.isNotBlank(type)) {
                return ResponseEntity.ok(PaginatedResponse.of(search.getPage(), search.getPageSize(),
                        List.of(searchByType(company, type))));
            }
            // get all schema types
            PaginatedResponse<TriggerSchema> baseSchemas = triggersClient.listTriggerSchemas(company,
                    search.toBuilder().pageSize(50).build());

            // get the component event trigger schema
            TriggerSchema componentEventTriggerSchema = baseSchemas.getResponse().getRecords().stream()
                    .filter(item -> item.getTriggerType() == TriggerType.COMPONENT_EVENT)
                    .findFirst()
                    .orElse(TriggerSchema.builder()
                            .triggerType(TriggerType.COMPONENT_EVENT)
                            .description("")
                            .fields(Map.of())
                            .build());

            // mix them up - non component_event first, then, component events sorted by name
            List<UITriggerSchema> uiTriggerSchemas = new ArrayList<>();
            baseSchemas.getResponse().getRecords().stream()
                    .filter(item -> item.getTriggerType() != TriggerType.COMPONENT_EVENT)
                    .map(this::mapToUITriggerSchema)
                    .sorted(Comparator.comparing(UITriggerSchema::getDisplayName))
                    .forEachOrdered(uiTriggerSchemas::add);
            mapEventsAndComponentEventTriggerSchemas(company, componentEventTriggerSchema, search).stream()
                    .sorted(Comparator.comparing(UITriggerSchema::getDisplayName))
                    .forEachOrdered(uiTriggerSchemas::add);
            return ResponseEntity
                    .ok(PaginatedResponse.of(search.getPage(), search.getPageSize(), uiTriggerSchemas.size(),
                            search.getPageSize() < uiTriggerSchemas.size()
                                    ? uiTriggerSchemas.subList(0, search.getPageSize())
                                    : uiTriggerSchemas));
        });
    }

    private UITriggerSchema searchByType(String company, String type)
            throws ResponseStatusException, TriggersRESTClientException {
        TriggerType triggerType = TriggerType.fromString(type);
        if (triggerType != TriggerType.UNKNOWN) {
            return mapToUITriggerSchema(triggersClient.getTriggerSchemas(company, triggerType)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Trigger Schema not found for type: " + type)));
        }
        var eventType = EventType.fromString(type);
        if (eventType == EventType.UNKNOWN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trigger Schema not found for type: " + type);
        }
        return mapToUITriggerSchema(
                triggersClient.getTriggerSchemas(company, TriggerType.COMPONENT_EVENT)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Trigger Schema not found for type: " + type)),
                triggersClient.getEventType(company, eventType)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Trigger Schema not found for type: " + type)));
    }

    private List<UITriggerSchema> mapEventsAndComponentEventTriggerSchemas(final String company,
                                                                           final TriggerSchema componentEventTriggerSchema,
                                                                           final DefaultListRequest search)
            throws TriggersRESTClientException {
        // get all event types
        PaginatedResponse<EventType> eventTypes = triggersClient.listEventTypes(company,
                search.toBuilder().pageSize(200).build());
        if (eventTypes.getMetadata().getTotalCount() == null || eventTypes.getMetadata().getTotalCount() == 0) {
            return List.of();
        }
        List<UITriggerSchema> uiTriggerSchemas = new ArrayList<>();
        eventTypes.getResponse().getRecords().forEach(
                eventType -> uiTriggerSchemas.add(mapToUITriggerSchema(componentEventTriggerSchema, eventType)));
        return uiTriggerSchemas;
    }

    private UITriggerSchema mapToUITriggerSchema(final TriggerSchema baseTriggerSchema) {
        return mapToUITriggerSchema(baseTriggerSchema, null);
    }

    private UITriggerSchema mapToUITriggerSchema(final TriggerSchema baseTriggerSchema, @Nullable final EventType eventType) {
        try {
            Map<String, KvField> fields = new HashMap<>();
            fields.putAll(baseTriggerSchema.getFields());
            String type = baseTriggerSchema.getTriggerType().toString();
            String description = baseTriggerSchema.getDescription();
            EventTypeCategory category = null;
            if (eventType != null) {
                type = eventType.toString();
                description = eventType.getDescription();
                category = eventType.getCategory();
                fields.putAll(eventType.getData());
                fields.put("component_type", fields.get("component_type").toBuilder()
                        .defaultValue(eventType.getComponent().getType().toString()).build());
                fields.put("component_name", fields.get("component_name").toBuilder()
                        .defaultValue(eventType.getComponent().getName()).build());
                fields.put("event_type",
                        fields.get("event_type").toBuilder().defaultValue(eventType.toString()).build());
            }
            String categoryDisplayName = category != null ? category.getDisplayName() : null;
            categoryDisplayName = StringUtils.defaultIfEmpty(categoryDisplayName, "LevelOps");

            return UITriggerSchema.builder()
                    .triggerType(baseTriggerSchema.getTriggerType())
                    .type(type)
                    .displayName(WordUtils.capitalize(type.replaceAll("_", " ")))
                    .description(MessageFormat.format("{0} - {1}", WordUtils.capitalizeFully(type.replaceAll("_", " ")), description))
                    .fields(fields)
                    .uiData(getUIData(baseTriggerSchema.getTriggerType(), eventType))
                    .category(categoryDisplayName)
                    .build();
        } catch (Exception e) {
            try {
                log.error("Error processing: trigger={} event={}",
                        DefaultObjectMapper.get().writeValueAsString(baseTriggerSchema),
                        DefaultObjectMapper.get().writeValueAsString(eventType));
            } catch (JsonProcessingException e1) {
                log.error("Error processing: trigger={} event={}",
                        baseTriggerSchema,
                        eventType);
            }
            throw e;
        }
    }

    private Map<String, Object> getUIData(@Nonnull TriggerType triggerType, @Nullable EventType eventType) {
        String icon = StringUtils.defaultIfEmpty(EventType.getIcon(triggerType, eventType), "levelops");
        return Map.of("icon", icon);
    }

}