package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.model.RunbookDTO;
import io.levelops.api.model.RunbookReportDTO;
import io.levelops.api.model.RunbookRunningNodeDTO;
import io.levelops.api.services.RunbookDTOService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNodeState;
import io.levelops.commons.databases.models.database.runbooks.RunbookTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookTemplateCategory;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.databases.models.database.runbooks.utils.RunbookVariableUtils;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.RunbookDatabaseService;
import io.levelops.commons.databases.services.RunbookNodeTemplateDatabaseService;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookRunDatabaseService;
import io.levelops.commons.databases.services.RunbookRunningNodeDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateCategoryDatabaseService;
import io.levelops.commons.databases.services.RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter;
import io.levelops.commons.databases.services.RunbookTemplateDatabaseService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.runbooks.clients.RunbookClient;
import io.levelops.runbooks.models.EvaluateNodeRequest;
import io.levelops.runbooks.models.EvaluateNodeResponse;
import io.levelops.runbooks.services.RunbookReportService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.comparison.ComparisonUtils.hasChanged;
import static io.levelops.commons.databases.services.RunbookTemplateDatabaseService.RunbookTemplateFilter;


@Log4j2
@RequestMapping("/v1/playbooks")
@RestController
public class RunbookController {

    private final ObjectMapper objectMapper;
    private final RunbookDatabaseService runbookDatabaseService;
    private final RunbookNodeTemplateDatabaseService nodeTemplateDatabaseService;
    private final RunbookRunDatabaseService runbookRunDatabaseService;
    private final RunbookReportDatabaseService runbookReportDatabaseService;
    private final RunbookReportService runbookReportService;
    private final RunbookDTOService runbookDTOService;
    private final ActivityLogService activityLogService;
    private final RunbookRunningNodeDatabaseService runbookRunningNodeDatabaseService;
    private final RunbookClient runbookClient;
    private final RunbookTemplateDatabaseService runbookTemplateDatabaseService;
    private final RunbookTemplateCategoryDatabaseService runbookTemplateCategoryDatabaseService;

    @Autowired
    public RunbookController(ObjectMapper objectMapper,
                             RunbookDatabaseService runbookDatabaseService,
                             RunbookNodeTemplateDatabaseService nodeTemplateDatabaseService,
                             RunbookRunDatabaseService runbookRunDatabaseService,
                             RunbookReportDatabaseService runbookReportDatabaseService,
                             RunbookReportService runbookReportService,
                             RunbookDTOService runbookDTOService,
                             ActivityLogService activityLogService,
                             RunbookRunningNodeDatabaseService runbookRunningNodeDatabaseService,
                             RunbookClient runbookClient,
                             RunbookTemplateDatabaseService runbookTemplateDatabaseService,
                             RunbookTemplateCategoryDatabaseService runbookTemplateCategoryDatabaseService) {
        this.objectMapper = objectMapper;
        this.runbookDatabaseService = runbookDatabaseService;
        this.nodeTemplateDatabaseService = nodeTemplateDatabaseService;
        this.runbookRunDatabaseService = runbookRunDatabaseService;
        this.runbookReportDatabaseService = runbookReportDatabaseService;
        this.runbookReportService = runbookReportService;
        this.runbookDTOService = runbookDTOService;
        this.activityLogService = activityLogService;
        this.runbookRunningNodeDatabaseService = runbookRunningNodeDatabaseService;
        this.runbookClient = runbookClient;
        this.runbookTemplateDatabaseService = runbookTemplateDatabaseService;
        this.runbookTemplateCategoryDatabaseService = runbookTemplateCategoryDatabaseService;
    }

