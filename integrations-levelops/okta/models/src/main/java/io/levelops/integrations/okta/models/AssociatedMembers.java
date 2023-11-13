package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AssociatedMembers.AssociatedMembersBuilder.class)
public class AssociatedMembers {

    @JsonProperty("primaryName")
    String primaryName;

    @JsonProperty("primaryTitle")
    String primaryTitle;

    @JsonProperty("primaryDescription")
    String primaryDescription;

    @JsonProperty("associatedName")
    String associatedName;

    @JsonProperty("associatedTitle")
    String associatedTitle;

    @JsonProperty("associatedDescription")
    String associatedDescription;

    @JsonProperty("associatedMembers")
    List<String> associatedMembers;
}
