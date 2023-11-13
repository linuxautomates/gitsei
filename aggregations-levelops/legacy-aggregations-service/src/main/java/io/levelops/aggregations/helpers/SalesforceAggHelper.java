package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCaseHistory;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.SalesforceCaseService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.salesforce.models.SalesforceCase;
import io.levelops.integrations.salesforce.models.SalesforceCaseHistory;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class SalesforceAggHelper {
    private static final String DEFAULT_CASES_DATATYPE = "cases";
    private static final String DEFAULT_CASE_HISTORIES_DATATYPE = "case_histories";

    private final JobDtoParser jobDtoParser;
    private final IntegrationTrackingService trackingService;
    private final SalesforceCaseService salesforceCaseService;

    @Autowired
    public SalesforceAggHelper(JobDtoParser jobDtoParser,
                               SalesforceCaseService salesforceCaseService,
                               IntegrationTrackingService trackingService) {
        this.jobDtoParser = jobDtoParser;
        this.trackingService = trackingService;
        this.salesforceCaseService = salesforceCaseService;
    }

    public boolean setupSalesforceCases(String customer, Date currentTime, String integrationId,
                                        MultipleTriggerResults results) {
        List<DbSalesforceCase> dbSalesforceCases = new ArrayList<>();
        log.info("currentTime {}", currentTime);
        Boolean result = jobDtoParser.applyToResults(customer, DEFAULT_CASES_DATATYPE,
                SalesforceCase.class, results.getTriggerResults().get(0),
                salesforceCase -> dbSalesforceCases.add(DbSalesforceCase.fromSalesforceCase(salesforceCase, integrationId, currentTime)),
                List.of(() -> {
                    if (dbSalesforceCases.size() > 0) {
                        dbSalesforceCases.forEach(sfCase -> {
                            Optional<DbSalesforceCase> oldCase = Optional.empty();
                            try {
                                oldCase = salesforceCaseService.get(customer, sfCase.getCaseId(),
                                        integrationId, sfCase.getIngestedAt());
                                if (oldCase.isEmpty()
                                        || oldCase.get().getLastModifiedAt().before(sfCase.getLastModifiedAt()))
                                    salesforceCaseService.insert(customer, sfCase);
                            } catch (Exception e) {
                                log.warn("Error updating case. id: {}. empty? {}",
                                        sfCase.getCaseId(),
                                        oldCase.isEmpty(),
                                        e);
                            }
                        });
                        dbSalesforceCases.clear();
                    }
                    return true;
                }));
        if (result)
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE))
                            .build());
        return result;
    }

    public boolean setupSalesforceCaseHistories(String customer,
                                                Date currentTime,
                                                String integrationId,
                                                MultipleTriggerResults results) {
        List<SalesforceCaseHistory> salesforceCaseHistories = new ArrayList<>();
        log.info("currentTime {}", currentTime);
        IntegrationTracker tracking = trackingService.get(customer, integrationId).orElse(null);
        if (tracking == null) {
            log.warn("Integration tracking data is missing, which means there is no salesforce data." +
                    "Cannot update history then.");
            return false;
        }
        return jobDtoParser.applyToResults(customer, DEFAULT_CASE_HISTORIES_DATATYPE,
                SalesforceCaseHistory.class, results.getTriggerResults().get(0),
                caseHistory -> salesforceCaseHistories.add(caseHistory),
                List.of(() -> {
                    if (salesforceCaseHistories.size() > 0) {
                        List<DbSalesforceCaseHistory> dbSalesforceCaseHistories = DbSalesforceCaseHistory
                                .fromSalesforceCaseHistories(salesforceCaseHistories, integrationId);
                        dbSalesforceCaseHistories.forEach(caseHistory ->
                                salesforceCaseService.insertCaseHistory(customer,
                                        caseHistory,
                                        tracking.getLatestIngestedAt()));
                        dbSalesforceCaseHistories.clear();
                    }
                    return true;
                }));
    }
}
