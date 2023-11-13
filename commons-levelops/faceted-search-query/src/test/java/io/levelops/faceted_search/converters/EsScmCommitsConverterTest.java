package io.levelops.faceted_search.converters;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.util.DateTime;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.IntStream;

public class EsScmCommitsConverterTest {

    @Test
    public void testGetAggResultForCodingDaysReport_ForWeek() {
        List<DateHistogramBucket> dateHistogramBuckets=new ArrayList<>();
        final int days=10;
        IntStream.range(0,days).forEach(day->{
            dateHistogramBuckets.add(new DateHistogramBucket.Builder().key(DateTime.ofEpochMilli(1696291200000L)).keyAsString("2023-10-03T00:00:00.000Z").docCount(6).build());
        });

        DateHistogramAggregate aggregate = AggregateBuilders.dateHistogram().buckets(x -> x.array(dateHistogramBuckets)).build();
        SumAggregate sumAgg = new SumAggregate.Builder().value(826222.0).build();
        TDigestPercentilesAggregate percentAgg = new TDigestPercentilesAggregate.Builder().values(new Percentiles.Builder().keyed(Map.of("50.0", String.valueOf(12.0))).build()).build();
        NestedAggregate nestedAgg = new NestedAggregate.Builder().aggregations(
                Map.of("commit_size", sumAgg._toAggregate(), "median_percentile", percentAgg._toAggregate())
        ).docCount(12795).build();

        StringTermsBucket stringTermsBucket = new StringTermsBucket.Builder().aggregations(
                Map.of("coding_days", aggregate._toAggregate(),"commit_size_change",nestedAgg._toAggregate()))
                .docCount(1000).key("key").build();
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().
                committedAtRange(ImmutablePair.of(1695859200L, 1698451199L))
                .across(ScmCommitFilter.DISTINCT.repo_id)
                .aggInterval(AGG_INTERVAL.week).build();
        Buckets<StringTermsBucket> objectBuckets = Buckets.of(x -> x.array(Collections.singletonList(stringTermsBucket)));

        StringTermsAggregate stringTermsAggregate = AggregateBuilders.sterms().buckets(objectBuckets).sumOtherDocCount(1322).build();
        SearchResponse<Void> searchResponse = SearchResponse.searchResponseOf(x -> x.aggregations(Map.of("across_repo_id",stringTermsAggregate._toAggregate())).took(21).timedOut(false).shards(Mockito.mock(ShardStatistics.class)).hits(Mockito.mock(HitsMetadata.class)));
        List<DbAggregationResult> aggResultForCodingDaysReport = EsScmCommitsConverter.getAggResultForCodingDaysReport(searchResponse, scmCommitFilter, Map.of());
        double expectedMean= ((double)days/30)*7;
        Assert.assertEquals(Double.valueOf(String.format("%.2f",expectedMean)), aggResultForCodingDaysReport.get(0).getMean());
    }

    @Test
    public void testGetAggResultForCodingDaysReport_ForMonth() {
        List<DateHistogramBucket> dateHistogramBuckets=new ArrayList<>();
        final int days=24;
        IntStream.range(0,days).forEach(day->{
            dateHistogramBuckets.add(new DateHistogramBucket.Builder().key(DateTime.ofEpochMilli(1696291200000L)).keyAsString("2023-10-03T00:00:00.000Z").docCount(6).build());
        });

        DateHistogramAggregate aggregate = AggregateBuilders.dateHistogram().buckets(x -> x.array(dateHistogramBuckets)).build();
        SumAggregate sumAgg = new SumAggregate.Builder().value(826222.0).build();
        TDigestPercentilesAggregate percentAgg = new TDigestPercentilesAggregate.Builder().values(new Percentiles.Builder().keyed(Map.of("50.0", String.valueOf(12.0))).build()).build();
        NestedAggregate nestedAgg = new NestedAggregate.Builder().aggregations(
                Map.of("commit_size", sumAgg._toAggregate(), "median_percentile", percentAgg._toAggregate())
        ).docCount(12795).build();

        StringTermsBucket stringTermsBucket = new StringTermsBucket.Builder().aggregations(
                        Map.of("coding_days", aggregate._toAggregate(),"commit_size_change",nestedAgg._toAggregate()))
                .docCount(1000).key("key").build();
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().
                committedAtRange(ImmutablePair.of(1695859200L, 1698451199L))
                .across(ScmCommitFilter.DISTINCT.repo_id)
                .aggInterval(AGG_INTERVAL.month).build();
        Buckets<StringTermsBucket> objectBuckets = Buckets.of(x -> x.array(Collections.singletonList(stringTermsBucket)));

        StringTermsAggregate stringTermsAggregate = AggregateBuilders.sterms().buckets(objectBuckets).sumOtherDocCount(1322).build();
        SearchResponse<Void> searchResponse = SearchResponse.searchResponseOf(x -> x.aggregations(Map.of("across_repo_id",stringTermsAggregate._toAggregate())).took(21).timedOut(false).shards(Mockito.mock(ShardStatistics.class)).hits(Mockito.mock(HitsMetadata.class)));
        List<DbAggregationResult> aggResultForCodingDaysReport = EsScmCommitsConverter.getAggResultForCodingDaysReport(searchResponse, scmCommitFilter, Map.of());
        double expectedMean= ((double)days/30)*30;
        Assert.assertEquals(Double.valueOf(String.format("%.2f",expectedMean)), aggResultForCodingDaysReport.get(0).getMean());
    }

}