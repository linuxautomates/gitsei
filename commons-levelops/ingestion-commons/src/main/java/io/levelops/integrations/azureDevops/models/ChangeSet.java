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
@JsonDeserialize(builder = ChangeSet.ChangeSetBuilder.class)
public class ChangeSet {

    @JsonProperty("changesetId")
    Integer changesetId;

    @JsonProperty("url")
    String url;

    @JsonProperty("createdDate")
    String createdDate;

    @JsonProperty("comment")
    String comment;

    @JsonProperty("author")
    IdentityRef author;

    @JsonProperty("checkedInBy")
    IdentityRef checkedInBy;

    @JsonProperty("changeSetChanges")
    List<ChangeSetChange> changeSetChanges;

    @JsonProperty("changeSetWorkitems")
    List<ChangeSetWorkitem> changeSetWorkitems;

}
