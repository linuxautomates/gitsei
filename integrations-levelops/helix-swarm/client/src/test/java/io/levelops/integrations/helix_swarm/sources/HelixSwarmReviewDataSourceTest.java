package io.levelops.integrations.helix_swarm.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClient;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientException;
import io.levelops.integrations.helix_swarm.client.HelixSwarmClientFactory;
import io.levelops.integrations.helix_swarm.models.ActivityResponse;
import io.levelops.integrations.helix_swarm.models.HelixSwarmActivity;
import io.levelops.integrations.helix_swarm.models.HelixSwarmChange;
import io.levelops.integrations.helix_swarm.models.HelixSwarmQuery;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReviewInfo;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.integrations.helix_swarm.services.HelixSwarmEnrichmentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


public class HelixSwarmReviewDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId("1").tenantId("test").build();
    private static final Date LATEST_REVIEW_UPDATED_AT = Date.from(Instant.now());
    private static final Date STALE_REVIEW_UPDATED_AT = Date.from(Instant.now().minus(120, ChronoUnit.DAYS));
    private static final SimpleDateFormat HELIX_SWARM_UPDATE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String LATEST_REVIEW_UPDATED_AT_STRING = HELIX_SWARM_UPDATE_DATE_FORMAT.format(LATEST_REVIEW_UPDATED_AT);
    private static final String STALE_REVIEW_UPDATED_AT_STRING = HELIX_SWARM_UPDATE_DATE_FORMAT.format(STALE_REVIEW_UPDATED_AT);

    private HelixSwarmReviewDataSource dataSource;
    private List<HelixSwarmReview> fetchedReviews;

    @Before
    public void setup() throws HelixSwarmClientException {
        if (dataSource != null)
            return;
        HelixSwarmClient client = Mockito.mock(HelixSwarmClient.class);
        HelixSwarmClientFactory clientFactory = Mockito.mock(HelixSwarmClientFactory.class);
        HelixSwarmEnrichmentService enrichmentService = new HelixSwarmEnrichmentService(2, 10);
        dataSource = new HelixSwarmReviewDataSource(clientFactory, enrichmentService);
        fetchedReviews = new ArrayList<>();
        when(clientFactory.get(eq(TEST_KEY))).thenReturn(client);
        List<HelixSwarmReview> reviews = getReviews(0, 10);
        fetchedReviews.addAll(reviews);
        when(client.getReviews("")).thenReturn(ReviewResponse.builder()
                .lastSeen(10L)
                .reviews(reviews)
                .build());
        List<HelixSwarmReview> nextReviews = getReviews(10, 20);
        fetchedReviews.addAll(nextReviews);
        when(client.getReviews(String.valueOf(10L))).thenReturn(ReviewResponse.builder()
                .lastSeen(20L)
                .reviews(nextReviews)
                .build());
        when(client.getReviews(String.valueOf(20L))).thenReturn(ReviewResponse.builder()
                .lastSeen(null)
                .reviews(List.of())
                .build());
        when(client.getReviewInfo(anyLong())).thenReturn(HelixSwarmReviewInfo.builder()
                .versions(List.of(HelixSwarmChange.builder()
                        .change(13L)
                        .user("test")
                        .time(1609225129L)
                        .pending(false)
                        .difference(1)
                        .addChangeMode("replace")
                        .stream("//SecondDepot/main")
                        .build()))
                .build());
        when(client.getReviewActivities(anyLong(), eq(""))).thenReturn(ActivityResponse.builder()
                .lastSeen(3L)
                .activities(List.of(HelixSwarmActivity.builder().id(1L).build(), HelixSwarmActivity.builder().id(2L).build(),
                        HelixSwarmActivity.builder().id(3L).build()))
                .build());
        when(client.getReviewActivities(anyLong(), eq(String.valueOf(3L)))).thenReturn(ActivityResponse.builder()
                .lastSeen(6L)
                .activities(List.of(HelixSwarmActivity.builder().id(4L).build(), HelixSwarmActivity.builder().id(5L).build(),
                        HelixSwarmActivity.builder().id(6L).build()))
                .build());
        when(client.getReviewActivities(anyLong(), eq(String.valueOf(6L)))).thenReturn(ActivityResponse.builder()
                .lastSeen(null)
                .activities(List.of())
                .build());

    }

    @Test
    public void testFetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(HelixSwarmQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void testFetchMany() throws FetchException {
        HelixSwarmQuery query = HelixSwarmQuery.builder()
                .integrationKey(TEST_KEY)
                .from(Date.from(Instant.now().minus(90, ChronoUnit.DAYS)))
                .to(Date.from(Instant.now().plus(90, ChronoUnit.DAYS)))
                .build();
        List<Data<HelixSwarmReview>> reviews = dataSource.fetchMany(query).collect(Collectors.toList());
        assertThat(reviews).isNotEmpty();
        assertThat(reviews).hasSizeLessThan(fetchedReviews.size());
        assertThat(reviews).hasSize(13);
    }

    private List<HelixSwarmReview> getReviews(int fromId, int toId) {
        return IntStream.range(fromId, toId)
                .mapToObj(i -> HelixSwarmReview.builder().id((long) i)
                        .updatedAt(i % 3 != 0 ? LATEST_REVIEW_UPDATED_AT_STRING : STALE_REVIEW_UPDATED_AT_STRING)
                        .build())
                .collect(Collectors.toList());
    }
}
