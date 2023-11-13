package io.levelops.integrations.helix_swarm.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClient;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientFactory;
import io.levelops.integrations.helix_swarm.models.HelixSwarmQuery;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.integrations.helix_swarm.services.HelixSwarmEnrichmentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Log4j2
public class HelixSwarmReviewDataSource implements DataSource<HelixSwarmReview, HelixSwarmQuery> {

    private static final String STARTING_CURSOR = "";

    private final HelixSwarmClientFactory clientFactory;
    private final HelixSwarmEnrichmentService enrichmentService;

    public HelixSwarmReviewDataSource(HelixSwarmClientFactory clientFactory,
                                      HelixSwarmEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<HelixSwarmReview> fetchOne(HelixSwarmQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<HelixSwarmReview>> fetchMany(HelixSwarmQuery query) throws FetchException {
        Validate.notNull(query.getFrom(), "The from timestamp in the query can't be null");
        HelixSwarmClient helixSwarmClient = clientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(helixSwarmClient, query, cursor))
                .filter(getTimelineFilter(query.getFrom().toInstant(), query.getTo().toInstant()))
                .map(BasicData.mapper(HelixSwarmReview.class));
    }

    private PaginationUtils.CursorPageData<HelixSwarmReview> getPageData(HelixSwarmClient client,
                                                                         HelixSwarmQuery query,
                                                                         String cursor) {
        try {
            if (cursor == null) {
                return null;
            }
            ReviewResponse reviewResponse = client.getReviews(cursor);
            List<HelixSwarmReview> reviews = reviewResponse.getReviews();
            Predicate<HelixSwarmReview> timelineFilter = getTimelineFilter(query.getFrom().toInstant(),
                    query.getTo().toInstant());
            List<HelixSwarmReview> enrichedReviews = enrichmentService.enrichReviews(client, query.getIntegrationKey(),
                    reviews, timelineFilter);
            return PaginationUtils.CursorPageData.<HelixSwarmReview>builder()
                    .data(enrichedReviews)
                    .cursor(Optional.ofNullable(reviewResponse.getLastSeen()).map(String::valueOf).orElse(null))
                    .build();
        } catch (HelixSwarmClientException e) {
            log.error("getPageData: encountered helix swarm client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered helix swarm client exception for " + query.getIntegrationKey(), e);
        }
    }

    private Predicate<HelixSwarmReview> getTimelineFilter(Instant fromTime, Instant toTime) {
        Predicate<HelixSwarmReview> predicate = review -> {
            Instant updatedAt = HelixSwarmReview.parseReviewUpdateDate(review.getUpdatedAt());
            return updatedAt != null && updatedAt.isAfter(fromTime) && updatedAt.isBefore(toTime);
        };
        return predicate;
    }
}
