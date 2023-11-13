package io.levelops.commons.databases.models.filters;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@Value
@Builder(toBuilder = true)
public class BlackDuckProjectFilter {
    DISTINCT across;
    CALCULATION calculation;
    List<String> projects;
    List<String> versions;
    List<String> phases;
    List<String> sources;
    List<String> policyStatuses;
    List<String> securityRiskProfiles;
    List<String> licenseRiskProfiles;
    List<String> operationalRiskProfiles;
    List<String> integrationIds;
    ImmutablePair<Long, Long> projectCreatedRange;
    ImmutablePair<Long, Long> projectUpdatedRange;
    ImmutablePair<Long, Long> versionReleasedOnRange;
    ImmutablePair<Long, Long> versionCreatedRange;
    ImmutablePair<Long, Long> versionSettingUpdatedRange;
    ImmutablePair<Long, Long> lastBomUpdateDateRange;
    Map<String, Map<String, String>> partialMatch;
    Map<String, SortingOrder> sort;

    List<String> excludeProjects;
    List<String> excludeVersions;
    List<String> excludePhases;
    List<String> excludePolicyStatuses;
    List<String> excludeSecurityRiskProfiles;
    List<String> excludeLicenseRiskProfiles;
    List<String> excludeOperationalRiskProfiles;


    public enum DISTINCT {
        project,
        version,
        phase,
        distribution,
        policyStatus,
        securityRiskProfile,
        licenseRiskProfile,
        operationalRiskProfile,
        //these are time based
        project_created,
        project_updated,
        version_created,
        lastBomUpdateDate;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(BlackDuckProjectFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {

        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(BlackDuckProjectFilter.CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(versions)) {
            ArrayList<String> tempList = new ArrayList<>(versions);
            Collections.sort(tempList);
            dataToHash.append(",versions=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(phases)) {
            ArrayList<String> tempList = new ArrayList<>(phases);
            Collections.sort(tempList);
            dataToHash.append(",phases=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(policyStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(policyStatuses);
            Collections.sort(tempList);
            dataToHash.append(",policyStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(securityRiskProfiles)) {
            ArrayList<String> tempList = new ArrayList<>(securityRiskProfiles);
            Collections.sort(tempList);
            dataToHash.append(",securityRiskProfiles=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(licenseRiskProfiles)) {
            ArrayList<String> tempList = new ArrayList<>(licenseRiskProfiles);
            Collections.sort(tempList);
            dataToHash.append(",licenseRiskProfiles=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(operationalRiskProfiles)) {
            ArrayList<String> tempList = new ArrayList<>(operationalRiskProfiles);
            Collections.sort(tempList);
            dataToHash.append(",operationalRiskProfiles=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (projectCreatedRange != null) {
            dataToHash.append(",projectCreatedRange=");
            if (projectCreatedRange.getLeft() != null)
                dataToHash.append(projectCreatedRange.getLeft()).append("-");
            if (projectCreatedRange.getRight() != null)
                dataToHash.append(projectCreatedRange.getRight());
        }
        if (versionCreatedRange != null) {
            dataToHash.append(",versionCreatedRange=");
            if (versionCreatedRange.getLeft() != null)
                dataToHash.append(versionCreatedRange.getLeft()).append("-");
            if (versionCreatedRange.getRight() != null)
                dataToHash.append(versionCreatedRange.getRight());
        }
        if (lastBomUpdateDateRange != null) {
            dataToHash.append(",lastBomUpdateDateRange=");
            if (lastBomUpdateDateRange.getLeft() != null)
                dataToHash.append(lastBomUpdateDateRange.getLeft()).append("-");
            if (lastBomUpdateDateRange.getRight() != null)
                dataToHash.append(lastBomUpdateDateRange.getRight());
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
