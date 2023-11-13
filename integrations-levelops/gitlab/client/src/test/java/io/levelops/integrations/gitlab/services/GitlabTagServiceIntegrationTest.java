package io.levelops.integrations.gitlab.services;

import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabProject;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

public class GitlabTagServiceIntegrationTest {

    private static GitlabClient gitlabClient;
    private static GitlabFetchTagService gitlabFetchTagService;

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor((StaticOauthTokenProvider.builder()
                        .build())))
                .build();
        gitlabClient = new GitlabClient(okHttpClient, DefaultObjectMapper.get(), "https://gitlab.com/", 0, false, false);
        gitlabFetchTagService = new GitlabFetchTagService();
    }

    @Test
    public void testTagsService() {

        GitlabProject project1 = GitlabProject.builder().id("31983767").build();
        int DEFAULT_PER_PAGE = 100;
        GitlabProject gitlabProject1 = gitlabFetchTagService.getProjectTags(gitlabClient, project1, DEFAULT_PER_PAGE).findFirst().orElse(null);
        Assert.assertNotNull(gitlabProject1.getTags());
        Assert.assertTrue(gitlabProject1.getTags().size() > 0);

        GitlabProject project2 = GitlabProject.builder().id("1111").build();
        GitlabProject gitlabProject2 = gitlabFetchTagService.getProjectTags(gitlabClient, project2, DEFAULT_PER_PAGE).findFirst().orElse(null);
        Assert.assertEquals(0, gitlabProject2.getTags().size());
    }
}