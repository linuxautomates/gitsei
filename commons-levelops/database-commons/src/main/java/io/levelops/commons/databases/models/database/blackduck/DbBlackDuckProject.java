package io.levelops.commons.databases.models.database.blackduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DbBlackDuckProject {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("attributes")
    DbBlackDuckProjectAttributes attributes;

    @JsonProperty("proj_created_at")
    private Date projCreatedAt;

    @JsonProperty("proj_updated_at")
    private Date projUpdatedAt;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;


}
