package io.levelops.ingestion.agent.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.tools.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import io.levelops.properties.YamlPropertySourceFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Component
@Setter
@ConfigurationProperties
@PropertySource(value = "file:${CONFIG_FILE:config.yml}", factory = YamlPropertySourceFactory.class)
@Profile("!encrypted")
public class SatelliteConfigFileProperties {

    /*
     * NOTE:
     * - Spring's @PropertySource is used for regular Yaml file.
     * - Jackson's @JsonProperty is used (with Yaml mapper) to parse encrypted Yaml file.
     */

    @JsonProperty("satellite")
    SatelliteProperties satellite;
    @JsonProperty("levelops")
    SatelliteProperties levelops; // backward compatibility
    @JsonProperty("jira")
    JiraProperties jira;
    @JsonProperty("github")
    GithubProperties github;
    @JsonProperty("helix")
    HelixProperties helix;
    @JsonProperty("integrations")
    List<IntegrationConfig> integrations;

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class SatelliteProperties {
        @JsonProperty("url")
        String url;
        @JsonProperty("api_key")
        String apiKey;
        @JsonProperty("tenant")
        String tenant;
        @JsonProperty("scheduling_interval")
        Long schedulingInterval;
        @JsonProperty("proxy")
        ProxySettings proxy;

        @Getter
        @Setter
        @NoArgsConstructor
        @ToString
        public static class ProxySettings {
            @JsonProperty("type")
            String type; // "http" or "socks", see java.net.Proxy.Type
            @JsonProperty("host")
            String host;
            @JsonProperty("port")
            Integer port;
            @JsonProperty("username")
            String username;
            @JsonProperty("password")
            String password;
            @JsonProperty("authorization_header")
            String authorizationHeader;
            @JsonProperty("all_traffic")
            Boolean allTraffic;

            public String getType() {
                return StringUtils.defaultIfBlank(type, "http");
            }

            public int getPort() {
                return MoreObjects.firstNonNull(port, 8080);
            }

            public String getAuthorizationHeader() {
                return StringUtils.defaultIfBlank(authorizationHeader, "Proxy-Authorization");
            }

            public boolean getAllTraffic() {
                return ObjectUtils.firstNonNull(allTraffic, false);
            }
        }

        public String getUrl() {
            return StringUtils.defaultIfBlank(url, "https://api.levelops.io");
        }

        public Long getSchedulingInterval() {
            return MoreObjects.firstNonNull(schedulingInterval, 60L);
        }

        public ProxySettings getProxy() {
            return proxy != null ? proxy : new ProxySettings();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class JiraProperties {
        @JsonProperty("allow_unsafe_ssl")
        Boolean allowUnsafeSsl;
        @JsonProperty("project")
        String project;
        @JsonProperty("rate_limit_per_second")
        Double rateLimitPerSecond;

        public Boolean getAllowUnsafeSsl() {
            return BooleanUtils.isTrue(allowUnsafeSsl);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class GithubProperties {
        @JsonProperty("throttling_ms")
        Integer throttlingMs;

        @JsonProperty("enable_caching")
        Boolean enableCaching;

        public Integer getThrottlingMs() {
            return MoreObjects.firstNonNull(throttlingMs, 100);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class HelixProperties {
        @JsonProperty("max_file_size")
        Integer maxFileSize;

        @JsonProperty("zone_id")
        String zoneId;

        public Integer getMaxFileSize() {
            return maxFileSize;
        }

        public String getZoneId() {
            return zoneId;
        }
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class IntegrationConfig {
        @JsonProperty("id")
        String id;
        @JsonProperty("application")
        String application;
        @JsonProperty("url")
        String url;
        @JsonProperty("metadata")
        Map<String, Object> metadata;
        @JsonProperty("satellite")
        Boolean satellite;
        @JsonProperty("authentication")
        String authentication; // api_key (default), multiple_api_keys, oauth, oauth1, adfs

        // region api key
        @JsonAlias({"username", "user_name"})
        String userName;
        @JsonProperty("api_key")
        String apiKey;
        @JsonProperty("keys")
        List<ApiKey> keys;
        // endregion

        // region oauth1
        @JsonProperty("private_key")
        String privateKey;
        @JsonProperty("consumer_key")
        String consumerKey;
        @JsonProperty("verification_code")
        String verificationCode;
        @JsonProperty("access_token")
        String accessToken;
        // endregion

        // region oauth/bearer
        @JsonProperty("token")
        String token;
        // endregion

        // region adfs
        @JsonProperty("adfs_url")
        String adfsUrl;
        @JsonProperty("adfs_client_id")
        String adfsClientId;
        @JsonProperty("adfs_resource")
        String adfsResource;
        @JsonProperty("adfs_username")
        String adfsUsername;
        @JsonProperty("adfs_password")
        String adfsPassword;
        // endregion
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class ApiKey {
        // region api key
        @JsonProperty("user_name")
        String userName;
        @JsonProperty("api_key")
        String apiKey;
        // endregion
    }

    public SatelliteProperties getSatellite() {
        if (satellite != null) {
            return satellite;
        }
        if (levelops != null) {
            return levelops;
        }
        this.satellite = new SatelliteProperties();
        return satellite;
    }

    public JiraProperties getJira() {
        return jira != null ? jira : new JiraProperties();
    }

    public GithubProperties getGithub() {
        return github != null ? github : new GithubProperties();
    }

    public HelixProperties getHelix() {
        return helix != null ? helix : new HelixProperties();
    }

    public List<IntegrationConfig> getIntegrations() {
        return ListUtils.emptyIfNull(integrations);
    }

    @JsonIgnore
    public List<String> getIntegrationIds() {
        if (integrations == null) {
            return Collections.emptyList();
        }
        return integrations.stream()
                .map(IntegrationConfig::getId)
                .collect(Collectors.toList());
    }
}
