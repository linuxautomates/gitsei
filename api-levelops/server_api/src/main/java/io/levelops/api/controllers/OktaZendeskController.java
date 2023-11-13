//package io.levelops.api.controllers;
//
//import io.levelops.commons.databases.models.database.Integration;
//import io.levelops.commons.databases.models.database.IntegrationTracker;
//import io.levelops.commons.databases.models.filters.OktaGroupsFilter;
//import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
//import io.levelops.commons.databases.models.response.DbAggregationResult;
//import io.levelops.commons.databases.services.IntegrationService;
//import io.levelops.commons.databases.services.IntegrationTrackingService;
//import io.levelops.commons.databases.services.ZendeskOktaService;
//import io.levelops.commons.dates.DateUtils;
//import io.levelops.commons.models.DbListResponse;
//import io.levelops.commons.models.DefaultListRequest;
//import io.levelops.commons.models.PaginatedResponse;
//import io.levelops.ingestion.models.IntegrationType;
//import io.levelops.web.util.SpringUtils;
//import lombok.extern.log4j.Log4j2;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.context.request.async.DeferredResult;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.sql.SQLException;
//import java.util.*;
//import java.util.stream.Collectors;

// import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
//
//@RestController
//@Log4j2
//@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
//@RequestMapping("/v1/okta_zendesk")
//public class OktaZendeskController {
//
//    private final ZendeskOktaService zendeskOktaService;
//    private final IntegrationTrackingService integrationTrackingService;
//    private final IntegrationService integService;
//
//    public OktaZendeskController(ZendeskOktaService zendeskOktaService,
//                                 IntegrationTrackingService integrationTrackingService,
//                                 IntegrationService integService) {
//        this.zendeskOktaService = zendeskOktaService;
//        this.integrationTrackingService = integrationTrackingService;
//        this.integService = integService;
//    }
//
//    private Long getIngestedAt(String company, DefaultListRequest filter) throws SQLException {
//        Integration integ = integService.listByFilter(company,
//                List.of(IntegrationType.ZENDESK.toString()),
//                null,
//                getListOrDefault(filter.getFilter(), "integration_ids").stream()
//                        .map(NumberUtils::toInt).collect(Collectors.toList()), List.of(),
//                0, 1).getRecords().stream().findFirst().orElse(null);
//        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
//        if (integ != null)
//            ingestedAt = integrationTrackingService.get(company, integ.getId())
//                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
//                    .getLatestIngestedAt();
//        return ingestedAt;
//    }
//
//    @RequestMapping(method = RequestMethod.POST, value = "/agg", produces = "application/json")
//    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> zendeskTicketsByOktaGroup(
//            @SessionAttribute(name = "company") String company,
//            @RequestBody DefaultListRequest filter) {
//        return SpringUtils.deferResponse(() -> {
//            List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
//            if (CollectionUtils.isEmpty(integrationIds))
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
//            Map<String, String> committedRange = filter.getFilterValue("zendesk_created_at", Map.class)
//                    .orElse(Map.of());
//            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
//            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;
//            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
//                    .brands(getListOrDefault(filter.getFilter(), "zendesk_brands"))
//                    .extraCriteria(getListOrDefault(filter.getFilter(), "zendesk_hygiene_types")
//                            .stream()
//                            .map(String::valueOf)
//                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
//                            .filter(Objects::nonNull)
//                            .collect(Collectors.toList()))
//                    .types(getListOrDefault(filter.getFilter(), "zendesk_types"))
//                    .ingestedAt(getIngestedAt(company, filter))
//                    .integrationIds(integrationIds)
//                    .priorities(getListOrDefault(filter.getFilter(), "zendesk_priorities"))
//                    .statuses(getListOrDefault(filter.getFilter(), "zendesk_statuses"))
//                    .organizations(getListOrDefault(filter.getFilter(), "zendesk_organizations"))
//                    .requesterEmails(getListOrDefault(filter.getFilter(), "zendesk_requesters"))
//                    .submitterEmails(getListOrDefault(filter.getFilter(), "zendesk_submitters"))
//                    .assigneeEmails(getListOrDefault(filter.getFilter(), "zendesk_assignees"))
//                    .ticketCreatedStart(zdStart)
//                    .ticketCreatedEnd(zdEnd)
//                    .build();
//            final OktaGroupsFilter oktaGroupsFilter = OktaGroupsFilter.builder()
//                    .integrationIds(integrationIds)
//                    .names(getListOrDefault(filter.getFilter(), "okta_group_names"))
//                    .types(getListOrDefault(filter.getFilter(), "okta_group_types"))
//                    .build();
//            final DbListResponse<DbAggregationResult> aggResult = zendeskOktaService.groupZendeskTicketsWithOkta(
//                    company,
//                    oktaGroupsFilter,
//                    zdFilter);
//            return ResponseEntity.ok(
//                    PaginatedResponse.of(
//                            filter.getPage(),
//                            filter.getPageSize(),
//                            aggResult));
//        });
//    }
//}
