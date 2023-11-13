package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Branch.BranchBuilder.class)
public class Branch {

    @JsonProperty("path")
    String path;

    @JsonProperty("owner")
    IdentityRef owner;

    @JsonProperty("createdDate")
    String createdDate;

    @JsonProperty("url")
    String url;

    @JsonProperty("relatedBranches")
    List<RelatedBranch> relatedBranches;

    @JsonProperty("mappings")
    List<Mapping> mappings;

    @JsonProperty("children")
    List<Branch> children;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RelatedBranch.RelatedBranchBuilder.class)
    public static class RelatedBranch {
        @JsonProperty("path")
        String path;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Mapping.MappingBuilder.class)
    public static class Mapping {
        @JsonProperty("depth")
        String depth;

        @JsonProperty("serverItem")
        String serverItem;

        @JsonProperty("type")
        String type;
    }

}
