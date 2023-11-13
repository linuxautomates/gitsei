package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class AWSDevToolsTestcasesFilter {

    List<String> statuses;
    List<String> regions;
    List<String> reportArns;
    List<String> projectNames;
    List<String> initiators;
    List<String> sourceTypes;
    List<String> buildBatchArns;

    DISTINCT across;

    CALCULATION calculation;

    public enum DISTINCT {
        status,
        region,
        report_arn,
        project_name,
        initiator,
        source_type,
        build_batch_arn,
        none;

        public static DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        duration,
        testcase_count;

        public static CALCULATION fromString(String calculation) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, calculation);
        }
    }
}
