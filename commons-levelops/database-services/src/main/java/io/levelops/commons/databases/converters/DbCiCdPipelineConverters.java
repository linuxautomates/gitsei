package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.dates.DateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

public class DbCiCdPipelineConverters {

    public static RowMapper<DbAggregationResult> distinctJobRunsAggsMapper(CiCdPipelineJobRunsFilter.DISTINCT key, CiCdPipelineJobRunsFilter.CALCULATION calculation) {
        return (rs, rowNumber) -> {
            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            StringBuffer ciCdJobIdStringBuffer = new StringBuffer();
            readJobRunsAggsAcrossOrStack(rs, key, keyStringBuffer, additionalKeyStringBuffer, ciCdJobIdStringBuffer);

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String ciCdJobIdString = StringUtils.isEmpty(ciCdJobIdStringBuffer.toString()) ? null : ciCdJobIdStringBuffer.toString();

            switch (calculation){
                case count:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .ciCdJobId(ciCdJobIdString)
                            .count(rs.getLong("ct"))
                            .build();
                case duration:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .ciCdJobId(ciCdJobIdString)
                            .min(rs.getLong("mn"))
                            .max(rs.getLong("mx"))
                            .median(rs.getLong("md"))
                            .sum(rs.getLong("sm"))
                            .count(rs.getLong("ct"))
                            .build();
                default:
                    return DbAggregationResult.builder().build();
            }
        };
    }
    private static void readJobRunsAggsAcrossOrStack(ResultSet rs, CiCdPipelineJobRunsFilter.DISTINCT acrossOrStack, StringBuffer keyStringBuffer, StringBuffer additionalKeyStringBuffer, StringBuffer ciCdJobIdStringBuffer ) throws SQLException {
        String instanceName = null;
        switch (acrossOrStack) {
            case cicd_job_id:
                keyStringBuffer.append(rs.getString("job_name"));
                instanceName = rs.getString("instance_name");
                if(instanceName != null) {
                    additionalKeyStringBuffer.append(instanceName);
                }
                ciCdJobIdStringBuffer.append(rs.getString("cicd_job_id"));
                break;
            case job_status:
                keyStringBuffer.append(rs.getString("status"));
                break;
            case qualified_job_name:
                keyStringBuffer.append(rs.getString("job_name"));
                instanceName = rs.getString("instance_name");
                if(instanceName != null) {
                    additionalKeyStringBuffer.append(instanceName);
                }
                break;
            case instance_name:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                additionalKeyStringBuffer.append(rs.getString("instance_id"));
                break;
            case job_name:
            case cicd_user_id:
            case job_normalized_full_name:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                break;
            case trend:
                keyStringBuffer.append(rs.getLong("trend"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case job_end:
                keyStringBuffer.append(rs.getLong("job_end"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case project_name:
                keyStringBuffer.append(rs.getString("project_name"));
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
    }
    public static RowMapper<DbAggregationResultStacksWrapper> distinctJobRunsAggsWithStacksMapper(CiCdPipelineJobRunsFilter filter) {
        return (rs, rowNumber) -> {
            StringBuffer acrossKeyStringBuffer = new StringBuffer();
            StringBuffer acrossAdditionalKeyStringBuffer = new StringBuffer();
            StringBuffer acrossCiCdJobIdStringBuffer = new StringBuffer();
            readJobRunsAggsAcrossOrStack(rs, filter.getAcross(), acrossKeyStringBuffer, acrossAdditionalKeyStringBuffer, acrossCiCdJobIdStringBuffer);

            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            StringBuffer ciCdJobIdStringBuffer = new StringBuffer();
            if(CollectionUtils.isNotEmpty(filter.getStacks())) {
                CiCdPipelineJobRunsFilter.DISTINCT stack = filter.getStacks().get(0);
                readJobRunsAggsAcrossOrStack(rs, stack, keyStringBuffer, additionalKeyStringBuffer, ciCdJobIdStringBuffer);
            }

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String ciCdJobIdString = StringUtils.isEmpty(ciCdJobIdStringBuffer.toString()) ? null : ciCdJobIdStringBuffer.toString();

            String acrossKeyString = StringUtils.isEmpty(acrossKeyStringBuffer.toString()) ? null : acrossKeyStringBuffer.toString();
            String acrossAdditionalKeyString = StringUtils.isEmpty(acrossAdditionalKeyStringBuffer.toString()) ? null : acrossAdditionalKeyStringBuffer.toString();
            String acrossCiCdJobIdString = StringUtils.isEmpty(acrossCiCdJobIdStringBuffer.toString()) ? null : acrossCiCdJobIdStringBuffer.toString();

            switch (CiCdPipelineJobRunsFilter.getSanitizedCalculation(filter)){
                case count:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .acrossCiCdJobId(acrossCiCdJobIdString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .ciCdJobId(ciCdJobIdString)
                                            .count(rs.getLong("ct"))
                                            .build()
                            ).build();

                case duration:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .acrossCiCdJobId(acrossCiCdJobIdString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .ciCdJobId(ciCdJobIdString)
                                            .min(rs.getLong("mn"))
                                            .max(rs.getLong("mx"))
                                            .median(rs.getLong("md"))
                                            .sum(rs.getLong("sm"))
                                            .count(rs.getLong("ct"))
                                            .build()
                            ).build();
                default:
                    return DbAggregationResultStacksWrapper.builder().build();
            }
        };
    }

    public static RowMapper<CICDJobRunDTO> jobRunsListMapper() {
        return (rs, rowNumber) -> {
            String url = rs.getString("url");
            String jobFullName = rs.getString("job_full_name");
            long jobRunNumber = rs.getLong("job_run_number");

            String buildUrl = null;
            if(StringUtils.isNotEmpty(url)) {
                buildUrl = url + "job" + "/" + jobFullName + "/" + jobRunNumber + "/";
            }

            return CICDJobRunDTO.builder()
                    .id((UUID)rs.getObject("id"))
                    .jobName(rs.getString("job_name"))
                    .cicdInstanceName(rs.getString("instance_name"))
                    .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid"): null)
                    .cicdJobId((UUID)rs.getObject("cicd_job_id"))
                    .jobRunNumber(jobRunNumber)
                    .cicdBuildUrl(buildUrl)
                    .status(rs.getString("status"))
                    .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                    .duration(rs.getInt("duration"))
                    .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                    .cicdUserId(rs.getString("cicd_user_id"))
                    .logGcspath(rs.getString("log_gcspath"))
                    .jobNormalizedFullName(rs.getString("job_normalized_full_name"))
                    .projectName(rs.getString("project_name"))
                    .integrationId(rs.getString("integration_id"))
                    .scmCommitIds(Arrays.asList((String[]) rs.getArray("scm_commit_ids").getArray()))
                    .scmUrl(rs.getString("scm_url"))
                    .build();
        };
    }
}
