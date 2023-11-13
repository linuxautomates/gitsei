package io.levelops.integrations.tenable.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.tenable.client.TenableClient;
import io.levelops.integrations.tenable.client.TenableClientException;
import io.levelops.integrations.tenable.client.TenableClientFactory;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import io.levelops.integrations.tenable.models.WASResponse;
import lombok.extern.log4j.Log4j2;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tenable's implementation of the {@link DataSource}. This class is used for web application scanning vulnerabilities detail.
 */
@Log4j2
public class TenableWASDataSource implements DataSource<WASResponse.Data, TenableScanQuery> {

    private final TenableClientFactory tenableClientFactory;
    private final Integer PAGE_SIZE = 50;
    private final String ORDERING = "desc";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public TenableWASDataSource(TenableClientFactory tenableClientFactory) {
        this.tenableClientFactory = tenableClientFactory;
    }

    @Override
    public Data<WASResponse.Data> fetchOne(TenableScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<WASResponse.Data>> fetchMany(TenableScanQuery query) throws FetchException {
        TenableClient tenableClient = tenableClientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(0, 1, page -> {
            try {
                return getWas(tenableClient, query.getSince(), page, PAGE_SIZE);
            } catch (TenableClientException e) {
                log.error("Encountered tenable client error for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered tenable client error for " +
                        "integration key: " + query.getIntegrationKey(), e);
            }
        });
    }

    private List<Data<WASResponse.Data>> getWas(TenableClient tenableClient, Long lastScanTime, Integer page,
                                                Integer size) throws TenableClientException {
        WASResponse wasResponse = tenableClient.getWasResponse(page, size, ORDERING);
        return wasResponse.getData().stream()
                .filter(data -> {
                    try {
                        Date createdAt = dateFormat.parse(data.getCreatedAt());
                        long createAtTS = createdAt.toInstant().getEpochSecond();
                        return createAtTS > lastScanTime;
                    } catch (ParseException e) {
                        log.error("Encountered Error while parsing date: " + e.getMessage(), e);
                        return false;
                    }
                })
                .map(BasicData.mapper(WASResponse.Data.class))
                .collect(Collectors.toList());
    }
}
