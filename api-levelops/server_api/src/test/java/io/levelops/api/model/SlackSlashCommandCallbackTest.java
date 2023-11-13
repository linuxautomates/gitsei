package io.levelops.api.model;

import io.levelops.api.model.slack.SlackSlashCommandCallback;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SlackSlashCommandCallbackTest {

    @Test
    public void test() throws IOException {
        String input = "token=abc&team_id=TM3U03S49&team_domain=propeloai&channel_id=DS4ULC8UW&channel_name=directmessage&user_id=UMC5G1JLX&user_name=maxime&command=%2Flevelops&text=test&api_app_id=AMUPV36SC&is_enterprise_install=false&response_url=https%3A%2F%2Fhooks.slack.com%2Fcommands%2FTM3U03S49%2F3391683824384%2FH9k4FqfaPQR6hODsyl2OCSHF&trigger_id=3370278772036.717952128145.bcb0eb1c4636a390e2d8b5a1252294c5";
        SlackSlashCommandCallback out = SlackSlashCommandCallback.fromFormDataString(input);
        DefaultObjectMapper.prettyPrint(out);
        assertThat(out.getToken()).isEqualTo("abc");
        assertThat(out.getUserName()).isEqualTo("maxime");
        assertThat(out.getCommand()).isEqualTo("/levelops");
        assertThat(out.getUserId()).isEqualTo("UMC5G1JLX");
        assertThat(out.getApiAppId()).isEqualTo("AMUPV36SC");
        assertThat(out.getTeamId()).isEqualTo("TM3U03S49");
        assertThat(out.getTeamDomain()).isEqualTo("propeloai");
        assertThat(out.getTriggerId()).isEqualTo("3370278772036.717952128145.bcb0eb1c4636a390e2d8b5a1252294c5");
    }
}