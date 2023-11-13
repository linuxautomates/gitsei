package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.models.BulkUpdateResponse;
import io.levelops.commons.models.CICDInstanceIntegAssignment;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@RequestMapping("/v1/cicd/instances")
@Log4j2
public class CICDInstancesController {

    private final CiCdInstancesDatabaseService instancesService;

    @Autowired
    public CICDInstancesController(final CiCdInstancesDatabaseService instancesService) {
        this.instancesService = instancesService;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<Map<String, Object>>> insert(
            @SessionAttribute(name = "company") String company,
            @RequestBody final CICDInstance originalInstance) {
        return SpringUtils.deferResponse(() -> {
            CICDInstance instance = originalInstance;
            if (instance.getId() == null) {
                instance = instance.toBuilder()
                        .id(UUID.randomUUID())
                        .build();
            }
            String id = instancesService.insert(company, instance);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<CICDInstance>> getInstanceById(@PathVariable("id") String id,
                                                                        @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(instancesService.get(company, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found."))));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PostMapping(path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDInstance>>> listInstances(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest filter) {
        log.debug("company = {} and filter = {}", company, filter);

        Map<String, String> createdAtRange = filter.getFilterValue("created_at", Map.class).orElse(Map.of());
        final Long createdAtStart = createdAtRange.get("$gt") != null ? Long.valueOf(createdAtRange.get("$gt")) : null;
        final Long createdAtEnd = createdAtRange.get("$lt") != null ? Long.valueOf(createdAtRange.get("$lt")) : null;

        Map<String, String> updatedAtRange = filter.getFilterValue("updated_at", Map.class).orElse(Map.of());
        final Long updatedAtStart = updatedAtRange.get("$gt") != null ? Long.valueOf(updatedAtRange.get("$gt")) : null;
        final Long updatedAtEnd = updatedAtRange.get("$lt") != null ? Long.valueOf(updatedAtRange.get("$lt")) : null;

        Map<String, Map<String, String>> partialMatchMap
                = MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        Map<String, Object> excludeFields
                = (Map<String, Object>) filter.getFilter().getOrDefault("exclude", Map.of());
        validatePartialMatchFilter(company, partialMatchMap,
                CiCdInstancesDatabaseService.PARTIAL_MATCH_COLUMNS,
                CiCdInstancesDatabaseService.PARTIAL_MATCH_ARRAY_COLUMNS);
        return SpringUtils.deferResponse(
                () -> ResponseEntity.ok(PaginatedResponse.of(
                        filter.getPage(),
                        filter.getPageSize(),
                        instancesService.list(
                                company,
                                CICDInstanceFilter.builder()
                                        .ids(getListOrDefault(filter.getFilter(), "ids"))
                                        .names(getListOrDefault(filter.getFilter(), "names"))
                                        .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(filter))
                                        .excludeNames(getListOrDefault(excludeFields, "names"))
                                        .excludeIds(getListOrDefault(excludeFields, "ids"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .instanceCreatedRange(ImmutablePair.of(createdAtStart, createdAtEnd))
                                        .instanceUpdatedRange(ImmutablePair.of(updatedAtStart, updatedAtEnd))
                                        .partialMatch(partialMatchMap)
                                        .missingFields(MapUtils.emptyIfNull(
                                                (Map<String, Boolean>) filter.getFilter().get("missing_fields")))
                                        .build(),
                                SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
                                filter.getPage(), filter.getPageSize())
                ))
        );
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}/config", produces = "application/json")
    public DeferredResult<ResponseEntity<CiCdInstanceConfig>> getInstanceConfig(@SessionAttribute(name = "company") final String company,
                                                                                @PathVariable("id") final String id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(instancesService.getConfig(company, id))
        );
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/{id}/config", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> updateInstanceConfig(@SessionAttribute(name = "company") final String company,
                                                                   @PathVariable("id") final String id,
                                                                   @RequestBody final CICDInstance instance) {
        return SpringUtils.deferResponse(() -> {
            instancesService.updateConfig(company, instance, id);
            return ResponseEntity.ok().body(Id.from(id));
        });
    }

    private void validatePartialMatchFilter(String company,
                                            Map<String, Map<String, String>> partialMatchMap,
                                            Set<String> partialMatchColumns, Set<String> partialMatchArrayColumns) {
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> (!partialMatchColumns.contains(key)) && (!partialMatchArrayColumns.contains(key)))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for cicd instance partial match based filter");
        }
    }

    @PostMapping(path = "/assign", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<BulkUpdateResponse>> assignIntegrationId(
            @SessionAttribute("company") String company,
            @RequestBody CICDInstanceIntegAssignment request) {
        log.debug("company = {} and request = {}", company, request);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                instancesService.assignIntegrationId(company, request)
        ));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        log.debug("company = {} and filter = {}", company, filter);
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();

            Map<String, String> createdAtRange = filter.getFilterValue("created_at", Map.class).orElse(Map.of());
            final Long createdAtStart = createdAtRange.get("$gt") != null ? Long.valueOf(createdAtRange.get("$gt")) : null;
            final Long createdAtEnd = createdAtRange.get("$lt") != null ? Long.valueOf(createdAtRange.get("$lt")) : null;

            Map<String, String> updatedAtRange = filter.getFilterValue("updated_at", Map.class).orElse(Map.of());
            final Long updatedAtStart = updatedAtRange.get("$gt") != null ? Long.valueOf(updatedAtRange.get("$gt")) : null;
            final Long updatedAtEnd = updatedAtRange.get("$lt") != null ? Long.valueOf(updatedAtRange.get("$lt")) : null;

            Map<String, Object> excludeFields = (Map<String, Object>) filter.getFilter()
                    .getOrDefault("exclude", Map.of());
            for (String field : filter.getFields()) {
                response.add(Map.of(field, instancesService.groupByAndCalculate(
                        company,
                        CICDInstanceFilter.builder()
                                .across(CICDInstanceFilter.DISTINCT.fromString(field))
                                .acrossLimit(filter.getAcrossLimit())
                                .ids(getListOrDefault(filter.getFilter(), "ids"))
                                .names(getListOrDefault(filter.getFilter(), "names"))
                                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                                .types(CICD_TYPE.parseFromFilter(filter))
                                .excludeNames(getListOrDefault(excludeFields, "names"))
                                .excludeIds(getListOrDefault(excludeFields, "ids"))
                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                .instanceCreatedRange(ImmutablePair.of(createdAtStart, createdAtEnd))
                                .instanceUpdatedRange(ImmutablePair.of(updatedAtStart, updatedAtEnd))
                                .missingFields(MapUtils.emptyIfNull(
                                        (Map<String, Boolean>) filter.getFilter().get("missing_fields")))
                                .build()
                ).getRecords()));
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

}