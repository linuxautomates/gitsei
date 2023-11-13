package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ListUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = IntegrationConfig.IntegrationConfigBuilder.class)
public class IntegrationConfig {
    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("config")
    private Map<String, List<ConfigEntry>> config;

    @JsonProperty("repository_config")
    private List<RepoConfigEntry> repoConfig;

    @JsonProperty("custom_hygienes")
    private List<CustomHygieneEntry> customHygieneList;

    @JsonProperty("metadata")
    private Metadata metadata;

    @JsonProperty("created_at")
    private Long createdAt;

    @Nonnull
    public List<IntegrationConfig.ConfigEntry> getConfigEntries(@Nonnull String key) {
        return ListUtils.emptyIfNull(MapUtils.emptyIfNull(config).get(key));
    }

    /**
     * Returns true if and only if all the config entries match.
     */
    public boolean isConfigEqualTo(IntegrationConfig other) {
        var configA = MapUtils.emptyIfNull(this.getConfig());
        var configB = MapUtils.emptyIfNull(other != null? other.getConfig() : null);

        // we can't just compare key sets, because keys with an empty list are equivalent to missing keys,
        // so we will join both key sets and look at the values

        for (String key : SetUtils.union(configA.keySet(), configB.keySet())) {
            Set<ConfigEntry> entriesA = new HashSet<>(ListUtils.emptyIfNull(configA.get(key)));
            Set<ConfigEntry> entriesB = new HashSet<>(ListUtils.emptyIfNull(configB.get(key)));
            if (!SetUtils.isEqualSet(entriesA, entriesB)) {
                return false;
            }
        }

        return true;
    }

    @Value
    @EqualsAndHashCode
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(builder = ConfigEntry.ConfigEntryBuilder.class)
    public static class ConfigEntry {
        @JsonProperty("name")
        String name;
        
        @JsonProperty("key")
        String key;

        @JsonProperty("delimiter")
        String delimiter;
    }

    @Value
    @EqualsAndHashCode
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(builder = RepoConfigEntry.RepoConfigEntryBuilder.class)
    public static class RepoConfigEntry {
        @JsonProperty("repo_id")
        String repoId;

        @JsonProperty("path_prefix")
        String pathPrefix;
    }

    @Value
    @EqualsAndHashCode
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(builder = CustomHygieneEntry.CustomHygieneEntryBuilder.class)
    public static class CustomHygieneEntry {
        @JsonProperty("id")
        String id;

        @JsonProperty("name")
        String name;

        @JsonProperty("filter")
        Map<String, Object> filter;

        @JsonProperty("missing_fields")
        Map<String, Boolean> missingFields;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Metadata.MetadataBuilder.class)
    public static class Metadata {
        @JsonProperty("config_updated_at")
        Long configUpdatedAt;
    }
}
