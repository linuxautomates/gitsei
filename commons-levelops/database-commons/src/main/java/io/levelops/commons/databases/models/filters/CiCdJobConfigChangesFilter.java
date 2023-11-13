package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.SortingOrder;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class CiCdJobConfigChangesFilter {
    DISTINCT across;
    List<DISTINCT> stacks;
    CALCULATION calculation;
    List<String> cicdUserIds;
    List<String> jobNames;
    List<String> instanceNames;
    Long changeStartTime;
    Long changeEndTime;
    @JsonProperty("qualified_job_names")
    List<CiCdJobQualifiedName> qualifiedJobNames;
    List<String> integrationIds;
    List<CICD_TYPE> types;
    List<String> projects;
    Set<UUID> orgProductsIds;
    Map<String, SortingOrder> sortBy;

    List<String> excludeJobNames;
    List<String> excludeInstanceNames;
    List<String> excludeProjects;
    List<String> excludeCiCdUserIds;
    List<CICD_TYPE> excludeTypes;
    List<CiCdJobQualifiedName> excludeQualifiedJobNames;
    @JsonProperty("across_count")
    Integer acrossCount;

    public enum DISTINCT {
        job_name,
        qualified_job_name,
        instance_name,
        cicd_user_id,
        project_name,
        //these are time based
        trend;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public static CALCULATION getSanitizedCalculation(CiCdJobConfigChangesFilter filter) {
        return (filter.getCalculation() != null) ? filter.getCalculation() : CiCdJobConfigChangesFilter.CALCULATION.count;
    }
}
