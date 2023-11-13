package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.temporary.TempJiraIssueFields;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.JiraIssueQueryTable;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueQueryTableTest {
    private final ObjectMapper m = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private PGSimpleDataSource simpleDataSource() {
        return (PGSimpleDataSource) pg.getEmbeddedPostgres().getPostgresDatabase();
    }

    @Test
    public void testBatchUpsert() throws SQLException, IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues.json");
        List<JiraIssue> jiraIssues = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, JiraIssue.class));

        PGSimpleDataSource ds = simpleDataSource();
        try (JiraIssueQueryTable jiqt = new JiraIssueQueryTable(ds, "test", "jiraissues", m)) {
            jiqt.createTempTable();
            List<TempJiraIssueFields> tjif = jiraIssues.stream()
                    .map(issue -> TempJiraIssueFields.builder().id(issue.getId())
                            .gif(issue.getFields())
                            .updatedAt(DateUtils.toEpochSecond(issue.getFields().getUpdated()))
                            .build()).collect(Collectors.toList());
            jiqt.insertRows(tjif);
            assertThat(jiqt.countRows(Collections.emptyList(), false)).isEqualTo(50);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("fix_versions"),
                            "2020")), QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isEqualTo(2);
            assertThat(jiqt.distinctValues(new QueryField("name", QueryField.FieldType.JSONB_ARRAY_FIELD,
                    QueryField.Operation.NON_NULL_CHECK, Lists.newArrayList("fix_versions"), null))
                    .size()).isEqualTo(4);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(Collections.singletonList(
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.PREFIX, Collections.singletonList("fix_versions"),
                            "302")), QueryGroup.GroupOperator.OR)), Boolean.TRUE)).isEqualTo(0);
            assertThat(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.REGEX, Collections.singletonList("fix_versions"),
                            "202.*"),
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.NON_NULL_CHECK, Collections.singletonList("fix_versions"),
                            "202.*")), QueryGroup.GroupOperator.AND)), Boolean.TRUE, 0, 2).size()).isEqualTo(2);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.REGEX, Collections.singletonList("fix_versions"),
                            "202.*"),
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.NON_NULL_CHECK, Collections.singletonList("fix_versions"),
                            "202.*")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(2);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("id", QueryField.FieldType.JSONB_ARRAY_FIELD,
                            QueryField.Operation.NON_NULL_CHECK, Collections.singletonList("fix_versions"),
                            null)), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(2);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("labels", QueryField.FieldType.STRING_ARRAY,
                            QueryField.Operation.EXACT_MATCH, Collections.emptyList(),
                            "test-label")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(1);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("labels", QueryField.FieldType.STRING_ARRAY,
                            QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(),
                            null)), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(3);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("id", QueryField.FieldType.STRING,
                            QueryField.Operation.PREFIX, Collections.singletonList("project"),
                            "100")), QueryGroup.GroupOperator.AND)), Boolean.TRUE)).isEqualTo(50);
            assertThat(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("name", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("creator"),
                            "maxime")), QueryGroup.GroupOperator.OR)), Boolean.FALSE, 0, 1000000).size())
                    .isGreaterThan(0);
            assertThat(jiqt.countRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("name", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("assignee"),
                            "meghana")), QueryGroup.GroupOperator.OR)), Boolean.FALSE)).isEqualTo(24);
            assertThat(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(
                    new QueryField("name", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("assignee"),
                            "meghana"),
                    new QueryField("name", QueryField.FieldType.STRING,
                            QueryField.Operation.EXACT_MATCH, Collections.singletonList("assignee"),
                            "amegh")), QueryGroup.GroupOperator.AND)), Boolean.TRUE, 0, 1000000).size()).isEqualTo(0);
            assertThat(jiqt.getRows(List.of(
                    new QueryGroup(List.of(
                            new QueryField("name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.singletonList("assignee"),
                                    "meghana"),
                            new QueryField("name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.singletonList("assignee"),
                                    "amegh")), QueryGroup.GroupOperator.AND),
                    new QueryGroup(List.of(
                            new QueryField("name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.singletonList("creator"),
                                    "maxime"),
                            new QueryField("name", QueryField.FieldType.STRING,
                                    QueryField.Operation.EXACT_MATCH, Collections.singletonList("creator"),
                                    "admin")), QueryGroup.GroupOperator.OR)), Boolean.FALSE, 0, 1000000).size()).isEqualTo(28);
            assertThat(jiqt.distinctValues(new QueryField("labels", QueryField.FieldType.STRING_ARRAY,
                    QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(), null)))
                    .containsExactlyInAnyOrder("test-label", "test-label24", "test2-label",
                            "sometest-label");
            assertThat(jiqt.distinctValues(new QueryField("labels", QueryField.FieldType.STRING_ARRAY,
                    QueryField.Operation.NON_NULL_CHECK, Collections.emptyList(), null))
                    .size()).isEqualTo(4);
            assertThat(jiqt.distinctValues(new QueryField("name", QueryField.FieldType.STRING,
                    QueryField.Operation.NON_NULL_CHECK, Lists.newArrayList("issue_type"), null))
                    .size()).isEqualTo(4);
            assertThat(jiqt.distinctValues(new QueryField("name", QueryField.FieldType.STRING,
                    QueryField.Operation.NON_NULL_CHECK, Lists.newArrayList("issue_type"), null)))
                    .containsExactlyInAnyOrder("Bug", "Task", "Epic", "Sub-task");
            assertThat(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(new QueryField("updated",
                    QueryField.FieldType.NUMBER, QueryField.Operation.GREATER_THAN, Collections.emptyList(),
                    1568418459267L)), QueryGroup.GroupOperator.OR)), Boolean.TRUE, 0, 1000000)
                    .size()).isGreaterThan(0);
            assertThat(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(new QueryField(
                    "updated", QueryField.FieldType.NUMBER, QueryField.Operation.GREATER_THAN,
                    Collections.emptyList(), 15684184)), QueryGroup.GroupOperator.AND)), Boolean.TRUE, 0, 1000000)
                    .size()).isGreaterThan(0);
            //this one just asserts that query field is creatable with null value for number null checking
            assertThat(new QueryField("name", QueryField.FieldType.NUMBER,
                    QueryField.Operation.NON_NULL_CHECK, Lists.newArrayList("issue_type"), null));
            System.out.println(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(new QueryField(
                    "updated", QueryField.FieldType.NUMBER, QueryField.Operation.GREATER_THAN,
                    Collections.emptyList(), 1568418459267L)), QueryGroup.GroupOperator.AND)), Boolean.TRUE, 0, 1000000)
                    .size());
            System.out.println(jiqt.getRows(Collections.singletonList(new QueryGroup(List.of(new QueryField(
                    "updated", QueryField.FieldType.NUMBER, QueryField.Operation.GREATER_THAN,
                    Collections.emptyList(), 156841)), QueryGroup.GroupOperator.AND)), Boolean.TRUE, 0, 1000000)
                    .size());
        }
    }
}
