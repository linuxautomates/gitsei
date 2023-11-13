package io.levelops.integrations.helix_swarm.services;

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
import io.levelops.integrations.helix_swarm.models.ReviewFileResponseData;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class HelixSwarmEnrichmentServiceTest {

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId("1").tenantId("test").build();
    private static final List<ReviewFileInfo> REVIEW_FILES_INFO = List.of(
            ReviewFileInfo.builder().action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").depotFile("//local/broadcom/test.txt").build(),
            ReviewFileInfo.builder().action("add").type("text").rev("1").fileSize("21").digest("62659BDD975C7E7A0857D69DEC1E42FE").depotFile("//local/symantec/test.txt").build()
    );

    private HelixSwarmClient client;
    private HelixSwarmEnrichmentService enrichmentService;

    @Before
    public void setup() throws HelixSwarmClientException {
        client = Mockito.mock(HelixSwarmClient.class);
        enrichmentService = new HelixSwarmEnrichmentService(2, 4);
        when(client.getReviewInfo(anyLong())).thenReturn(HelixSwarmReviewInfo.builder()
                .versions(List.of(HelixSwarmChange.builder()
                                .change(13L)
                                .user("test")
                                .time(1609225129L)
                                .pending(false)
                                .difference(1)
                                .addChangeMode("replace")
                                .stream("//SecondDepot/main")
                                .build(),
                        HelixSwarmChange.builder()
                                .change(14L)
                                .user("test1")
                                .time(1609225190L)
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
        when(client.getReviewFiles(eq(1L), isNull())).thenReturn(ReviewFileResponse.builder()
                .data(ReviewFileResponseData.builder()
                        .files(REVIEW_FILES_INFO)
                        .build())
                .build());
        when(client.getReviewFiles(not(eq(1L)), isNull())).thenThrow(new HelixSwarmClientException("Response not successful: Response{protocol=http/1.1, code=404, message=Not Found, url=http://10.128.0.35/api/v10/reviews/14/files?max=100}"));
    }

    @Test
    public void test() {
        List<HelixSwarmReview> reviews = getReviews();
        List<HelixSwarmReview> enrichedReviews = enrichmentService.enrichReviews(client, TEST_INTEGRATION_KEY,
                reviews, review -> true);
        assertThat(enrichedReviews).isNotEmpty();
        assertThat(enrichedReviews).hasSize(reviews.size());
        assertThat(enrichedReviews.stream()
                .filter(review -> CollectionUtils.isEmpty(review.getVersions()) || CollectionUtils.isEmpty(review.getReviews()))
                .findAny()).isEmpty();
        enrichedReviews.stream()
                .filter(r -> r.getId().longValue() != 1l)
                .forEach(r -> {
                    Assert.assertTrue(r.getReviewFilesApiNotFound());
                    Assert.assertTrue(CollectionUtils.isEmpty(r.getFileInfos()));
                });
        enrichedReviews.stream()
                .filter(r -> r.getId().longValue() == 1l)
                .forEach(r -> {
                    Assert.assertFalse(r.getReviewFilesApiNotFound());
                    Assert.assertEquals(REVIEW_FILES_INFO, r.getFileInfos());
                });
    }

    private List<HelixSwarmReview> getReviews() {
        return IntStream.range(1, 10)
                .mapToObj(i -> HelixSwarmReview.builder().id((long) i).build())
                .collect(Collectors.toList());
    }

}
