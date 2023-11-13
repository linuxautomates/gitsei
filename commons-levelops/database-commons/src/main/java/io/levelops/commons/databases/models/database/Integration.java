package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Integration {
    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("url")
    private String url;

    @JsonProperty("status")
    private String status;

    @JsonProperty("application")
    private String application;

    @JsonProperty("satellite")
    private Boolean satellite;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("append_metadata")
    private boolean appendMetadata;

    /**
     * If the integration supports multiple authentication schemes, this represents the one that was selected.
     */
    @JsonProperty("authentication")
    Authentication authentication;

    @JsonProperty("linked_credentials")
    private String linkedCredentials; // (optional) integration id; if specified, this integration will use the credentials of that integration instead

    public enum Authentication {
        UNKNOWN, // leaving this for migration purposes
        NONE, // no authentication needed or provided
        OAUTH,
        API_KEY,
        MULTIPLE_API_KEYS,
        DB,
        ATLASSIAN_CONNECT_JWT,
        ADFS;

        @JsonCreator
        @Nullable
        public static Authentication fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Authentication.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    @JsonIgnore
    public boolean isGithubAppsIntegration() {
        if (application.equals("github")) {
            Map<String, Object> metadata = MapUtils.emptyIfNull(getMetadata());
            return metadata.containsKey("app_id");
        }
        return false;
    }
}