    private void logActivity(String company, String sessionUser, ActivityLog.Action action, String msg, String id, Object item) throws SQLException {
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(id)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.PLAYBOOK)
                .action(action)
                .details(Collections.singletonMap("item", item))
                .body(String.format("%s Playbook: %s.", msg, id))
                .build());

    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping
    public DeferredResult<ResponseEntity<Map<String, ?>>> createRunbook(@SessionAttribute("company") String company,
                                                                        @SessionAttribute("session_user") final String sessionUser,
                                                                        @RequestBody String rawRunbookUiData,
                                                                        @SessionAttribute(name = "entitlementsConfig") Map<String, String> entitlementsConfig) {
        return SpringUtils.deferResponse(() -> {

            if(MapUtils.isNotEmpty(entitlementsConfig) &&
                    entitlementsConfig.containsKey("PROPELS_COUNT")){

                boolean isCountMaxedOut = runbookDatabaseService.isCountMaxedOut(company, entitlementsConfig);
                if(isCountMaxedOut)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max Propel count reached for tiral user");
            }


            // parse UI data into Backend data
            RunbookDTO runbookDTO = objectMapper.readValue(rawRunbookUiData, RunbookDTO.class);
            Validate.notNull(runbookDTO.getUiData(), "ui_data cannot be null.");
            Runbook runbook = runbookDTOService.parseDTO(company, runbookDTO);

            RunbookDatabaseService.InsertResult ids = runbookDatabaseService.insertAndReturnIds(company, runbook)
                    .orElseThrow(() -> new ServerApiException("Cannot create Runbook: already exists"));

            logActivity(company, sessionUser, ActivityLog.Action.CREATED, "Created", ids.getId(), runbook);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "id", ids.getId(),
                            "permanent_id", ids.getPermanentId()));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/{id}")
    public DeferredResult<ResponseEntity<RunbookDTO>> getRunbook(@SessionAttribute("company") String company,
                                                                 @PathVariable("id") UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            Runbook runbook = runbookDatabaseService.get(company, id)
                    .orElseThrow(() -> new NotFoundException("Could not find Runbook with id=" + id));

            return ResponseEntity.ok()
                    .body(runbookDTOService.toDTO(runbook));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/permanent-id/{id}")
    public DeferredResult<ResponseEntity<RunbookDTO>> getByPermanentId(@SessionAttribute("company") String company,
                                                                       @PathVariable("id") UUID permanentId) {
        return SpringUtils.deferResponse(() -> {
            Runbook runbook = runbookDatabaseService.getLatestByPermanentId(company, permanentId.toString())
                    .orElseThrow(() -> new NotFoundException("Could not find latest runbook with permanentId=" + permanentId));

            return ResponseEntity.ok()
                    .body(runbookDTOService.toDTO(runbook));
        });
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookUpdateDTO.RunbookUpdateDTOBuilder.class)
    public static class RunbookUpdateDTO {
        @JsonProperty("id")
        String id;
        @JsonProperty("previous_id")
        String previousId;
        @JsonProperty("permanent_id")
        String permanentId;
        @JsonProperty("updated")
        boolean updated;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteRunbook(@SessionAttribute("company") String company,
                                                                        @SessionAttribute("session_user") final String sessionUser,
                                                                        @PathVariable("id") UUID uuid) throws SQLException {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(internalDeleteRunbook(company, uuid.toString(), sessionUser)));
    }

    private DeleteResponse internalDeleteRunbook(String company, String id, String sessionUser) {
        try {
            boolean delete = runbookDatabaseService.deletePreviousRevisions(company, id, true);
            if (BooleanUtils.isTrue(delete)) {
                try {
                    logActivity(company, sessionUser, ActivityLog.Action.DELETED, "Deleted", id, id);
                } catch (SQLException e) {
                    log.error("Unable to insert the log in activity log for runbook with id: " + id + ". " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return (DeleteResponse.builder().id(id).success(false).error(e.getMessage()).build());
        }
        return DeleteResponse.builder().id(id).success(true).build();
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @DeleteMapping
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteRunbooks(@SessionAttribute("company") String company,
                                                                             @SessionAttribute("session_user") final String sessionUser,
                                                                             @RequestBody List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> ids = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            List<DeleteResponse> response = ids.stream()
                    .map(id -> internalDeleteRunbook(company, id, sessionUser))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(BulkDeleteResponse.of(response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/permanent-id/{id}")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteRunbookByPermanentId(@SessionAttribute("company") String company,
                                                                                          @SessionAttribute("session_user") final String sessionUser,
                                                                                          @PathVariable("id") UUID permanentId) throws SQLException {
        return SpringUtils.deferResponse(() -> {
            // TODO LOCK
            Runbook latest = runbookDatabaseService.getLatestByPermanentId(company, permanentId.toString())
                    .orElseThrow(() -> new NotFoundException("Could not find any playbook to delete using permanentId=" + permanentId));
            Boolean delete = runbookDatabaseService.deletePreviousRevisions(company, latest.getId(), true);
            if (BooleanUtils.isTrue(delete)) {
                logActivity(company, sessionUser, ActivityLog.Action.DELETED, "Deleted", permanentId.toString(), permanentId);
            }
            return ResponseEntity.ok(Map.of(
                    "permanent_id", permanentId,
                    "deleted", delete));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public DeferredResult<ResponseEntity<RunbookUpdateDTO>> updateRunbook(@SessionAttribute("company") String company,
                                                                          @SessionAttribute("session_user") final String sessionUser,
                                                                          @PathVariable("id") UUID requestId,
                                                                          @RequestBody RunbookDTO runbookDTO) {
        return SpringUtils.deferResponse(() -> {
            // parse UI data into Backend data
            Runbook update = runbookDTOService.parseDTO(company, runbookDTO);
            boolean nodesDirty = BooleanUtils.isTrue(runbookDTO.getNodesDirty());
            if (update.getId() != null && !update.getId().equalsIgnoreCase(requestId.toString())) {
                // if id was provided, make sure it matches; otherwise use the one in the path
                throw new BadRequestException("Runbook id provided does not match request");
            }

            // TODO LOCK THIS ENDPOINT

            String latestId = runbookDatabaseService.getLatestRevision(company, requestId.toString());
            if (latestId == null) {
                throw new NotFoundException("Could not find playbook with id=" + requestId);
            }
            Runbook current = runbookDatabaseService.get(company, latestId)
                    .orElseThrow(() -> new NotFoundException(String.format("Could not update runbook id=%s: not found", latestId)));

            RunbookUpdateDTO updateResponse = doUpdateRunbook(company, current, update, nodesDirty);

            // TODO UNLOCK

            if (BooleanUtils.isTrue(updateResponse.isUpdated())) {
                logActivity(company, sessionUser, ActivityLog.Action.EDITED, "Edited", latestId, update);
            }

            return ResponseEntity.ok(updateResponse);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PutMapping("/permanent-id/{id}")
    public DeferredResult<ResponseEntity<RunbookUpdateDTO>> updateByPermanentId(@SessionAttribute("company") String company,
                                                                                @SessionAttribute("session_user") final String sessionUser,
                                                                                @PathVariable("id") UUID permanentId,
                                                                                @RequestBody RunbookDTO runbookDTO) {
        return SpringUtils.deferResponse(() -> {
            // parse UI data into Backend data
            Runbook update = runbookDTOService.parseDTO(company, runbookDTO);
            boolean nodesDirty = BooleanUtils.isTrue(runbookDTO.getNodesDirty());

            // TODO LOCK THIS ENDPOINT

            Runbook latest = runbookDatabaseService.getLatestByPermanentId(company, permanentId.toString())
                    .orElseThrow(() -> new NotFoundException("Could not find playbook with permanentId=" + permanentId));

            RunbookUpdateDTO updateResponse = doUpdateRunbook(company, latest, update, nodesDirty);

            // TODO UNLOCK

            if (BooleanUtils.isTrue(updateResponse.isUpdated())) {
                logActivity(company, sessionUser, ActivityLog.Action.EDITED, "Edited", latest.getId(), update);
            }

            return ResponseEntity.ok(updateResponse);
        });
    }

    /**
     * @param company
     * @param current latest revision to base the update from
     * @param update
     * @return
     */
    private RunbookUpdateDTO doUpdateRunbook(String company, Runbook current, Runbook update, boolean nodesDirty) throws SQLException, NotFoundException {
        String currentId = current.getId();
        String permanentId = current.getPermanentId();

        // TODO OPTIMIZE THIS AND GET RID OF DIRTY FLAG

        if (nodesDirty) {
            // create new playbook linked to current version
            update = update.toBuilder()
                    .id(null)
                    .previousId(currentId)
                    .permanentId(permanentId)
                    .build();
            String newId = runbookDatabaseService.insertAndReturnId(company, update)
                    .orElseThrow(() -> new ServerApiException("Cannot create Runbook: already exists"));

            // disable old one
            runbookDatabaseService.update(company, Runbook.builder()
                    .id(currentId)
                    .enabled(false)
                    .build());

            return RunbookUpdateDTO.builder()
                    .id(newId)
                    .previousId(currentId)
                    .permanentId(permanentId)
                    .updated(true)
                    .build();
        }

        boolean hasChanged = false;
        Runbook.RunbookBuilder builder = Runbook.builder();
        if (hasChanged(current, update, Runbook::getName)) {
            builder.name(update.getName());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getDescription)) {
            builder.description(update.getDescription());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getEnabled)) {
            builder.enabled(update.getEnabled());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getTriggerData)) {
            builder.triggerType(update.getTriggerType());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getTriggerData)) {
            builder.triggerData(update.getTriggerData());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getUiData)) {
            builder.uiData(update.getUiData());
            hasChanged = true;
        }
        if (hasChanged(current, update, Runbook::getSettings)) {
            builder.settings(update.getSettings());
            hasChanged = true;
        }
        if (!hasChanged) {
            return RunbookUpdateDTO.builder()
                    .id(currentId)
                    .updated(false)
                    .permanentId(permanentId)
                    .build();
        }

        Runbook updatedRunbook = builder
                .id(currentId)
                .permanentId(permanentId)
                .build();
        boolean updated = runbookDatabaseService.update(company, updatedRunbook);
        if (!updated) {
            throw new ServerApiException("Failed to persist updated playbook");
        }

        return RunbookUpdateDTO.builder()
                .id(currentId)
                .permanentId(permanentId)
                .updated(true)
                .build();
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<Runbook>>> listRunbooks(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            //noinspection rawtypes
            List<String> runbookIds = listRequest.getFilterValue("runbook_ids", List.class).orElse(null);
            //noinspection unchecked
            DbListResponse<Runbook> response = runbookDatabaseService.filter(
                    listRequest.getPage(), listRequest.getPageSize(), company,
                    listRequest.getFilterValue("partial", Map.class).map(m -> (String) m.get("name")).orElse(null),
                    listRequest.getFilterValue("enabled", Boolean.class).orElse(null),
                    listRequest.getFilterValue("trigger_type", String.class)
                            .map(TriggerType::fromString).orElse(null),
                    listRequest.getFilterValue("trigger_template_type", String.class).orElse(null),
                    runbookIds,
                    listRequest.getFilterValue("only_latest_revision", Boolean.class).orElse(CollectionUtils.isEmpty(runbookIds)),
                    null,
                    listRequest.getFilterValue("permanent_ids", List.class).orElse(null));
            return ResponseEntity.ok().body(PaginatedResponse.of(listRequest.getPage(), listRequest.getPageSize(),
                    response.getTotalCount(),
                    response.getRecords().stream()
                            .map(rb -> rb.toBuilder()
                                    .nodes(null)
                                    .build())
                            .collect(Collectors.toList())));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/node_templates/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookNodeTemplate>>> listNoteTemplates(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            //noinspection unchecked
            DbListResponse<RunbookNodeTemplate> response = nodeTemplateDatabaseService.filter(
                    listRequest.getPage(), listRequest.getPageSize(), company,
                    listRequest.getFilterValue("types", List.class).orElse(null),
                    listRequest.getFilterValue("partial", Map.class).map(m -> (String) m.get("name")).orElse(null),
                    listRequest.getFilterValue("ids", List.class).orElse(null),
                    listRequest.getFilterValue("categories", List.class).orElse(null),
                    listRequest.getFilterValue("hidden", Boolean.class).orElse(null));
            return ResponseEntity.ok().body(PaginatedResponse.of(listRequest.getPage(), listRequest.getPageSize(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/node_templates/categories/list")
    public DeferredResult<ResponseEntity<DbListResponse<String>>> listNoteTemplateCategories(@SessionAttribute("company") String company) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<String> response = nodeTemplateDatabaseService.listCategories(company);
            return ResponseEntity.ok().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/runs/{id}")
    public DeferredResult<ResponseEntity<RunbookRun>> getRun(@SessionAttribute("company") String company,
                                                             @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                runbookRunDatabaseService.get(company, id.toString())
                        .orElseThrow(() -> new NotFoundException("Could not find run with id=" + id))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @DeleteMapping("/runs")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteRuns(@SessionAttribute("company") String company,
                                                                         @RequestBody List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> runIds = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            try {
                runbookRunDatabaseService.bulkDelete(company, runIds);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(runIds, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(runIds, false, e.getMessage()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/runs/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookRun>>> listRuns(@SessionAttribute("company") String company,
                                                                                  @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {

            Map<String, Integer> updateRange = listRequest.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;

            var response = runbookRunDatabaseService.filter(
                    listRequest.getPage(),
                    listRequest.getPageSize(),
                    company,
                    listRequest.getFilterValue("runbook_id", String.class).orElse(null),
                    listRequest.getFilterValue("state", String.class).map(RunbookRunState::fromString).orElse(null),
                    null,
                    updatedAtStart,
                    updatedAtEnd);
            return ResponseEntity.ok(PaginatedResponse.of(listRequest.getPage(), listRequest.getPageSize(), response.getTotalCount(), response.getRecords()));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/reports/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookReport>>> listReports(@SessionAttribute("company") String company,
                                                                                        @RequestBody DefaultListRequest listRequest) {
        //noinspection unchecked
        var response = runbookReportDatabaseService.filter(listRequest.getPage(), listRequest.getPageSize(), company,
                listRequest.getFilterValue("runbook_ids", List.class).orElse(null),
                listRequest.getFilterValue("run_id", String.class).orElse(null),
                listRequest.getFilterValue("source", String.class).orElse(null),
                listRequest.getFilterValue("partial", Map.class).map(m -> (String) m.get("title")).orElse(null));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(listRequest.getPage(), listRequest.getPageSize(), response.getTotalCount(), response.getRecords())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @DeleteMapping("/reports")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteBulkReports(@SessionAttribute("company") String company,
                                                                                 @SessionAttribute("session_user") final String sessionUser,
                                                                                 @RequestBody List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> reportIds = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            try {
                runbookReportDatabaseService.deleteBulkReports(company, reportIds);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(reportIds, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(reportIds, false, e.getMessage()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping("/reports/{reportId}")
    public DeferredResult<ResponseEntity<RunbookReportDTO>> getReport(@SessionAttribute("company") String company,
                                                                      @PathVariable("reportId") UUID reportId) {
        return SpringUtils.deferResponse(() -> runbookReportService.fetchReportData(company, reportId.toString())
                .map(data -> RunbookReportDTO.builder()
                        .id(data.getReport().getId())
                        .runbookId(data.getReport().getRunbookId())
                        .runId(data.getReport().getRunId())
                        .title(data.getReport().getTitle())
                        .columns(data.getColumns())
                        .sectionTitles(data.getSectionTitles())
                        .records(data.getRecords())
                        .createdAt(data.getReport().getCreatedAt())
                        .build())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Could not find report with id=" + reportId)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/{runbookId}/runs/{runId}/nodes/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookRunningNodeDTO>>> listRunNodes(@SessionAttribute("company") String company,
                                                                                                 @PathVariable("runbookId") UUID runbookId,
                                                                                                 @PathVariable("runId") UUID runId,
                                                                                                 @RequestBody DefaultListRequest defaultListRequest) {
        return SpringUtils.deferResponse(() -> {
            // TODO use client
            List<String> nodeIds = defaultListRequest.<String>getFilterValueAsList("node_ids").orElse(null);
            List<RunbookRunningNodeState> states = defaultListRequest.getFilterValueAsList("states")
                    .map(l -> l.stream()
                            .map(s -> RunbookRunningNodeState.fromString((String) s))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .orElse(null);
            log.debug("nodes/list nodesIds={}, states={}, request={}", nodeIds, states, defaultListRequest);
            DbListResponse<RunbookRunningNode> response = runbookRunningNodeDatabaseService.filter(defaultListRequest.getPage(), defaultListRequest.getPageSize(), company,
                    runId.toString(), nodeIds, null, states, null);
            List<RunbookRunningNodeDTO> records = ListUtils.emptyIfNull(response.getRecords()).stream()
                    .map(RunbookRunningNodeDTO::convert)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.of(defaultListRequest.getPage(), defaultListRequest.getPageSize(), response.getTotalCount(), records));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping("/nodes/evaluate")
    public DeferredResult<ResponseEntity<EvaluateNodeResponse>> evaluateNode(@SessionAttribute("company") String company,
                                                                             @RequestBody EvaluateNodeRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(runbookClient.evaluateNode(company, request)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{permanent-id}/trigger/manual")
    public DeferredResult<ResponseEntity<Map<String, ?>>> triggerPlaybookManually(@SessionAttribute("company") String company,
                                                                                  @PathVariable("permanent-id") UUID permanentId,
                                                                                  @RequestBody Map<String, Object> data) {
        return SpringUtils.deferResponse(() -> {
            Runbook runbook = runbookDatabaseService.getLatestByPermanentId(company, permanentId.toString())
                    .orElseThrow(() -> new NotFoundException("Could not find latest playbook with permanentId=" + permanentId));

            if (BooleanUtils.isNotTrue(runbook.getEnabled())) {
                throw new BadRequestException(String.format("Cannot trigger disabled playbook (id=%s)", runbook.getId()));
            }

            List<RunbookVariable> inputVariables = MapUtils.emptyIfNull(data).entrySet().stream().map(entry ->
                    RunbookVariableUtils.fromJsonObject(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            var response = runbookClient.createRun(company, runbook.getId(), TriggerType.MANUAL.toString(), inputVariables);
            return ResponseEntity.ok().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/run/{run-id}/retry")
    public DeferredResult<ResponseEntity<Map<String, ?>>> createPlaybookRunManually(@SessionAttribute("company") String company,
                                                                                    @PathVariable("run-id") UUID runId) {
        return SpringUtils.deferResponse(() -> {
            Optional<RunbookRun> runbookRun = runbookRunDatabaseService.get(company, runId.toString());
            if (runbookRun.isEmpty()) {
                throw new NotFoundException("Could not find playbook run with runId=" + runId);
            } else {
                String permanentRunbookId = runbookRun.get().getPermanentId();
                Runbook runbook = runbookDatabaseService.getLatestByPermanentId(company, permanentRunbookId)
                        .orElseThrow(() -> new NotFoundException("Could not find latest playbook with permanentId=" + permanentRunbookId));
                if (BooleanUtils.isNotTrue(runbook.getEnabled())) {
                    throw new BadRequestException(String.format("Cannot trigger disabled playbook (id=%s)", runbook.getId()));
                }
                List<RunbookVariable> inputVariables = new ArrayList<>(runbookRun.get().getArgs().values());
                var response = runbookClient.createRun(company, runbook.getId(), TriggerType.MANUAL.toString(), inputVariables);
                return ResponseEntity.ok().body(response);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN')")
    @PostMapping("/templates/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookTemplate>>> listPlaybookTemplates(@SessionAttribute("company") String company,
                                                                                                    @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<String> categories = (List) request.getFilterValueAsList("categories").orElse(Collections.emptyList());
            Map<Object, Object> partialFilters = request.getFilterValueAsMap("partial").orElse(Map.of());
            String partialName = (String) partialFilters.get("name");
            String partialDescription = (String) partialFilters.get("description");
            var filter = RunbookTemplateFilter.builder()
                    .description(partialDescription)
                    .partialName(partialName)
                    .categories(categories)
                    .build();
            DbListResponse<RunbookTemplate> list = runbookTemplateDatabaseService.filter(request.getPage(), request.getPageSize(), company, filter);
            return ResponseEntity.ok().body(PaginatedResponse.of(request.getPage(), request.getPageSize(), list.getTotalCount(), list.getRecords()));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','SUPER_ADMIN')")
    @PostMapping("/templates/categories/list")
    public DeferredResult<ResponseEntity<PaginatedResponse<RunbookTemplateCategory>>> listPlaybookTemplateCategories(@SessionAttribute("company") String company,
                                                                                                                     @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            Map<Object, Object> partialFilters = request.getFilterValueAsMap("partial").orElse(Map.of());
            String partialName = (String) partialFilters.get("name");
            String partialDescription = (String) partialFilters.get("description");
            var filter = RunbookTemplateCategoryFilter.builder()
                    .description(partialDescription)
                    .partialName(partialName)
                    .build();
            DbListResponse<RunbookTemplateCategory> list = runbookTemplateCategoryDatabaseService.filter(request.getPage(), request.getPageSize(), company, filter);
            return ResponseEntity.ok().body(PaginatedResponse.of(request.getPage(), request.getPageSize(), list.getTotalCount(), list.getRecords()));
        });
    }

}