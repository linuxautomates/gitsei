package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComponentInstance {
    private UUID id;
    private Component component;
    private String instanceName;
    private String status;
    private String description;
    private Long createdAt;
    private Long updatedAt;
}