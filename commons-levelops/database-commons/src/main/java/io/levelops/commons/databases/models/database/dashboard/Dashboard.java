package io.levelops.commons.databases.models.database.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dashboard {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private String type;

    @JsonProperty(value = "owner_id")
    private String ownerId;

    @JsonProperty(value = "demo")
    @Default
    private Boolean demo = false;

    @JsonProperty(value = "query")
    private Object query;

    @JsonProperty(value = "metadata")
    private Object metadata;

    @JsonProperty(value = "widgets")
    private List<Widget> widgets;

    @JsonProperty(value = "default")
    private Boolean isDefault; //this is not stored in the same table and comes from the config table.

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @JsonProperty(value = "owner_first_name")
    private String firstName;

    @JsonProperty(value = "owner_last_name")
    private String lastName;

    @JsonProperty("public")
    @Default
    private boolean isPublic = false;

    @JsonProperty("dashboard_order")
    Integer dashboardOrder;

    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "updated_at")
    private Long updatedAt;
}