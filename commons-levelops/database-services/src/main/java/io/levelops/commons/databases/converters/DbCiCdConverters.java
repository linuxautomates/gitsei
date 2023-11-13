package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.Json;
import io.levelops.commons.databases.models.database.CICDJobConfigChangeDTO;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Log4j2
public class DbCiCdConverters {
    public static RowMapper<DbAggregationResult> distinctJobRunsAggsMapper(CiCdJobRunsFilter.DISTINCT key, CiCdJobRunsFilter.CALCULATION calculation) {
        return (rs, rowNumber) -> {
            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            StringBuffer cicdJobIdStringBuffer = new StringBuffer();
            readJobRunsAggsAcrossOrStack(rs, key, keyStringBuffer, additionalKeyStringBuffer, cicdJobIdStringBuffer);

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String cicdJobIdString = StringUtils.isEmpty(cicdJobIdStringBuffer.toString()) ? null : cicdJobIdStringBuffer.toString();

            switch (calculation){
                case count:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .ciCdJobId(cicdJobIdString)
                            .count(rs.getLong("ct"))
                            .build();
                case duration:
                    return DbAggregationResult.builder()
                            .key(keyString)
                            .additionalKey(additionalKeyString)
                            .ciCdJobId(cicdJobIdString)
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

    private static void readJobRunsAggsAcrossOrStack(ResultSet rs, CiCdJobRunsFilter.DISTINCT acrossOrStack,
                                                     StringBuffer keyStringBuffer,
                                                     StringBuffer additionalKeyStringBuffer,
                                                     StringBuffer cicdJobIdStringBuffer ) throws SQLException {
        String instanceName = null;
        switch (acrossOrStack) {
            case job_status:
                keyStringBuffer.append(rs.getString("status"));
                break;
            case service:
                if (rs.getString("service") != null) {
                    keyStringBuffer.append(rs.getString("service"));
                }
                break;
            case environment:
                if (rs.getString("environment") != null) {
                    keyStringBuffer.append(rs.getString("environment"));
                }
                break;
            case repository:
                if (rs.getString("repository") != null) {
                    keyStringBuffer.append(rs.getString("repository"));
                }
                break;
            case infrastructure:
                if (rs.getString("infrastructure") != null) {
                    keyStringBuffer.append(rs.getString("infrastructure"));
                }
                break;
            case deployment_type:
                if (rs.getString("deployment_type") != null) {
                    keyStringBuffer.append(rs.getString("deployment_type"));
                }
                break;
            case rollback:
                if (rs.getString("rollback") != null) {
                    keyStringBuffer.append(rs.getString("rollback"));
                }
                break;
            case branch:
                if (rs.getString("branch") != null) {
                    keyStringBuffer.append(rs.getString("branch"));
                }
                break;
            case tag:
                if (rs.getString("tag") != null) {
                    keyStringBuffer.append(rs.getString("tag"));
                }
                break;
            case cicd_job_id:
                keyStringBuffer.append(rs.getString("job_name"));
                instanceName = rs.getString("instance_name");
                if(instanceName != null) {
                    additionalKeyStringBuffer.append(instanceName);
                }
                cicdJobIdStringBuffer.append(rs.getString("cicd_job_id"));
                break;
            case qualified_job_name:
                keyStringBuffer.append(rs.getString("job_name"));
                instanceName = rs.getString("instance_name");
                if(instanceName != null) {
                    additionalKeyStringBuffer.append(instanceName);
                }
                break;
            case project_name:
            case instance_name:
            case job_name:
            case cicd_user_id:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                break;
            case job_normalized_full_name:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                cicdJobIdStringBuffer.append(rs.getString("cicd_job_id"));
                break;
            case trend:
                keyStringBuffer.append(rs.getLong("trend"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case job_end:
                keyStringBuffer.append(rs.getLong("job_end"));
                additionalKeyStringBuffer.append(rs.getString("interval"));
                break;
            case triage_rule:
                keyStringBuffer.append(rs.getString("triage_rule"));
                additionalKeyStringBuffer.append(rs.getString("triage_rule_id"));
                break;
            case stage_name:
                String stageName = rs.getString("stage_name");
                if (stageName != null)
                    keyStringBuffer.append(stageName);
                break;
            case step_name:
                String stepName = rs.getString("step_name");
                if (stepName != null)
                    keyStringBuffer.append(stepName);
                break;
            case stage_status:
                String stageStatus = rs.getString("stage_status");
                if (stageStatus != null)
                    keyStringBuffer.append(stageStatus);
                break;
            case step_status:
                String stepStatus = rs.getString("step_status");
                if (stepStatus != null)
                    keyStringBuffer.append(stepStatus);
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
    }
    public static RowMapper<DbAggregationResultStacksWrapper> distinctJobRunsAggsWithStacksMapper(CiCdJobRunsFilter filter) {
        return (rs, rowNumber) -> {
            StringBuffer acrossKeyStringBuffer = new StringBuffer();
            StringBuffer acrossAdditionalKeyStringBuffer = new StringBuffer();
            StringBuffer acrossCicdJobIdStringBuffer = new StringBuffer();
            readJobRunsAggsAcrossOrStack(rs, filter.getAcross(), acrossKeyStringBuffer, acrossAdditionalKeyStringBuffer, acrossCicdJobIdStringBuffer);

            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            StringBuffer cicdJobIdStringBuffer = new StringBuffer();
            if(CollectionUtils.isNotEmpty(filter.getStacks())) {
                CiCdJobRunsFilter.DISTINCT stack = filter.getStacks().get(0);
                readJobRunsAggsAcrossOrStack(rs, stack, keyStringBuffer, additionalKeyStringBuffer, cicdJobIdStringBuffer);
            }

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String cicdJobIdString = StringUtils.isEmpty(cicdJobIdStringBuffer.toString()) ? null : cicdJobIdStringBuffer.toString();
            String acrossKeyString = StringUtils.isEmpty(acrossKeyStringBuffer.toString()) ? null : acrossKeyStringBuffer.toString();
            String acrossAdditionalKeyString = StringUtils.isEmpty(acrossAdditionalKeyStringBuffer.toString()) ? null : acrossAdditionalKeyStringBuffer.toString();
            String acrossCicdJobIdString = StringUtils.isEmpty(acrossCicdJobIdStringBuffer.toString()) ? null : acrossCicdJobIdStringBuffer.toString();

            switch (CiCdJobRunsFilter.getSanitizedCalculation(filter)){
                case count:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .acrossCiCdJobId(acrossCicdJobIdString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .ciCdJobId(cicdJobIdString)
                                            .count(rs.getLong("ct"))
                                            .build()
                            ).build();

                case duration:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .acrossCiCdJobId(acrossCicdJobIdString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .ciCdJobId(cicdJobIdString)
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

    public static RowMapper<DbAggregationResult> distinctJobConfigChangesAggsMapper(CiCdJobConfigChangesFilter.DISTINCT key, CiCdJobConfigChangesFilter.CALCULATION calculation) {
        return (rs, rowNumber) -> {
            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            readJobConfigChangesAggsAcrossOrStack(rs, key, keyStringBuffer, additionalKeyStringBuffer);

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();

            switch (calculation){
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

    private static void readJobConfigChangesAggsAcrossOrStack(ResultSet rs, CiCdJobConfigChangesFilter.DISTINCT acrossOrStack, StringBuffer keyStringBuffer, StringBuffer additionalKeyStringBuffer ) throws SQLException {
        switch (acrossOrStack) {
            case qualified_job_name:
                keyStringBuffer.append(rs.getString("job_name"));
                String instanceName = rs.getString("instance_name");
                if(instanceName != null) {
                    additionalKeyStringBuffer.append(instanceName);
                }
                break;
            case job_name:
            case cicd_user_id:
                keyStringBuffer.append(rs.getString(acrossOrStack.toString()));
                break;
            case instance_name:
                keyStringBuffer.append(rs.getString("instance_name"));
                break;
            case trend:
                keyStringBuffer.append(String.valueOf(rs.getTimestamp("trend").toInstant().getEpochSecond()));
                break;
            case project_name:
                keyStringBuffer.append(rs.getString("project_name"));
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
    }
    public static RowMapper<DbAggregationResultStacksWrapper> distinctJobConfigChangesAggsWithStacksMapper(CiCdJobConfigChangesFilter filter) {
        return (rs, rowNumber) -> {
            StringBuffer acrossKeyStringBuffer = new StringBuffer();
            StringBuffer acrossAdditionalKeyStringBuffer = new StringBuffer();
            readJobConfigChangesAggsAcrossOrStack(rs, filter.getAcross(), acrossKeyStringBuffer, acrossAdditionalKeyStringBuffer);

            StringBuffer keyStringBuffer = new StringBuffer();
            StringBuffer additionalKeyStringBuffer = new StringBuffer();
            if(CollectionUtils.isNotEmpty(filter.getStacks())) {
                CiCdJobConfigChangesFilter.DISTINCT stack = filter.getStacks().get(0);
                readJobConfigChangesAggsAcrossOrStack(rs, stack, keyStringBuffer, additionalKeyStringBuffer);
            }

            String keyString = StringUtils.isEmpty(keyStringBuffer.toString()) ? null : keyStringBuffer.toString();
            String additionalKeyString = StringUtils.isEmpty(additionalKeyStringBuffer.toString()) ? null : additionalKeyStringBuffer.toString();
            String acrossKeyString = StringUtils.isEmpty(acrossKeyStringBuffer.toString()) ? null : acrossKeyStringBuffer.toString();
            String acrossAdditionalKeyString = StringUtils.isEmpty(acrossAdditionalKeyStringBuffer.toString()) ? null : acrossAdditionalKeyStringBuffer.toString();

            switch (CiCdJobConfigChangesFilter.getSanitizedCalculation(filter)){
                case count:
                    return DbAggregationResultStacksWrapper.builder()
                            .acrossKey(acrossKeyString)
                            .acrossAdditionalKey(acrossAdditionalKeyString)
                            .dbAggregationResult(
                                    DbAggregationResult.builder()
                                            .key(keyString)
                                            .additionalKey(additionalKeyString)
                                            .count(rs.getLong("ct")).build()
                            ).build();
                default:
                    return DbAggregationResultStacksWrapper.builder().build();
            }
        };
    }

    public static RowMapper<CICDJobConfigChangeDTO> jobConfigChangesListMapper() {
        return (rs, rowNumber) -> {
            return CICDJobConfigChangeDTO.builder()
                    .id((UUID)rs.getObject("id"))
                    .cicdJobId((UUID)rs.getObject("cicd_job_id"))
                    .jobName(rs.getString("job_name"))
                    .cicdInstanceName(rs.getString("instance_name"))
                    .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid"): null)
                    .changeTime(DateUtils.toInstant(rs.getTimestamp("change_time")))
                    .changeType(rs.getString("change_type"))
                    .cicdUserId(rs.getString("cicd_user_id"))
                    .build();
        };
    }

    public static RowMapper<CICDJobRunDTO> jobRunsListMapper() {
        return (rs, rowNumber) -> {
            return CICDJobRunDTO.builder()
                    .id((UUID)rs.getObject("id"))
                    .jobName(rs.getString("job_name"))
                    .cicdInstanceName(rs.getString("instance_name"))
                    .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid"): null)
                    .cicdJobId((UUID)rs.getObject("cicd_job_id"))
                    .jobRunNumber(rs.getLong("job_run_number"))
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

    public static RowMapper<CICDJobRunDTO> doraJobRunsListMapper() {
        return (rs, rowNumber) -> {
            return CICDJobRunDTO.builder()
                    .id((UUID)rs.getObject("id"))
                    .jobName(rs.getString("job_name"))
                    .cicdInstanceName(rs.getString("instance_name"))
                    .cicdInstanceGuid((rs.getObject("instance_guid") != null) ? (UUID) rs.getObject("instance_guid"): null)
                    .cicdJobId((UUID)rs.getObject("cicd_job_id"))
                    .jobRunNumber(rs.getLong("job_run_number"))
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
                    .infraIds(columnPresent(rs, "infra_ids") ?
                            prepareStringList(rs,"infra_ids")  : null)
                    .serviceTypes(columnPresent(rs, "service_types") ?
                            prepareStringList(rs, "service_types"): null)
                    .serviceIds(columnPresent(rs, "service_ids") ?
                            prepareStringList(rs, "service_ids") : null)
                    .environmentIds(columnPresent(rs, "env_ids") ?
                            prepareStringList(rs, "env_ids") : null)
                    .tags(columnPresent(rs, "tags") ?
                            prepareStringList(rs, "tags") : null)
                    .cicdBranch(columnPresent(rs, "branch") ? rs.getString("branch") : null)
                    .rollBack(columnPresent(rs, "rollback") ? rs.getBoolean("rollback") : null)
                    .repoUrl(columnPresent(rs, "repo_url") ? rs.getString("repo_url") : null)
                    .build();
        };
    }

    private static List<String> prepareStringList(ResultSet rs, String column) {
        List<String> returnValue = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString  = rs.getObject(column,String.class);
            returnValue = mapper.readValue(jsonString, new TypeReference<List<String>>(){});
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    public static RowMapper<CiCdJobRunTest> jobRunTestsListMapper() {
        return ((rs, rowNum) -> CiCdJobRunTest.builder()
                .id(rs.getString("id"))
                .cicdJobRunId(rs.getString("cicd_job_run_id"))
                .testSuite(rs.getString("test_suite"))
                .testName(rs.getString("test_name"))
                .status(EnumUtils.getEnumIgnoreCase(CiCdJobRunTest.Status.class, rs.getString("status")))
                .errorDetails(rs.getString("error_details"))
                .errorStackTrace(rs.getString("error_stacktrace"))
                .duration(rs.getFloat("duration"))
                .jobStatus(rs.getString("job_status"))
                .jobName(rs.getString("job_name"))
                .jobRunNumber(rs.getString("job_run_number"))
                .cicdUserId(rs.getString("cicd_user_id"))
                .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                .endTime(DateUtils.toInstant(rs.getTimestamp("end_time")))
                .jobNormalizedFullName(rs.getString("job_normalized_full_name"))
                .projectName(rs.getString("project_name"))
                .build());
    }

    public static RowMapper<DbAggregationResult> cicdTestsAggRowMapper(String key,
                                                                       CiCdJobRunTestsFilter.CALCULATION calculation) {
        return ((rs, rowNum) -> {
            if (CiCdJobRunTestsFilter.CALCULATION.count == calculation) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(key.equals("job_end") ? rs.getString("interval") : null)
                        .totalTests(rs.getLong("ct"))
                        .build();
            } else {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(key.equals("job_end") ? rs.getString("interval") : null)
                        .totalTests(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("md"))
                        .build();
            }
        });
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }
}
