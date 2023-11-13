package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.SnykVulnQueryTable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykVulnerabilityAggWrapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SnykVulnQueryTableTest {
    private final ObjectMapper m = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private PGSimpleDataSource simpleDataSource() {
        return (PGSimpleDataSource) pg.getEmbeddedPostgres().getPostgresDatabase();
    }

    @Test
    public void testBatchUpsert() throws SQLException, IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/snykvulns.json");
        List<SnykIssues> snykIssues = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, SnykIssues.class));
        PGSimpleDataSource ds = simpleDataSource();
        try (SnykVulnQueryTable svqt = new SnykVulnQueryTable(ds, "test", "snykvulns", m)) {
            svqt.createTempTable();
            List<SnykVulnerabilityAggWrapper> data = new ArrayList<>();
            snykIssues.forEach(si -> data.addAll(SnykVulnerabilityAggWrapper.fromSnykIssues(si)));
            svqt.insertRows(data);
            List<SnykVulnerabilityAggWrapper> snykVulnerabilityAggWrappers = svqt.getRows(Collections.emptyList(), false, 0, 100000);
            assertThat(svqt.getRows(Collections.emptyList(), false, 0, 100000).size()).isGreaterThan(0);

            assertThat(svqt.countRows(Collections.emptyList(), false)).isEqualTo(545);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "high")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(454);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "low")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(30);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "medium")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(61);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "high"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(9);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "low"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "medium"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);

            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "high"),
                    new QueryField("isPatched", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "low"),
                    new QueryField("isPatched", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "medium"),
                    new QueryField("isPatched", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);

            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "high"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true"),
                    new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("ignored"),
                            "wont-fix")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(3);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "medium"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true"),
                    new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("ignored"),
                            "wont-fix")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);
            assertThat(svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("severity", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "low"),
                    new QueryField("isIgnored", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "true"),
                    new QueryField("ignored", QueryField.FieldType.STRING,
                            QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                            null),
                    new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("ignored"),
                            "wont-fix")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(0);

            assertThat(svqt.countRows(List.of(
                    new QueryGroup(List.of(
                            new QueryField("isIgnored", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                                    "true"),
                            new QueryField("ignored", QueryField.FieldType.STRING,
                                    QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                                    null),
                            new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                                    QueryField.Operation.NOT_EQUALS, Collections.singletonList("ignored"),
                                    "wont-fix"),
                            new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                                    QueryField.Operation.NOT_EQUALS, Collections.singletonList("ignored"),
                                    "temporary-ignore"),
                            new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                                    QueryField.Operation.NOT_EQUALS, Collections.singletonList("ignored"),
                                    "not-vulnerable")), QueryGroup.GroupOperator.AND),
                    new QueryGroup(List.of(
                            new QueryField("isIgnored", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                                    "true"),
                            new QueryField("ignored", QueryField.FieldType.STRING,
                                    QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                                    null),
                            new QueryField("reasonType", QueryField.FieldType.JSONB_ARRAY_FIELD,
                                    QueryField.Operation.NULL_CHECK, Collections.singletonList("ignored"),
                                    null)), QueryGroup.GroupOperator.AND)), Boolean.FALSE)).isEqualTo(0);

            List<String> maturities = svqt.distinctValues(new QueryField("exploitMaturity",
                    QueryField.FieldType.STRING, QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                    null));
            for (String maturity : maturities) {
                System.out.println("maturity: " + maturity + " ct: "
                        + svqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                        new QueryField("severity", QueryField.FieldType.STRING,
                                QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                                "low"),
                        new QueryField("exploitMaturity", QueryField.FieldType.STRING,
                                QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                                maturity)), QueryGroup.GroupOperator.AND)), Boolean.TRUE));
            }
        }
    }
}
