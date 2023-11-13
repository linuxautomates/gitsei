package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class TestRailsTestCase {

    public TestRailsTestCase() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    Map<String, Object> dynamicCustomFields = new HashMap<>();
    @JsonAnySetter
    public void setDynamicCustomFields(String key, Object value) {
        if (key == null || value == null || !key.startsWith("custom_")) {
            return;
        }
        dynamicCustomFields.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getDynamicCustomFields() {
        return dynamicCustomFields;
    }

    @JsonProperty("custom_case_fields")
    Map<String, Object> customCaseFields;

    @JsonProperty("id")
    Integer id;
    @JsonProperty("title")
    String title;
    @JsonProperty("project_id")
    Integer projectId;
    @JsonProperty("section_id")
    Integer sectionId;
    @JsonProperty("template_id")
    Integer templateId;
    @JsonProperty("type_id")
    Integer typeId;
    @JsonProperty("priority_id")
    Integer priorityId;
    @JsonProperty("milestone_id")
    Integer milestoneId;
    @JsonProperty("refs")
    String refs;
    @JsonProperty("priority")
    String priority;
    @JsonProperty("type")
    String type;
    @JsonProperty("created_by_user")
    String createdByUser;
    @JsonProperty("updated_by_user")
    String updatedByUser;
    @JsonProperty("created_by")
    Integer createdBy;
    @JsonProperty("created_on")
    Long createdOn;
    @JsonProperty("updated_by")
    Integer updatedBy;
    @JsonProperty("updated_on")
    Long updatedOn;
    @JsonProperty("estimate")
    String estimate;
    @JsonProperty("estimate_forecast")
    String estimateForecast;
    @JsonProperty("suite_id")
    Integer suiteId;
    @JsonProperty("display_order")
    Integer displayOrder;
    @JsonProperty("is_deleted")
    Integer isDeleted;
}