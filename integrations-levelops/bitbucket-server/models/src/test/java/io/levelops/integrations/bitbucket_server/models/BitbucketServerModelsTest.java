package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketServerModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserializeProject() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_projects.json");
        BitbucketServerPaginatedResponse<BitbucketServerProject> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerProject.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketServerProject> projects = paginatedResponse.getValues();
        Assert.assertNotNull(projects);
        Assert.assertEquals(1, projects.size());
    }

    @Test
    public void testBitbucketServerPaginatedResponse() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_repositories.json");
        BitbucketServerPaginatedResponse<BitbucketServerRepository> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerRepository.class));
        var builder = BitbucketServerPaginatedResponse.<BitbucketServerRepository>builder();
        builder.currentPageStart(paginatedResponse.currentPageStart)
                .isLastPage(paginatedResponse.isLastPage)
                .nextPageStart(paginatedResponse.nextPageStart)
                .size(paginatedResponse.size)
                .limit(paginatedResponse.limit)
                .values(paginatedResponse.values)
                .build();
        assertThat(paginatedResponse).isEqualTo(paginatedResponse);
    }

    @Test
    public void testDeserializeRepository() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_repositories.json");
        BitbucketServerPaginatedResponse<BitbucketServerRepository> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerRepository.class));
        Assert.assertNotNull(paginatedResponse);
        assertThat(paginatedResponse.getCurrentPageStart()).isEqualTo(0);
        assertThat(paginatedResponse.getLimit()).isEqualTo(25);
        assertThat(paginatedResponse.getSize()).isEqualTo(1);
        assertThat(paginatedResponse.getIsLastPage()).isEqualTo(true);
        assertThat(paginatedResponse.getNextPageStart()).isEqualTo(null);
        List<BitbucketServerRepository> repositories = paginatedResponse.getValues();
        Assert.assertNotNull(repositories);
        Assert.assertEquals(1, repositories.size());
    }

    @Test
    public void testDeserializeCommit() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_commits.json");
        BitbucketServerPaginatedResponse<BitbucketServerCommit> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerCommit.class));
        Assert.assertNotNull(paginatedResponse);
        assertThat(paginatedResponse.getCurrentPageStart()).isEqualTo(0);
        assertThat(paginatedResponse.getLimit()).isEqualTo(25);
        assertThat(paginatedResponse.getSize()).isEqualTo(1);
        assertThat(paginatedResponse.getIsLastPage()).isEqualTo(true);
        assertThat(paginatedResponse.getNextPageStart()).isEqualTo(null);
        List<BitbucketServerCommit> commits = paginatedResponse.getValues();
        Assert.assertNotNull(commits);
        Assert.assertEquals(1, commits.size());
    }

    @Test
    public void testDeserializePullRequest() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_pull_requests.json");
        BitbucketServerPaginatedResponse<BitbucketServerPullRequest> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerPullRequest.class));
        Assert.assertNotNull(paginatedResponse);
        assertThat(paginatedResponse.getCurrentPageStart()).isEqualTo(0);
        assertThat(paginatedResponse.getLimit()).isEqualTo(25);
        assertThat(paginatedResponse.getSize()).isEqualTo(1);
        assertThat(paginatedResponse.getIsLastPage()).isEqualTo(true);
        assertThat(paginatedResponse.getNextPageStart()).isEqualTo(null);
        List<BitbucketServerPullRequest> pullRequests = paginatedResponse.getValues();
        Assert.assertNotNull(pullRequests);
        Assert.assertEquals(1, pullRequests.size());
    }

    @Test
    public void testDeserializePRActivity() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_pr_activities.json");
        BitbucketServerPaginatedResponse<BitbucketServerPRActivity> paginatedResponse = MAPPER.readValue(resource,
                BitbucketServerPaginatedResponse.ofType(MAPPER, BitbucketServerPRActivity.class));
        Assert.assertNotNull(paginatedResponse);
        assertThat(paginatedResponse.getCurrentPageStart()).isEqualTo(0);
        assertThat(paginatedResponse.getLimit()).isEqualTo(25);
        assertThat(paginatedResponse.getSize()).isEqualTo(3);
        assertThat(paginatedResponse.getIsLastPage()).isEqualTo(true);
        assertThat(paginatedResponse.getNextPageStart()).isEqualTo(null);
        List<BitbucketServerPRActivity> prActivities = paginatedResponse.getValues();
        Assert.assertNotNull(prActivities);
        Assert.assertEquals(6, prActivities.size());
    }

    @Test
    public void testDeserializeQuery() throws IOException {
        String resource = ResourceUtils.getResourceAsString("query.json");
        BitbucketServerIterativeScanQuery query = MAPPER.readValue(resource, BitbucketServerIterativeScanQuery.class);
        assertThat(query).isNotNull();
        assertThat(query.getIntegrationKey()).isNotNull();
        assertThat(query.getIntegrationKey().getTenantId()).isEqualTo("foo");
        assertThat(query.getIntegrationKey().getIntegrationId()).isEqualTo("1");
        assertThat(query.getShouldFetchRepos()).isEqualTo(true);
        assertThat(query.getFrom()).isNotNull();
        assertThat(query.getTo()).isNotNull();
    }

    @Test
    public void testDeserializeCommitDiff() throws IOException {
        String resource = ResourceUtils.getResourceAsString("bitbucket_server_commit_diff.json");
        BitbucketServerCommitDiffInfo commitDiffInfo = MAPPER.readValue(resource, BitbucketServerCommitDiffInfo.class);
        assertThat(commitDiffInfo).isNotNull();
        assertThat(commitDiffInfo.getDiffs()).isNotNull();
        assertThat(commitDiffInfo.getContextLines()).isEqualTo(10);
        assertThat(commitDiffInfo.getFromHash()).isEqualTo("a0f224fd7bd5f28ea5a752d41b9c9f6372fc6d9e");
        assertThat(commitDiffInfo.getToHash()).isEqualTo("dc93f22caadcde35daf5cc2cd65d2738c87e31ca");
        assertThat(commitDiffInfo.getWhiteSpace()).isEqualTo("SHOW");
        assertThat(commitDiffInfo.getTruncated()).isEqualTo(true);

        BitbucketServerCommitDiffInfo.Diff diff = commitDiffInfo.getDiffs().get(0);
        assertThat(diff).isNotNull();
        assertThat(diff.getContextLines()).isEqualTo(10);
        assertThat(diff.getFromHash()).isEqualTo("a0f224fd7bd5f28ea5a752d41b9c9f6372fc6d9e");
        assertThat(diff.getToHash()).isEqualTo("dc93f22caadcde35daf5cc2cd65d2738c87e31ca");
        assertThat(diff.getWhiteSpace()).isEqualTo("SHOW");
        assertThat(diff.getTruncated()).isEqualTo(true);
        assertThat(diff.getHunks()).isNotNull();
        assertThat(diff.getHunks().size()).isEqualTo(1);

        BitbucketServerCommitDiffInfo.Diff.FileReference source = diff.getSource();
        assertThat(source).isNotNull();
        assertThat(source.getComponents()).isEqualTo(List.of("path", "to", "file.txt"));
        assertThat(source.getName()).isEqualTo("file.txt");
        assertThat(source.getParent()).isEqualTo("path/to");
        assertThat(source.getToString()).isEqualTo("path/to/file.txt");
        assertThat(source.getExtension()).isEqualTo("txt");

        BitbucketServerCommitDiffInfo.Diff.FileReference destination = diff.getDestination();
        assertThat(destination).isNotNull();
        assertThat(destination.getComponents()).isEqualTo(List.of("path", "to", "file.txt"));
        assertThat(destination.getName()).isEqualTo("file.txt");
        assertThat(destination.getParent()).isEqualTo("path/to");
        assertThat(destination.getToString()).isEqualTo("path/to/file.txt");
        assertThat(destination.getExtension()).isEqualTo("txt");

        BitbucketServerCommitDiffInfo.Diff.Hunk hunk = diff.getHunks().get(0);
        assertThat(hunk).isNotNull();
        assertThat(hunk.getSourceLine()).isEqualTo(1);
        assertThat(hunk.getSourceSpan()).isEqualTo(1);
        assertThat(hunk.getDestinationLine()).isEqualTo(1);
        assertThat(hunk.getDestinationSpan()).isEqualTo(2);
        assertThat(hunk.getTruncated()).isEqualTo(false);
        assertThat(hunk.getSegments()).isNotNull();
        assertThat(hunk.getSegments().size()).isEqualTo(2);

        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment segment1 = hunk.getSegments().get(0);
        assertThat(segment1).isNotNull();
        assertThat(segment1.getType()).isEqualTo("REMOVED");
        assertThat(segment1.getTruncated()).isEqualTo(false);
        assertThat(segment1.getLines()).isNotNull();
        assertThat(segment1.getLines().size()).isEqualTo(1);

        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line line1 = segment1.getLines().get(0);
        assertThat(line1).isNotNull();
        assertThat(line1.getSource()).isEqualTo(1);
        assertThat(line1.getDestination()).isEqualTo(1);
        assertThat(line1.getTruncated()).isEqualTo(false);

        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment segment2 = hunk.getSegments().get(1);
        assertThat(segment2).isNotNull();
        assertThat(segment2.getType()).isEqualTo("ADDED");
        assertThat(segment2.getTruncated()).isEqualTo(false);
        assertThat(segment2.getLines()).isNotNull();
        assertThat(segment2.getLines().size()).isEqualTo(2);

        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line line2 = segment2.getLines().get(0);
        assertThat(line2).isNotNull();
        assertThat(line2.getSource()).isEqualTo(2);
        assertThat(line2.getDestination()).isEqualTo(1);
        assertThat(line2.getTruncated()).isEqualTo(false);

        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line line3 = segment2.getLines().get(1);
        assertThat(line3).isNotNull();
        assertThat(line3.getSource()).isEqualTo(3);
        assertThat(line3.getDestination()).isEqualTo(2);
        assertThat(line3.getTruncated()).isEqualTo(false);
    }
}
