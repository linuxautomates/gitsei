package io.levelops.commons.databases.models.database.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbScmCommitTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testRepoConfigFilter() throws IOException {
        HelixCoreChangeList changeList = parseChangeList("helix/helix_changelist.json");
        Set<String> repoIds = new HashSet<>(Arrays.asList("repo_1", "repo_2"));
        DbScmCommit dbScmCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, "1");
        Assert.assertEquals(2, dbScmCommit.getRepoIds().size());
        Assert.assertEquals(4, dbScmCommit.getFilesCt().intValue());
        Assert.assertEquals("repo_1", dbScmCommit.getProject());
        Assert.assertEquals(List.of("LEV-2785", "LEV-2788"), dbScmCommit.getIssueKeys());
        Assert.assertEquals(List.of("2788", "2785"), dbScmCommit.getWorkitemIds());
    }

    private HelixCoreChangeList parseChangeList(String resourceUrl) throws IOException {
        String data = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(HelixCoreChangeList.class));
    }

    @Test
    public void testFromGithubCommit() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_commit.json");
        GithubCommit githubCommit = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubCommit.class));
        DbScmCommit dbScmCommit = DbScmCommit
                .fromGithubCommit(githubCommit, "repo_1", "1", 1619531929L, 0L);
        Assert.assertEquals(List.of("repo_1"), dbScmCommit.getRepoIds());
        Assert.assertEquals("repo_1", dbScmCommit.getProject());
    }

    @Test
    public void testFromBitbucketServerCommit() throws IOException {
        String data = ResourceUtils.getResourceAsString("bitbucket-server/bitbucket_server_commit.json");
        BitbucketServerCommit bitbucketServerCommit = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(BitbucketServerCommit.class));
        Long truncate = DateUtils.truncate(new Date(), Calendar.DATE);
        DbScmCommit dbScmCommit = DbScmCommit
                .fromBitbucketServerCommit(bitbucketServerCommit, "repo_1", "1", truncate);
        Assert.assertNotNull(dbScmCommit);
        Assert.assertEquals("1", dbScmCommit.getIntegrationId());
        Assert.assertEquals(List.of("repo_1"), dbScmCommit.getRepoIds());
        Assert.assertEquals("sample-project", dbScmCommit.getProject());
        Assert.assertEquals("https://www.sampleurl.com/repo/sample-commit", dbScmCommit.getCommitUrl());
        Assert.assertEquals("Srinath Chandrashekhar", dbScmCommit.getCommitter());
        Assert.assertEquals(DbScmUser.builder()
                        .integrationId("1")
                        .cloudId("3")
                        .displayName("Srinath Chandrashekhar")
                        .originalDisplayName("Srinath Chandrashekhar")
                        .build(),
                dbScmCommit.getCommitterInfo());
        Assert.assertEquals("Srinath Chandrashekhar", dbScmCommit.getAuthor());
        Assert.assertEquals(DbScmUser.builder()
                        .integrationId("1")
                        .cloudId("3")
                        .displayName("Srinath Chandrashekhar")
                        .originalDisplayName("Srinath Chandrashekhar")
                        .build(),
                dbScmCommit.getAuthorInfo());
        Assert.assertEquals("Commit for test", dbScmCommit.getMessage());
        Assert.assertEquals(Long.valueOf("1622636137"), dbScmCommit.getCommittedAt());
        Assert.assertEquals(truncate, dbScmCommit.getIngestedAt());
        Assert.assertEquals("80091a3200b99700b55208a225e842e6f1e4c1af", dbScmCommit.getCommitSha());
    }

    @Test
    public void testFromHelixCoreChangeList() throws IOException {
        String data = ResourceUtils.getResourceAsString("helix/helix_changelist.json");
        HelixCoreChangeList changeList = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(HelixCoreChangeList.class));
        Set<String> repoIds = new HashSet<>(Arrays.asList("repo_1", "repo_2"));
        DbScmCommit dbScmCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, "1");
        Assert.assertEquals(List.of("repo_1", "repo_2"), dbScmCommit.getRepoIds());
        Assert.assertEquals("repo_1", dbScmCommit.getProject());
    }

    @Test
    public void testFromGitlabCommit() throws IOException {
        String data = ResourceUtils.getResourceAsString("gitlab/gitlab_commit.json");
        GitlabCommit gitlabCommit = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GitlabCommit.class));
        DbScmCommit dbScmCommit = DbScmCommit
                .fromGitlabCommit(gitlabCommit, "repo_1", "1");
        Assert.assertEquals(List.of("repo_1"), dbScmCommit.getRepoIds());
        Assert.assertEquals("repo_1", dbScmCommit.getProject());
    }

}
