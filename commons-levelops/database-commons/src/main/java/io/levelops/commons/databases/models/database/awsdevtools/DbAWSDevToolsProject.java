package io.levelops.commons.databases.models.database.awsdevtools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.awsdevtools.models.CBProject;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAWSDevToolsProject.DbAWSDevToolsProjectBuilder.class)
public class DbAWSDevToolsProject {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("name")
    String name;

    @JsonProperty("arn")
    String arn;

    @JsonProperty("project_created_at")
    Date projectCreatedAt;

    @JsonProperty("project_modified_at")
    Date projectModifiedAt;

    @JsonProperty("source_type")
    String sourceType;

    @JsonProperty("source_location")
    String sourceLocation;

    @JsonProperty("source_version")
    String sourceVersion;

    @JsonProperty("region")
    String region;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    public static DbAWSDevToolsProject fromProject(CBProject project, String integrationId) {
        Date date = new Date();
        return DbAWSDevToolsProject.builder()
                .integrationId(integrationId)
                .name(project.getProject().getName())
                .arn(project.getProject().getArn())
                .projectCreatedAt(project.getProject().getCreated())
                .projectModifiedAt(project.getProject().getLastModified())
                .sourceType(project.getProject().getSource().getType())
                .sourceLocation(project.getProject().getSource().getLocation())
                .sourceVersion(project.getProject().getSourceVersion())
                .region(project.getRegion())
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
