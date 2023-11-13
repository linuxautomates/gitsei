package io.levelops.aggregations.helpers;

import io.levelops.aggregations.exceptions.AggregationFailedException;
import io.levelops.aggregations.functions.TenableAggQueries;
import io.levelops.commons.databases.models.database.temporary.TempTenableVulnObject;
import io.levelops.commons.databases.services.temporary.TenableVulnsQueryTable;
import io.levelops.aggregations.models.TenableAggData;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Log4j2
@SuppressWarnings("unused")
public class TenableAggregatorService {
    public TenableAggData aggregateTenableVulns(TenableVulnsQueryTable queryTable, TenableAggData tenableAggData) throws AggregationFailedException {
        try {
            Set<String> severities = new HashSet<>(queryTable.distinctValues(TenableAggQueries.NON_NULL_SEVERITIES));
            Set<String> statuses = new HashSet<>(queryTable.distinctValues(TenableAggQueries.NON_NULL_STATUS));

            for (String severity : severities) {
                tenableAggData.getAggBySeverity().put(severity, getVulnBySeverity(severity, queryTable));
            }
            for (String status : statuses) {
                tenableAggData.getAggByStatus().put(status, getVulnByStatus(status, queryTable));
            }
        } catch (SQLException e) {
            throw new AggregationFailedException("failed to agg tenable data.", e);
        }
        return tenableAggData;
    }

    private List<TempTenableVulnObject> getVulnBySeverity(String severity, TenableVulnsQueryTable queryTable) throws SQLException {
        return queryTable.getRows(List.of(TenableAggQueries.getVulnsQueryBySeverity(severity)),
                true, 0, 10000000);
    }

    private List<TempTenableVulnObject> getVulnByStatus(String status, TenableVulnsQueryTable queryTable) throws SQLException {
        return queryTable.getRows(List.of(TenableAggQueries.getVulnsQueryByStatus(status)),
                true, 0, 10000000);
    }
}
