package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.access.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessKey {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "bcrypt_secret")
    private String bcryptSecret;

    @JsonProperty(value = "role")
    private RoleType roleType;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}