package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class AWSDevToolsProjectsFilter {

    List<String> sourceTypes;
    List<String> regions;
    List<String> integrationIds;

    DISTINCT across;

    CALCULATION calculation;

    public enum DISTINCT {
        source_type,
        region,
        none;

        public static DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        project_count;

        public static CALCULATION fromString(String calculation) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, calculation);
        }
    }
}
