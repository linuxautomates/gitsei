package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

@Log4j2
@Service
public class CiCdJobRunStageStepsDatabaseService extends DatabaseService<JobRunStageStep> {
    public static final String TABLE_NAME = "cicd_job_run_stage_steps";
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.cicd_job_run_stage_steps (cicd_job_run_stage_id, step_id, display_name, display_description, start_time, result, state, duration, gcspath) VALUES(?,?,?,?,?,?,?,?,?)\n" +
            "ON CONFLICT(cicd_job_run_stage_id, step_id) DO UPDATE SET (display_name,display_description,start_time, result,state,duration,gcspath) = " +
            "(EXCLUDED.display_name,EXCLUDED.display_description,EXCLUDED.start_time,EXCLUDED.result,EXCLUDED.state,EXCLUDED.duration,EXCLUDED.gcspath)\n" +
            " RETURNING id";
    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.cicd_job_run_stage_steps SET display_name = ?, display_description = ?, start_time = ?, result = ?, state = ?, duration = ?, gcspath = ?, updated_at = now() WHERE id = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_job_run_stage_steps WHERE id = ?";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public CiCdJobRunStageStepsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunStageDatabaseService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, JobRunStageStep t) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID cicdJobRunStageStepId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, t.getCicdJobRunStageId());
            pstmt.setString(2, t.getStepId());
            pstmt.setString(3, t.getDisplayName());
            pstmt.setString(4, t.getDisplayDescription());
            pstmt.setTimestamp(5, Timestamp.from(t.getStartTime()));
            pstmt.setString(6, t.getResult());
            pstmt.setString(7, t.getState());
            pstmt.setInt(8, t.getDuration());
            pstmt.setString(9, t.getGcsPath());

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
                cicdJobRunStageStepId = (UUID) rs.getObject(1);
                return cicdJobRunStageStepId.toString();
            }
        }
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, JobRunStageStep t) throws SQLException {
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, t.getDisplayName());
            pstmt.setString(2, t.getDisplayDescription());
            pstmt.setTimestamp(3, Timestamp.from(t.getStartTime()));
            pstmt.setString(4, t.getResult());
            pstmt.setString(5, t.getState());
            pstmt.setInt(6, t.getDuration());
            pstmt.setString(7, t.getGcsPath());
            pstmt.setObject(8, t.getId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        }
    }
    // endregion

    // region get
    @Override
    public Optional<JobRunStageStep> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)),null,null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region Get List Common
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }
    public DbListResponse<JobRunStageStep> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobRunStageIds, List<String> stepIds) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_job_run_stage_steps";

        String orderBy = " ORDER BY start_time DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(cicdJobRunStageIds)) {
            criteria = formatCriterea(criteria, values, "cicd_job_run_stage_id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", cicdJobRunStageIds));
        }
        if (CollectionUtils.isNotEmpty(stepIds)) {
            criteria = formatCriterea(criteria, values, "step_id = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", stepIds));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<JobRunStageStep> retval = new ArrayList<>();
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
                UUID id = (UUID) rs.getObject("id");
                UUID cicdJobRunStageId = (UUID) rs.getObject("cicd_job_run_stage_id");
                String stepId = rs.getString("step_id");
                String displayName = rs.getString("display_name");
                String displayDescription = rs.getString("display_description");
                Instant startTime = DateUtils.toInstant(rs.getTimestamp("start_time"));
                String result = rs.getString("result");
                String state = rs.getString("state");
                Integer duration = rs.getInt("duration");
                String gcsPath = rs.getString("gcspath");
                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));

                JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                        .id(id)
                        .cicdJobRunStageId(cicdJobRunStageId)
                        .stepId(stepId)
                        .displayName(displayName).displayDescription(displayDescription)
                        .startTime(startTime).result(result).state(state).duration(duration)
                        .gcsPath(gcsPath)
                        .createdAt(createdAt).updatedAt(updatedAt)
                        .build();

                retval.add(jobRunStageStep);
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
    public DbListResponse<JobRunStageStep> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }
    public DbListResponse<JobRunStageStep> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobRunStageIds, List<String> stepIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, cicdJobRunStageIds, stepIds);
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

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".cicd_job_run_stage_steps( \n" +
                "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                "        cicd_job_run_stage_id UUID NOT NULL REFERENCES " + company + ".cicd_job_run_stages, \n" +
                "        step_id VARCHAR NOT NULL, \n" +
                "        display_name VARCHAR, \n" +
                "        display_description VARCHAR, \n" +
                "        start_time TIMESTAMP WITH TIME ZONE, \n" +
                "        result VARCHAR, \n" +
                "        state VARCHAR, \n" +
                "        duration INTEGER,\n" +
                "        gcspath VARCHAR, \n" +
                "        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                "        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                "    )",
                
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_cicd_job_run_stage_steps_cicd_job_run_stage_id_step_id_idx on " + company + ".cicd_job_run_stage_steps (cicd_job_run_stage_id,step_id)",

                "CREATE INDEX IF NOT EXISTS cicd_job_run_params_cicd_job_run_id_idx ON " + company + ".cicd_job_run_params(cicd_job_run_id)");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
