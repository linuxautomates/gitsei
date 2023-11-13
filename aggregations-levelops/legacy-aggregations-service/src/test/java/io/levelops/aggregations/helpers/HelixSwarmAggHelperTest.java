package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.BadSqlGrammarException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.levelops.aggregations.helpers.HelixSwarmAggHelper.UNKNOWN_REPO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HelixSwarmAggHelperTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final RepoConfigEntryMatcher REPO_CONFIG_ENTRY_MATCHER = new RepoConfigEntryMatcher(
            List.of(
                    IntegrationConfig.RepoConfigEntry.builder().repoId("DummyProject").pathPrefix("//DummyProject/main").build(),
                    IntegrationConfig.RepoConfigEntry.builder().repoId("DBOverride").pathPrefix("//DBOverride/main").build(),
                    IntegrationConfig.RepoConfigEntry.builder().repoId("broadcom").pathPrefix("//local/broadcom").build(),
                    IntegrationConfig.RepoConfigEntry.builder().repoId("symantec").pathPrefix("//local/symantec").build()
            )
    );
    private static final String MESSAGE_ID = UUID.randomUUID().toString();
    private static final String CUSTOMER = "test";
    private static final String INTEGRATION_ID = "1";
    private static final Date TRUNCATED_DATE = new Date();

    @Mock
    private ScmAggService scmAggService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    //region Stream Depot - No File Info - Get Commits NPE - Repo Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepot1Case1() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("952")), eq(INTEGRATION_ID))).thenThrow(NullPointerException.class);
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("DummyProject"));
    }
    //endregion

    //region Stream Depot - No File Info - Get Commits DB Exception - Repo Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepot1Case2() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("952")), eq(INTEGRATION_ID))).thenThrow(BadSqlGrammarException.class);
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("DummyProject"));
    }
    //endregion

    //region Stream Depot - No File Info - Get Commits Not Found - Repo Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepot1Case3() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("952")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("DummyProject"));
    }
    //endregion

    //region Stream Depot - No File Info - Get Commits Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepot1Case4() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("952")), eq(INTEGRATION_ID))).thenReturn(List.of(DbScmCommit.builder().repoIds(List.of("DBOverride")).build()));
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("DBOverride"));
    }
    //endregion

    //region Stream Depot - No File Info - Repo Not Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepot2() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_2.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("964")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains(UNKNOWN_REPO));
    }
    //endregion

    //region Stream Depot - With File Info - Repo Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepotWithFiles1() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_with_files_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(2, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("broadcom"));
        Assert.assertEquals(true, repoIds.contains("symantec"));
    }
    //endregion

    //region Stream Depot - With File Info - Repo Not Found
    @Test
    public void testParseHelixReviewGetRepoIdsStreamDepotWithFiles2() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_stream_depot_with_files_2.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("964")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains(UNKNOWN_REPO));
    }
    //endregion

    //region Local Depot - No File Info - Repo Not Found
    @Test
    public void testParseHelixReviewGetRepoIdsLocalDepot() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_local_depot.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("983")), eq(INTEGRATION_ID))).thenThrow(RuntimeException.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("985")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains(UNKNOWN_REPO));
    }
    //endregion

    //region Local Depot - File Info - Repo Not Found
    @Test
    public void testParseHelixReviewGetRepoIdsLocalDepotWithFiles1() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_local_depot_with_files_1.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("983")), eq(INTEGRATION_ID))).thenThrow(RuntimeException.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("985")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(2, repoIds.size());
        Assert.assertEquals(true, repoIds.contains("broadcom"));
        Assert.assertEquals(true, repoIds.contains("symantec"));
    }
    //endregion

    //region Local Depot - File Info - Repo Not Found
    @Test
    public void testParseHelixReviewGetRepoIdsLocalDepotWithFiles2() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_local_depot_with_files_2.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("983")), eq(INTEGRATION_ID))).thenThrow(RuntimeException.class);
        when(scmAggService.getCommits(eq(CUSTOMER),eq(List.of("985")), eq(INTEGRATION_ID))).thenReturn(Collections.emptyList());
        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        Set<String> repoIds = helixSwarmAggHelper.parseHelixReviewGetRepoIds(review, CUSTOMER, INTEGRATION_ID, REPO_CONFIG_ENTRY_MATCHER);
        Assert.assertEquals(1, repoIds.size());
        Assert.assertEquals(true, repoIds.contains(UNKNOWN_REPO));
    }
    //endregion

    @Test
    public void test() throws IOException, SQLException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_reviews.json");
        List<HelixSwarmReview> reviews = MAPPER.readValue(content, MAPPER.getTypeFactory().constructCollectionType(List.class, HelixSwarmReview.class));

        final AtomicInteger prsCount = new AtomicInteger();
        final AtomicInteger prDBErrorsCount = new AtomicInteger();

        ArgumentCaptor<DbScmPullRequest> prArgumentCaptor = ArgumentCaptor.forClass(DbScmPullRequest.class);

        HelixSwarmAggHelper helixSwarmAggHelper = new HelixSwarmAggHelper(null, null, scmAggService);
        for(HelixSwarmReview review : reviews) {
            helixSwarmAggHelper.processHelixSwarmReview(MESSAGE_ID, CUSTOMER, INTEGRATION_ID, TRUNCATED_DATE, REPO_CONFIG_ENTRY_MATCHER, review,
                    prsCount, prDBErrorsCount);
        }
        verify(scmAggService, times(500)).insert(eq(CUSTOMER), prArgumentCaptor.capture());
        assertThat(500).isEqualTo(prArgumentCaptor.getAllValues().size());
    }
}