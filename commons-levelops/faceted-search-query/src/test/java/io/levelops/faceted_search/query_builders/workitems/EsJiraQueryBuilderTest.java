package io.levelops.faceted_search.query_builders.workitems;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter.DISTINCT;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.faceted_search.querybuilders.workitems.EsJiraQueryBuilder;
import io.levelops.faceted_search.utils.ESAggResultUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static io.levelops.faceted_search.querybuilders.workitems.EsJiraQueryBuilder.buildSearchRequest;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class EsJiraQueryBuilderTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String indexName = "jira_issues_foo_1630454400";

    //region Done
    @Test
    public void testIncludes() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .priorities(List.of("HIGH"))
                .keys(List.of("LEV-3733", "LEV-3699"))
                .isActive(true)
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_priority\": [\"HIGH\"]}},\n" +
                "            {\"terms\": { \"w_workitem_id\": [\"LEV-3733\",\"LEV-3699\"]}},\n" +
                "            {\"term\": {\"w_is_active\": true}}" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).term().field(), searchRequest.query().bool().must().get(3).term().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).term().value().booleanValue(), searchRequest.query().bool().must().get(3).term().value().booleanValue());

    }

    @Test
    public void testIncludes2() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .projects(List.of("p1"))
                .statuses(List.of("s1", "s2"))
                .assignees(List.of("id1", "id2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_project\": [\"p1\"]}},\n" +
                "            {\"terms\": { \"w_status\": [\"s1\",\"s2\"]}},\n" +
                "            {\"terms\": { \"w_assignee.id\": [\"id1\",\"id2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
    }

    @Test
    public void testIncludes3() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .assigneeDisplayNames(List.of("name"))
                .issueTypes(List.of("issue-1", "issue-2"))
                .parentKeys(List.of("k1", "k2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_assignee.display_name\": [\"name\"]}},\n" +
                "            {\"terms\": { \"w_workitem_type\": [\"issue-1\",\"issue-2\"]}},\n" +
                "            {\"terms\": { \"w_parent_key\": [\"k1\",\"k2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
    }

    @Test
    public void testIncludes4() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .components(List.of("c1"))
                .labels(List.of("l1", "l2"))
                .historicalAssignees(List.of("a1", "a2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_components\": [\"c1\"]}},\n" +
                "            {\"terms\": { \"w_labels\": [\"l1\",\"l2\"]}},\n" +
                "            {\"terms\": { \"w_hist_assignees.assignee.display_name\": [\"a1\",\"a2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
    }

    @Test
    public void testIncludes5() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .sprintIds(List.of("s1"))
                .sprintNames(List.of("name1", "name2"))
                .sprintStates(List.of("state1", "state2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            { \"terms\": { \"w_sprints.id\": [\"s1\"]}}," +
                "            { \"terms\": { \"w_sprints.name\": [\"name1\",\"name2\"]}}," +
                "            { \"terms\": { \"w_sprints.state\": [\"state1\",\"state2\"]}}" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
    }

    @Test
    public void testIncludes6() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .statusCategories(List.of("s1"))
                .integrationIds(List.of("id1", "id2"))
                .epics(List.of("e1", "e2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_status_category\": [\"s1\"]}},\n" +
                "            {\"terms\": { \"w_integration_id\": [\"id1\",\"id2\"]}},\n" +
                "            {\"terms\": { \"w_epic\": [\"e1\",\"e2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
    }

    @Test
    public void testIncludes7() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .resolutions(List.of("r1"))
                .versions(List.of("v1", "v2"))
                .fixVersions(List.of("fv1", "fv2"))
                .age(ImmutablePair.of(5L,7L))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"terms\": { \"w_resolution\": [\"r1\"]}},\n" +
                "            {\"terms\": { \"w_versions.name\": [\"v1\",\"v2\"]}},\n" +
                "            {\"terms\": { \"w_fix_versions.name\": [\"fv1\",\"fv2\"]}},\n" +
                "            {\"range\": { \"w_age\": {\"gt\": 5, \"lt\": 7}}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().field(), searchRequest.query().bool().must().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(), searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().field(), searchRequest.query().bool().must().get(3).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().must().get(3).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().field(),
                searchRequest.query().bool().must().get(4).range().field());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().gt().toString(),
                searchRequest.query().bool().must().get(4).range().gt().toString());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().lt().toString(),
                searchRequest.query().bool().must().get(4).range().lt().toString());
    }

    @Test
    public void testIncludes8() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .sprintMappingSprintNames(List.of("s1"))
                .build();

        Reader input = new StringReader("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        {\n" +
                "          \"terms\": {\n" +
                "            \"w_integ_type\": [\n" +
                "              \"jira\"\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"nested\": {\n" +
                "            \"path\": \"w_hist_sprints\",\n" +
                "            \"query\": {\n" +
                "              \"bool\": {\n" +
                "                \"must\": [\n" +
                "                  {\n" +
                "                    \"terms\": {\n" +
                "                      \"w_hist_sprints.name\": [\n" +
                "                        \"s1\"\n" +
                "                      ]\n" +
                "                    }\n" +
                "                  }\n" +
                "                ]\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"term\": {\n" +
                "            \"w_is_active\": {\n" +
                "              \"value\": true\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().path(), searchRequest.query().bool().must().get(1).nested().path());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field()
                , searchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().query().bool().must().get(0).terms().terms().value().get(0).stringValue()
                , searchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().terms().value().get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).term().field(), searchRequest.query().bool().must().get(2).term().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).term().value().booleanValue(), searchRequest.query().bool().must().get(2).term().value().booleanValue());
    }

    @Test
    public void testIncludes9() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .sprintMappingSprintState("CLOSED")
                .sprintMappingSprintNameStartsWith("s1")
                .build();

        Reader input = new StringReader("{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        {\n" +
                "          \"terms\": {\n" +
                "            \"w_integ_type\": [\n" +
                "              \"jira\"\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"nested\": {\n" +
                "            \"path\": \"w_hist_sprints\",\n" +
                "            \"query\": {\n" +
                "              \"term\": {\n" +
                "                \"w_hist_sprints.state\": {\n" +
                "                  \"value\": \"CLOSED\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"nested\": {\n" +
                "            \"path\": \"w_hist_sprints\",\n" +
                "            \"query\": {\n" +
                "              \"wildcard\": {\n" +
                "                \"w_hist_sprints.name\": {\n" +
                "                  \"wildcard\": \"s1*\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"term\": {\n" +
                "            \"w_is_active\": {\n" +
                "              \"value\": true\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().path(), searchRequest.query().bool().must().get(1).nested().path());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().query().term().field()
                , searchRequest.query().bool().must().get(1).nested().query().term().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).nested().query().term().value().stringValue()
                , searchRequest.query().bool().must().get(1).nested().query().term().value().stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).nested().query().wildcard().field(),
                searchRequest.query().bool().must().get(2).nested().query().wildcard().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).nested().query().wildcard().value()
                , searchRequest.query().bool().must().get(2).nested().query().wildcard().value());
        Assertions.assertEquals(expected.query().bool().must().get(3).term().field(),
                searchRequest.query().bool().must().get(3).term().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).term().value().booleanValue(),
                searchRequest.query().bool().must().get(3).term().value().booleanValue());
    }

    @Test
    public void testExcludes() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludeProjects(List.of("p1"))
                .excludeStatuses(List.of("s1", "s2"))
                .excludeAssignees(List.of("id1", "id2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            {\"terms\": { \"w_project\": [\"p1\"]}},\n" +
                "            {\"terms\": { \"w_assignee.id\": [\"id1\",\"id2\"]}},\n" +
                "            {\"terms\": { \"w_status\": [\"s1\",\"s2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");


        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testExcludes2() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludePriorities(List.of("HIGH"))
                .excludeKeys(List.of("LEV-3733", "LEV-3699"))
                .excludeIntegrationIds(List.of("123", "233"))
                .build();


        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            {\"terms\": { \"w_priority\": [\"HIGH\"]}},\n" +
                "            {\"terms\": { \"w_workitem_id\": [\"LEV-3733\",\"LEV-3699\"]}},\n" +
                "            {\"terms\": {\"w_integration_id\": [\"123\",\"233\"]}}" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testExcludes3() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludeIssueTypes(List.of("issue-1", "issue-2"))
                .excludeParentKeys(List.of("k1", "k2"))
                .excludeEpics(List.of("e1", "e2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            {\"terms\": { \"w_epic\": [\"e1\",\"e2\"]}},\n" +
                "            {\"terms\": { \"w_workitem_type\": [\"issue-1\",\"issue-2\"]}},\n" +
                "            {\"terms\": { \"w_parent_key\": [\"k1\",\"k2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testExcludes4() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludeResolutions(List.of("r1", "r2"))
                .excludeStatusCategories(List.of("cat1", "cat2"))
                .excludeReporters(List.of("r_id1", "r_id2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            {\"terms\": { \"w_resolution\": [\"r1\",\"r2\"]}},\n" +
                "            {\"terms\": { \"w_status_category\": [\"cat1\",\"cat2\"]}},\n" +
                "            {\"terms\": { \"w_reporter.id\": [\"r_id1\",\"r_id2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testExcludes5() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludeComponents(List.of("c1", "c2"))
                .excludeLabels(List.of("l1", "l2"))
                .excludeVersions(List.of("v1", "v2"))
                .excludeFixVersions(List.of("fv1", "fv2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            {\"terms\": { \"w_components\": [\"c1\",\"c2\"]}},\n" +
                "            {\"terms\": { \"w_labels\": [\"l1\",\"l2\"]}},\n" +
                "            {\"terms\": { \"w_versions.name\": [\"v1\",\"v2\"]}},\n" +
                "            {\"terms\": { \"w_fix_versions.name\": [\"fv1\",\"fv2\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testExcludes6() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .excludeSprintIds(List.of("id1", "id2"))
                .excludeSprintNames(List.of("s1", "s2"))
                .excludeSprintStates(List.of("state1", "state2"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must_not\": [\n" +
                "            { \"terms\": { \"w_sprints.id\": [\"id1\",\"id2\"]}}," +
                "            { \"terms\": { \"w_sprints.name\": [\"s1\",\"s2\"]}}," +
                "            { \"terms\": { \"w_sprints.state\": [\"state1\",\"state2\"]}}" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().field(), searchRequest.query().bool().mustNot().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().field(), searchRequest.query().bool().mustNot().get(1).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(1).terms().terms().value()
                .get(1).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().field(), searchRequest.query().bool().mustNot().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue(), searchRequest.query().bool().mustNot().get(2).terms().terms().value()
                .get(1).stringValue());

    }

    @Test
    public void testRangeQuery() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .issueCreatedRange(ImmutablePair.of(1651217628L, 1651217668L))
                .issueUpdatedRange(ImmutablePair.of(1651217628L, 1651217668L))
                .issueDueRange(ImmutablePair.of(1651217628L, 1651217668L))
                .snapshotRange(ImmutablePair.of(1651217628L, 1651217668L))
                .storyPoints(Map.of("1651217628", "1651217668"))
                .build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"range\": { \"w_created_at\": {\"time_zone\": \"UTC\",\n" +
                "              \"format\": \"epoch_second\",\n" +
                "              \"gt\": 1651217628,\n" +
                "              \"lt\": 1651217668}}},\n" +
                "            {\"range\": { \"w_updated_at\": {\"time_zone\": \"UTC\",\n" +
                "              \"format\": \"epoch_second\",\n" +
                "              \"gt\": 1651217628,\n" +
                "              \"lt\": 1651217668}}},\n" +
                "            {\"range\": { \"w_due_at\": {\"time_zone\": \"UTC\",\n" +
                "              \"format\": \"epoch_second\",\n" +
                "              \"gt\": 1651217628,\n" +
                "              \"lt\": 1651217668}}},\n" +
                "            {\"range\": { \"w_ingested_at\": {\"time_zone\": \"UTC\",\n" +
                "              \"format\": \"epoch_second\",\n" +
                "              \"gt\": 1651217628,\n" +
                "              \"lt\": 1651217668}}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(1).range().field(), searchRequest.query().bool().must().get(1).range().field());
        Assertions.assertEquals(expected.query().bool().must().get(1).range().format(), searchRequest.query().bool().must().get(1).range().format());
        Assertions.assertEquals(expected.query().bool().must().get(1).range().gt().toString(), searchRequest.query().bool().must().get(1).range().gt().toString());
        Assertions.assertEquals(expected.query().bool().must().get(1).range().lt().toString(), searchRequest.query().bool().must().get(1).range().lt().toString());

        Assertions.assertEquals(expected.query().bool().must().get(2).range().field(), searchRequest.query().bool().must().get(2).range().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).range().format(), searchRequest.query().bool().must().get(2).range().format());
        Assertions.assertEquals(expected.query().bool().must().get(2).range().gt().toString(), searchRequest.query().bool().must().get(2).range().gt().toString());
        Assertions.assertEquals(expected.query().bool().must().get(2).range().lt().toString(), searchRequest.query().bool().must().get(2).range().lt().toString());

        Assertions.assertEquals(expected.query().bool().must().get(3).range().field(), searchRequest.query().bool().must().get(3).range().field());
        Assertions.assertEquals(expected.query().bool().must().get(3).range().format(), searchRequest.query().bool().must().get(3).range().format());
        Assertions.assertEquals(expected.query().bool().must().get(3).range().gt().toString(), searchRequest.query().bool().must().get(3).range().gt().toString());
        Assertions.assertEquals(expected.query().bool().must().get(3).range().lt().toString(), searchRequest.query().bool().must().get(3).range().lt().toString());

        Assertions.assertEquals(expected.query().bool().must().get(4).range().field(), searchRequest.query().bool().must().get(4).range().field());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().format(), searchRequest.query().bool().must().get(4).range().format());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().gt().toString(), searchRequest.query().bool().must().get(4).range().gt().toString());
        Assertions.assertEquals(expected.query().bool().must().get(4).range().lt().toString(), searchRequest.query().bool().must().get(4).range().lt().toString());

    }

    @Test
    public void testCustomFilterQuery() {

        Reader input = new StringReader("{" +
                "\"query\": {" +
                "    \"bool\": {" +
                "      \"must\": [" +
                "        {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "        {" +
                "          \"nested\": {" +
                "            \"path\": \"w_custom_fields\"," +
                "            \"query\": {" +
                "              \"bool\": {" +
                "                \"must\": [" +
                "                  {" +
                "                    \"terms\": {" +
                "                      \"w_custom_fields.name\": [" +
                "                        \"custom_field0111\"" +
                "                      ]" +
                "                    }" +
                "                  }," +
                "                  {" +
                "                    \"terms\": {" +
                "                      \"w_custom_fields.str\": [" +
                "                        \"foo\"" +
                "                      ]" +
                "                    }" +
                "                  }" +
                "                ]" +
                "              }" +
                "            }" +
                "          }" +
                "        }" +
                "      ]," +
                "      \"must_not\":[]" +
                "    }" +
                "  }" +
                "}");

        SearchRequest expected = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(input)
        );

        SearchRequest actual = EsJiraQueryBuilder.buildSearchRequest(JiraIssuesFilter.builder()
                        .customFields(Map.of("custom_field0111", List.of("foo"))).build(),
                List.of(), List.of(), null, List.of(DbJiraField.builder()
                        .name("custom_field0111")
                        .fieldKey("custom_field0111")
                        .fieldType("string")
                        .build()), false, null,
                indexName, false, null, null, false).build();

        Assertions.assertNotNull(expected.query());
        Assertions.assertNotNull(actual.query());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), actual.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), actual.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        assertThat(expected.query()._kind()).isEqualTo(actual.query()._kind());
        assertThat(((BoolQuery) expected.query()._get()).must().get(1)._kind())
                .isEqualTo(((BoolQuery) actual.query()._get()).must().get(1)._kind());
        assertThat(expected.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().field())
                .isEqualTo(actual.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().field());
        assertThat(expected.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().terms().value().get(0).stringValue())
                .isEqualTo(actual.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().terms().value().get(0).stringValue());
    }

    @Test
    public void testMissingFields() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .missingFields(Map.of("priority", false,
                        "epic", true)).build();

        Reader input = new StringReader("{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "           {" +
                "             \"exists\": {" +
                "                  \"field\": \"w_priority\"" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[" +
                "           {" +
                "               \"exists\": {" +
                "                   \"field\": \"w_epic\"" +
                "               }" +
                "           }" +
                "        ]" +
                "    }" +
                "}}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        assertThat(expected.query()._kind()).isEqualTo(searchRequest.query()._kind());
        assertThat(((BoolQuery) expected.query()._get()).mustNot().get(0)._kind())
                .isEqualTo(((BoolQuery) searchRequest.query()._get()).mustNot().get(0)._kind());
        assertThat(((ExistsQuery) ((BoolQuery) expected.query()._get()).mustNot().get(0)._get()).field()).
                isEqualTo(((ExistsQuery) ((BoolQuery) searchRequest.query()._get())
                        .mustNot().get(0)._get()).field());
        assertThat(((ExistsQuery) ((BoolQuery) expected.query()._get()).must().get(1)._get()).field())
                .isEqualTo(((ExistsQuery) ((BoolQuery) searchRequest.query()._get())
                        .must().get(1)._get()).field());

    }

    @Test
    public void testPartialMatchFields() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .partialMatch(Map.of("project", Map.of("$begins", "foo"))).build();

        Reader input = new StringReader("{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "           {" +
                "              \"wildcard\": {" +
                "                   \"w_project\": {" +
                "                   \"wildcard\": \"foo*\"" +
                "                   }" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        assertThat(expected.query()._kind()).isEqualTo(searchRequest.query()._kind());
        assertThat(((BoolQuery) expected.query()._get()).must().get(1)._kind())
                .isEqualTo(((BoolQuery) searchRequest.query()._get()).must().get(1)._kind());
        assertThat(((WildcardQuery) ((BoolQuery) expected.query()._get()).must().get(1)._get()).field())
                .isEqualTo(((WildcardQuery) ((BoolQuery) searchRequest.query()._get())
                        .must().get(1)._get()).field());
        assertThat(expected.query().bool().must().get(1).wildcard().value())
                .isEqualTo(searchRequest.query().bool().must().get(1).wildcard().value());
    }

    @Test
    public void testExtraCriteria() {

        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .extraCriteria(List.of(JiraIssuesFilter.EXTRA_CRITERIA.no_assignee)).build();

        Reader input = new StringReader("{\n" +
                "\"query\": {\n" +
                "    \"bool\": {\n" +
                "        \"must\": [\n" +
                "            {\"terms\": { \"w_integ_type\": [\"jira\"]}},\n" +
                "            {\"term\": { \"w_is_active\": \"true\"}},\n" +
                "            {\"terms\": { \"w_assignee.display_name\": [\"_UNASSIGNED_\"]}}\n" +
                "    ]\n" +
                "    }\n" +
                "}\n" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input));

        Assertions.assertEquals(expected.query().bool().must().get(0).terms().field(), searchRequest.query().bool().must().get(0).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(0).terms().terms().value()
                .get(0).stringValue());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().field(),
                searchRequest.query().bool().must().get(2).terms().field());
        Assertions.assertEquals(expected.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue(), searchRequest.query().bool().must().get(2).terms().terms().value()
                .get(0).stringValue());
    }

    @Test
    public void acrossAndStackTest() {
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.label).build();

        Reader input1 = new StringReader("{" +
                "  \"aggs\": {" +
                "    \"across_label\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_labels\"" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        SearchRequest expected = SearchRequest.of(q -> q
                .withJson(input1));

        Assertions.assertEquals(expected.aggregations().get("across_label").terms().field(),
                searchRequest.aggregations().get("across_label").terms().field());

        jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.fix_version).build();

        Reader input2 = new StringReader("{" +
                "  \"aggs\": {" +
                "    \"across_fix_version\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_fix_versions.name\"" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        expected = SearchRequest.of(q -> q
                .withJson(input2));

        Assertions.assertEquals(expected.aggregations().get("across_fix_version").terms().field(),
                searchRequest.aggregations().get("across_fix_version").terms().field());

        jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.epic).build();

        Reader input3 = new StringReader("{" +
                "  \"aggs\": {" +
                "    \"across_epic\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_epic\"" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        expected = SearchRequest.of(q -> q
                .withJson(input3));

        Assertions.assertEquals(expected.aggregations().get("across_epic").terms().field(),
                searchRequest.aggregations().get("across_epic").terms().field());


        jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.custom_field)
                .customAcross("customfield_123456").build();

        Reader input4 = new StringReader("{" +
                "  \"aggs\": {" +
                "    \"across_custom_field\": {" +
                "      \"nested\": {" +
                "        \"path\": \"w_custom_fields\"" +
                "      }," +
                "      \"aggs\": {" +
                "        \"filter_custom_fields_name\": {" +
                "          \"filter\": {" +
                "            \"term\": {" +
                "              \"w_custom_fields.name\": \"customfield_123456\"" +
                "            }" +
                "          }," +
                "          \"aggs\": {" +
                "            \"across_custom_fields_type\": {" +
                "              \"terms\": {" +
                "                \"field\": \"w_custom_fields.str\"" +
                "              }" +
                "            }" +
                "          }" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}");

        searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, List.of(DbJiraField.builder()
                .fieldKey("customfield_123456")
                .name("customfield_123456")
                .fieldType("string")
                .build()), false, null, indexName, false, null, null, false).build();
        expected = SearchRequest.of(q -> q
                .withJson(input4));

        Assertions.assertEquals(expected.aggregations().get("across_custom_field").nested().path(),
                searchRequest.aggregations().get("across_custom_field").nested().path());
        Assertions.assertEquals(expected.aggregations().get("across_custom_field").aggregations().get("filter_custom_fields_name")
                        .filter().term().field(),
                searchRequest.aggregations().get("across_custom_field").aggregations().get("filter_custom_fields_name")
                        .filter().term().field());
        Assertions.assertEquals(expected.aggregations().get("across_custom_field").aggregations().get("filter_custom_fields_name")
                        .aggregations().get("across_custom_fields_type").terms().field(),
                searchRequest.aggregations().get("across_custom_field").aggregations().get("filter_custom_fields_name")
                        .aggregations().get("across_custom_fields_type").terms().field());
    }
    //endregion

    @Test
    public void testAcrossFilterPartialMatchLabel() throws IOException {
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.label)
                .labels(List.of("full1", "full2"))
                .partialMatch(Map.of("labels", Map.of(
                        "$begins", "begin",
                        "$ends", "end",
                        "$contains", "contain"
                )))
                .filterAcrossValues(true)
                .isActive(true)
                .build();

        String expected = "{\"aggregations\":{\"across_label\":{\"aggregations\":{\"total_story_points\":{\"sum\":{\"field\":\"w_story_points\"}},\"bucket_pagination\":{\"bucket_sort\":{\"from\":0,\"size\":90}},\"mean_story_points\":{\"avg\":{\"field\":\"w_story_points\",\"missing\":0}}},\"terms\":{\"field\":\"w_labels\",\"include\":[\"full1\",\"full2\",\"begin.*\",\".*end\",\".*contain.*\"],\"min_doc_count\":1,\"size\":2147483647}}},\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_integ_type\":[\"jira\"]}},{\"terms\":{\"w_labels\":[\"full1\",\"full2\"]}},{\"wildcard\":{\"w_labels\":{\"wildcard\":\"begin*\"}}},{\"wildcard\":{\"w_labels\":{\"wildcard\":\"*end\"}}},{\"wildcard\":{\"w_labels\":{\"wildcard\":\"*contain*\"}}},{\"term\":{\"w_is_active\":{\"value\":true}}}]}}}";
        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        String actual = ESAggResultUtils.getQueryString(searchRequest);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testAcrossFilterPartialMatchComponent() throws IOException {
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.component)
                .components(List.of("full1", "full2"))
                .partialMatch(Map.of("components", Map.of(
                        "$begins", "begin",
                        "$ends", "end",
                        "$contains", "contain"
                )))
                .filterAcrossValues(true)
                .isActive(true)
                .build();

        String expected = "{\"aggregations\":{\"across_component\":{\"aggregations\":{\"total_story_points\":{\"sum\":{\"field\":\"w_story_points\"}},\"bucket_pagination\":{\"bucket_sort\":{\"from\":0,\"size\":90}},\"mean_story_points\":{\"avg\":{\"field\":\"w_story_points\",\"missing\":0}}},\"terms\":{\"field\":\"w_components\",\"include\":[\"full1\",\"full2\",\"begin.*\",\".*end\",\".*contain.*\"],\"min_doc_count\":1,\"size\":2147483647}}},\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_integ_type\":[\"jira\"]}},{\"terms\":{\"w_components\":[\"full1\",\"full2\"]}},{\"wildcard\":{\"w_components\":{\"wildcard\":\"begin*\"}}},{\"wildcard\":{\"w_components\":{\"wildcard\":\"*end\"}}},{\"wildcard\":{\"w_components\":{\"wildcard\":\"*contain*\"}}},{\"term\":{\"w_is_active\":{\"value\":true}}}]}}}";
        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        String actual = ESAggResultUtils.getQueryString(searchRequest);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testAcrossComponent() throws IOException {
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.component)
                .filterAcrossValues(true)
                .isActive(true)
                .build();

        String expected = "{\"aggregations\":{\"across_component\":{\"aggregations\":{\"total_story_points\":{\"sum\":{\"field\":\"w_story_points\"}},\"bucket_pagination\":{\"bucket_sort\":{\"from\":0,\"size\":90}},\"mean_story_points\":{\"avg\":{\"field\":\"w_story_points\",\"missing\":0}}},\"terms\":{\"field\":\"w_components\",\"min_doc_count\":1,\"size\":2147483647}}},\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_integ_type\":[\"jira\"]}},{\"term\":{\"w_is_active\":{\"value\":true}}}]}}}";
        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        String actual = ESAggResultUtils.getQueryString(searchRequest);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testAcrossLabel() throws IOException {
        JiraIssuesFilter jiraIssuesFilter = JiraIssuesFilter.builder()
                .across(DISTINCT.label)
                .filterAcrossValues(true)
                .isActive(true)
                .build();

        String expected = "{\"aggregations\":{\"across_label\":{\"aggregations\":{\"total_story_points\":{\"sum\":{\"field\":\"w_story_points\"}},\"bucket_pagination\":{\"bucket_sort\":{\"from\":0,\"size\":90}},\"mean_story_points\":{\"avg\":{\"field\":\"w_story_points\",\"missing\":0}}},\"terms\":{\"field\":\"w_labels\",\"min_doc_count\":1,\"size\":2147483647}}},\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_integ_type\":[\"jira\"]}},{\"term\":{\"w_is_active\":{\"value\":true}}}]}}}";
        SearchRequest searchRequest = buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, null, false, null,
                indexName, false, null, null, false).build();
        String actual = ESAggResultUtils.getQueryString(searchRequest);
        Assertions.assertEquals(expected, actual);
    }
}
