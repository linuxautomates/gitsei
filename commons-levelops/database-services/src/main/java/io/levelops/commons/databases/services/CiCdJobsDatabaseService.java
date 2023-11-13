package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Log4j2
@Service
public class CiCdJobsDatabaseService extends DatabaseService<CICDJob> {
    private static final String CHECK_EXISTING_CICD_INSTANCE_ID_NOT_NULL_SQL_FORMAT = "SELECT id from %s.cicd_jobs where job_full_name = ? AND cicd_instance_id = ?";
    private static final String CHECK_EXISTING_CICD_INSTANCE_ID_NULL_SQL_FORMAT = "SELECT id from %s.cicd_jobs where job_full_name = ? and cicd_instance_id IS NULL";

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.cicd_jobs (cicd_instance_id, job_name, job_full_name, branch_name, module_name, scm_url, scm_user_id, job_normalized_full_name, project_name) VALUES(?,?,?,?,?,?,?,?,?)\n" +
            "ON CONFLICT(COALESCE(cicd_instance_id, '00000000-0000-0000-0000-000000000000'),job_full_name) DO UPDATE SET (scm_url,scm_user_id,job_normalized_full_name,branch_name, updated_at,project_name) = (EXCLUDED.scm_url,EXCLUDED.scm_user_id,EXCLUDED.job_normalized_full_name,EXCLUDED.branch_name,now(),EXCLUDED.project_name)\n" +
            "RETURNING id";
    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.cicd_jobs SET scm_url = ?, scm_user_id = ?, job_normalized_full_name = ?, updated_at = now() WHERE id = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_jobs WHERE id = ?";

    private static final String GET_JOBS_WITHOUT_INSTANCES_COUNT_SQL_FORMAT = "SELECT COUNT(ID) FROM %s.cicd_jobs WHERE cicd_instance_id IS NULL";
    private static final String DELETE_JOBS_BY_JOB_NAMES_WITHOUT_INSTANCES_SQL_FORMAT = "DELETE FROM %s.cicd_jobs WHERE cicd_instance_id IS NULL AND job_name = ANY(?::varchar[])";


