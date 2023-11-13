package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for web application scanning vulnerability respnse <a href="https://developer.tenable.com/reference#was-v2-vulns-list</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WASResponse.WASResponseBuilder.class)
public class WASResponse {

    @JsonProperty("page_number")
    Integer pageNumber;

    @JsonProperty("page_size")
    Integer pageSize;

    @JsonProperty("order_by")
    String orderBy;

    @JsonProperty
    String ordering;

    @JsonProperty("total_size")
    Integer totalSize;

    @JsonProperty
    List<Data> data;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Data.DataBuilder.class)
    public static class Data {
        @JsonProperty("vuln_id")
        String vulnId;

        @JsonProperty("scan_id")
        String scanId;

        @JsonProperty("plugin_id")
        Integer pluginId;

        @JsonProperty("created_at")
        String createdAt;

        @JsonProperty
        String uri;

        @JsonProperty
        Detail details;

        @JsonProperty
        List<Attachment> attachments;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Detail.DetailBuilder.class)
    public static class Detail {
        @JsonProperty
        String output;

        @JsonProperty
        String payload;

        @JsonProperty
        String proof;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Attachment.AttachmentBuilder.class)
    public static class Attachment {
        @JsonProperty("attachment_id")
        String attachmentId;

        @JsonProperty("created_at")
        String createdAt;

        @JsonProperty("attachment_name")
        String attachmentName;

        @JsonProperty
        String md5;

        @JsonProperty("file_type")
        String fileType;
    }
}
