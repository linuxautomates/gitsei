package io.levelops.integrations.checkmarx.sources.cxsast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.exceptions.RuntimeInterruptedException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClient;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.models.CxQuery;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.levelops.integrations.checkmarx.models.CxXmlResults;
import io.levelops.integrations.checkmarx.models.DateAndTime;
import io.levelops.integrations.checkmarx.models.cxsast.CxSastReportAckResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
public class CxSastScanDataSource implements DataSource<CxSastScan, CxSastScanDataSource.CxSastScanQuery> {

    private static final String FINISHED = "Finished";
    private static final String POST_SCAN = "PostScan";
    private static final String CANCELED = "Canceled";
    private static final String FAILED = "Failed";
    private static final String NONE = "None";
    private final String REPORT_TYPE = "XML";
    private final CxSastClientFactory clientFactory;
    private final int MAX_ATTEMPTS = 10;
    private long maxWaitMs = 1000 * 60;
    private long multiplierMs = 400;
    private int count = 0;

    public CxSastScanDataSource(CxSastClientFactory factory) {
        this.clientFactory = factory;
    }

    @Override
    public Data<CxSastScan> fetchOne(CxSastScanQuery cxSastScanQuery) throws FetchException {
        throw new UnsupportedOperationException("Fetch one is not supported");
    }

    @Override
    public Stream<Data<CxSastScan>> fetchMany(CxSastScanQuery cxSastScanQuery) throws FetchException {
        CxSastClient client = clientFactory.get(cxSastScanQuery.getIntegrationKey());
        List<CxSastScan> cxSastScans = client.getScans();
        Stream<Data<CxSastScan>> stream = cxSastScans.stream()
                .filter(cxSastScan -> checkIfScanInInterval(cxSastScan, cxSastScanQuery))
                .filter(this::isScanFailed)
                .map(scan -> {
                    try {
                        return parseAndEnrichScan(client, scan);
                    } catch (CxSastClientException | JsonProcessingException | InterruptedException e) {
                        log.error("Encountered CxSast client error for integration key: "
                                + cxSastScanQuery.getIntegrationKey() + " as : " + e.getMessage(), e);
                        throw new RuntimeStreamException("Encountered CxSast client error for integration key: " +
                                cxSastScanQuery.getIntegrationKey(), e);
                    }
                })
                .map(BasicData.mapper(CxSastScan.class));
        return stream.filter(Objects::nonNull);
    }

    private CxSastScan parseAndEnrichScan(CxSastClient client, CxSastScan scan) throws CxSastClientException,
            JsonProcessingException, InterruptedException {
        if (!checkIfScanIsFinished(scan))
            return scan;
        CxXmlResults result;
        CxSastReportAckResponse cxSastReportAckResponse = client.registerScanReport(scan.getId(), REPORT_TYPE);
        String reportId = cxSastReportAckResponse.getReportId();
        while (!client.getScanReportStatus(reportId).getStatus().getValue().equals("Created")) {
            if (count > MAX_ATTEMPTS) {
                throw new CxSastClientException(String.format("Response not successful after retrying %d times",
                        MAX_ATTEMPTS));
            }
            sleepWithExponentialBackoff(count);
            count++;
        }
        result = client.getScanReport(reportId);
        List<CxQuery> cxQueries = IterableUtils.parseIterable(result.getQueries(),
                cxQuery -> {
                    List<String> category = parseCategories(cxQuery.getCategories());
                    cxQuery.setCategory(category);
                    return cxQuery;
                });
        result.setQueries(cxQueries);
        return scan.toBuilder()
                .report(result)
                .build();
    }

    private List<String> parseCategories(String categories) {
        if (StringUtils.isBlank(categories)) {
            return Collections.emptyList();
        }
        String[] category = categories.trim().split(";");
        List<String> cxQueryCategories = new ArrayList<>();
        List<String> cxQueryDescription = new ArrayList<>();
        for (String cat : category) {
            String[] fields = cat.trim().split(",");
            if (fields[0] != null) {
                cxQueryDescription.add(fields[0]);
            }
            if (fields.length > 1) {
                cxQueryCategories.add(fields[1]);
            }
        }
        return cxQueryCategories;
    }

    private boolean checkIfScanInInterval(CxSastScan scan, CxSastScanQuery cxSastScanQuery) {
        Date startedOn = Optional.ofNullable(scan.getDateAndTime()).map(DateAndTime::getStartedOn).orElse(null);
        Date finishedOn = Optional.ofNullable(scan.getDateAndTime()).map(DateAndTime::getFinishedOn).orElse(null);
        if (startedOn == null && finishedOn == null)
            return false;
        else if (finishedOn == null)
            return startedOn.after(cxSastScanQuery.getFrom()) &&
                    startedOn.before(cxSastScanQuery.getTo());
        else
            return (startedOn.after(cxSastScanQuery.getFrom()) &&
                    startedOn.before(cxSastScanQuery.getTo())) || (finishedOn.after(cxSastScanQuery.getFrom()) &&
                    finishedOn.before(cxSastScanQuery.getTo()));
    }

    private boolean checkIfScanIsFinished(CxSastScan scan) {
        return scan.getStatus().getName().equalsIgnoreCase(FINISHED) ||
                scan.getStatus().getName().equalsIgnoreCase(POST_SCAN);
    }

    private boolean isScanFailed(CxSastScan scan) {
        return !(scan.getStatus().getName().equalsIgnoreCase(CANCELED) ||
                scan.getStatus().getName().equalsIgnoreCase(FAILED) ||
                scan.getStatus().getName().equalsIgnoreCase(NONE));
    }

    protected void sleepWithExponentialBackoff(int attemptNumber) {
        try {
            Thread.sleep(calculateExponentialBackoff(attemptNumber));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeInterruptedException(e);
        }
    }

    protected long calculateExponentialBackoff(int attemptNumber) {
        double exp = Math.pow(2, attemptNumber);
        long result = Math.round(multiplierMs * exp);
        return Math.min(result, maxWaitMs);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxSastScanDataSource.CxSastScanQuery.CxSastScanQueryBuilder.class)
    public static class CxSastScanQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("project_id")
        String projectId;

        @JsonProperty("scan_status")
        String scanStatus;

        @JsonProperty("last")
        Integer last;

        @JsonProperty("from")
        Date from;

        @JsonProperty("to")
        Date to;

    }
}
