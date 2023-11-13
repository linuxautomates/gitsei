package io.levelops.integrations.template.sources;

import java.util.Map;
import java.util.stream.Stream;

import java.util.Collections;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.template.client.TemplateClientFactory;
import io.levelops.integrations.template.models.TemplateDataQuery;

public class TemplateApiDataSource<D extends TemplateDataQuery> implements DataSource<Map, D>{

    private TemplateClientFactory clientFactory;

    public TemplateApiDataSource(final TemplateClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<Map> fetchOne(D query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        
        var client = clientFactory.get(query.getIntegrationKey());
        var results = client.getTemplateResourceExample(query);
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
        var results = client.getTemplateResourceExample(query);
        if(CollectionUtils.isEmpty(results) ){
            return Collections.<Data<Map>>emptyList().stream();
        }
        return results.stream().map(BasicData.mapper(Map.class));
    }
}