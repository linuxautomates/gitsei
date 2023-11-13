package io.levelops.commons.databases.models.database.atlassian_connect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AtlassianConnectAppMetadata.AtlassianConnectAppMetadataBuilder.class)
public class AtlassianConnectAppMetadata {
    @JsonProperty("id")
    String id;

    @NonNull
    @JsonProperty("atlassian_client_key")
    String atlassianClientKey;

    @NonNull
    @JsonProperty("installed_app_key")
    String installedAppKey;

    @NonNull
    @JsonProperty("atlassian_base_url")
    String atlassianBaseUrl;

    @JsonProperty("atlassian_display_url")
    String atlassianDisplayUrl;

    @NonNull
    @JsonProperty("product_type")
    String productType;

    @NonNull
    @JsonProperty("description")
    String description;

    @JsonProperty("events")
    List<AtlassianConnectEvent> events;

    @JsonProperty("atlassian_user_account_id")
    String atlassianUserAccountId;

    @JsonProperty("otp")
    String otp;

    @NonNull
    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;
}
