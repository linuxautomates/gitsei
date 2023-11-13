package io.levelops.aggregations.functions;

import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;

import java.util.Collections;
import java.util.List;

public class TenableAggQueries {
    public static QueryField NON_NULL_SEVERITIES = new QueryField("vuln_severity",
            QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, List.of(), null);

    public static QueryField NON_NULL_STATUS = new QueryField("asset_vuln_status",
            QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, List.of(), null);

    public static QueryGroup getVulnsQueryBySeverity(String severity) {
        return QueryGroup.and(
                new QueryField("vuln_severity", QueryField.FieldType.STRING, QueryField.Operation.EXACT_MATCH,
                        Collections.emptyList(), severity));
    }

    public static QueryGroup getVulnsQueryByStatus(String status) {
        return QueryGroup.and(
                new QueryField("asset_vuln_status", QueryField.FieldType.STRING, QueryField.Operation.EXACT_MATCH,
                        Collections.emptyList(), status));
    }
}
