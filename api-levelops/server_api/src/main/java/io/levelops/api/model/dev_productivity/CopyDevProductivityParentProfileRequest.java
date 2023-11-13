package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CopyDevProductivityParentProfileRequest.CopyDevProductivityParentProfileRequestBuilder.class)
public class CopyDevProductivityParentProfileRequest {

    @JsonProperty("parent_profile")
    DevProductivityParentProfile parentProfile;

    @JsonProperty("target_ou_ref_ids")
    List<String> targetOuRefIds;
}
