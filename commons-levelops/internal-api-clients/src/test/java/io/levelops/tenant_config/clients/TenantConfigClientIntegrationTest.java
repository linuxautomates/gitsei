package io.levelops.tenant_config.clients;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.OkHttpClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class TenantConfigClientIntegrationTest {
    @Test
    public void test() throws InternalApiClientException {
        TenantConfigClient client = new TenantConfigClient(
                new OkHttpClient(), DefaultObjectMapper.get(), "http://127.0.0.1:8080"
        );
        var a = client.get("foo", "SPRINT_GRACE_PERIOD");
        a = client.get("foo", "junk");
        System.out.println(a);
    }
}
