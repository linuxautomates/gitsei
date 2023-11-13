package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class CxSastScanFilter {

    DISTINCT across;
    CALCULATION calculation;
    AGG_INTERVAL aggInterval;
    Integer acrossLimit;
    List<String> integrationIds;
    List<String> scanIds;
    List<String> scanTypes;
    List<String> scanPaths;
    List<String> languages;
    List<String> owners;
    List<String> initiatorNames;
    List<String> projectNames;
    List<String> statuses;
    Boolean isPublic;

    public enum DISTINCT {
        scan_type,
        language,
        owner,
        scan_path,
        status,
        initiator_name,
        none;

        public static CxSastScanFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CxSastScanFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count;

        public static CxSastScanFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CxSastScanFilter.CALCULATION.class, st);
        }
    }

}
