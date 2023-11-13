package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class SlackTokenServiceTest  {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("slack_oauth/slack_oauth_v2_response.json");
        SlackTokenService.SlackOauthV2Response expected = SlackTokenService.SlackOauthV2Response.builder()
                .ok(true)
                .accessToken("xoxb-17653672481-19874698323-pdFZKVeTuE8sk7oOcBrzbqgy").tokenType("bot")
                .scope("commands,incoming-webhook")
                .botUserId("U0KRQLJ9H").appId("A0KRD7HC3")
                .team(SlackTokenService.SlackTeam.builder().id("T9TK3CUKW").name("Slack Softball Team").build())
                .authedUser(SlackTokenService.SlackAuthedUser.builder()
                        .id("U1234").scope("chat:write").accessToken("xoxp-1234").tokenType("user")
                        .build())
                .build();
        SlackTokenService.SlackOauthV2Response actual = MAPPER.readValue(serialized, SlackTokenService.SlackOauthV2Response.class);
        Assert.assertEquals(expected, actual);
    }
}