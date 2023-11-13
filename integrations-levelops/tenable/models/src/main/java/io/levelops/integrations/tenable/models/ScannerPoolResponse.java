package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for Scanner pools list response <a href="https://developer.tenable.com/reference#scanner-groups-list</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScannerPoolResponse.ScannerPoolResponseBuilder.class)
public class ScannerPoolResponse {
    @JsonProperty("scanner_pools")
    List<ScannerPool> scannerPools;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ScannerPool.ScannerPoolBuilder.class)
    public static class ScannerPool {
        @JsonProperty("creation_date")
        Long creationDate;

        @JsonProperty("last_modification_date")
        Long lastModificationDate;

        @JsonProperty("owner_id")
        Integer ownerId;

        @JsonProperty
        String owner;

        @JsonProperty("default_permissions")
        Integer defaultPermissions;

        @JsonProperty("user_permissions")
        Integer userPermissions;

        @JsonProperty
        Integer shared;

        @JsonProperty("scan_count")
        Integer scanCount;

        @JsonProperty("scanner_count")
        String scannerCount;

        @JsonProperty
        String uuid;

        @JsonProperty
        String token;

        @JsonProperty
        String flag;

        @JsonProperty
        String type;

        @JsonProperty
        String name;

        @JsonProperty("network_name")
        String networkName;

        @JsonProperty
        Integer id;

        @JsonProperty("scanner_id")
        Integer scannerId;
    }
}
