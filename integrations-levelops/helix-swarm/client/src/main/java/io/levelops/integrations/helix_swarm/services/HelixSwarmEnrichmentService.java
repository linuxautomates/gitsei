package io.levelops.integrations.helix_swarm.services;

import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClient;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientException;
import io.levelops.integrations.helix_swarm.models.ActivityResponse;
import io.levelops.integrations.helix_swarm.models.HelixSwarmActivity;
import io.levelops.integrations.helix_swarm.models.HelixSwarmChange;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReviewInfo;
import io.levelops.integrations.helix_swarm.models.ReviewFileInfo;
import io.levelops.integrations.helix_swarm.models.ReviewFileResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log4j2
public class HelixSwarmEnrichmentService {

    private final int forkThreshold;
    private final ForkJoinPool pool;

    public HelixSwarmEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    public List<HelixSwarmReview> enrichReviews(HelixSwarmClient swarmClient, IntegrationKey integrationKey,
                                                List<HelixSwarmReview> reviews, Predicate<HelixSwarmReview> predicate) {
        EnrichReviewTask enrichReviewTask = new EnrichReviewTask(swarmClient, reviews, predicate, forkThreshold);
        log.info("enrichTickets: started enriching {} reviews for {}", reviews.size(), integrationKey);
        List<HelixSwarmReview> enrichedReviews = pool.invoke(enrichReviewTask);
        log.info("enrichTickets: enriched {} reviews for {}", enrichedReviews.size(), integrationKey);
        return enrichedReviews;
    }

    static class EnrichReviewTask extends RecursiveTask<List<HelixSwarmReview>> {

        private static final String STARTING_CURSOR = "";

        private final HelixSwarmClient swarmClient;
        private final List<HelixSwarmReview> reviews;
        private final Predicate<HelixSwarmReview> predicate;
        private final int forkThreshold;

        EnrichReviewTask(HelixSwarmClient swarmClient, List<HelixSwarmReview> reviews,
                         Predicate<HelixSwarmReview> predicate, int forkThreshold) {
            this.swarmClient = swarmClient;
            this.reviews = reviews;
            this.predicate = predicate;
            this.forkThreshold = forkThreshold;
        }

        @Override
        protected List<HelixSwarmReview> compute() {
            if (reviews.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichReviews();
            }
        }

        private List<HelixSwarmReview> computeInSubTask() {
            int size = reviews.size();
            EnrichReviewTask enrichReviewTask1 = new EnrichReviewTask(swarmClient, reviews.subList(0, size / 2),
                    predicate, forkThreshold);
            EnrichReviewTask enrichReviewTask2 = new EnrichReviewTask(swarmClient, reviews.subList(size / 2, size),
                    predicate, forkThreshold);
            enrichReviewTask1.fork();
            enrichReviewTask2.fork();
            List<HelixSwarmReview> enrichedReviews = new ArrayList<>(enrichReviewTask1.join());
            enrichedReviews.addAll(enrichReviewTask2.join());
            return enrichedReviews;
        }

        private List<HelixSwarmReview> enrichReviews() {
            return this.reviews.stream()
                    .map(this::enrichReview)
                    .collect(Collectors.toList());
        }

        private HelixSwarmReview enrichReview(HelixSwarmReview review) {
            List<HelixSwarmChange> changes = null;
            List<HelixSwarmActivity> reviews = null;
            boolean reviewFilesApiNotFound = false;
            List<ReviewFileInfo> files = null;

            if (predicate.test(review)) {
                Long reviewId = review.getId();
                try {
                    HelixSwarmReviewInfo reviewInfo = swarmClient.getReviewInfo(reviewId);
                    changes = reviewInfo.getVersions();
                    reviews = PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(reviewId, cursor))
                            .collect(Collectors.toList());
                } catch (HelixSwarmClientException e) {
                    log.warn("process: encountered client exception while enriching helix swarm review, review id " + reviewId, e);
                }

                try {
                    ReviewFileResponse reviewFileResponse = swarmClient.getReviewFiles(reviewId, null);
                    if((reviewFileResponse != null) && (reviewFileResponse.getData() != null)) {
                        files = reviewFileResponse.getData().getFiles();
                    }
                } catch (HelixSwarmClientException e) {
                    if (HelixSwarmClient.isApiNotFoundExceptions(e)) {
                        reviewFilesApiNotFound = true;
                        log.info("cannot enrich helix swarm review with files info, api v10 is not available, review id " + reviewId);
                    } else {
                        log.warn("process: encountered client exception while enriching helix swarm review with files info, review id " + reviewId, e);
                    }
                }
            }
            return review.toBuilder()
                    .versions(changes)
                    .reviews(reviews)
                    .reviewFilesApiNotFound(reviewFilesApiNotFound)
                    .fileInfos(ListUtils.emptyIfNull(files))
                    .build();
        }

        private PaginationUtils.CursorPageData<HelixSwarmActivity> getPageData(Long reviewId,
                                                                               String cursor) {
            if (cursor == null) {
                return null;
            }
            try {
                ActivityResponse response = swarmClient.getReviewActivities(reviewId, cursor);
                return PaginationUtils.CursorPageData.<HelixSwarmActivity>builder()
                        .data(response.getActivities())
                        .cursor(Optional.ofNullable(response.getLastSeen()).map(String::valueOf).orElse(null))
                        .build();
            } catch (HelixSwarmClientException e) {
                log.warn("getPageData: encountered helix swarm client error for review id: "
                        + reviewId + " as : " + e.getMessage(), e);
                return null;
            }
        }
    }
}
