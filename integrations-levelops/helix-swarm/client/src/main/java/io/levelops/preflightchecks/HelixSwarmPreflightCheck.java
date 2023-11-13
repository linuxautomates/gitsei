package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClient;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientFactory;
import io.levelops.integrations.helix_swarm.models.ActivityResponse;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReviewInfo;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class HelixSwarmPreflightCheck implements PreflightCheck {

    private static final String HELIX_SWARM = "helix_swarm";
    private static final int PAGE_SIZE = 1;

    private final HelixSwarmClientFactory clientFactory;

    @Autowired
    public HelixSwarmPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = HelixSwarmClientFactory.builder()
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(PAGE_SIZE)
                .build();
    }


    @Override
    public String getIntegrationType() {
        return HELIX_SWARM;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        HelixSwarmClient helixSwarmClient;
        try {
            helixSwarmClient = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        PreflightCheckResultWithReviewId result = checkReviews(helixSwarmClient);
        builder.check(result.getPreflightCheckResult());
        if (result.getReviewId() != null) {
            builder.check(checkReviewInfo(helixSwarmClient, result.getReviewId()));
            builder.check(checkReviewActivities(helixSwarmClient, result.getReviewId()));
        }
        return builder.build();
    }

    private PreflightCheckResultWithReviewId checkReviews(HelixSwarmClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/reviews")
                .success(true);
        Long reviewId = null;
        try {
            ReviewResponse response = client.getReviews(null);
            if (response == null || CollectionUtils.isEmpty(response.getReviews())) {
                checkResultBuilder.success(false).error("response from /reviews returned null or empty result");
            }
            else {
                reviewId = ListUtils.emptyIfNull(response.getReviews()).stream().findAny()
                        .map(HelixSwarmReview::getId).orElse(null);
            }
        } catch (HelixSwarmClientException e) {
            log.error("checkReviews: encountered error while fetching reviews: " + e.getMessage(), e);
            checkResultBuilder.success(false)
                    .error("response from /reviews failed")
                    .exception(e.getMessage());
        }
        return PreflightCheckResultWithReviewId.builder()
                .preflightCheckResult(checkResultBuilder.build())
                .reviewId(reviewId)
                .build();
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


    @Value
    @Builder(toBuilder = true)
    static class PreflightCheckResultWithReviewId {

        PreflightCheckResult preflightCheckResult;

        Long reviewId;
    }
}
