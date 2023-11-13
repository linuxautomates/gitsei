package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
@RestController
@RequestMapping("/v1/services")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
public class ServicesController {

    private final ServicesDatabaseService services;
    private final PagerDutyIncidentsDatabaseService incidentsService;
    private final PagerDutyAlertsDatabaseService alertsService;

    @Autowired
    public ServicesController(final ServicesDatabaseService services,
                            final PagerDutyIncidentsDatabaseService incidentsService,
                            final PagerDutyAlertsDatabaseService alertsService) {
        this.services = services;
        this.incidentsService = incidentsService;
        this.alertsService = alertsService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping(path = "/{id}")
    public DeferredResult<ResponseEntity<Map<String, String>>> getService(
        @SessionAttribute("company") final String company,
        @PathVariable("id") final UUID id) {

        return SpringUtils.deferResponse(() -> {
            var service = services.get(company, id.toString()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the service with id " + id));
            return ResponseEntity.ok(convertDBServiceToResponse(service));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, String>>>> listServices(@SessionAttribute("company") final String company, @RequestBody final DefaultListRequest body){
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(body.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var results = services.list(company, QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            List<Map<String, String>> data = results.getRecords().stream()
                .map(this::convertDBServiceToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(), results.getTotalCount(), data));
        });
    }

    protected Map<String, String> convertDBServiceToResponse(Service item) {
        return Map.of(
                    "id", item.getId().toString(),
                    "name", item.getName(),
                    "type", item.getType().toString().toLowerCase()
                );
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(path = "/aggregate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> aggregate(
        @SessionAttribute("company") final String company, 
        @RequestBody final DefaultListRequest body){

        return SpringUtils.deferResponse(() -> {
            if (Strings.isBlank(body.getAcross()) && CollectionUtils.isEmpty(body.getStacks())) {
                log.error("missing across: {}", body);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "either 'across' or 'stacks' must be specified");
            }
            var across = body.getAcross();
            DbListResponse<Map<String, Object>> records;
            var orgProductIdsList = getListOrDefault(body.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            if (Strings.isNotBlank(across) && CollectionUtils.isEmpty(body.getStacks())) {
                records = getAggregatedRecords(company, across, body, orgProductIdsSet);
            }
            else {
                // get all the records and combine them
                final Map<String, Map<String, Object>> resultsMap = new HashMap<>();
                int maxCount = 0;
                // var tmp = new ArrayList<Map<String, Object>>();
                for (String stack:body.getStacks()) {
                    // get the aggregations for the stack in turn 
                    var results = getAggregatedRecords(company, stack, body, orgProductIdsSet);
                    // if the total results object is empty then initialize it
                    if (MapUtils.isEmpty(resultsMap)) {
                        resultsMap.putAll(results.getRecords().stream().collect(Collectors.toMap((i) -> i.get("id").toString(), Function.identity())));
                        maxCount = results.getTotalCount();
                        continue;
                    }
                    // keep track of the biggest page
                    if (results.getTotalCount() > maxCount) {
                        maxCount = results.getTotalCount();
                    }
                    // merge the new results with the ones from the previous stacks
                    results.getRecords().forEach(record -> {
                        var element = resultsMap.get(record.get("id"));
                        // add a new result if not present
                        if (element == null) {
                            resultsMap.put(record.get("id").toString(), record);
                            return;
                        }
                        // add all the aggregations from the current results into the existing ones
                        ((List<Map<String, Object>>) element.get("aggregations")).addAll((List<Map<String, Object>>) record.get("aggregations"));
                    });
                };
                records = DbListResponse.<Map<String,Object>>builder().records(resultsMap.entrySet().stream().map(item -> item.getValue()).collect(Collectors.toList())).totalCount(maxCount).build();
            }
            return ResponseEntity.ok(PaginatedResponse.of(body.getPage(), body.getPageSize(), records));
        });
    }

    private DbListResponse<Map<String, Object>> getAggregatedRecords(final String company, final String across, final DefaultListRequest body, Set<UUID> orgProductIdsSet) throws SQLException {
        switch(across) {
            case "incident_priority":
                return incidentsService.aggregate(company, "priority", "count", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            case "incident_urgency":
                return incidentsService.aggregate(company, "urgency", "count", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            case "incident_priority_trend":
                return incidentsService.aggregate(company, "priority", "trend", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            case "incident_urgency_trend":
                return incidentsService.aggregate(company, "urgency", "trend", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            case "alert_severity":
                return alertsService.aggregate(company, "severity", "count", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            case "alert_severity_trend":
                return alertsService.aggregate(company, "severity", "trend", QueryFilter.fromRequestFilters(body.getFilter()), body.getPage(), body.getPageSize(), orgProductIdsSet);
            default:
                log.error("unsuported across: {}", across);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported across: " + across);
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(path = "/aggregate/values", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> aggregateValues(
        @SessionAttribute("company") final String company,
        @RequestBody final DefaultListRequest body){
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(body.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            List<Map<String, Object>> records = new ArrayList<>();
            if (body.getFields() == null) {
                log.error("missing fields: {}", body);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the 'fields' must be specified");
            }
            var fields = body.getFields();
            var filters = QueryFilter.fromRequestFilters(body.getFilter());
            if (fields.size() == 0){
                fields.add("pd_service");
                fields.add("incident_urgency");
                fields.add("incident_priority");
                fields.add("alert_severity");
            }
            fields.forEach(field -> {
                try {
                    records.add(Map.of(field, incidentsService.getValues(company, field, filters, body.getPage(), body.getPageSize(), orgProductIdsSet).getRecords()));
                } catch (SQLException e) {
                    log.error("Error while retrieving values...{0}" + e.getMessage(), e);
                }
            });
            return ResponseEntity.ok(Map.of("records", records, "_metadata", Map.of("page", body.getPage(), "page_size", body.getPage(), "has_next", false, "total_count", records.size())));
        });
    }
}