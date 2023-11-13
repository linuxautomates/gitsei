package io.levelops.commons.databases.models.filters;

import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

@Log4j2
@Value
@Builder(toBuilder = true)
public class BlackDuckIssueFilter {

    DISTINCT across;
    CALCULATION calculation;
    List<String> vulnerabilities;
    List<String> componentNames;
    List<String> componentVersionNames;
    List<String> severities;
    List<String> remediationStatuses;
    List<String> cweIds;
    List<String> sources;
    List<String> versionIds;
    List<String> bdsaTags;
    List<String> integrationIds;
    List<String> projects;
    List<String> versions;
    List<String> phases;
    Map<String, String> baseScoreRange;
    Map<String, String> overallScoreRange;
    Map<String, String> exploitabilitySubScoreRange;
    Map<String, String> impactSubScoreRange;
    ImmutablePair<Long, Long> remediationCreatedAtRange;
    ImmutablePair<Long, Long> remediationUpdatedAtRange;
    ImmutablePair<Long, Long> vulnerabilityUpdatedAtRange;
    ImmutablePair<Long, Long> vulnerabilityPublishedAtRange;

    public enum DISTINCT {
        vulnerability,
        component,
        severity,
        remediation_status,
        cweId,
        source,
        base_Score,
        overall_score,
        exploitability_subscore,
        impact_subscore,
        project,
        version,
        phase,
        none,

        //these are time based
        remediation_created_at,
        remediation_updated_at,
        vulnerability_updated_at,
        vulnerability_created_at;

        public static BlackDuckIssueFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(BlackDuckIssueFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        total_issues,
        overall_score,
        count; // just a count of rows

        public static BlackDuckIssueFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(BlackDuckIssueFilter.CALCULATION.class, st);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (CollectionUtils.isNotEmpty(vulnerabilities)) {
            ArrayList<String> tempList = new ArrayList<>(vulnerabilities);
            Collections.sort(tempList);
            dataToHash.append(",vulnerabilities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(componentNames)) {
            ArrayList<String> tempList = new ArrayList<>(componentNames);
            Collections.sort(tempList);
            dataToHash.append(",componentNames=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(componentVersionNames)) {
            ArrayList<String> tempList = new ArrayList<>(componentVersionNames);
            Collections.sort(tempList);
            dataToHash.append(",componentVersionNames=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(severities)) {
            ArrayList<String> tempList = new ArrayList<>(severities);
            Collections.sort(tempList);
            dataToHash.append(",severities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(remediationStatuses)) {
            ArrayList<String> tempList = new ArrayList<>(remediationStatuses);
            Collections.sort(tempList);
            dataToHash.append(",remediationStatuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(cweIds)) {
            ArrayList<String> tempList = new ArrayList<>(cweIds);
            Collections.sort(tempList);
            dataToHash.append(",cweIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(sources)) {
            ArrayList<String> tempList = new ArrayList<>(sources);
            Collections.sort(tempList);
            dataToHash.append(",sources=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(bdsaTags)) {
            ArrayList<String> tempList = new ArrayList<>(bdsaTags);
            Collections.sort(tempList);
            dataToHash.append(",bdsaTags=").append(String.join(",", tempList));
        }
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
        if (remediationCreatedAtRange != null) {
            dataToHash.append(",remediationCreatedAtRange=");
            if (remediationCreatedAtRange.getLeft() != null)
                dataToHash.append(remediationCreatedAtRange.getLeft()).append("-");
            if (remediationCreatedAtRange.getRight() != null)
                dataToHash.append(remediationCreatedAtRange.getRight());
        }
        if (remediationUpdatedAtRange != null) {
            dataToHash.append(",remediationUpdatedAtRange=");
            if (remediationUpdatedAtRange.getLeft() != null)
                dataToHash.append(remediationUpdatedAtRange.getLeft()).append("-");
            if (remediationUpdatedAtRange.getRight() != null)
                dataToHash.append(remediationUpdatedAtRange.getRight());
        }
        if(MapUtils.isNotEmpty(baseScoreRange)) {
            TreeSet<String> fields = new TreeSet<>(baseScoreRange.keySet());
            dataToHash.append(",baseScoreRange=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(baseScoreRange.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if(MapUtils.isNotEmpty(impactSubScoreRange)) {
            TreeSet<String> fields = new TreeSet<>(impactSubScoreRange.keySet());
            dataToHash.append(",impactSubscoreRange=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(impactSubScoreRange.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if(MapUtils.isNotEmpty(exploitabilitySubScoreRange)) {
            TreeSet<String> fields = new TreeSet<>(exploitabilitySubScoreRange.keySet());
            dataToHash.append(",exploitabilitySubscoreRange=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(exploitabilitySubScoreRange.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        if(MapUtils.isNotEmpty(overallScoreRange)) {
            TreeSet<String> fields = new TreeSet<>(overallScoreRange.keySet());
            dataToHash.append(",overallScoreRange=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(overallScoreRange.get(field).toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
