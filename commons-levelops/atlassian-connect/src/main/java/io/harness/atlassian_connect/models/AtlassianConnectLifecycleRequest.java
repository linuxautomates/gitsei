package io.harness.atlassian_connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AtlassianConnectLifecycleRequest {
    @JsonProperty("key")
    String key;

    @JsonProperty("clientKey")
    String clientKey;

    @JsonProperty("sharedSecret")
    String sharedSecret;

    @JsonProperty("baseUrl")
    String baseUrl;

    @JsonProperty("displayUrl")
    String displayUrl;

    @JsonProperty("productType")
    String productType;

    @JsonProperty("description")
    String description;

    @JsonProperty("eventType")
    String eventType;

    public AtlassianConnectAppMetadata toMetadata(
            String sub,
            boolean enabled
    ) {
        return AtlassianConnectAppMetadata.builder()
                .atlassianClientKey(getClientKey())
                .installedAppKey(getKey())
                .atlassianBaseUrl(getBaseUrl())
                .atlassianDisplayUrl(MoreObjects.firstNonNull(getDisplayUrl(), ""))
                .productType(getProductType())
                .description(getDescription())
                .events(List.of(AtlassianConnectEvent.builder()
                        .eventType(getEventType())
                        .timestamp(Instant.now())
                        .build()))
                .atlassianUserAccountId(sub)
                .enabled(enabled)
                .otp("")
                .build();
    }
}
