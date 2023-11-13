package io.levelops.integrations.github.services;

import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubTag;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GithubTagServiceIntegrationTest {
    private static GithubClient githubClient;
    private static GithubTagService githubTagService;

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor((StaticOauthTokenProvider.builder()
                        .token(System.getenv("GITHUB_API_KEY"))
                        .build())))
                .build();
        githubClient = new GithubClient(okHttpClient, null, DefaultObjectMapper.get(), null, 0, true);
        githubTagService = new GithubTagService();
    }

    @Test
    public void testTagsService() {

        int DEFAULT_PER_PAGE = 100;
        List<GithubTag> githubTags1 = githubTagService.getTags(githubClient, "ToolJet", "ToolJet", DEFAULT_PER_PAGE);
        Assert.assertNotNull(githubTags1);
        Assert.assertTrue(githubTags1.size() > 0);

        List<GithubTag> githubTags2 = githubTagService.getTags(githubClient, "ToolJet", "", DEFAULT_PER_PAGE);
        Assert.assertEquals(0, githubTags2.size());

        List<GithubTag> githubTags3 = githubTagService.getTags(githubClient, "", "", DEFAULT_PER_PAGE);
        Assert.assertEquals(0, githubTags3.size());
    }
}