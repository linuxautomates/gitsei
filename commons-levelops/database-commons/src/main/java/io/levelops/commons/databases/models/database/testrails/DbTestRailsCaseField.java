package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.testrails.models.CaseField;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsCaseField.DbTestRailsCaseFieldBuilder.class)
public class DbTestRailsCaseField {
    @JsonProperty("uuid")
    String id;
    @JsonProperty("case_field_id")
    Integer caseFieldId;
    @JsonProperty("is_active")
    Boolean isActive;
    @JsonProperty("label")
    String label;
    @JsonProperty("name")
    String name;
    @JsonProperty("system_name")
    String systemName;
    @JsonProperty("type")
    String type;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("is_global")
    Boolean isGlobal;
    @JsonProperty("project_ids")
    List<Integer> projectIds;

    public static DbTestRailsCaseField fromCaseField(CaseField caseField, String integrationId) {
        return DbTestRailsCaseField.builder()
                .caseFieldId(Integer.parseInt(caseField.getId().toString()))
                .systemName(caseField.getSystemName())
                .name(caseField.getName())
                .label(caseField.getLabel())
                .type(caseField.getType().toString())
                .isGlobal(ListUtils.emptyIfNull(caseField.getConfigs()).size() > 0 ? ListUtils.emptyIfNull(caseField.getConfigs()).get(0).getContext().getIsGlobal() : null)
                .projectIds(ListUtils.emptyIfNull(caseField.getConfigs()).size() > 0 ? ListUtils.emptyIfNull(ListUtils.emptyIfNull(caseField.getConfigs()).stream().map(context -> ListUtils.emptyIfNull(context.getContext().getProjectIds())).flatMap(List::stream).collect(Collectors.toList())) : List.of())
                .isActive(caseField.getIsActive())
                .integrationId(integrationId)
                .build();
    }
}
