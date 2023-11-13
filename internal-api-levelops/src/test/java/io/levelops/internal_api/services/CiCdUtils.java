package io.levelops.internal_api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CiCdUtils {

    public static final String arrayUniq = "CREATE OR REPLACE FUNCTION anyarray_uniq(with_array anyarray)\n" +
            "\tRETURNS anyarray AS\n" +
            "$BODY$\n" +
            "\tDECLARE\n" +
            "\t\t-- The variable used to track iteration over \"with_array\".\n" +
            "\t\tloop_offset integer;\n" +
            "\n" +
            "\t\t-- The array to be returned by this function.\n" +
            "\t\treturn_array with_array%TYPE := '{}';\n" +
            "\tBEGIN\n" +
            "\t\tIF with_array IS NULL THEN\n" +
            "\t\t\treturn NULL;\n" +
            "\t\tEND IF;\n" +
            "\t\t\n" +
            "\t\tIF with_array = '{}' THEN\n" +
            "\t\t    return return_array;\n" +
            "\t\tEND IF;\n" +
            "\n" +
            "\t\t-- Iterate over each element in \"concat_array\".\n" +
            "\t\tFOR loop_offset IN ARRAY_LOWER(with_array, 1)..ARRAY_UPPER(with_array, 1) LOOP\n" +
            "\t\t\tIF with_array[loop_offset] IS NULL THEN\n" +
            "\t\t\t\tIF NOT EXISTS(\n" +
            "\t\t\t\t\tSELECT 1 \n" +
            "\t\t\t\t\tFROM UNNEST(return_array) AS s(a)\n" +
            "\t\t\t\t\tWHERE a IS NULL\n" +
            "\t\t\t\t) THEN\n" +
            "\t\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\t\tEND IF;\n" +
            "\t\t\t-- When an array contains a NULL value, ANY() returns NULL instead of FALSE...\n" +
            "\t\t\tELSEIF NOT(with_array[loop_offset] = ANY(return_array)) OR NOT(NULL IS DISTINCT FROM (with_array[loop_offset] = ANY(return_array))) THEN\n" +
            "\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\tEND IF;\n" +
            "\t\tEND LOOP;\n" +
            "\n" +
            "\tRETURN return_array;\n" +
            " END;\n" +
            "$BODY$ LANGUAGE plpgsql;";

    @Deprecated
    public static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, int i) throws SQLException {
        return createCiCdInstance(ciCdInstancesDatabaseService, company, "1", i);
    }

    private static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, String integrationId, int i) throws SQLException {
        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("instance-name-" + i)
                .url("https://jenkins.dev.levelops.io/")
                .integrationId(integrationId)
                .type(CICD_TYPE.jenkins.toString());
        CICDInstance cicdInstance = bldr.build();
        String id = ciCdInstancesDatabaseService.insert(company, cicdInstance);
        return cicdInstance.toBuilder().id(UUID.fromString(id)).build();
    }

    public static long calculateOffset() {
        long diff = Instant.now().getEpochSecond() - 1593062362;
        long days = diff / (TimeUnit.DAYS.toSeconds(1));
        long offset = days * (TimeUnit.DAYS.toSeconds(1));
        return offset;
    }

    public static List<CiCdUtils.JobDetails> fixJobRunTimestamps(List<CiCdUtils.JobDetails> allJobDetails, Long offset) {
        if (CollectionUtils.isEmpty(allJobDetails)) {
            return allJobDetails;
        }
        return allJobDetails.stream()
                .map(jobDetails -> {
                    if (CollectionUtils.isEmpty(jobDetails.getRuns())) {
                        return jobDetails;
                    }
                    jobDetails.setRuns(
                            jobDetails.getRuns().stream()
                                    .map(run -> {
                                        run.setStartTime(run.getStartTime() + offset);
                                        run.setEndTime(run.getEndTime() != null ? run.getEndTime() + offset : null);
                                        return run;
                                    })
                                    .collect(Collectors.toList())
                    );
                    return jobDetails;
                })
                .collect(Collectors.toList());
    }


    public static class JobDetails {
        @JsonProperty("job_name")
        public String jobName;
        @JsonProperty("job_full_name")
        public String jobFullName;
        @JsonProperty("job_normalized_full_name")
        public String jobNormalizedFullName;
        @JsonProperty("branch_name")
        public String branchName;
        @JsonProperty("module_name")
        public String moduleName;
        @JsonProperty("scm_url")
        public String scmUrl;
        @JsonProperty("scm_user_id")
        public String scmUserId;
        @JsonProperty("runs")
        public List<JobRunDetails> runs;

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getJobFullName() {
            return jobFullName;
        }

        public void setJobFullName(String jobFullName) {
            this.jobFullName = jobFullName;
        }

        public String getJobNormalizedFullName() {
            return jobNormalizedFullName;
        }

        public void setJobNormalizedFullName(String jobNormalizedFullName) {
            this.jobNormalizedFullName = jobNormalizedFullName;
        }

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getScmUrl() {
            return scmUrl;
        }

        public void setScmUrl(String scmUrl) {
            this.scmUrl = scmUrl;
        }

        public String getScmUserId() {
            return scmUserId;
        }

        public void setScmUserId(String scmUserId) {
            this.scmUserId = scmUserId;
        }

        public List<JobRunDetails> getRuns() {
            return runs;
        }

        public void setRuns(List<JobRunDetails> runs) {
            this.runs = runs;
        }
    }

    public static class JobRunDetails {
        @JsonProperty("number")
        private Long number;
        @JsonProperty("status")
        private String status;
        @JsonProperty("start_time")
        private Long startTime;
        @JsonProperty("end_time")
        private Long endTime;
        @JsonProperty("duration")
        private Long duration;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("commit_ids")
        private List<String> commitIds;
        @JsonProperty("params")
        private List<JobRunParam> params;

        public Long getNumber() {
            return number;
        }

        public void setNumber(Long number) {
            this.number = number;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public List<String> getCommitIds() {
            return commitIds;
        }

        public void setCommitIds(List<String> commitIds) {
            this.commitIds = commitIds;
        }

        public List<JobRunParam> getParams() {
            return params;
        }

        public void setParams(List<JobRunParam> params) {
            this.params = params;
        }
    }

    public static class JobRunParam {
        @JsonProperty("type")
        private String type;
        @JsonProperty("name")
        private String name;
        @JsonProperty("value")
        private String value;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

    }

}
