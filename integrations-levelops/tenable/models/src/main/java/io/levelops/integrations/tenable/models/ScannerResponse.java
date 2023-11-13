package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Bean for scanners list response <a href="https://developer.tenable.com/reference#scanners-list</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScannerResponse.ScannerResponseBuilder.class)
public class ScannerResponse {
    @JsonProperty
    List<Scanner> scanners;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Scanner.ScannerBuilder.class)
    public static class Scanner {
        @JsonProperty("creation_date")
        Long creationDate;

        @JsonProperty
        Boolean group;

        @JsonProperty
        Integer id;

        @JsonProperty
        String key;

        @JsonProperty("last_connect")
        String lastConnect;

        @JsonProperty("last_modification_date")
        Long lastModificationDate;

        @JsonProperty("engine_version")
        String engineVersion;

        @JsonProperty
        String platform;

        @JsonProperty("loaded_plugin_set")
        String loadedPluginSet;

        @JsonProperty("registration_code")
        String registrationCode;

        @JsonProperty
        License license;

        @JsonProperty
        Integer linked;

        @JsonProperty
        String name;

        @JsonProperty("network_name")
        String networkName;

        @JsonProperty("num_scans")
        Integer numScans;

        @JsonProperty
        String owner;

        @JsonProperty("owner_id")
        Integer ownerId;

        @JsonProperty("owner_name")
        String ownerName;

        @JsonProperty("owner_uuid")
        String ownerUUID;

        @JsonProperty
        Boolean pool;

        @JsonProperty("scan_count")
        Integer scanCount;

        @JsonProperty
        Boolean shared;

        @JsonProperty
        String source;

        @JsonProperty
        String status;

        @JsonProperty
        Long timestamp;

        @JsonProperty
        String type;

        @JsonProperty("user_permissions")
        Integer userPermissions;

        @JsonProperty
        String uuid;

        @JsonProperty("supports_remote_logs")
        Boolean supportsRemoteLogs;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = License.LicenseBuilder.class)
    public static class License {
        @JsonProperty
        String type;

        @JsonProperty
        Integer ips;

        @JsonProperty
        Integer agents;

        @JsonProperty
        Integer scanners;

        @JsonProperty
        Integer users;

        @JsonProperty("enterprise_pause")
        Boolean enterprisePause;

        @JsonProperty("expiration_date")
        Long expirationDate;

        @JsonProperty
        Boolean evaluation;

        @JsonProperty("scanners_used")
        Integer scannersUsed;

        @JsonProperty("agents_used")
        Integer agentsUsed;

        @JsonProperty
        Map<String, Object> apps;
    }
}
