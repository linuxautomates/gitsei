package io.levelops.preflightchecks;

import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamIdDataObj;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.coverity.client.CoverityClient;
import io.levelops.integrations.coverity.client.CoverityClientException;
import io.levelops.integrations.coverity.client.CoverityClientFactory;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Log4j2
@Component
public class CoverityPreflightCheck implements PreflightCheck {

    private static final String COVERITY = "coverity";

    private final CoverityClientFactory clientFactory;

    @Autowired
    public CoverityPreflightCheck() {
        clientFactory = CoverityClientFactory.builder().build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link CoverityPreflightCheck#COVERITY}
     */
    @Override
    public String getIntegrationType() {
        return COVERITY;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list tickets,
     * list requests and jira links api. Validates successful response.
     *
     * @param tenantId    {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration {@link Integration} to validate
     * @param token       {@link Token} containing the credentials for the {@code integration}
     * @return {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        CoverityClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkStreams(client, builder));
        return builder.build();
    }

    /**
     * validates the response from {@link CoverityClient#getStreams()}
     *
     * @param client {@link CoverityClient} with authentication interceptor
     * @param builder
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkStreams(CoverityClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("streams")
                .success(true);
        try {
            List<StreamDataObj> streams = client.getStreams();
            if (CollectionUtils.isNotEmpty(streams)) {
                StreamDataObj stream = streams.iterator().next();
                Date date = new Date();
                builder.check(checkSnapshots(client, stream.getId(), date, date));
                builder.check(checkDefects(client, stream.getId(), date, date, 10, 0));
            }
        } catch (CoverityClientException e) {
            log.error("checkStreams: encountered error while fetching streams: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link CoverityClient#getSnapshotsForStream(StreamIdDataObj, Date, Date)} ()}
     *
     * @param client {@link CoverityClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkSnapshots(CoverityClient client, StreamIdDataObj stream, Date startDate, Date endDate) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("snapshots")
                .success(true);
        try {
            client.getSnapshotsForStream(stream, startDate, endDate);
        } catch (CoverityClientException e) {
            log.error("checkSnapshots: encountered error while fetching snapshots: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link CoverityClient#getMergedDefectsForStreams(List, Date, Date, int, int)}
     *
     * @param client {@link CoverityClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkDefects(CoverityClient client, StreamIdDataObj streamId,
                                              Date startDate, Date endDate, int pageSize, int offset) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("defects")
                .success(true);
        try {
            client.getMergedDefectsForStreams(List.of(streamId), startDate, endDate, pageSize, offset);
        } catch (CoverityClientException e) {
            log.error("checkDefects: encountered error while fetching defects: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
