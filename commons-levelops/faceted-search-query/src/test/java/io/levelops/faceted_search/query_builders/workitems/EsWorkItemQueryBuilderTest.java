package io.levelops.faceted_search.query_builders.workitems;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static io.levelops.faceted_search.querybuilders.workitems.EsWorkItemQueryBuilder.buildSearchRequest;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class EsWorkItemQueryBuilderTest {

    public static final String indexName = "workitems_zdtenant_1645142400";

    private void checkMustSearchRequest(SearchRequest expectedSearchRequest, SearchRequest actualSearchRequest,
                                        int mustIdxNum, int idxNum) {
        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());

        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(mustIdxNum)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(mustIdxNum)._kind());
        assertThat(((TermsQuery) ((BoolQuery) expectedSearchRequest.query()._get())
                .must().get(mustIdxNum)._get()).field())
                .isEqualTo(((TermsQuery) ((BoolQuery) actualSearchRequest.query()._get())
                        .must().get(mustIdxNum)._get()).field());
        assertThat(expectedSearchRequest.query().bool().must().get(mustIdxNum).terms()
                .terms().value().get(idxNum).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().must().get(mustIdxNum)
                        .terms().terms().value().get(idxNum).stringValue());
    }

    private void checkMustNotSearchRequest(SearchRequest expectedSearchRequest, SearchRequest actualSearchRequest,
                                           int mustNotIdxNum, int idxNum) {
        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).mustNot().get(mustNotIdxNum)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).mustNot().get(mustNotIdxNum)._kind());
        assertThat(((TermsQuery) ((BoolQuery) expectedSearchRequest.query()._get())
                .mustNot().get(mustNotIdxNum)._get()).field())
                .isEqualTo(((TermsQuery) ((BoolQuery) actualSearchRequest.query()._get())
                        .mustNot().get(mustNotIdxNum)._get()).field());
        assertThat(expectedSearchRequest.query().bool().mustNot().get(mustNotIdxNum)
                .terms().terms().value().get(idxNum).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().mustNot().get(mustNotIdxNum)
                        .terms().terms().value().get(idxNum).stringValue());
    }

    private void checkRangeSearchRequest(SearchRequest expectedSearchRequest, SearchRequest actualSearchRequest, Integer mustIdx) {
        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(0)._kind());
        assertThat(((RangeQuery) ((BoolQuery) expectedSearchRequest.query()._get()).must().get(mustIdx)._get()).field())
                .isEqualTo(((RangeQuery) ((BoolQuery) actualSearchRequest.query()._get()).must().get(mustIdx)._get()).field());
        assertThat(expectedSearchRequest.query().bool().must().get(mustIdx).range().gt().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(mustIdx).range().gt().toString());
        assertThat(expectedSearchRequest.query().bool().must().get(mustIdx).range().lt().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(mustIdx).range().lt().toString());
    }

    public void checkAggCondition(SearchRequest expectedSearchRequest, SearchRequest actualSearchRequest,
                                  String aggName) {
        Assertions.assertNotNull(expectedSearchRequest.aggregations());
        Assertions.assertNotNull(actualSearchRequest.aggregations());
        assertThat(expectedSearchRequest.aggregations().get(aggName)._kind())
                .isEqualTo(actualSearchRequest.aggregations().get(aggName)._kind());
        assertThat(expectedSearchRequest.aggregations().get(aggName).terms().field())
                .isEqualTo(actualSearchRequest.aggregations().get(aggName).terms().field());
    }

    @Test
    public void integrationIdTest() {
        String expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_integration_id\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString = expectedString;
        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .integrationIds(List.of("1")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);
    }

    @Test
    public void partialMatchTest() {
        String expectedString;
        SearchRequest actualSearchRequest;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "              \"wildcard\": {" +
                "                   \"w_project\": {" +
                "                     \"value\": \"foo*\"" +
                "                   }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString43 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString43))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .partialMatch(Map.of("workitem_project", Map.of("$begins", "foo"))).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());

        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(0)._kind());
        assertThat(((WildcardQuery) ((BoolQuery) expectedSearchRequest.query()._get()).must().get(1)._get()).field())
                .isEqualTo(((WildcardQuery) ((BoolQuery) actualSearchRequest.query()._get())
                        .must().get(1)._get()).field());
        assertThat(expectedSearchRequest.query().bool().must().get(1).wildcard().value())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).wildcard().value());
    }

    @Test
    public void customFieldsTest() {
        SearchRequest expectedSearchRequest;
        String expectedString;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\": {" +
                "    \"bool\": {" +
                "      \"must\": [" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
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
                "}";

        String finalExpectedString40 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString40))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .customFields(Map.of("custom_field0111", List.of("foo"))).build(),
                WorkItemsMilestoneFilter.builder().build(),
                List.of(), List.of(), null, List.of(DbWorkItemField.builder()
                        .name("custom_field0111")
                        .fieldKey("custom_field0111")
                        .fieldType("string")
                        .build()),
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(0)._kind());
        assertThat(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().field())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().field());
        assertThat(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\": {" +
                "    \"bool\": {" +
                "      \"must_not\": [" +
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
                "      \"must\": [" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "      ]" +
                "    }" +
                "  }" +
                "}";

        String finalExpectedString41 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString41))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeCustomFields(Map.of("custom_field0111", List.of("foo"))).build(),
                WorkItemsMilestoneFilter.builder().build(),
                List.of(), List.of(), null, List.of(DbWorkItemField.builder()
                        .name("custom_field0111")
                        .fieldKey("custom_field0111")
                        .fieldType("string")
                        .build()),
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).mustNot().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).mustNot().get(0)._kind());
        assertThat(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                .terms().field())
                .isEqualTo(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                        .terms().field());
        assertThat(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                .terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                        .terms().terms().value().get(0).stringValue());
        assertThat(expectedSearchRequest.query().bool().must().get(0).terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().must().get(0).terms().terms().value().get(0).stringValue());
    }

    @Test
    public void attributesTest() {
        SearchRequest expectedSearchRequest;
        String expectedString;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "            \"nested\": {" +
                "              \"path\": \"w_attributes\"," +
                "              \"query\": {" +
                "                \"bool\": {" +
                "                  \"must\": [" +
                "                    {" +
                "                      \"terms\": {" +
                "                        \"w_attributes.name\": [" +
                "                          \"organization\"" +
                "                        ]" +
                "                      }" +
                "                    }," +
                "                    {" +
                "                      \"terms\": {" +
                "                        \"w_attributes.str\": [" +
                "                          \"foo\"" +
                "                        ]" +
                "                      }" +
                "                    }" +
                "                  ]" +
                "                }" +
                "               }" +
                "             }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString38 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString38))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .attributes(Map.of("organization", List.of("foo"))).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(0)._kind());
        assertThat(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().field())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().field());
        assertThat(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .terms().terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {" +
                "            \"nested\": {" +
                "              \"path\": \"w_attributes\"," +
                "              \"query\": {" +
                "                \"bool\": {" +
                "                  \"must\": [" +
                "                    {" +
                "                      \"terms\": {" +
                "                        \"w_attributes.name\": [" +
                "                          \"organization\"" +
                "                        ]" +
                "                      }" +
                "                    }," +
                "                    {" +
                "                      \"terms\": {" +
                "                        \"w_attributes.str\": [" +
                "                          \"foo\"" +
                "                        ]" +
                "                      }" +
                "                    }" +
                "                  ]" +
                "                }" +
                "               }" +
                "             }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString39 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString39))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeAttributes(Map.of("organization", List.of("foo"))).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).mustNot().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).mustNot().get(0)._kind());
        assertThat(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                .terms().field())
                .isEqualTo(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                        .terms().field());
        assertThat(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                .terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0)
                        .terms().terms().value().get(0).stringValue());
        assertThat(expectedSearchRequest.query().bool().must().get(0).terms().terms().value().get(0).stringValue())
                .isEqualTo(actualSearchRequest.query().bool().must().get(0).terms().terms().value().get(0).stringValue());
    }

    @Test
    public void rangeQueryTest() {
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        String expectedString;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"range\":{" +
                "               \"w_story_points\":{" +
                "                   \"gt\": 1.0," +
                "                   \"lt\": 2.0," +
                "                   \"time_zone\": \"UTC\"" +
                "                }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString36 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString36))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .storyPointsRange(ImmutablePair.of(1.0f, 2.0f)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkRangeSearchRequest(expectedSearchRequest, actualSearchRequest, 1);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"range\":{" +
                "               \"w_resolved_at\":{" +
                "                   \"gt\": 1," +
                "                   \"lt\": 2," +
                "                   \"time_zone\": \"UTC\"" +
                "                }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString31 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString31))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemResolvedRange(ImmutablePair.of(1L, 2L)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkRangeSearchRequest(expectedSearchRequest, actualSearchRequest, 1);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"range\":{" +
                "               \"w_created_at\":{" +
                "                   \"gt\": 1," +
                "                   \"lt\": 2," +
                "                   \"time_zone\": \"UTC\"" +
                "                }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString33 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString33))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemCreatedRange(ImmutablePair.of(1L, 2L)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkRangeSearchRequest(expectedSearchRequest, actualSearchRequest, 1);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"range\":{" +
                "               \"w_updated_at\":{" +
                "                   \"gt\": 1," +
                "                   \"lt\": 2," +
                "                   \"time_zone\": \"UTC\"" +
                "                }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString34 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString34))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemUpdatedRange(ImmutablePair.of(1L, 2L)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkRangeSearchRequest(expectedSearchRequest, actualSearchRequest, 1);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"range\":{" +
                "               \"w_ingested_at\":{" +
                "                   \"gt\": 1," +
                "                   \"lt\": 2," +
                "                   \"time_zone\": \"UTC\"" +
                "                }" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString35 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString35))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .snapshotRange(ImmutablePair.of(1L, 2L)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkRangeSearchRequest(expectedSearchRequest, actualSearchRequest, 1);
    }

    @Test
    public void stagesTest() {
        String expectedString;
        SearchRequest actualSearchRequest;
        SearchRequest expectedSearchRequest;
        expectedString = "{\"aggregations\":{},\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_integ_type\":[\"issue_mgmt\"]}},{\"nested\":{\"path\":\"w_hist_statuses\",\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_hist_statuses.status\":[\"sam\"]}}]}}}}],\"must_not\":[]}}}";

        String finalExpectedString15 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString15))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .stages(List.of("sam")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"nested\":{\"path\":\"w_hist_statuses\",\"query\":{\"bool\":{\"must\":[{\"terms\":{\"w_hist_statuses.status\":[\"Completed\"]}}]}}}}" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString30 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString30))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeStages(List.of("Completed"))
                        .calculation(WorkItemsFilter.CALCULATION.stage_bounce_report)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void firstAssigneeTest() {
        SearchRequest actualSearchRequest;
        String expectedString;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_first_assignee.id\":[\"sam\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString14 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString14))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .firstAssignees(List.of("sam")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_first_assignee.id\":[\"sam\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString29 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString29))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeFirstAssignees(List.of("sam")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void reporterTest() {
        SearchRequest expectedSearchRequest;
        String expectedString;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_reporter.id\":[\"sam\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString13 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString13))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .reporters(List.of("sam")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_reporter.id\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString28 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString28))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeReporters(List.of("1")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void projectTest() {
        SearchRequest actualSearchRequest;
        String expectedString;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_project\":[\"Agile\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString12 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString12))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .projects(List.of("Agile")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_project\":[\"Agile\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString27 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString27))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeProjects(List.of("Agile")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void parentWorkItemIdTest() {
        String expectedString;
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_parent_workitem_id\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString11 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString11))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .parentWorkItemIds(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_parent_workitem_id\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString26 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString26))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeParentWorkItemIds(List.of("1")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void statusCategoryTest() {
        String expectedString;
        SearchRequest actualSearchRequest;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_status_category\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString10 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString10))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .statusCategories(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_status_category\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString25 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString25))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeStatusCategories(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void labelsTest() {
        String expectedString;
        SearchRequest actualSearchRequest;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_labels\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString8 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString8))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .labels(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_labels\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString22 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString22))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeLabels(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void workItemTypeTest() {
        SearchRequest expectedSearchRequest;
        String expectedString;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_workitem_type\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString7 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString7))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemTypes(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_workitem_type\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString21 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString21))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeWorkItemTypes(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void stateTest() {
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        String expectedString;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_status\":[\"good\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString6 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString6))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .statuses(List.of("good")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_status\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "        ]" +
                "    }" +
                "}}";

        String finalExpectedString20 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString20))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeStatuses(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void versionsTest() {
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        String expectedString;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_versions.name\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString5 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString5))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .versions(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_versions.name\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString19 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString19))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeVersions(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void assigneeTest() {
        SearchRequest expectedSearchRequest;
        String expectedString;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_assignee.id\":[\"532103e3-2a42-4f20-a23c-84eaec029574\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString05 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString05))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .assignees(List.of("532103e3-2a42-4f20-a23c-84eaec029574")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_assignee.id\":[\"sam\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString019 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString019))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeAssignees(List.of("sam")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{ " +
                "               \"w_assignee.display_name\": [\"_UNASSIGNED_\"]" +
                "               }" +
                "           }" +
                "        ]" +
                "    }" +
                "}}";

        String finalExpectedString37 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString37))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .extraCriteria(List.of(WorkItemsFilter.EXTRA_CRITERIA.no_assignee)).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());
        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        Assertions.assertEquals(expectedSearchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue(), actualSearchRequest.query().bool().must().get(1).terms().terms().value()
                .get(0).stringValue());
    }

    @Test
    public void epicTest() {
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        String expectedString;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_epic\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString4 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString4))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .epics(List.of("1")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_epic\":[\"2\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString18 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString18))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeEpics(List.of("2")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void priorityTest() {
        String expectedString;
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_priority\":[\"2\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString16 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString16))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludePriorities(List.of("2")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_priority\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString3 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString3))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .priorities(List.of("1")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);
    }

    @Test
    public void historicalSprintTest() {
        SearchRequest actualSearchRequest;
        String expectedString;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_hist_sprints.id\":[\"1\", \"3\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString2 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString2))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .sprintIds(List.of("1", "3")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_hist_sprints.id\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString24 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString24))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeSprintIds(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void workItemIdTest() {
        SearchRequest actualSearchRequest;
        String expectedString;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_workitem_id\":[\"2\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString17 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString17))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeWorkItemIds(List.of("2")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_workitem_id\":[\"1\", \"2\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString1 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString1))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemIds(List.of("1", "2")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 1);
    }

    @Test
    public void searchRequestMilestoneFilterBuilderTest() {
        String expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_integration_id\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString = expectedString;
        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .integrationIds(List.of("1"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.name\": [" +
                "                         \"sam\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString1 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString1))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .names(List.of("sam"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.state\": [" +
                "                         \"done\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString2 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString2))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .states(List.of("done"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.parent_name\": [" +
                "                         \"test1\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString3 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString3))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .parentFieldValues(List.of("test1"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.full_name\": [" +
                "                         \"parentNameAndName\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString04 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString04))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .fullNames(List.of("parentNameAndName"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_integration_id\":[\"1\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString4 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString4))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeIntegrationIds(List.of("1"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.name\": [" +
                "                         \"sam\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString5 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString5))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeNames(List.of("sam"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.state\": [" +
                "                         \"done\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString6 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString6))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeStates(List.of("done"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.parent_name\": [" +
                "                         \"test\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString7 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString7))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeParentFieldValues(List.of("test"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"terms\": {" +
                "                       \"w_milestones.full_name\": [" +
                "                         \"parentNameAndName\"" +
                "                       ]" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString08 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString08))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeFullNames(List.of("parentNameAndName"))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested()
                .query().bool().must().get(0).terms().field())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms().field());
        assertThat(actualSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                .terms().value().get(0).stringValue())
                .isEqualTo(expectedSearchRequest.query().bool().mustNot().get(0).nested().query().bool().must().get(0).terms()
                        .terms().value().get(0).stringValue());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"range\":{" +
                "                      \"w_milestones.start_time\":{" +
                "                          \"gte\": 1," +
                "                          \"lt\": 2," +
                "                          \"time_zone\": \"UTC\"" +
                "                       }" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString8 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString8))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .startedAtRange(ImmutablePair.of(1L, 2L))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).range().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).range().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().gte().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().gte().toString());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().lt().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().lt().toString());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"range\":{" +
                "                      \"w_milestones.completed_at\":{" +
                "                          \"gte\": 1," +
                "                          \"lt\": 2," +
                "                          \"time_zone\": \"UTC\"" +
                "                       }" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString9 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString9))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .completedAtRange(ImmutablePair.of(1L, 2L))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).range().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).range().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().gte().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().gte().toString());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().lt().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().lt().toString());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                     \"range\":{" +
                "                      \"w_milestones.end_time\":{" +
                "                          \"gte\": 1," +
                "                          \"lt\": 2," +
                "                          \"time_zone\": \"UTC\"" +
                "                       }" +
                "                     }" +
                "                   }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString10 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString10))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .endedAtRange(ImmutablePair.of(1L, 2L))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        assertThat(actualSearchRequest.query().bool().must().get(1).nested()
                .query().bool().must().get(0).range().field())
                .isEqualTo(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).range().field());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().gte().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().gte().toString());
        assertThat(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                .range().lt().toString())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0)
                        .range().lt().toString());

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {" +
                "           \"nested\": {" +
                "             \"path\": \"w_milestones\"," +
                "             \"query\": {" +
                "               \"bool\": {" +
                "                 \"must\": [" +
                "                   {" +
                "                      \"wildcard\": {" +
                "                         \"w_milestones.state\": {" +
                "                             \"value\": \"foo*\"" +
                "                         }" +
                "                      }" +
                "                    }" +
                "                  ]" +
                "                 }" +
                "                }" +
                "              }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString43 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString43))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder().build(),
                WorkItemsMilestoneFilter.builder()
                        .partialMatch(Map.of("workitem_state", Map.of("$begins", "foo")))
                        .build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.query());
        Assertions.assertNotNull(actualSearchRequest.query());

        assertThat(expectedSearchRequest.query()._kind()).isEqualTo(actualSearchRequest.query()._kind());
        assertThat(((BoolQuery) expectedSearchRequest.query()._get()).must().get(0)._kind())
                .isEqualTo(((BoolQuery) actualSearchRequest.query()._get()).must().get(0)._kind());
        assertThat(((WildcardQuery) ((BoolQuery) expectedSearchRequest.query()._get()).must().get(1).nested().query().bool().must().get(0)._get()).field())
                .isEqualTo(((WildcardQuery) ((BoolQuery) actualSearchRequest.query()._get())
                        .must().get(1).nested().query().bool().must().get(0)._get()).field());
        assertThat(expectedSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).wildcard().value())
                .isEqualTo(actualSearchRequest.query().bool().must().get(1).nested().query().bool().must().get(0).wildcard().value());
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
    }

    @Test
    public void SprintMappingTest() {
        String expectedString = "{" +
                " \"aggs\": {" +
                "    \"across_sprint_mapping\": {" +
                "      \"multi_terms\": {" +
                "        \"terms\": [" +
                "          {" +
                "            \"field\": \"w_integration_id\"" +
                "          }," +
                "          {" +
                "            \"field\": \"w_sprint_mappings.id\"" +
                "          }," +
                "          {" +
                "            \"field\": \"w_milestones.name\"" +
                "          }," +
                "          {" +
                "            \"field\": \"w_milestones.start_time\"" +
                "          }," +
                "          {" +
                "            \"field\": \"w_milestones.completed_at\"" +
                "          }" +
                "        ]" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(expectedString))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.sprint_mapping).build(),
                WorkItemsMilestoneFilter.builder().build(),
                List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        Assertions.assertNotNull(expectedSearchRequest.aggregations());
        Assertions.assertNotNull(actualSearchRequest.aggregations());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping")._kind())
                .isEqualTo(actualSearchRequest.aggregations().get("across_sprint_mapping")._kind());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                .terms().get(0).field())
                .isEqualTo(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                        .terms().get(0).field());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                .terms().get(1).field())
                .isEqualTo(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                        .terms().get(1).field());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                .terms().get(2).field())
                .isEqualTo(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                        .terms().get(2).field());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                .terms().get(3).field())
                .isEqualTo(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                        .terms().get(3).field());
        assertThat(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                .terms().get(4).field())
                .isEqualTo(expectedSearchRequest.aggregations().get("across_sprint_mapping").multiTerms()
                        .terms().get(4).field());
    }

    @Test
    public void CustomFieldsTest() {
        String expectedString;
        SearchRequest expectedSearchRequest;
        SearchRequest actualSearchRequest;
        expectedString = "{" +
                "  \"aggs\": {" +
                "    \"across_custom_fields\": {" +
                "      \"nested\": {" +
                "        \"path\": \"w_custom_fields\"" +
                "      }," +
                "      \"aggs\": {" +
                "        \"filter_custom_fields_name\": {" +
                "          \"filter\": {" +
                "            \"term\": {" +
                "              \"w_custom_fields.name\": \"custom_field123456\"" +
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
                "}";

        String finalExpectedString4 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString4))
        );
    }

    @Test
    public void StoryPointsTest() {
        String expectedString;
        SearchRequest actualSearchRequest;
        SearchRequest expectedSearchRequest;
        expectedString = "{" +
                "  \"aggs\": {" +
                "    \"across_story_points\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_story_points\"" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        String finalExpectedString04 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString04))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.story_points)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkAggCondition(expectedSearchRequest, actualSearchRequest, "across_story_points");
    }

    @Test
    public void fixVersionsTest() {
        String expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must\":[" +
                "          {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "           {\"terms\":{" +
                "               \"w_fix_versions.name\":[\"fix\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must_not\":[]" +
                "    }" +
                "}}";

        String finalExpectedString9 = expectedString;
        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString9))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .fixVersions(List.of("fix")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);

        expectedString = "{" +
                "\"query\":{" +
                "   \"bool\":{" +
                "       \"must_not\":[" +
                "           {\"terms\":{" +
                "               \"w_fix_versions.name\":[\"task\"]" +
                "               }" +
                "           }" +
                "       ]," +
                "       \"must\":[" +
                "           {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }" +
                "       ]" +
                "    }" +
                "}}";

        String finalExpectedString23 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString23))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .excludeFixVersions(List.of("task")).build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustNotSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);

        expectedString = "{" +
                "  \"aggs\": {" +
                "    \"across_fix_version\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_fix_versions.name\"" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        String finalExpectedString2 = expectedString;
        expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString2))
        );

        actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.fix_version)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkAggCondition(expectedSearchRequest, actualSearchRequest, "across_fix_version");

    }

    @Test
    public void LabelTest() {
        String expectedString = "{" +
                "  \"aggs\": {" +
                "    \"across_label\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_labels\"" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        String finalExpectedString1 = expectedString;
        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(finalExpectedString1))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.label)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkAggCondition(expectedSearchRequest, actualSearchRequest, "across_label");
    }

    @Test
    public void queryAndAggCombineTest() {
        String expectedString = "{" +
                "  \"query\": {" +
                "    \"bool\": {" +
                "      \"must\": [" +
                "          {\"terms\":{" +
                "               \"w_integ_type\":[\"issue_mgmt\"]" +
                "               }" +
                "           }," +
                "         {" +
                "          \"terms\": {" +
                "            \"w_workitem_id\": [" +
                "              \"1\", \"2\"" +
                "            ]" +
                "          }" +
                "        }," +
                "        {" +
                "          \"terms\": {" +
                "            \"w_versions.name\": [" +
                "              \"fix\"" +
                "            ]" +
                "          }" +
                "        }" +
                "      ]" +
                "    }" +
                "  }," +
                "  \"aggs\": {" +
                "    \"across_label\": {" +
                "      \"terms\": {" +
                "        \"field\": \"w_labels\"" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        SearchRequest expectedSearchRequest = SearchRequest.of(b -> b
                .index(indexName)
                .withJson(new StringReader(expectedString))
        );

        SearchRequest actualSearchRequest = buildSearchRequest(WorkItemsFilter.builder()
                        .workItemIds(List.of("1", "2"))
                        .versions(List.of("fix"))
                        .across(WorkItemsFilter.DISTINCT.label)
                        .build(),
                WorkItemsMilestoneFilter.builder().build(), List.of(), List.of(), null, null,
                false, null, null, indexName, false, null, null, false).build();

        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 0, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 0);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 1, 1);
        checkMustSearchRequest(expectedSearchRequest, actualSearchRequest, 2, 0);
        checkAggCondition(expectedSearchRequest, actualSearchRequest, "across_label");
    }
}
