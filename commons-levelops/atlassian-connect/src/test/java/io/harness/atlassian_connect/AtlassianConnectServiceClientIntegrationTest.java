package io.harness.atlassian_connect;

import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

class AtlassianConnectServiceClientIntegrationTest {
    @Test
    public void testMetadata() throws AtlassianConnectServiceClientException {
        AtlassianConnectServiceClient client = new AtlassianConnectServiceClient(
                "http://localhost:8080",
                new OkHttpClient(),
                DefaultObjectMapper.get()
        );
        var metadata = client.getMetadata("95fd3c11-6016-3664-8b98-57ade60ec451");
        var secret = client.getSecret("95fd3c11-6016-3664-8b98-57ade60ec451");

        System.out.println(metadata);
        System.out.println(secret);
    }

    @Test
    public void testOtp() throws AtlassianConnectServiceClientException {
        AtlassianConnectServiceClient client = new AtlassianConnectServiceClient(
                "http://localhost:8080",
                new OkHttpClient(),
                DefaultObjectMapper.get()
        );
        var otp = client.generateOtp("foo");
        var submitResult = client.submitOtp("95fd3c11-6016-3664-8b98-57ade60ec451", otp);
        var claimResult = client.claimOtp("foo", otp);

        System.out.println(otp);
        System.out.println(submitResult);
        System.out.println(claimResult);
    }
}