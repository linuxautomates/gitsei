package io.levelops.aggregations.helpers;

import io.levelops.aggregations.functions.SnykAggQueries;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssue;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.snyk.SnykDatabaseService;
import io.levelops.commons.databases.services.temporary.SnykVulnQueryTable;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykVulnerabilityAggWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class SnykAggHelper {
    private static final String SNYK_TRIGGER_DATATYPE = "issues";

    private final JobDtoParser jobDtoParser;
    private final SnykDatabaseService snykDatabaseService;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public SnykAggHelper(JobDtoParser jobDtoParser,
                         SnykDatabaseService snykDatabaseService,
                         IntegrationTrackingService trackingService) {
        this.jobDtoParser = jobDtoParser;
        this.snykDatabaseService = snykDatabaseService;
        this.trackingService = trackingService;
    }

    public boolean setupSnykIssues(String customer,
                                   SnykVulnQueryTable queryTable,
                                   MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer, SNYK_TRIGGER_DATATYPE,
                SnykIssues.class, results.getTriggerResults().get(0),
                vuln -> {
                    try {
                        List<SnykVulnerabilityAggWrapper> vulnerability = SnykVulnerabilityAggWrapper.fromSnykIssues(vuln);
                        queryTable.insertRows(vulnerability);
                    } catch (Exception e) {
                        log.warn("Failed to insert vulnerability issue for project "
                                + vuln.getProjectName() + " for company " + customer, e);
                    }
                },
                List.of());
    }

    public Set<String> getVulnSet(String customer, MultipleTriggerResults results) {
        Set<String> oldVulnSet = new HashSet<>();
        boolean success = jobDtoParser.applyToResults(customer,
                SNYK_TRIGGER_DATATYPE, SnykIssues.class,
                results.getTriggerResults().get(0), //we only expect 1 trigger results at this time
                issues -> issues.getIssues().getVulnerabilities()
                        .forEach(oldVuln -> oldVulnSet.add(SnykAggQueries.generateUniqueString(oldVuln))),
                Collections.emptyList());

        if (!success) {
            log.warn("failed to get the vulns from the old list.");
            return null;
        }
        return oldVulnSet;
    }

    public boolean setupSnykIssues(String customer,
                                   String integrationId,
                                   MultipleTriggerResults triggerResults,
                                   Date currentTime) {
        Optional<TriggerResults> latestTriggerResult = triggerResults.getTriggerResults().stream().findFirst();
        boolean result = false;
        if (latestTriggerResult.isPresent()) {
            result = jobDtoParser.applyToResults(
                    customer,
                    SNYK_TRIGGER_DATATYPE,
                    SnykIssues.class,
                    latestTriggerResult.get(),
                    snykIssues -> DbSnykIssue.fromIssues(snykIssues, integrationId, currentTime)
                            .forEach(issue -> snykDatabaseService.insert(customer, issue)),
                    List.of()
            );
            if (result) {
                trackingService.upsert(customer,
                        IntegrationTracker.builder()
                                .integrationId(integrationId)
                                .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE))
                                .build());
            }
        }
        return result;
    }
}
