package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VCSSettings.VCSSettingsBuilder.class)
public class VCSSettings {

    @JsonProperty("git_settings")
    GitSettings gitSettings;

    @JsonProperty("exclude_settings")
    ExcludeSettings excludeSettings;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitSettings.GitSettingsBuilder.class)
    public static class GitSettings {

        @JsonProperty("link")
        Link link;

        @JsonProperty("url")
        String url;

        @JsonProperty("branch")
        String branch;

        @JsonProperty("useSsh")
        boolean useSsh;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ExcludeSettings.ExcludeSettingsBuilder.class)
    public static class ExcludeSettings {
        @JsonProperty("link")
        Link link;

        @JsonProperty("projectId")
        String projectId;

        @JsonProperty("excludeFoldersPattern")
        String excludeFoldersPattern;

        @JsonProperty("excludeFilesPattern")
        String excludeFilesPattern;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Uri.UriBuilder.class)
    public static class Uri {
        @JsonProperty("absoluteUrl")
        String absoluteUrl;

        @JsonProperty("port")
        String port;
    }
}

