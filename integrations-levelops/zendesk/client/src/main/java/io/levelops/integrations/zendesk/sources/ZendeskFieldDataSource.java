package io.levelops.integrations.zendesk.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.models.Field;
import io.levelops.integrations.zendesk.models.ZendeskTicketQuery;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import java.util.stream.Stream;

@Log4j2
public class ZendeskFieldDataSource implements DataSource<Field, ZendeskTicketQuery> {

    private final ZendeskClientFactory clientFactory;

    public ZendeskFieldDataSource(ZendeskClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<Field> fetchOne(ZendeskTicketQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Field>> fetchMany(ZendeskTicketQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        log.debug("fetching fields for integration key: {}", query.getIntegrationKey());
        try {
            return clientFactory.get(query.getIntegrationKey())
                    .getTicketFields()
                    .getFields()
                    .stream()
                    .map(BasicData.mapper(Field.class));
        } catch (ZendeskClientException e) {
            log.error("fetchMany: encountered Zendesk client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered Zendesk client exception for " + query.getIntegrationKey(), e);
        }
    }
}
