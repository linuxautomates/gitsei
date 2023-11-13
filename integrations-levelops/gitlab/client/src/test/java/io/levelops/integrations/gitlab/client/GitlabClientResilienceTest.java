package io.levelops.integrations.gitlab.client;

import io.levelops.commons.functional.IngestionResult;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.gitlab.models.GitlabBranch;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabIssueNote;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabMergeRequestChanges;
import io.levelops.integrations.gitlab.models.GitlabMilestone;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabStateEvent;
import io.levelops.integrations.gitlab.models.GitlabUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class GitlabClientResilienceTest {

    public static final int PER_PAGE = 20;
    public static final int PAGE = 1;
    public static final String TEST_ID = "1";
    public static GitlabClient client;

    @Before
    public void setup() throws GitlabClientException {
        client = Mockito.mock(GitlabClient.class);
        when(client.streamProjects(anyBoolean())).thenCallRealMethod();
        when(client.getProjects(anyInt(), anyInt(), anyBoolean())).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamGroups(PER_PAGE)).thenCallRealMethod();
        when(client.getGroups(PAGE, PER_PAGE)).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamPipelines(TEST_ID, null, null, PER_PAGE)).thenCallRealMethod();
        when(client.getProjectPipelines(TEST_ID, null, null, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamJobs(TEST_ID, TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getJobs(TEST_ID, TEST_ID, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamProjectCommits(TEST_ID, null, null, PER_PAGE)).thenCallRealMethod();
        when(client.getCommits(TEST_ID, null, null, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamProjectBranches(TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getBranches(TEST_ID, PAGE, PER_PAGE)).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamMergeRequests(TEST_ID, null, null, PER_PAGE)).thenCallRealMethod();
        when(client.getMergeRequests(TEST_ID, null, null, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamUsers(TEST_ID, null, null, PER_PAGE)).thenCallRealMethod();
        when(client.getUsers(TEST_ID, null, null, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamIssues(null, null, PER_PAGE)).thenCallRealMethod();
        when(client.getIssues(null, null, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamMilestones(TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getMilestones(TEST_ID, PAGE, PER_PAGE)).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamMRCommit(TEST_ID, TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getMRCommits(TEST_ID, TEST_ID, PAGE, PER_PAGE)).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamMRStateEvent(TEST_ID, TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getMRStateEvents(TEST_ID, TEST_ID, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamMREvent(TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getMREvents(TEST_ID, PAGE, PER_PAGE)).thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamIssueNotes(TEST_ID, TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getProjectIssueNotes(TEST_ID, TEST_ID, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.streamCommitChanges(TEST_ID, TEST_ID, PER_PAGE)).thenCallRealMethod();
        when(client.getCommitChanges(TEST_ID, TEST_ID, PAGE, PER_PAGE))
                .thenThrow(new GitlabClientException("Not Authorised"));
        when(client.getMRChanges(TEST_ID, TEST_ID)).thenReturn(Optional.empty());
        when(client.getCommitWithStats(TEST_ID, TEST_ID)).thenReturn(Optional.empty());
    }

    @Test
    public void testResilience() throws FetchException {
        try{
        List<GitlabProject> projects = client.streamProjects(anyBoolean()).collect(Collectors.toList());
        assertThat(projects).hasSize(0);
        List<IngestionResult<GitlabGroup>> groups = client.streamGroups(PER_PAGE).collect(Collectors.toList());
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getData()).isNull();
        List<GitlabPipeline> pipelines = client.streamPipelines(
                TEST_ID, null, null, PER_PAGE).collect(Collectors.toList());
        assertThat(pipelines).hasSize(0);
        List<GitlabJob> jobs = client.streamJobs(TEST_ID, TEST_ID, PER_PAGE).collect(Collectors.toList());
        assertThat(jobs).hasSize(0);
        List<GitlabCommit> commits = client.streamProjectCommits(TEST_ID, null, null, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(commits).hasSize(0);
        List<GitlabBranch> branches = client.streamProjectBranches(TEST_ID, PER_PAGE).collect(Collectors.toList());
        assertThat(branches).hasSize(0);
        List<GitlabMergeRequest> mergeRequests = client.streamMergeRequests(TEST_ID, null, null, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(mergeRequests).hasSize(0);
        List<GitlabUser> users = client.streamUsers(TEST_ID, null, null, PER_PAGE).collect(Collectors.toList());
        assertThat(users).hasSize(0);
        List<GitlabIssue> issues = client.streamIssues(null, null, PER_PAGE).collect(Collectors.toList());
        assertThat(issues).hasSize(0);
        List<GitlabMilestone> milestones = client.streamMilestones(TEST_ID, PER_PAGE).collect(Collectors.toList());
        assertThat(milestones).hasSize(0);
        List<GitlabCommit> mrCommits = client.streamMRCommit(TEST_ID, TEST_ID, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(mrCommits).hasSize(0);
        List<GitlabStateEvent> mrStateEvents = client.streamMRStateEvent(TEST_ID, TEST_ID, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(mrStateEvents).hasSize(0);
        List<GitlabEvent> mrEvents = client.streamMREvent(TEST_ID, PER_PAGE).collect(Collectors.toList());
        assertThat(mrEvents).hasSize(0);
        List<GitlabIssueNote> issueNotes = client.streamIssueNotes(TEST_ID, TEST_ID, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(issueNotes).hasSize(0);
        List<GitlabChange> commitChanges = client.streamCommitChanges(TEST_ID, TEST_ID, PER_PAGE)
                .collect(Collectors.toList());
        assertThat(commitChanges).hasSize(0);
        Optional<GitlabMergeRequestChanges> mrChangesOptional = client.getMRChanges(TEST_ID, TEST_ID);
        assertThat(mrChangesOptional.isEmpty()).isTrue();
        Optional<GitlabCommit> commitWithStatsOptional = client.getCommitWithStats(TEST_ID, TEST_ID);
        assertThat(commitWithStatsOptional.isEmpty()).isTrue();
        }
        catch(Exception e){
            Assert.assertEquals(e.getMessage(), "Failed to get projects after page 1");
        }

    }
}
