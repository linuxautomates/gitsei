package io.levelops.faceted_search.utils;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.PercentilesBucketAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TDigestPercentilesAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonFactory;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.StringWriter;

@Log4j2
public class ESAggResultUtils {
    public static Long getPercentileFromPercentileBucket(PercentilesBucketAggregate percentilesBucket, String percentile) {
        if(percentilesBucket == null) {
            return null;
        }

        String value = percentilesBucket.values().keyed().getOrDefault(percentile, null);
        if (value == null) {
            return null;
        }

        try {
            double val = Double.parseDouble(value);
            return (long) val;
        } catch (NumberFormatException e) {
            log.warn("Error parsing percentile bucket for " + percentile, e);
            return null;
        }
    }

    public static Long getPercentileFromTDigestPercentile(TDigestPercentilesAggregate tDigestPercentiles, String percentile) {
        if(tDigestPercentiles == null) {
            return null;
        }

        String value = tDigestPercentiles.values().keyed().getOrDefault(percentile, null);
        if (value == null) {
            return null;
        }

        try {
            double val = Double.parseDouble(value);
            return (long) val;
        } catch (NumberFormatException e) {
            log.warn("Error parsing tdigest percentile bucket for " + percentile, e);
            return null;
        }
    }

    public static String getQueryString(JsonpSerializable searchRequestOrQuery) throws IOException {
        StringWriter writer = new StringWriter();
        try (final JacksonJsonpGenerator generator = new JacksonJsonpGenerator(new JsonFactory().createGenerator(writer))) {
            searchRequestOrQuery.serialize(generator, new JacksonJsonpMapper());
        }
        return writer.toString();
    }

    public static String getQueryStringSafe(JsonpSerializable searchRequestOrQuery)  {
        StringWriter writer = new StringWriter();
        try {
            try (final JacksonJsonpGenerator generator = new JacksonJsonpGenerator(new JsonFactory().createGenerator(writer))) {
                searchRequestOrQuery.serialize(generator, new JacksonJsonpMapper());
            }
            return writer.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static SortOrder getSortOrder(JiraIssuesFilter filter) {
        if(filter.getSort() != null) {
            SortingOrder sortingOrder = filter.getSort().values().stream().findFirst().orElse(SortingOrder.DESC);
            return sortingOrder.name().equalsIgnoreCase("DESC") ? SortOrder.Desc : SortOrder.Asc;
        }
        return SortOrder.Desc;
    }

    public static SortOrder getSortOrder(WorkItemsFilter filter) {
        if(filter.getSort() != null) {
            SortingOrder sortingOrder = filter.getSort().values().stream().findFirst().orElse(SortingOrder.DESC);
            return sortingOrder.name().equalsIgnoreCase("DESC") ? SortOrder.Desc : SortOrder.Asc;
        }
        return SortOrder.Desc;
    }

}