package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class TestRailsCaseFieldFilter {
    List<String> integrationIds;
    Integer caseFieldId;
    List<String> systemNames;
    Long ingestedAt;
    Boolean isActive;
    Boolean needAssignedFieldsOnly; // for such fields which are not assigned to any project
}
