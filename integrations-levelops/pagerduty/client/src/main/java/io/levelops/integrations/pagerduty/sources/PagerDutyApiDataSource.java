package io.levelops.integrations.pagerduty.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.pagerduty.client.PagerDutyClientFactory;
import io.levelops.integrations.pagerduty.models.PagerDutyDataQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public class PagerDutyApiDataSource<T,D extends PagerDutyDataQuery> implements DataSource<Map, D> {

    private PagerDutyClientFactory clientFactory;
    private Class<T> claz;

    public PagerDutyApiDataSource(final PagerDutyClientFactory clientFactory, Class<T> claz) {
        this.clientFactory = clientFactory;
        this.claz = claz;
    }

    @Override
    public Data<Map> fetchOne(D query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        
        var client = clientFactory.get(query.getIntegrationKey());
        var results = client.getPagerDutyItems(query, claz);
        if( CollectionUtils.isEmpty(results) ){
            return BasicData.empty(Map.class);
        }
        return BasicData.of(Map.class, results.get(0));
    }

    @Override
    public Stream<Data<Map>> fetchMany(D query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        
        var client = clientFactory.get(query.getIntegrationKey());
        var results = client.getPagerDutyItems(query, claz);
        if(CollectionUtils.isEmpty(results) ){
            return Collections.<Data<Map>>emptyList().stream();
        }
        return results.stream().map(BasicData.mapper(Map.class));
    }
}