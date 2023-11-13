package io.levelops.integrations.github.client;

import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubProjectCard;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class GithubClientProjectIntegrationTest {

    private GithubClient client;

    @Before
    public void setUp() {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor((StaticOauthTokenProvider.builder()
                        .token(System.getenv("GITHUB_API_KEY"))
                        .build())))
                .build();
        client = new GithubClient(okHttpClient, null, DefaultObjectMapper.get(), null, 0, true);
    }

    // Assumption : In the organisation provided, there is at least one project, with at least one column, with at least one card
    @Test
    public void test() throws GithubClientException {
        List<GithubProject> projects = client.streamProjects(System.getenv("GITHUB_ORG_NAME"))
                .collect(Collectors.toList());
        Assert.assertNotNull(projects);
        Assert.assertTrue(projects.size() > 0);

        List<GithubProjectColumn> columns = client.streamProjectColumns(projects.get(0).getId())
                .collect(Collectors.toList());
        Assert.assertNotNull(columns);
        Assert.assertTrue(columns.size() > 0);

        List<GithubProjectCard> cards = client.streamProjectColumnCards(columns.get(0).getId(), true)
                .collect(Collectors.toList());
        Assert.assertNotNull(cards);
        Assert.assertTrue(cards.size() > 0);
    }
}
