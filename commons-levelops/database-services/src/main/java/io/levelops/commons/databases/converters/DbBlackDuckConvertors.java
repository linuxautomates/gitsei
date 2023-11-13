package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckIssue;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProject;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProjectAttributes;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProjectVersion;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckVersion;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckVersionAttributes;
import io.levelops.commons.databases.models.filters.BlackDuckIssueFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.integrations.blackduck.models.BlackDuckIssue;
import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckRiskCounts;
import io.levelops.integrations.blackduck.models.BlackDuckVersion;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class DbBlackDuckConvertors {

    public static DbBlackDuckProject fromProject(BlackDuckProject project, String integrationId) {
        Date date = new Date();
        return DbBlackDuckProject.builder()
                .integrationId(integrationId)
                .name(project.getName())
                .description(project.getDescription())
                .attributes(buildAttributes(project))
                .projCreatedAt(project.getProjCreatedAt())
                .projUpdatedAt(project.getProjUpdatedAt())
                .createdAt(date)
                .updatedAt(date)
                .build();
    }

    public static DbBlackDuckProjectAttributes buildAttributes(BlackDuckProject project) {
        return DbBlackDuckProjectAttributes.builder()
                .cloneCategories(project.getCloneCategories())
                .projectLevelAdjustments(project.getProjectLevelAdjustments())
                .projectTier(project.getProjectTier())
                .deepLicenseDataEnabled(project.getDeepLicenseDataEnabled())
                .licenseConflictsEnabled(project.getLicenseConflictsEnabled())
                .snippetAdjustmentApplied(project.getSnippetAdjustmentApplied())
                .build();
    }


    public static DbBlackDuckVersion fromVersion(BlackDuckVersion version, String projectId) {
        Date date = new Date();
        return DbBlackDuckVersion.builder()
                .projectId(projectId)
                .versionName(version.getName())
                .versionDescription(version.getDescription())
                .versionNickName(version.getVersionNickName())
                .releaseDate(version.getReleaseDate())
                .versionCreatedAt(version.getVersionCreatedAt())
                .licenseRiskProfile(version.getLicenseRiskProfile())
                .securityRiskProfile(version.getSecurityRiskProfile())
                .operationalRiskProfile(version.getOperationalRiskProfile())
                .source(version.getSource())
                .createdAt(date)
                .updatedAt(date)
                .versionAttributes(buildVersionAttributes(version))
                .build();
    }

    public static DbBlackDuckVersionAttributes buildVersionAttributes(BlackDuckVersion version) {
        return DbBlackDuckVersionAttributes.builder()
                .versionNickName(version.getVersionNickName())
                .distribution(version.getDistribution())
                .phase(version.getPhase())
                .settingsUpdatedAt(version.getSettingsUpdatedAt())
                .lastBomUpdate(version.getLastBomUpdate())
                .policyStatus(version.getPolicyStatus())
                .build();
    }


    public static DbBlackDuckIssue fromIssue(BlackDuckIssue issue, String insertedVersionId) {
        return DbBlackDuckIssue.builder()
                .versionId(insertedVersionId)
                .componentName(issue.getComponentName())
                .componentVersionName(issue.getComponentVersionName())
                .description(issue.getBlackDuckVulnerability().getDescription())
                .vulnerabilityName(issue.getBlackDuckVulnerability().getVulnerabilityName())
                .vulnerabilityPublishedAt(issue.getBlackDuckVulnerability().getVulnerabilityPublishedDate())
                .baseScore(issue.getBlackDuckVulnerability().getBaseScore())
                .overallScore(issue.getBlackDuckVulnerability().getOverallScore())
                .exploitabilitySubScore(issue.getBlackDuckVulnerability().getExploitabilitySubScore())
                .impactSubScore(issue.getBlackDuckVulnerability().getImpactSubScore())
                .source(issue.getBlackDuckVulnerability().getSource())
                .severity(issue.getBlackDuckVulnerability().getSeverity())
                .remediationStatus(issue.getBlackDuckVulnerability().getRemediationStatus())
                .cwdId(issue.getBlackDuckVulnerability().getCweId())
                .remediationCreatedAt(issue.getBlackDuckVulnerability().getRemediationCreatedAt())
                .remediationUpdatedAt(issue.getBlackDuckVulnerability().getRemediationUpdatedAt())
                .relatedVulnerability(issue.getBlackDuckVulnerability().getRelatedVulnerability())
                .bdsaTags(issue.getBlackDuckVulnerability().getBdsaTags())
                .build();
    }

    public static RowMapper<DbBlackDuckIssue> issueRowMapper() {
        return (rs, rowNumber) -> DbBlackDuckIssue.builder()
                .id(rs.getString("id"))
                .versionId(rs.getString("version_id"))
                .description(rs.getString("description"))
                .componentName(rs.getString("component_name"))
                .componentVersionName(rs.getString("component_version_name"))
                .vulnerabilityName(rs.getString("vulnerability_name"))
                .vulnerabilityPublishedAt(rs.getTimestamp("vulnerability_published_at"))
                .vulnerabilityUpdatedAt(rs.getTimestamp("vulnerability_updated_at"))
                .baseScore(rs.getFloat("base_score"))
                .overallScore(rs.getFloat("overall_score"))
                .exploitabilitySubScore(rs.getFloat("exploitability_subscore"))
                .impactSubScore(rs.getFloat("impact_subscore"))
                .source(rs.getString("source"))
                .severity(rs.getString("severity"))
                .remediationStatus(rs.getString("remediation_status"))
                .cwdId(rs.getString("cwe_id"))
                .relatedVulnerability(rs.getString("related_vulnerability"))
                .bdsaTags((rs.getArray("bdsa_tags") != null &&
                        rs.getArray("bdsa_tags").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("bdsa_tags").getArray()) : List.of())
                .remediationCreatedAt(rs.getTimestamp("remediation_created_at"))
                .remediationUpdatedAt(rs.getTimestamp("remediation_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .build();
    }

    public static RowMapper<DbBlackDuckProjectVersion> projectRowMapper() {
        return (rs, rowNumber) -> DbBlackDuckProjectVersion.builder()

                .name(rs.getString("proj_name"))
                .attributes(ParsingUtils.parseObject(DefaultObjectMapper.get(),
                        "proj_attributes",
                        DbBlackDuckProjectAttributes.class,
                        rs.getString("proj_attributes")))
                .integrationId(rs.getString("integration_id"))
                .projectDescription(rs.getString("description"))
                .projCreatedAt(rs.getTimestamp("project_created_at"))
                .projUpdatedAt(rs.getTimestamp("project_updated_at"))
                .versionName(rs.getString("name"))
                .releaseDate(rs.getTimestamp("release_date"))
                .source(rs.getString("source"))
                .versionAttributes(ParsingUtils.parseObject(DefaultObjectMapper.get(),
                        "ver_attributes",
                        DbBlackDuckVersionAttributes.class,
                        rs.getString("ver_attributes")))
                .securityRiskProfile(ParsingUtils.parseObject(DefaultObjectMapper.get(),
                        "security_risks ",
                        BlackDuckRiskCounts.class,
                        rs.getString("security_risks")))
                .licenseRiskProfile(ParsingUtils.parseObject(DefaultObjectMapper.get(),
                        "license_risks ",
                        BlackDuckRiskCounts.class,
                        rs.getString("license_risks")))
                .operationalRiskProfile(ParsingUtils.parseObject(DefaultObjectMapper.get(),
                        "operational_risks ",
                        BlackDuckRiskCounts.class,
                        rs.getString("operational_risks")))
                .versionCreatedAt(rs.getTimestamp("version_created_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String groupByKey, BlackDuckIssueFilter.CALCULATION calculation) {
        return (rs, rowNum) -> {
            if (calculation == BlackDuckIssueFilter.CALCULATION.overall_score) {
                return DbAggregationResult.builder()
                        .key(rs.getString(groupByKey))
                        .total(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .mean(rs.getDouble("mean"))
                        .build();

            } else {
                return DbAggregationResult.builder()
                        .key(rs.getString(groupByKey))
                        .total(rs.getLong("ct"))
                        .build();
            }
        };
    }
}
