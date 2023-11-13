package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClient;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientFactory;
import io.levelops.integrations.helix_swarm.models.ActivityResponse;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReviewInfo;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Log4j2
@Component
public class HelixPreflightCheck implements PreflightCheck {
    public static final String HELIX = "helix";
    private static final int PAGE_SIZE = 1;
    private static final int MAX_FILE_SIZE = 1000000;

    private final HelixCoreClientFactory helixCoreClientFactory;
    private final HelixSwarmClientFactory helixSwarmclientFactory;

    @Autowired
    public HelixPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.helixCoreClientFactory = HelixCoreClientFactory.builder().build();
        this.helixSwarmclientFactory = HelixSwarmClientFactory.builder()
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(PAGE_SIZE)
                .build();
    }

    @Override
    public String getIntegrationType() {
        return HELIX;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        HelixCoreClient helixCoreClient;
        HelixSwarmClient helixSwarmClient = null;
        boolean needHelixSwarm = needHelixSwarm(integration);
        try {
            helixCoreClient = helixCoreClientFactory.buildFromToken(tenantId, integration, token);
            if (needHelixSwarm)
                helixSwarmClient = helixSwarmclientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkDepots(helixCoreClient));
        builder.check(checkChangeLists(helixCoreClient));
        if (needHelixSwarm)
            checkReviews(helixSwarmClient, builder);
        return builder.build();
    }

    private boolean needHelixSwarm(Integration integration) {
        boolean result = false;
        boolean needHelixSwarmData = false;
        if (integration.getMetadata().get("helix_swarm_url") != null) {
            if (StringUtils.isNotEmpty(String.valueOf(integration.getMetadata().get("helix_swarm_url"))))
                needHelixSwarmData = true;
            result = needHelixSwarmData;
        }
        return result;
    }

    private PreflightCheckResult checkDepots(HelixCoreClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/Depots")
                .success(true);
        try {
            var depots = client.getDepots();
            if (depots == null) {
                checkResultBuilder.success(false).error("response from /depots returned null result");
            }
        } catch (HelixCoreClientException e) {
            log.error("checkDepots: encountered error while getting depots: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkChangeLists(HelixCoreClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/ChangeLists")
                .success(true);
        try {
            ZoneId zoneId = ZoneId.of("UTC");

            Instant untilDate = Instant.now();
            Instant sinceDate = untilDate.minus(1, ChronoUnit.MINUTES);
            LocalDate specFrom = LocalDate.ofInstant(sinceDate, zoneId);
            LocalDate specTo = LocalDate.ofInstant(untilDate, zoneId);

            var changeLists = client.getChangeLists(specFrom, specTo, sinceDate, untilDate, MAX_FILE_SIZE);
            if (changeLists == null) {
                checkResultBuilder.success(false).error("response from /changeLists returned null result");
            }
        } catch (HelixCoreClientException e) {
            log.error("checkChangeLists: encountered error while getting changeLists: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private void checkReviews(HelixSwarmClient client,
                              PreflightCheckResults.PreflightCheckResultsBuilder builder) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/reviews")
                .success(true);
        Long reviewId = null;
        try {
            ReviewResponse response = client.getReviews(null);
            if (response == null || CollectionUtils.isEmpty(response.getReviews())) {
                checkResultBuilder.success(false).error("response from /reviews returned null or empty result");
            } else {
                reviewId = ListUtils.emptyIfNull(response.getReviews()).stream().findAny()
                        .map(HelixSwarmReview::getId).orElse(null);
            }
        } catch (HelixSwarmClientException e) {
            log.error("checkReviews: encountered error while fetching reviews: " + e.getMessage(), e);
            checkResultBuilder.success(false)
                    .error("response from /reviews failed")
                    .exception(e.getMessage());
        }
        builder.check(checkResultBuilder.build());
        if (reviewId != null) {
            builder.check(checkReviewInfo(client, reviewId));
            builder.check(checkReviewActivities(client, reviewId));
        }
    }

    private PreflightCheckResult checkReviewInfo(HelixSwarmClient client, long reviewId) {
        String api = "/reviews/" + reviewId;
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(api)
                .success(true);
        try {
            HelixSwarmReviewInfo reviewInfo = client.getReviewInfo(reviewId);
            if (reviewInfo == null)
                checkResultBuilder.success(false).error("response from " + api + " returned null result");
        } catch (HelixSwarmClientException e) {
            log.error("checkReviewInfo: encountered error while fetching review info: " + e.getMessage(), e);
            checkResultBuilder.success(false)
                    .error("response from " + api + " failed")
                    .exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    private PreflightCheckResult checkReviewActivities(HelixSwarmClient client, long reviewId) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/activity")
                .success(true);
        try {
            ActivityResponse response = client.getReviewActivities(reviewId, null);
            if (response == null)
                checkResultBuilder.success(false).error("response from /activity?type=review returned null result");
        } catch (HelixSwarmClientException e) {
            log.error("checkReviewActivities: encountered error while fetching review activities: " + e.getMessage(), e);
            checkResultBuilder.success(false)
                    .error("response from /activity?type=review for review: " + reviewId + " failed")
                    .exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
