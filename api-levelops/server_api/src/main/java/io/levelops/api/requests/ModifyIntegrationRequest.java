package io.levelops.api.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModifyIntegrationRequest {
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "description", defaultValue = "")
    private String description;

    @JsonProperty(value = "tags")
    private List<String> tags;

    @JsonProperty("url")
    private String url;

    @JsonProperty("method")
    private String type;

    @JsonProperty("application")
    private String application;

    @JsonProperty("code")
    private String code;

    @JsonProperty("state")
    private String state;

    @JsonProperty("username")
    private String username;

    @JsonProperty("apikey")
    private String apikey;

    @JsonProperty("keys")
    private List<Key> keys;

    @JsonProperty("satellite")
    private Boolean satellite;

    @JsonProperty("server")
    private String server;

    @JsonProperty("password")
    private String password;

    @JsonProperty("database_name")
    private String databaseName;

    @JsonProperty("private_key")
    private String privateKey;

    @JsonProperty("gha_installation_id")
    private String ghaInstallationId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("skip_preflight_check")
    private Boolean skipPreflightCheck;

    @JsonProperty("start_ingestion")
    private Boolean startIngestion; // defaults to true when creating integration

    @JsonProperty("client_key")
    private String clientKey;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Key {
        @JsonProperty("username")
        private String username;

        @JsonProperty("apikey")
        private String apikey;
    }
}
