package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TestRailsCaseFieldDTO.TestRailsCaseFieldDTOBuilder.class)
public class TestRailsCaseFieldDTO {
    @JsonProperty("id")
    String id;
    @JsonProperty("is_active")
    Boolean isActive;
    @JsonProperty("name")
    String name;
    @JsonProperty("field_key")
    String fieldKey;
    @JsonProperty("field_type")
    String fieldType;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("is_global")
    Boolean isGlobal;
    @JsonProperty("project_ids")
    List<Integer> projectIds;
    @JsonProperty("ingested_at")
    Date ingestedAt;

    public static TestRailsCaseFieldDTO fromDbCaseField(DbTestRailsCaseField caseField) {
        return TestRailsCaseFieldDTO.builder()
                .id(caseField.getId())
                .name(caseField.getLabel())
                .fieldKey(caseField.getSystemName())
                .fieldType(caseField.getType())
                .integrationId(caseField.getIntegrationId())
                .isActive(caseField.getIsActive())
                .isGlobal(caseField.getIsGlobal())
                .projectIds(caseField.getProjectIds())
                .build();
    }
}
