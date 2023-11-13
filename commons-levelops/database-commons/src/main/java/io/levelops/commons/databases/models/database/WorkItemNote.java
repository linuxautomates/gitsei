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
public class WorkItemNote {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "creator") //email of the creating user
    private String creator;

    @JsonProperty(value = "body")
    private String body;

    @JsonProperty(value = "work_item_id")
    private String workItemId;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}