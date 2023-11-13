package io.levelops.integrations.pagerduty.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.pagerduty.client.PagerDutyClient;
import io.levelops.integrations.pagerduty.client.PagerDutyClientException;
import io.levelops.integrations.pagerduty.client.PagerDutyClientFactory;
import io.levelops.integrations.pagerduty.models.PagerDutyIncidentsPage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class PagerDutyIncidentsDataSource implements DataSource<Map, PagerDutyIncidentsPage.Query> {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private final int PAGE_LIMIT = 100;
    private final int THRESHOLD_OFFSET = 9000;

    private final PagerDutyClientFactory clientFactory;

    public PagerDutyIncidentsDataSource(PagerDutyClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<Map> fetchOne(PagerDutyIncidentsPage.Query query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Map>> fetchMany(PagerDutyIncidentsPage.Query query) throws FetchException {
        PagerDutyClient pagerDutyClient = clientFactory.get(query.getIntegrationKey());
        AtomicInteger pageNumber = new AtomicInteger(query.getOffset());
        AtomicReference<String> since = new AtomicReference<>(query.getSince());
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> {
            int offset = pageNumber.get() * PAGE_LIMIT;
            PaginationUtils.CursorPageData<Data<Map>> pageData = getPageData(pagerDutyClient, query,
                    cursor, offset, since.get());
            if ((pageNumber.incrementAndGet() * PAGE_LIMIT) > THRESHOLD_OFFSET) {
                pageNumber.set(0);
                since.set(cursor);
            }
            return pageData;
        });
    }

    private PaginationUtils.CursorPageData<Data<Map>> getPageData(PagerDutyClient pagerDutyClient, PagerDutyIncidentsPage.Query query,
                                                                  String cursor, int offset, String since) {
        if (cursor == null) {
            return null;
        }
        PagerDutyIncidentsPage.Query newQuery = STARTING_CURSOR.equals(cursor) ? query :
                PagerDutyIncidentsPage.Query.builder()
                        .integrationKey(query.getIntegrationKey())
                        .since(since)
                        .until(query.getUntil())
                        .offset(offset)
                        .limit(PAGE_LIMIT)
                        .build();
        try {
            List<Map<String, Object>> incidents = pagerDutyClient.getPagerDutyIncidents(newQuery);
            String createdAt;
            if (incidents.size() == 0) {
                createdAt = null;
            } else {
                createdAt = (String) incidents.get(incidents.size() - 1).get("created_at");
            }
            return PaginationUtils.CursorPageData.<Data<Map>>builder()
                    .data(incidents.stream().map(BasicData.mapper(Map.class)).collect(Collectors.toList()))
                    .cursor(createdAt)
                    .build();
        } catch (PagerDutyClientException e) {
            log.error("getPageData: encountered pagerduty client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered pagerduty client exception for " + query.getIntegrationKey(), e);
        }
    }
}
