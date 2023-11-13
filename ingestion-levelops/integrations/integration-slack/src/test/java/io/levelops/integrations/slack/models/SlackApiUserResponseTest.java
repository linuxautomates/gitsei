package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackApiUserResponseTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_users/slack_user_response_1.json");
        SlackApiUserResponse actual = MAPPER.readValue(serialized, SlackApiUserResponse.class);
        Assert.assertNotNull(actual);

        SlackApiUser.Profile profile = SlackApiUser.Profile.builder()
                .title("").phone("")
                .realName("Viraj Ajgaonkar").realNameNormalized("Viraj Ajgaonkar")
                .displayName("Viraj Ajgaonkar").displayNameNormalized("Viraj Ajgaonkar")
                .email("viraj@levelops.io")
                .team("TM3U03S49")
                .build();

        SlackApiUser user = SlackApiUser.builder()
                .id("US4JC7ZM5")
                .teamId("TM3U03S49")
                .name("viraj")
                .deleted(false)
                .color("bb86b7")
                .realName("Viraj Ajgaonkar")
                .tz("America/Los_Angeles")
                .tzLabel("Pacific Daylight Time")
                .isAdmin(false)
                .isOwner(false)
                .isPrimaryOwner(false)
                .isRestricted(false)
                .isUltraRestricted(false)
                .isBot(false)
                .isAppUser(false)
                .profile(profile)
                .build();

        SlackApiUserResponse expected = SlackApiUserResponse.builder()
                .user(user)
                .build();

        Assert.assertEquals(expected, actual);
    }
}