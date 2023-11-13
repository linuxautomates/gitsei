package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.CICDScmJobRunDTO;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class DbCiCdScmConverters {
    public static RowMapper<DbAggregationResult> mapAggregationsResults(CiCdScmFilter.DISTINCT key, CiCdScmFilter.CALCULATION calculation) {
        return (rs, rowNumber) -> {
            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            readAggsAcrossOrStack(rs, key, keyStringBuffer, additionalKeyStringBuffer);

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            boolean fetchExtraColumns = true;
            try {
                rs.findColumn("change_ct");
                rs.findColumn("deploy_job_runs_count");
            } catch (SQLException e) {
                fetchExtraColumns = false;
            }
            switch (calculation) {
                case change_volume:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .linesAddedCount(rs.getLong("la"))
                            .linesRemovedCount(rs.getLong("ld"))
                            .filesChangedCount(rs.getLong("fc"))
                            .linesChangedCount(fetchExtraColumns ? rs.getLong("change_ct") : null)
                            .deployJobRunsCount((fetchExtraColumns) ? rs.getLong("deploy_job_runs_count") : null)
                            .build();
                case lead_time:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .min(rs.getLong("mn"))
                            .max(rs.getLong("mx"))
                            .median(rs.getLong("md"))
                            .sum(rs.getLong("sm"))
                            .count(rs.getLong("ct"))
                            .build();
                case count:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .count(rs.getLong("ct"))
                            .build();
                default:
                    return DbAggregationResult.builder().build();
            }
        };
    }

    public static RowMapper<DbAggregationResultStacksWrapper> mapAggregationsResultsWithStacksMapper(CiCdScmFilter filter) {
        return (rs, rowNumber) -> {
            StringBuffer acrossKeyStringBuffer = new StringBuffer();
            StringBuffer acrossAdditionalKeyStringBuffer = new StringBuffer();
            readAggsAcrossOrStack(rs, filter.getAcross(), acrossKeyStringBuffer, acrossAdditionalKeyStringBuffer);

            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            if (CollectionUtils.isNotEmpty(filter.getStacks())) {
                CiCdScmFilter.DISTINCT stack = filter.getStacks().get(0);
                readAggsAcrossOrStack(rs, stack, keyStringBuffer, additionalKeyStringBuffer);
            }

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String acrossKeyString = StringUtils.isEmpty(acrossKeyStringBuffer.toString()) ? null : acrossKeyStringBuffer.toString();
            String acrossAdditionalKeyString = StringUtils.isEmpty(acrossAdditionalKeyStringBuffer.toString()) ? null : acrossAdditionalKeyStringBuffer.toString();

            switch (CiCdScmFilter.getSanitizedCalculation(filter)) {
                case change_volume:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .linesAddedCount(rs.getLong("la"))
                                            .linesRemovedCount(rs.getLong("ld"))
                                            .filesChangedCount(rs.getLong("fc"))
                                            .build()
                            ).build();

                case lead_time:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .min(rs.getLong("mn"))
                                            .max(rs.getLong("mx"))
                                            .median(rs.getLong("md"))
                                            .sum(rs.getLong("sm"))
                                            .count(rs.getLong("ct"))
                                            .build()
                            ).build();
                case count:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .count(rs.getLong("ct"))
                                            .build()
                            ).build();
                default:
                    return DbAggregationResultStacksWrapper.builder().build();
            }
        };

    }

    private static void readAggsAcrossOrStack(ResultSet rs, CiCdScmFilter.DISTINCT acrossOrStack, StringBuffer keyStringBuffer, StringBuffer additionalKeyStringBuffer) throws SQLException {
        switch (acrossOrStack) {
            case job_status:
                keyStringBuffer.append(rs.getString("status"));
                break;
            case qualified_job_name:
                keyStringBuffer.append(rs.getString("job_name"));
                additionalKeyStringBuffer.append(rs.getString("instance_name"));
                break;
            case project_name:
            case author:
            case job_name:
            case cicd_user_id:
            case job_normalized_full_name:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                break;
            case instance_name:
                keyStringBuffer.append(rs.getString("instance_name"));
                break;
            case trend:
                keyStringBuffer.append(rs.getLong("trend"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case job_end:
                keyStringBuffer.append(rs.getLong("job_end"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case repo:
                keyStringBuffer.append(rs.getString("repo_ids"));
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }

    }

    public static RowMapper<CICDScmJobRunDTO> mapAggListResults() {
        return (rs, rowNumber) -> CICDScmJobRunDTO.builder()
                .id((UUID) rs.getObject("run_id"))
                .initialCommitToDeployTime(rs.getLong("initial_commit_to_deploy_time"))
                .linesModified(rs.getInt("lines_modified"))
                .filesModified(rs.getInt("files_modified"))
                .build();
    }

    public static RowMapper<Map.Entry<UUID, CICDScmJobRunDTO>> mapListResults() {
        return (rs, rowNumber) -> {
            var cicdScmJobRunDTO = CICDScmJobRunDTO.builder()
                    .id((UUID) rs.getObject("run_id"))
                    .jobName(rs.getString("job_name"))
                    .cicdInstanceName(rs.getString("instance_name"))
                    .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid") : null)
                    .cicdJobId((UUID) rs.getObject("cicd_job_id"))
                    .jobRunNumber(rs.getLong("job_run_number"))
                    .status(rs.getString("status"))
                    .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                    .duration(rs.getInt("duration"))
                    .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                    .cicdUserId(rs.getString("cicd_user_id"))
                    .jobNormalizedFullName(rs.getString("job_normalized_full_name"))
                    .projectName(rs.getString("project_name"))
                    .cicdIntegrationId(rs.getString("cicd_integration_id"))
                    .scmUrl(rs.getString("scm_url"))
                    .build();
            return Map.of(cicdScmJobRunDTO.getId(), cicdScmJobRunDTO).entrySet().stream().findFirst().get();
        };
    }

    public static RowMapper<Map.Entry<UUID, CICDScmJobRunDTO.ScmCommit>> mapScmCommits() {
        return (rs, rowNumber) -> {
            //c.id as commit_id, repo_id, integration_id, author, message, files_ct, additions, deletions, committed_at, m.cicd_job_run_id
            UUID jobRunId = (UUID) rs.getObject("run_id");
            UUID commitId = (UUID) rs.getObject("commit_id");
            Integer integrationId = rs.getInt("integration_id");
            Instant commitedAt = DateUtils.toInstant(rs.getTimestamp("committed_at"));
            CICDScmJobRunDTO.ScmCommit scmCommit = CICDScmJobRunDTO.ScmCommit.builder()
                    .commitId(commitId.toString())
                    .repoIds((rs.getArray("repo_id") != null &&
                            rs.getArray("repo_id").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of())
                    .commitUrl(rs.getString("commit_url"))
                    .integrationId(integrationId.toString())
                    .author(rs.getString("author"))
                    .message(rs.getString("message"))
                    .filesChangedCount(rs.getInt("files_ct"))
                    .linesAddedCount(rs.getInt("additions"))
                    .linesRemovedCount(rs.getInt("deletions"))
                    .committedAt(commitedAt.getEpochSecond())
                    .build();

            return Map.of(jobRunId, scmCommit).entrySet().stream().collect(Collectors.toList()).get(0);
        };
    }

    public static RowMapper<CICDScmJobRunDTO.ScmCommit> mapScmCommits1() {
        return (rs, rowNumber) -> {
            //c.id as commit_id, repo_id, integration_id, author, message, files_ct, additions, deletions, committed_at, m.cicd_job_run_id
            UUID commitId = (UUID) rs.getObject("commit_id");
            int integrationId = rs.getInt("integration_id");
            Instant commitedAt = DateUtils.toInstant(rs.getTimestamp("committed_at"));
            return CICDScmJobRunDTO.ScmCommit.builder()
                    .commitId(commitId.toString())
                    .repoIds((rs.getArray("repo_id") != null &&
                            rs.getArray("repo_id").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of())
                    .commitUrl(rs.getString("commit_url"))
                    .integrationId(Integer.toString(integrationId))
                    .author(rs.getString("author"))
                    .message(rs.getString("message"))
                    .filesChangedCount(rs.getInt("files_ct"))
                    .linesAddedCount(rs.getInt("additions"))
                    .linesRemovedCount(rs.getInt("deletions"))
                    .committedAt(commitedAt.getEpochSecond())
                    .build();
        };
    }

    public static RowMapper<CICDScmJobRunDTO> codeVsDeployListRowMapper() {
        return (rs, rowNumber) ->
                CICDScmJobRunDTO.builder()
                        .id((UUID) rs.getObject("deploy_run_id"))
                        .buildId((UUID) rs.getObject("build_run_id"))
                        .jobName(rs.getString("deploy_job_name"))
                        .cicdInstanceName(rs.getString("instance_name"))
                        .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid") : null)
                        .cicdJobId((UUID) rs.getObject("cicd_job_id"))
                        .jobRunNumber(rs.getLong("job_run_number"))
                        .status(rs.getString("status"))
                        .startTime(DateUtils.toInstant(rs.getTimestamp("deploy_start_time")))
                        .duration(rs.getInt("duration"))
                        .cicdInstanceType(rs.getString("cicd_instance_type"))
                        .endTime(DateUtils.toInstant(rs.getTimestamp("deploy_end_time")))
                        .cicdUserId(rs.getString("cicd_user_id"))
                        .jobNormalizedFullName(rs.getString("job_normalized_full_name"))
                        .projectName(rs.getString("project_name"))
                        .cicdIntegrationId(rs.getString("deploy_integ_id"))
                        .build();
    }
}
