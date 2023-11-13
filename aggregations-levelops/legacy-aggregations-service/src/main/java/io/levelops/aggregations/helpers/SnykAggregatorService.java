package io.levelops.aggregations.helpers;

import io.levelops.aggregations.exceptions.AggregationFailedException;
import io.levelops.aggregations.functions.SnykAggQueries;
import io.levelops.aggregations.models.SnykAggData;
import io.levelops.aggregations.models.snyk.SnykAggForSeverity;
import io.levelops.commons.databases.services.temporary.SnykVulnQueryTable;
import io.levelops.integrations.snyk.models.SnykVulnerabilityAggWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Log4j2
@SuppressWarnings("unused")
public class SnykAggregatorService {

    public SnykAggData aggregateSnykVulns(SnykVulnQueryTable queryTable, SnykAggData aggData)
            throws AggregationFailedException {
        try {
            aggData.setVulnerabilityCount(queryTable.countRows(Collections.emptyList(), false));
            Set<String> severities = new HashSet<>(queryTable.distinctValues(
                    SnykAggQueries.NON_NULL_SEVERITIES));

            for (String severity : severities) {
                aggData.getAggBySeverity().put(severity, getVulnAggBySeverity(severity,null, null, queryTable));
            }
            aggData.setSuppressedIssues(queryTable.getRows(List.of(SnykAggQueries.getSuppressedVulnsQuery()),
                    true, 0, 100000000).stream().map(SnykVulnerabilityAggWrapper::getSnykVulnerability).collect(Collectors.toList()));

            Set<String> scmUrls = new HashSet<>(queryTable.distinctValues(SnykAggQueries.NON_NULL_SCM_URL));
            for(String currentScmUrl : scmUrls){
                for (String severity : severities) {
                    SnykAggForSeverity snykAggForSeverity = getVulnAggBySeverity(severity, currentScmUrl, null, queryTable);
                    if(!aggData.getAggBySCMUrlBySeverity().containsKey(currentScmUrl)){
                        aggData.getAggBySCMUrlBySeverity().put(currentScmUrl, new HashMap<>());
                    }
                    aggData.getAggBySCMUrlBySeverity().get(currentScmUrl).put(severity, snykAggForSeverity);
                }
            }

            Set<String> scmRepoNamesPartial = new HashSet<>(queryTable.distinctValues(SnykAggQueries.NON_NULL_SCM_REPO_NAME_PARTIAL));
            for(String scmRepoNamePartial : scmRepoNamesPartial){
                for (String severity : severities) {
                    SnykAggForSeverity snykAggForSeverity = getVulnAggBySeverity(severity, null, scmRepoNamePartial, queryTable);
                    if(!aggData.getAggBySCMRepoNamePartialBySeverity().containsKey(scmRepoNamePartial)){
                        aggData.getAggBySCMRepoNamePartialBySeverity().put(scmRepoNamePartial, new HashMap<>());
                    }
                    aggData.getAggBySCMRepoNamePartialBySeverity().get(scmRepoNamePartial).put(severity, snykAggForSeverity);
                }
            }
        } catch (SQLException e) {
            throw new AggregationFailedException("failed to agg snyk data.", e);
        }
        return aggData;
    }

    private SnykAggForSeverity getVulnAggBySeverity(String severity, String scmUrl, String scmRepoNamePartial, SnykVulnQueryTable queryTable)
            throws SQLException {
        SnykAggForSeverity agg = new SnykAggForSeverity();

        agg.setVulns(queryTable.countRows(List.of(
                SnykAggQueries.getVulnsQueryBySeverity(severity, scmUrl, scmRepoNamePartial)), true));
        agg.setSuppressed(queryTable.countRows(List.of(
                SnykAggQueries.getQueryForSuppressedWithSeverity(severity, scmUrl, scmRepoNamePartial)), true));
        //TODO: figure out if this is accurate
        agg.setPatched(queryTable.countRows(
                List.of(SnykAggQueries.getQueryForPatchedWithSeverity(severity, scmUrl, scmRepoNamePartial)), true));

        if (agg.getSuppressed() > 0) {
            agg.setCustomSuppress(queryTable.countRows(SnykAggQueries.getQueryForCustomSuppressWithSeverity(severity, scmUrl, scmRepoNamePartial),
                    false)); //'or' between the various groups
            agg.setNotVulnerableSuppress(queryTable.countRows(
                    List.of(SnykAggQueries.getQueryForNotVulnWithSeverity(severity, scmUrl, scmRepoNamePartial)), true));
            agg.setTemporarySuppress(queryTable.countRows(
                    List.of(SnykAggQueries.getQueryForTempWithSeverity(severity, scmUrl, scmRepoNamePartial)), true));
            agg.setWontFixSuppress(queryTable.countRows(
                    List.of(SnykAggQueries.getQueryForWontFixWithSeverity(severity, scmUrl, scmRepoNamePartial)), true));
        }

        return agg;
    }
}
