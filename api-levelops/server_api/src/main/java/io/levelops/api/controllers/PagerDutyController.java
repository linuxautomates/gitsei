package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
@RestController
@RequestMapping("/v1/pagerduty")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
@SuppressWarnings("unused")
public class PagerDutyController {

    private final PagerDutyIncidentsDatabaseService incidentsService;
    private final PagerDutyAlertsDatabaseService alertsService;

    @Autowired
    public PagerDutyController(final PagerDutyIncidentsDatabaseService incidentsService,
                                final PagerDutyAlertsDatabaseService alertsService) {
        this.incidentsService = incidentsService;
        this.alertsService = alertsService;
    }
    
    @PostMapping(path = "/incidents/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<DbPagerDutyIncident>>> listIncidents(@SessionAttribute("company") String company,
                                                                            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var results = incidentsService.list(company, QueryFilter.fromRequestFilters(request.getFilter()),
                    request.getPage(), request.getPageSize(), orgProductIdsSet);
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), results));
        });
    }
    
    @PostMapping(path = "/alerts/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<DbPagerDutyAlert>>> listAlerts(@SessionAttribute("company") String company,
                                                                            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var results = alertsService.list(company, QueryFilter.fromRequestFilters(request.getFilter()), request.getPage(), request.getPageSize(), orgProductIdsSet);
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), results));
        });
    }
}