    private final NamedParameterJdbcTemplate template;
    // region CSTOR
    @Autowired
    public CiCdJobsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ProductService.class, CiCdInstancesDatabaseService.class);
    }
    // endregion

    // region insert
    /*
    When Jenkins Plugin detects job run:
    we do get the full name, branch name, module name.
    we do get the latest scm url and scm user id.

    So in this flow every time we need to upsert.
    i.e. we will create job if it does not exist. if job exists we will update only the scm url and scm user id.
     */
    @Override
    public String insert(String company, CICDJob t) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID cicdJobId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, t.getCicdInstanceId());
            pstmt.setString(2, t.getJobName());
            pstmt.setString(3, t.getJobFullName());
            pstmt.setString(4, t.getBranchName());
            pstmt.setString(5, t.getModuleName());
            pstmt.setString(6, t.getScmUrl());
            pstmt.setObject(7, t.getScmUserId());
            pstmt.setObject(8, t.getJobNormalizedFullName());
            pstmt.setObject(9, t.getProjectName());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd job!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cicd job!");
                }
                cicdJobId = (UUID) rs.getObject(1);
                return cicdJobId.toString();
            }
        }
    }
    private Optional<UUID> checkExisting(PreparedStatement checkExistingPstmt, CICDJob t) throws SQLException {
        checkExistingPstmt.setObject(1, t.getJobFullName());
        if (t.getCicdInstanceId() != null) {
            checkExistingPstmt.setObject(2, t.getCicdInstanceId());
        }
        try (ResultSet rs = checkExistingPstmt.executeQuery()) {
            if (rs.next()) {
                UUID existingId = (UUID) rs.getObject(1);
                return Optional.ofNullable(existingId);
            }
            return Optional.empty();
        }
    }
    /*
    When Jenkins Plugin detects config changes:
    we get full name, branch name, module name,
    we may not have the scm url & scm user id.

    So in this flow, we we only insert job. i.e. if job does not exist we will create it.
    If it exists we will not update the scm url and scm user id.
     */
    public String insertOnly(String company, CICDJob t) throws SQLException {
        String checkExistingSql = (t.getCicdInstanceId() != null) ?
                String.format(CHECK_EXISTING_CICD_INSTANCE_ID_NOT_NULL_SQL_FORMAT, company)
                : String.format(CHECK_EXISTING_CICD_INSTANCE_ID_NULL_SQL_FORMAT, company);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkExistingPstmt = conn.prepareStatement(checkExistingSql, Statement.RETURN_GENERATED_KEYS)) {
            Optional<UUID> optionalExistingId = checkExisting(checkExistingPstmt, t);
            if(optionalExistingId.isPresent()){
                return optionalExistingId.get().toString();
            }
        }
        return insert(company,t);
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, CICDJob t) throws SQLException {
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, t.getScmUrl());
            pstmt.setObject(2, t.getScmUserId());
            pstmt.setObject(3, t.getJobNormalizedFullName());
            pstmt.setObject(4, t.getId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        }
    }
    // endregion
    public int updateJobScmUrl(String company, CICDJob job) throws SQLException {
        String updateSql = "UPDATE " + company + ".cicd_jobs SET scm_url = ? WHERE cicd_instance_id = ? AND project_name = ? AND job_name = ?";
        boolean success;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, job.getScmUrl());
            pstmt.setObject(2, job.getCicdInstanceId());
            pstmt.setString(3, job.getProjectName());
            pstmt.setObject(4, job.getJobName());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return affectedRows;
        }
    }
    // region get
    @Override
    public Optional<CICDJob> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)),null,null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region get and list commons
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }
    private DbListResponse<CICDJob> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> jobNames, List<String> jobNormalizedFullNames, List<String> jobFullNames, List<UUID> cicdInstanceIds) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_jobs";


        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(jobNames)) {
            criteria = formatCriterea(criteria, values, "job_name = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", jobNames));
        }
        if (CollectionUtils.isNotEmpty(jobNormalizedFullNames)) {
            criteria = formatCriterea(criteria, values, "job_normalized_full_name = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", jobNormalizedFullNames));
        }
        if (CollectionUtils.isNotEmpty(jobFullNames)) {
            criteria = formatCriterea(criteria, values, "job_full_name = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", jobFullNames));
        }
        if (CollectionUtils.isNotEmpty(cicdInstanceIds)) {
            criteria = formatCriterea(criteria, values, "cicd_instance_id = ANY(?::UUID[]) ");
            values.add(new ArrayWrapper<>("uuid", cicdInstanceIds));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<CICDJob> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSql)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.debug("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                //j.id, j.job_name, j.scm_url, j.scm_user_id, j.created_at, j.updated_at
                UUID id = (UUID) rs.getObject("id");
                UUID cicdInstanceId = (UUID) rs.getObject("cicd_instance_id");
                String jobName = rs.getString("job_name");
                String jobFullName = rs.getString("job_full_name");
                String jobNormalizedFullName = rs.getString("job_normalized_full_name");
                String branchName = rs.getString("branch_name");
                String moduleName = rs.getString("module_name");
                String scmUrl = rs.getString("scm_url");
                String scmUserId = rs.getString("scm_user_id");
                String projectName = rs.getString("project_name");

                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));

                CICDJob aggregationRecord = CICDJob.builder()
                        .id(id)
                        .cicdInstanceId(cicdInstanceId)
                        .jobName(jobName).jobFullName(jobFullName).jobNormalizedFullName(jobNormalizedFullName)
                        .projectName(projectName)
                        .branchName(branchName).moduleName(moduleName)
                        .scmUrl(scmUrl).scmUserId(scmUserId)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .build();

                retval.add(aggregationRecord);
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }
    // endregion

    // region list
    @Override
    public DbListResponse<CICDJob> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null,null,null, null, null);
    }
    public DbListResponse<CICDJob> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> jobNames, List<String> jobNormalizedFullNames, List<String> jobFullNames, List<UUID> cicdInstanceIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, jobNames, jobNormalizedFullNames, jobFullNames, cicdInstanceIds);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    // endregion

    // region get count of jobs without cicd instance
    public Integer getCountOfJobsWithoutCiCdInstance(String company) throws SQLException {
        String getJobsWithoutInstancesCountSql = String.format(GET_JOBS_WITHOUT_INSTANCES_COUNT_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getJobsWithoutInstancesCountSql, Statement.RETURN_GENERATED_KEYS)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                if (! rs.next()) {
                    throw new SQLException("Failed to create get count of jobs without cicdinstance!");
                }
                Integer jobsCount = rs.getInt(1);
                return jobsCount;
            }
        }
    }
    // endregion

    // region delete jobs by job name without cicd instance
    public Boolean deleteByJobsWithoutCiCdInstanceByJobName(String company, List<String> jobNames) throws SQLException {
        if(CollectionUtils.isEmpty(jobNames)) {
            return true;
        }
        String deleteSql = String.format(DELETE_JOBS_BY_JOB_NAMES_WITHOUT_INSTANCES_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setObject(1, DBUtils.processArrayValues(conn, new ArrayWrapper<>("varchar", jobNames)));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    // endregion



    private static final Pattern branchMatcher = Pattern.compile("^.*\\/branches\\/(.*)$");
    private static final Pattern lastSegmentMatcher = Pattern.compile("^.*\\/(.*)$");

    public static String getFullUrl(String company, String instanceUrl, String jobFullName, Integer jobNumber, String stageId, String lineRef) {
        try {
            if (Strings.isBlank(company) || Strings.isBlank(instanceUrl) || Strings.isBlank(jobFullName) || jobNumber == null || jobNumber < 0) {
                return "";
            }
            //blue/organizations/jenkins/master-sanity-pipeline/detail/master-sanity-pipeline/20/pipeline/34
            //blue/organizations/jenkins/Pipelines%2FTest Pipeline 1/detail/Test Pipeline 1/326/pipeline/20
            //blue/rest/organizations/jenkins/pipelines/Pipelines/pipelines/Test Pipeline 1/runs/282/nodes/20/
            //blue/organizations/jenkins/multi1/detail/master/1/pipeline/
            //                      /job/multi1/branches/master/1/console
            //blue/organizations/jenkins/multi1/detail/master/1/pipeline/27
            HttpUrl.Builder urlBuilder = HttpUrl.parse(instanceUrl).newBuilder();
            if (Strings.isBlank(stageId)) {
                urlBuilder.addPathSegment("job");
                var fullName = jobFullName
                    .replaceAll("/jobs/", "/job/")
                    .replaceAll("/branches/", "/job/");
                for(String segment:fullName.split("/")) {
                    urlBuilder.addEncodedPathSegment(segment);
                }
                urlBuilder
                    .addPathSegment(jobNumber.toString())
                    .addPathSegment("console");
                return urlBuilder.build().toString();
            }

            var fullName = jobFullName.replaceAll("/jobs/", "/");
            var lastSegmentName = fullName;
            var matcher = branchMatcher.matcher(fullName);
            if (matcher.find()) {
                lastSegmentName = matcher.group(1);
                fullName = fullName.substring(0, matcher.start(1) - "/branches/".length());
            }
            else {
                matcher = lastSegmentMatcher.matcher(fullName);
                if (matcher.find()) {
                    lastSegmentName = matcher.group(1);
                }
            }
            urlBuilder
                .addPathSegment("blue")
                .addPathSegment("organizations")
                .addPathSegment("jenkins")
                .addEncodedPathSegment(fullName)
                .addPathSegment("detail")
                .addEncodedPathSegment(lastSegmentName)
                .addPathSegment(jobNumber.toString())
                .addPathSegment("pipeline")
                .addPathSegment(stageId);
            return urlBuilder.build().toString() + StringUtils.defaultString(lineRef);
        }
        catch(Exception e){
            return "";
        }
    }

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.cicd_jobs(\n" +
                "    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    project_name            VARCHAR,\n" +
                "    job_name                VARCHAR NOT NULL,\n" +
                "    job_full_name           VARCHAR NOT NULL,\n" +
                "    job_normalized_full_name VARCHAR,\n" +
                "    branch_name             VARCHAR,\n" +
                "    module_name             VARCHAR,\n" +
                "    cicd_instance_id        UUID REFERENCES {0}.cicd_instances(id) ON DELETE CASCADE,\n" +
                "    scm_url                 VARCHAR,\n" +
                "    scm_user_id             VARCHAR,\n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                ")",

                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_cicd_jobs_cicd_instance_id_job_full_name_idx on {0}.cicd_jobs (COALESCE(cicd_instance_id, ''00000000-0000-0000-0000-000000000000''),job_full_name)"
                );

        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
