package io.levelops.integrations.github.models;

import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubWebhookTest {

    @Test
    public void deserializeGithubWebhookRequest() throws IOException {
        GithubWebhookRequest webhookRequest = ResourceUtils.getResourceAsObject("integrations/github/webhook_request.json",
                GithubWebhookRequest.class);
        assertThat(webhookRequest.getName()).isEqualTo("web");
        assertThat(webhookRequest.getConfig().getContentType()).isEqualTo("json");
        assertThat(webhookRequest.getConfig().getUrl()).isEqualTo("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28");
        assertThat(webhookRequest.getConfig().getSecret()).isEqualTo("########");
        assertThat(webhookRequest.getConfig().getInsecureSsl()).isEqualTo(String.valueOf(0));
    }

    @Test
    public void deserializeGithubWebhookResponse() throws IOException {
        GithubWebhookResponse webhookResponse = ResourceUtils.getResourceAsObject("integrations/github/webhook_response.json",
                GithubWebhookResponse.class);
        assertThat(webhookResponse.getId()).isEqualTo(296776201);
        assertThat(webhookResponse.getName()).isEqualTo("web");
        assertThat(webhookResponse.getType()).isEqualTo("Organization");
        assertThat(webhookResponse.getActive()).isEqualTo(true);
        assertThat(webhookResponse.getUrl()).isEqualTo("https://api.github.com/orgs/cog1/hooks/296776201");
        assertThat(webhookResponse.getPingUrl()).isEqualTo("https://api.github.com/orgs/cog1/hooks/296776201/pings");
        assertThat(webhookResponse.getConfig().getContentType()).isEqualTo("json");
        assertThat(webhookResponse.getConfig().getUrl()).isEqualTo("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28");
        assertThat(webhookResponse.getConfig().getSecret()).isEqualTo("********");
    }
}
