package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder(toBuilder = true)
public class PropeloUserId {
    @JsonProperty("user_id")
    int userId;
    @JsonProperty("full_name")
    String fullName;
    @JsonProperty("email")
    String email;
}
