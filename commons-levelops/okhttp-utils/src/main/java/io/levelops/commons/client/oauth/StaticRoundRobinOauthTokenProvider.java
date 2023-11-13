package io.levelops.commons.client.oauth;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.levelops.commons.client.models.ApiKey;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class StaticRoundRobinOauthTokenProvider implements OauthTokenProvider {
    private final Iterator<ApiKey> iterator;
    private final String refreshToken;

    StaticRoundRobinOauthTokenProvider(final Iterator<ApiKey> iterator, final String refreshToken) {
        this.iterator = iterator;
        this.refreshToken = refreshToken;
    }

    @Nullable
    @Override
    public String getToken() {
        return iterator.next().getApiKey();
    }

    @Nullable
    @Override
    public String refreshToken() {
        return refreshToken;
    }

    public static StaticRoundRobinOauthTokenProviderBuilder builder() {
        return new StaticRoundRobinOauthTokenProviderBuilder();
    }

    public static class StaticRoundRobinOauthTokenProviderBuilder {
        private List<ApiKey> apiKeys;
        private String refreshToken;

        StaticRoundRobinOauthTokenProviderBuilder() {
        }

        public StaticRoundRobinOauthTokenProviderBuilder apiKeys(List<ApiKey> apiKeys) {
            this.apiKeys = apiKeys;
            return this;
        }
        public StaticRoundRobinOauthTokenProviderBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public StaticRoundRobinOauthTokenProvider build() {
            ImmutableSet.Builder<ApiKey> builder = ImmutableSet.builder();
            builder.addAll(apiKeys);
            return new StaticRoundRobinOauthTokenProvider(Iterables.cycle(builder.build()).iterator(), refreshToken);
        }

        public String toString() {
            return "StaticRoundRobinOauthTokenProvider.StaticRoundRobinOauthTokenProviderBuilder(apiKeys=" + this.apiKeys + ", refreshToken=" + this.refreshToken + ")";
        }
    }
}
