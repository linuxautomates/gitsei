package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bean describing a Project from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-projects.html#project-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ProjectInfo.ProjectInfoBuilder.class)
public class ProjectInfo {

    @JsonProperty
    String id;

    @JsonProperty
    String name;

    @JsonProperty
    String parent;

    @JsonProperty
    String description;

    @JsonProperty
    String state;

    @JsonProperty
    Map<String, String> branches;

    @JsonProperty
    Map<String, LabelTypeInfo> labels;

    @JsonProperty("web_links")
    List<WebLinkInfo> webLinks;

    @JsonProperty
    List<BranchInfo> enrichedBranches;

    @JsonProperty
    List<TagInfo> tags;

    @JsonProperty
    List<LabelDefinitionInfo> enrichedLabels;

    @JsonProperty
    List<ChangeInfo> changes;

    /**
     * Bean describing a Branch from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-projects.html#branch-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BranchInfo.BranchInfoBuilder.class)
    public static class BranchInfo {

        @JsonProperty
        String ref;

        @JsonProperty
        String revision;

        @JsonProperty(value = "can_delete", defaultValue = "false")
        boolean canDelete;

        @JsonProperty("web_links")
        List<WebLinkInfo> webLinks;
    }

    /**
     * Bean describing a Tag from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-projects.html#tag-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TagInfo.TagInfoBuilder.class)
    public static class TagInfo {

        @JsonProperty
        String tag;

        @JsonProperty
        String revision;

        @JsonProperty
        String object;

        @JsonProperty
        String message;

        @JsonProperty
        GitPersonInfo tagger;

        @JsonProperty
        LocalDateTime created;

        @JsonProperty(value = "can_delete", defaultValue = "false")
        boolean canDelete;

        @JsonProperty("web_links")
        List<WebLinkInfo> weblinks;

        /**
         * Bean describing a GitPersonInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#git-person-info
         */
        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = GitPersonInfo.GitPersonInfoBuilder.class)
        public static class GitPersonInfo {

            @JsonProperty
            String name;

            @JsonProperty
            String email;

            @JsonProperty
            LocalDateTime date;

            @JsonProperty("tz")
            String timeZone;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = LabelTypeInfo.LabelTypeInfoBuilder.class)
    public static class LabelTypeInfo {

        @JsonProperty
        Map<String, String> values;

        @JsonProperty("default_value")
        String defaultValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WebLinkInfo.WebLinkInfoBuilder.class)
    public static class WebLinkInfo {

        @JsonProperty
        String name;

        @JsonProperty
        String url;

        @JsonProperty("image_url")
        String imageUrl;
    }

    /**
     * Bean describing a LabelDefinitionInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-projects.html#label-definition-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = LabelDefinitionInfo.LabelDefinitionInfoBuilder.class)
    public static class LabelDefinitionInfo {

        @JsonProperty
        String name;

        @JsonProperty("project_name")
        String projectName;

        @JsonProperty
        String function;

        @JsonProperty
        Map<String, String> values;

        @JsonProperty("default_value")
        Integer defaultValue;

        @JsonProperty
        String branches;

        @JsonProperty(value = "can_override", defaultValue = "false")
        Boolean canOverride;

        @JsonProperty(value = "copy_any_score", defaultValue = "false")
        Boolean copyAnyScore;

        @JsonProperty(value = "copy_min_score", defaultValue = "false")
        Boolean copyMinScore;

        @JsonProperty(value = "copy_all_scores_if_no_change", defaultValue = "false")
        Boolean copyAllScoresIfNoChange;

        @JsonProperty(value = "copy_all_scores_if_no_code_change", defaultValue = "false")
        Boolean copyAllScoresIfNoCodeChange;

        @JsonProperty(value = "copy_all_scores_on_trivial_rebase", defaultValue = "false")
        Boolean copyAllScoresOnTrivialRebase;

        @JsonProperty("copy_all_scores_on_merge_first_parent_update")
        Boolean copyAllScoresOnMergeFirstParentUpdate;

        @JsonProperty("copy_values")
        String copyValues;

        @JsonProperty(value = "allow_post_submit", defaultValue = "false")
        Boolean allowPostSubmit;

        @JsonProperty(value = "ignore_self_approval", defaultValue = "false")
        Boolean ignoreSelfApproval;

    }
}
