package io.levelops.faceted_search.query_builders;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class EsScmCommitQueryBuilderTest {

    @Test
    public void testBuildAggsConditions_AcrossTrend()
    {
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().
                committedAtRange(ImmutablePair.of(1695859200L, 1698451199L))
                .across(ScmCommitFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.month).build();
        Map<String, Aggregation> aggConditions = EsScmCommitQueryBuilder.buildAggsConditions(scmCommitFilter);
        DateHistogramAggregation dateHistogram = aggConditions.get("across_trend").dateHistogram();
        Assert.assertNull(dateHistogram.offset());
        Assert.assertEquals("c_committed_at",dateHistogram.field());
        Assert.assertEquals("UTC",dateHistogram.timeZone());
        Assert.assertEquals(CalendarInterval.Month,dateHistogram.calendarInterval());
    }

}
