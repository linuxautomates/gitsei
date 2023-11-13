package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class AWSDevToolsBuildBatchesFilter {

    List<String> projectNames;
    List<String> lastPhases;
    List<String> lastPhaseStatuses;
    List<String> statuses;
    List<String> sourceTypes;
    List<String> initiators;
    List<String> regions;
    List<String> integrationIds;

    DISTINCT across;

    CALCULATION calculation;

    public enum DISTINCT {
        project_name,
        last_phase,
        last_phase_status,
        status,
        source_type,
        initiator,
        region,
        none;

        public static DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        duration,
        build_batch_count;

        public static CALCULATION fromString(String calculation) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, calculation);
        }
    }
}
