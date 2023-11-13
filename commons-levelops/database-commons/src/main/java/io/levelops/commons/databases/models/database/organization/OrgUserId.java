package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder(toBuilder = true)
public class OrgUserId {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("ref_id")
    int refId;
    @JsonProperty("full_name")
    String fullName;
    @JsonProperty("email")
    String email;
}
