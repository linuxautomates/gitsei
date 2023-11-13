package io.levelops.commons.client.oauth;

import io.levelops.commons.client.models.ApiKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaticRoundRobinOauthTokenProviderTest {
    @Test
    public void test() {
        Random rnd = new Random(System.currentTimeMillis());
        int n = rnd.nextInt(100);
        List<ApiKey> keys = new ArrayList<>();
        for(int i = 0; i < n ; i++) {
            keys.add(ApiKey.builder().apiKey("key-" + i).userName("user-name-" + i).build());
        }
        StaticRoundRobinOauthTokenProvider tokenProvider = StaticRoundRobinOauthTokenProvider.builder()
                .apiKeys(keys)
                .build();
        for(int i=0; i< 100; i++) {
            int index = i % n;
            Assert.assertEquals(keys.get(index).getApiKey(), tokenProvider.getToken());
        }
    }
}
