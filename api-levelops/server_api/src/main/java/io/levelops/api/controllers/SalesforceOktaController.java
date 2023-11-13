//package io.levelops.api.controllers;
//
//import com.google.common.base.MoreObjects;
//import io.levelops.commons.databases.models.database.Integration;
//import io.levelops.commons.databases.models.database.IntegrationTracker;
//import io.levelops.commons.databases.models.filters.OktaGroupsFilter;
//import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
//import io.levelops.commons.databases.models.response.DbAggregationResult;
//import io.levelops.commons.databases.services.IntegrationService;
//import io.levelops.commons.databases.services.IntegrationTrackingService;
//import io.levelops.commons.databases.services.SalesforceOktaService;
//import io.levelops.commons.dates.DateUtils;
//import io.levelops.commons.models.DefaultListRequest;
//import io.levelops.commons.models.PaginatedResponse;
//import io.levelops.ingestion.models.IntegrationType;
//import io.levelops.web.util.SpringUtils;
//import lombok.extern.log4j.Log4j2;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.math.NumberUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.bind.annotation.SessionAttribute;
//import org.springframework.web.context.request.async.DeferredResult;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.sql.SQLException;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;
//@RestController
//@Log4j2
//@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
//@RequestMapping("/v1/okta_salesforce")
//public class SalesforceOktaController {
//    private final IntegrationService integService;
//    private final SalesforceOktaService salesforceOktaService;
//    private final IntegrationTrackingService integrationTrackingService;
//
//    @Autowired
//    public SalesforceOktaController(IntegrationService integService, SalesforceOktaService salesforceOktaService,
//                                    IntegrationTrackingService integrationTrackingService) {
//        this.integService = integService;
//        this.salesforceOktaService = salesforceOktaService;
//        this.integrationTrackingService = integrationTrackingService;
//    }
//
//    @RequestMapping(method = RequestMethod.POST, value = "/agg", produces = "application/json")
//    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> groupCasesList(
//            @SessionAttribute(name = "company") String company,
//            @RequestBody DefaultListRequest filter) {
//        return SpringUtils.deferResponse(() -> {
//            List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
//            if (CollectionUtils.isEmpty(integrationIds))
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
//            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilter.builder()
//                    .ingestedAt(getIngestedAt(company, IntegrationType.SALESFORCE, filter))
//                    .extraCriteria(MoreObjects.firstNonNull(
//                            getListOrDefault(filter.getFilter(), "salesforce_hygiene_types"),
//                            List.of())
//                            .stream()
//                            .map(String::valueOf)
//                            .map(SalesforceCaseFilter.EXTRA_CRITERIA::fromString)
//                            .filter(Objects::nonNull)
//                            .collect(Collectors.toList()))
//                    .caseIds(getListOrDefault(filter.getFilter(), "salesforce_case_ids"))
//                    .priorities(getListOrDefault(filter.getFilter(), "salesforce_priorities"))
//                    .statuses(getListOrDefault(filter.getFilter(), "salesforce_statuses"))
//                    .contacts(getListOrDefault(filter.getFilter(), "salesforce_contacts"))
//                    .types(getListOrDefault(filter.getFilter(), "salesforce_types"))
//                    .integrationIds(integrationIds)
//                    .accounts(getListOrDefault(filter.getFilter(), "salesforce_accounts"))
//                    .build();
//            OktaGroupsFilter oktaGroupsFilter = OktaGroupsFilter.builder()
//                    .integrationIds(integrationIds)
//                    .names(getListOrDefault(filter.getFilter(), "okta_group_names"))
//                    .types(getListOrDefault(filter.getFilter(), "okta_group_types"))
//                    .build();
//            return ResponseEntity.ok(
//                    PaginatedResponse.of(
//                            filter.getPage(),
//                            filter.getPageSize(),
//                            salesforceOktaService.groupCaseWithOkta(company,
//                                    salesforceCaseFilter,
//                                    oktaGroupsFilter)));
//        });
//    }
//
//    private Long getIngestedAt(String company, IntegrationType type, DefaultListRequest filter)
//            throws SQLException {
//        Integration integ = integService.listByFilter(company,
//                List.of(type.toString()),
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
//}
