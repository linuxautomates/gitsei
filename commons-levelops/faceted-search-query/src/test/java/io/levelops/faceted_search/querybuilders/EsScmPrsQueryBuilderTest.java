package io.levelops.faceted_search.querybuilders;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.faceted_search.utils.ESAggResultUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder.INCLUDE_CONDITIONS;

public class EsScmPrsQueryBuilderTest {


    @Test
    public void testCreateCodeChangeCondition_withSingleCodeChanges() throws IOException {
        List<Query> queryList = new ArrayList<>();
        Query rangeQuery =  new RangeQuery.Builder().field("pr_changes")
                .lte(JsonData.of(500))
                .build()._toQuery();
        queryList.add(rangeQuery);
        queryList.add(Query.of(q -> q.bool( b-> b.mustNot( e1 -> e1.exists(e -> e.field("pr_changes"))))));
        Query expectedQuery = Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .should(queryList))));

        SearchRequest searchRequest = new SearchRequest.Builder().query(expectedQuery).build();
        String expectedQueryString = ESAggResultUtils.getQueryString(searchRequest);

        ScmPrFilter scmPrFilter = ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "500"))
                .commentDensitySizeConfig(Map.of())
                .build();
        Map<String, List<Query>> result = EsScmPrsQueryBuilder.buildQueryConditionsForPrs(scmPrFilter);
        String actualQueryString = ESAggResultUtils.getQueryString(
          new SearchRequest.Builder().query(result.get(INCLUDE_CONDITIONS).get(0)).build()
        );
        Assert.assertEquals(expectedQueryString,actualQueryString);
    }

    @Test
    public void testCreateCodeChangeCondition_withMultipleCodeChanges() throws IOException {
        List<Query> queryList = new ArrayList<>();
        queryList.add( new RangeQuery.Builder().field("pr_changes")
                .lte(JsonData.of(500))
                .build()._toQuery());
        queryList.add(Query.of(q -> q.bool( b-> b.mustNot( e1 -> e1.exists(e -> e.field("pr_changes"))))));
        queryList.add( new RangeQuery.Builder().field("pr_changes")
                .gt(JsonData.of(500))
                .lte(JsonData.of(1000))
                .build()._toQuery());
        Query expectedQuery = Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .should(queryList))));

        SearchRequest searchRequest = new SearchRequest.Builder().query(expectedQuery).build();
        String expectedQueryString = ESAggResultUtils.getQueryString(searchRequest);

        ScmPrFilter scmPrFilter = ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small","medium"))
                .codeChangeSizeConfig(Map.of("small", "500","medium","1000"))
                .commentDensitySizeConfig(Map.of())
                .build();
        Map<String, List<Query>> result = EsScmPrsQueryBuilder.buildQueryConditionsForPrs(scmPrFilter);
        String actualQueryString = ESAggResultUtils.getQueryString(
                new SearchRequest.Builder().query(result.get(INCLUDE_CONDITIONS).get(0)).build()
        );
        Assert.assertEquals(expectedQueryString,actualQueryString);
    }

    @Test
    public void testCreateCodeChangeCondition_withMultipleCodeChangesAndCommentDensity() throws IOException {
        List<Query> queryList = new ArrayList<>();
        queryList.add( new RangeQuery.Builder().field("pr_changes")
                .lte(JsonData.of(500))
                .build()._toQuery());
        queryList.add(Query.of(q -> q.bool( b-> b.mustNot( e1 -> e1.exists(e -> e.field("pr_changes"))))));
        queryList.add( new RangeQuery.Builder().field("pr_changes")
                .gt(JsonData.of(500))
                .lte(JsonData.of(1000))
                .build()._toQuery());
        Query expectedQuery = Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .should(queryList))));

        SearchRequest searchRequest = new SearchRequest.Builder().query(expectedQuery).build();
        String expectedCodeChangeQueryString = ESAggResultUtils.getQueryString(searchRequest);

        Query expectedCommentDensityQuery = Query.of(q -> q
                .bool(BoolQuery.of(b -> b
                        .should(
                                List.of(new RangeQuery.Builder().field("pr_comment_count")
                                        .lte(JsonData.of(100))
                                        .build()._toQuery(),
                                        new RangeQuery.Builder().field("pr_comment_count")
                                                .gt(JsonData.of(100))
                                                .lte(JsonData.of(1000))
                                                .build()._toQuery()
                                        )
                        ))));

        String expectedCommentDensityQueryString = ESAggResultUtils.getQueryString(
                new SearchRequest.Builder().query(expectedCommentDensityQuery).build()
        );

        ScmPrFilter scmPrFilter = ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChanges(List.of("small","medium"))
                .commentDensities(List.of("shallow","good"))
                .codeChangeSizeConfig(Map.of("small", "500","medium","1000"))
                .commentDensitySizeConfig(Map.of("shallow","100","good","1000"))
                .build();
        Map<String, List<Query>> result = EsScmPrsQueryBuilder.buildQueryConditionsForPrs(scmPrFilter);
        String actualCodeChangeQueryString = ESAggResultUtils.getQueryString(
                new SearchRequest.Builder().query(result.get(INCLUDE_CONDITIONS).get(0)).build()
        );
        String actualCommentDensityQueryString = ESAggResultUtils.getQueryString(
                new SearchRequest.Builder().query(result.get(INCLUDE_CONDITIONS).get(1)).build()
        );
        Assert.assertEquals(expectedCodeChangeQueryString,actualCodeChangeQueryString);
        Assert.assertEquals(expectedCommentDensityQueryString,actualCommentDensityQueryString);
    }
}
