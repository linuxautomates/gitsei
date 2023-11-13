package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bean describing a RevisionInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#revision-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RevisionInfo.RevisionInfoBuilder.class)
public class RevisionInfo {

    @JsonProperty
    String kind;

    @JsonProperty("_number")
    String number;

    @JsonProperty
    LocalDateTime created;

    @JsonProperty
    AccountInfo uploader;

    @JsonProperty
    String ref;

    @JsonProperty
    Map<String, FetchInfo> fetch;

    @JsonProperty
    CommitInfo commit;

    @JsonProperty
    Map<String, FileInfo> files;

    @JsonProperty
    Map<String, ChangeInfo.ActionInfo> actions;

    @JsonProperty
    Boolean reviewed;

    @JsonProperty("commit_with_footers")
    String commitWithFooters;

    @JsonProperty("push_certificate")
    PushCertificateInfo pushCertificateInfo;

    @JsonProperty
    String description;

    @JsonProperty
    List<ReviewerInfo> reviewers;

    /**
     * Bean describing a FetchInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#fetch-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FetchInfo.FetchInfoBuilder.class)
    public static class FetchInfo {

        @JsonProperty
        String url;

        @JsonProperty
        String ref;

        @JsonProperty
        Map<String, String> commands;
    }

    /**
     * Bean describing a FileInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#file-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FileInfo.FileInfoBuilder.class)
    public static class FileInfo {

        @JsonProperty(defaultValue = "M")
        String status;

        @JsonProperty(defaultValue = "false")
        Boolean binary;

        @JsonProperty("old_path")
        String oldPath;

        @JsonProperty("lines_inserted")
        Integer linesInserted;

        @JsonProperty("lines_deleted")
        Integer linesDeleted;

        @JsonProperty("size_delta")
        String sizeDelta;

        @JsonProperty
        String size;
    }

    /**
     * Bean describing a PushCertificateInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#push-certificate-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PushCertificateInfo.PushCertificateInfoBuilder.class)
    public static class PushCertificateInfo {

        @JsonProperty
        String certificate;

        @JsonProperty
        GpgKeyInfo key;


        /**
         * Bean describing a GpgKeyInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-accounts.html#gpg-key-info
         */
        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = GpgKeyInfo.GpgKeyInfoBuilder.class)
        public static class GpgKeyInfo {

            @JsonProperty
            String id;

            @JsonProperty
            String fingerPrint;

            @JsonProperty("user_ids")
            List<String> userIds;

            @JsonProperty
            String key;

            @JsonProperty
            String status;

            @JsonProperty
            List<String> problems;
        }
    }
}
