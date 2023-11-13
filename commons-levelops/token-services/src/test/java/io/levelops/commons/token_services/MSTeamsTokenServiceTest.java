package io.levelops.commons.token_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MSTeamsTokenServiceTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("msteams_oauth/msteams_oauth_v2_response.json");
        MSTeamsTokenService.MSTeamsOauthV2Response expected = MSTeamsTokenService.MSTeamsOauthV2Response.builder()
                .tokenType("Bearer")
                .expires_in(3600L)
                .accessToken("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5HVEZ2ZEstZnl0aEV1Q...")
                .refreshToken("AwABAAAAvPM1KaPlrEqdFSBzjqfTGAMxZGUTdM0t4B4...")
                .scope("user.read%20Fmail.read")
                .build();
        MSTeamsTokenService.MSTeamsOauthV2Response actual = MAPPER.readValue(serialized, MSTeamsTokenService.MSTeamsOauthV2Response.class);
        Assert.assertEquals(expected, actual);
    }

}