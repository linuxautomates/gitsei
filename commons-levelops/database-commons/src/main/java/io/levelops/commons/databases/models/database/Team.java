package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Team {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "organization_id")
    private String organizationId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}
