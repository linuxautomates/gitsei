package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.temporary.TempTenableVulnObject;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.TenableVulnsQueryTable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.tenable.models.NetworkVulnerability;
import io.levelops.integrations.tenable.models.WasVulnerability;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TenableVulnsQueryTableTest {
    private final ObjectMapper m = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private PGSimpleDataSource simpleDataSource() {
        return (PGSimpleDataSource) pg.getEmbeddedPostgres().getPostgresDatabase();
    }

    @Test
    public void testBatchUpsert() throws SQLException, IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/tenablenetworkvulns.json");
        PaginatedResponse<NetworkVulnerability> networkVulns = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, NetworkVulnerability.class));
        input = ResourceUtils.getResourceAsString("json/databases/tenablewasvulns.json");
        PaginatedResponse<WasVulnerability> wasVulns = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, WasVulnerability.class));

        List<TempTenableVulnObject> list = networkVulns.getResponse()
                .getRecords()
                .stream()
                .map(TempTenableVulnObject::fromNetworkVuln)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        list.addAll(wasVulns.getResponse()
                .getRecords()
                .stream()
                .map(TempTenableVulnObject::fromWasVuln)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        PGSimpleDataSource ds = simpleDataSource();
        try (TenableVulnsQueryTable tvqt = new TenableVulnsQueryTable(ds, "test", "tenablevulns", m)) {
            tvqt.createTempTable();
            tvqt.insertRows(list);
            assertThat(tvqt.countRows(Collections.emptyList(), false)).isEqualTo(187);
            assertThat(tvqt.distinctValues(new QueryField("vuln_severity", QueryField.FieldType.STRING,
                    QueryField.Operation.NON_NULL_CHECK, List.of(), null))
                    .size()).isEqualTo(3);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("vuln_severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "MEDIUM")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("vuln_severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "INFO")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("vuln_severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "UNDEFINED")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("asset_vuln_status", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "UNDEFINED")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("asset_vuln_status", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "OPEN")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
            assertThat(tvqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("asset_vuln_status", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, List.of(), "REOPENED")),
                    QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isGreaterThan(0);
        }
    }
}
