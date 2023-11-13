package io.levelops.commons.databases.models.database.helix;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DbScmFileTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testRepoConfigFilter() throws IOException {
        HelixCoreChangeList changeList = parseChangeList("helix/helix_changelist.json");
        List<IntegrationConfig.RepoConfigEntry> configEntries = parseRepoConfig("helix/helix_integ_config.json");

        List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, "11", configEntries);
        Assert.assertEquals(3, dbScmFiles.size());
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getProject());

        List<DbScmFile> dbScmFilesWithZeroConfigEntries = DbScmFile.fromHelixCoreChangeList(changeList, "11", List.of());
        Assert.assertEquals(0, dbScmFilesWithZeroConfigEntries.size());

        Optional<DbScmFile> nonMatchingFileRepoId = dbScmFiles.stream()
                .filter(dbScmFile -> !dbScmFile.getRepoId().equals("repo_1"))
                .findFirst();
        Assert.assertTrue(nonMatchingFileRepoId.isEmpty());
    }

    private List<IntegrationConfig.RepoConfigEntry> parseRepoConfig(String resourceUrl) throws IOException {
        String configData = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(configData,
                MAPPER.getTypeFactory().constructCollectionType(List.class, IntegrationConfig.RepoConfigEntry.class));
    }

    private HelixCoreChangeList parseChangeList(String resourceUrl) throws IOException {
        String data = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(HelixCoreChangeList.class));
    }

    @Test
    public void testFromGithubCommit() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_commit.json");
        GithubCommit githubCommit = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubCommit.class));
        List<DbScmFile> dbScmFiles = DbScmFile
                .fromGithubCommit(githubCommit, "repo_1", "1", 1619531929L);
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getRepoId());
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getProject());
    }

    @Test
    public void testFromBitbucketCommit() throws IOException {
        String data = ResourceUtils.getResourceAsString("bitbucket/bitbucket_commit.json");
        BitbucketCommit bitbucketCommit = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(BitbucketCommit.class));
        List<DbScmFile> dbScmFiles = DbScmFile
                .fromBitbucketCommit(bitbucketCommit, "repo_1", "1", 1619531929L);
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getRepoId());
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getProject());
    }

    @Test
    public void testFromHelixCoreChangeList() throws IOException {
        String data = ResourceUtils.getResourceAsString("helix/helix_changelist.json");
        HelixCoreChangeList changeList =  MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(HelixCoreChangeList.class));
        Set<String> repoIds = new HashSet<>(Arrays.asList("repo_1", "repo_2"));
        String configData = ResourceUtils.getResourceAsString("helix/helix_integ_config.json");
        List<IntegrationConfig.RepoConfigEntry> configEntries = MAPPER.readValue(configData,
                MAPPER.getTypeFactory().constructCollectionType(List.class, IntegrationConfig.RepoConfigEntry.class));
        List<DbScmFile> dbScmFiles = DbScmFile
                .fromHelixCoreChangeList(changeList, "1", configEntries);
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getRepoId());
        Assert.assertEquals("repo_1", dbScmFiles.get(0).getProject());
    }

}