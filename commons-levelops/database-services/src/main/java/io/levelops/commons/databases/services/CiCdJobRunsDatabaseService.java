package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.converters.CICDJobRunCommitsConverters;
import io.levelops.commons.databases.converters.cicd.CiCdJobRunConverters;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.databases.models.database.cicd.JobRunSegment;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CiCdJobRunsDatabaseService extends DatabaseService<CICDJobRun> {
    public static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("job_name", "job_full_name");
    public static final String TABLE_NAME = "cicd_job_runs";
    private static final String CHECK_EXISTING_SQL_FORMAT = "SELECT id from %s.cicd_job_runs where cicd_job_id = ? AND job_run_number = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_job_runs WHERE id = ?";

    private static final String CICD_JOB_RUN_PARAMS_DELETE_SQL = "DELETE FROM %s.cicd_job_run_params WHERE cicd_job_run_id = ?";
    private static final String CICD_JOB_RUN_PARAMS_INSERT_SQL = "INSERT INTO %s.cicd_job_run_params(cicd_job_run_id,type,name,value) VALUES(?,?,?,?)";

    private static final String JENKINS_RULE_HITS_TABLE = ", {0}.jenkins_rule_hits jrh";

    private static String AGGS_BY_STATUS_INNER_SQL_FORMAT =
            "    SELECT {3} a.start_day, json_object_agg(a.status, a.count) results \n"
                    + "    FROM (\n"
                    + "        SELECT {4} status, extract(epoch from date_trunc(''day'', start_time)::date) start_day, count(*) count \n"
                    + "        FROM \n"
                    + "            {0}.cicd_job_runs c {5} {1}\n" // parent filter
                    + "        {2}" // where statement
                    + "        GROUP BY {4} status, start_day \n"
                    + "        ) as a \n"
                    + "    GROUP BY {3} a.start_day \n";

    private static final String AGGS_BY_STATUS_SQL_FORMAT =
            "SELECT j.id id, j.job_name AS name, j.job_full_name as full_name, json_object_agg(start_day, results) aggs \n"
                    + "FROM {0}.cicd_jobs j, (\n"
                    + "    {1} \n"
                    + "    ) as r \n"
                    + "    {2} \n"
                    + "GROUP BY j.id \n";

    private static final String AGGS_BY_STATUS_SQL_FILTER_BY_PARENT_FORMAT = ", \n"
            + "            (SELECT r.id \n"
            + "                FROM \n"
            + "                    {0}.cicd_job_runs r, \n"
            + "                    (SELECT \n"
            + "                        j.id parent_job_id, \n"
            + "                        j.job_name parent_job_name, \n"
            + "                        r.id parent_job_run_id, \n"
            + "                        t.cicd_job_run_id run_id, \n"
            + "                        t.id trigger_id, \n"
            + "                        t.type \n"
            + "                    FROM \n"
            + "                        {0}.cicd_job_runs r, \n"
            + "                        {0}.cicd_jobs j, \n"
            + "                        {0}.cicd_job_run_triggers t \n"
            + "                    WHERE \n"
            + "                        j.id = r.cicd_job_id \n"
            + "                        AND j.job_full_name = t.trigger_id \n"
            + "                        AND r.job_run_number = t.job_run_number \n"
            + "                        AND t.type = ''UpstreamCause'' \n"
            + "                        AND j.id = ANY( {1} ::uuid[])) as a \n"
            + "                WHERE \n"
            + "                    r.id = a.run_id) as p \n"
            + "        WHERE p.id = c.id ";

    private static final Set<String> allowedAggsFilters = Set.of("job_ids", "parent_job_ids", "start_time", "end_time", "results", "triage_rule_ids", "cicd_user_ids", "job_names", "job_normalized_full_names", "project_names", "cicd_instance_ids");

    private static final String GET_JOBRUN_SQL_FORMAT =
            "SELECT r.id, j.job_name as name, j.job_full_name full_name, r.job_run_number run_number, t.trigger_id, t.job_run_number as trigger_run_number, extract(epoch from r.start_time)::varchar(10) start_time, r.log_gcspath logs, r.duration, r.status as result \n"
                    + "FROM {0}.cicd_jobs j, {0}.cicd_job_runs r \n"
                    + "  LEFT OUTER JOIN {0}.cicd_job_run_triggers AS t ON t.cicd_job_run_id = r.id AND t.type = ''UpstreamCause'' \n"
                    + "WHERE \n"
                    + "  r.id = :id::uuid \n"
                    + "  AND j.id = r.cicd_job_id \n";

    private static final String GET_JOBRUN_BY_NAME_SQL_FORMAT =
            "SELECT r.id, j.job_name as name, j.job_full_name full_name, r.job_run_number run_number, t.trigger_id, t.job_run_number as trigger_run_number, extract(epoch from r.start_time)::varchar(10) start_time, r.log_gcspath logs, r.duration, r.status as result \n"
                    + "FROM {0}.cicd_jobs j, {0}.cicd_job_runs r \n"
                    + "  LEFT OUTER JOIN {0}.cicd_job_run_triggers AS t ON t.cicd_job_run_id = r.id AND t.type = ''UpstreamCause'' \n"
                    + "WHERE \n"
                    + "  j.job_full_name = :fullName \n"
                    + "  AND r.job_run_number = :runNumber \n"
                    + "  AND j.id = r.cicd_job_id \n";

    private static final String GET_SUBJOBS_PARENTS_SQL_FORMAT =
            "WITH RECURSIVE full_tree AS ( \n"
                    + "   SELECT r.id, j.job_name as name, j.job_full_name full_name, r.job_run_number run_number, t.trigger_id, t.job_run_number as trigger_run_number, extract(epoch from r.start_time)::varchar(10) start_time, r.log_gcspath logs, r.duration, r.status as result \n"
                    + "   FROM {0}.cicd_jobs j, {0}.cicd_job_runs r, {0}.cicd_job_run_triggers t \n"
                    + "   WHERE \n"
                    + "       t.type = ''UpstreamCause'' \n"
                    + "       AND t.trigger_id = ''{1}'' \n"
                    + "       AND t.job_run_number = {2} \n"
                    + "       AND r.id = t.cicd_job_run_id\n"
                    + "       AND j.id = r.cicd_job_id \n"
                    + "   UNION ALL \n"
                    + "       SELECT r.id, j.job_name as name, j.job_full_name full_name, r.job_run_number run_number, t.trigger_id, t.job_run_number as trigger_run_number, extract(epoch from r.start_time)::varchar(10) start_time, r.log_gcspath logs, r.duration, r.status as result \n"
                    + "       FROM \n"
                    + "           {0}.cicd_jobs j, \n"
                    + "           {0}.cicd_job_runs r, \n"
                    + "           {0}.cicd_job_run_triggers t \n"
                    + "           INNER JOIN full_tree f ON t.trigger_id = f.full_name AND t.job_run_number = f.run_number \n"
                    + "       WHERE  \n"
                    + "           t.type = ''UpstreamCause'' \n"
                    + "           AND t.cicd_job_run_id = r.id \n"
                    + "           AND j.id = r.cicd_job_id \n"
                    + "            \n"
                    + ") \n";

    private static final String GET_ALL_LOGS_FORMAT =
            "SELECT \n"
                    + "    array_remove(array_agg(r.log_gcspath), NULL) as job_logs, \n"
                    + "    array_remove(array_agg(s.logs), '''') as stage_logs,\n"
                    + "    array_remove(array_agg(ss.gcspath), NULL) as step_logs \n"
                    + "FROM \n"
                    + "    {0}.{1} r \n"
                    + "    LEFT OUTER JOIN {0}.{2} s \n"
                    + "        ON s.cicd_job_run_id = r.id \n"
                    + "    LEFT OUTER JOIN {0}.{3} ss \n"
                    + "        ON ss.cicd_job_run_stage_id = s.id\n"
                    + "WHERE \n"
                    + "    r.id = ''{4}''::uuid";

    private static final String DEFAULT_SORTING = "start_time DESC \n";

    // private static final PathSegment emptyParentSegment = PathSegment.builder().build();

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    // region CSTOR
    @Autowired
    public CiCdJobRunsDatabaseService(final ObjectMapper mapper, final DataSource dataSource) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class);
    }
    // endregion

    // region Job Run Triggers

    /**
     * Persists a CICD trigger in the db and all its parent triggerss.
     *
     * @param company      tenant
     * @param cicdJobRunId the cicd of the cicd job being processed (if this trigger is part of a chain, this is the id of the last job triggered).
     * @param trigger      the trigger
     * @param conn
     * @return
     * @throws SQLException
     */
    public UUID insertJobRunTrigger(final String company, final UUID cicdJobRunId, final CICDJobTrigger trigger, final Connection conn) throws SQLException {
        if (trigger == null) {
            return null;
        }
        Set<UUID> parentIds = ensureGetDBIdsForDirectParentTriggers(company, cicdJobRunId, trigger.getDirectParents(), conn);

        var selectQuery = "SELECT t.id FROM {0}.cicd_job_run_triggers AS t ";
        var insertQuery = "INSERT INTO {0}.cicd_job_run_triggers ";
        List<Object> selectParams;
        List<Object> insertParams;
        // for cicd job triggers
        if (!Strings.isBlank(trigger.getBuildNumber())) {
            insertQuery += "(cicd_job_run_id, trigger_id, type, job_run_number, direct_parent_triggers) "
                    + "VALUES(?::uuid,?,?,?,?::uuid[]) ON CONFLICT (cicd_job_run_id,trigger_id,type) DO UPDATE SET type=EXCLUDED.type RETURNING id";
            selectQuery += "WHERE t.cicd_job_run_id = ?::uuid AND t.trigger_id = ? AND t.type = ? AND t.job_run_number = ?";
            insertParams = List.of(cicdJobRunId.toString(), trigger.getId(), trigger.getType(), Integer.valueOf(trigger.getBuildNumber()), conn.createArrayOf("uuid", parentIds != null ? parentIds.toArray() : new Object[0]));
            selectParams = List.of(cicdJobRunId.toString(), trigger.getId(), trigger.getType(), Integer.valueOf(trigger.getBuildNumber()));
        }
        // for non cicd job triggers.
        else {
            insertQuery += "(cicd_job_run_id, trigger_id, type, direct_parent_triggers) "
                    + "VALUES(?::uuid,?,?,?::uuid[]) ON CONFLICT (cicd_job_run_id,trigger_id,type) DO UPDATE SET type=EXCLUDED.type RETURNING id";
            selectQuery += "WHERE t.trigger_id = ? ANDt.type = ?";
            insertParams = List.of(cicdJobRunId.toString(), trigger.getId(), trigger.getType(), conn.createArrayOf("uuid", parentIds != null ? parentIds.toArray() : new Object[0]));
            selectParams = List.of(cicdJobRunId.toString(), trigger.getId(), trigger.getType());
        }
        try (PreparedStatement ps = conn.prepareStatement(MessageFormat.format(insertQuery, company), Statement.RETURN_GENERATED_KEYS)) {
            int index = 0;
            for (Object param : insertParams) {
                ps.setObject(++index, param);
            }
            var count = ps.executeUpdate();
            if (count > 0) {
                var rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("Unable to obtain the id after the row was inserted for the trigger: " + trigger.getId());
                }
                return (UUID) rs.getObject("id");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(MessageFormat.format(selectQuery, company))) {
            int index = 0;
            for (Object param : selectParams) {
                ps.setObject(++index, param);
            }
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                log.warn("Unable to insert the record and unable to obtain the id of an existing record: run_id={}, trigger_id={}, trigger_type={}", cicdJobRunId, trigger.getId(), trigger.getType());
                return null;
            }
            return (UUID) rs.getObject("id");
        }
    }

    public void insertCICDJobRunTriggers(final String company, final UUID cicdJobRunId, final Set<CICDJobTrigger> triggerChain, final Connection conn) throws SQLException {
        if (triggerChain == null) {
            return;
        }
        for (CICDJobTrigger trigger : triggerChain) {
            insertJobRunTrigger(company, cicdJobRunId, trigger, conn);
        }
    }

    private Set<UUID> ensureGetDBIdsForDirectParentTriggers(final String company, final UUID cicdJobId, final Set<CICDJobTrigger> parents, final Connection conn)
            throws SQLException {
        if (CollectionUtils.isEmpty(parents)) {
            return Set.of();
        }
        Set<UUID> parentIds = new HashSet<>();
        // get db id for each parent
        for (CICDJobTrigger trigger : parents) {
            parentIds.add(insertJobRunTrigger(company, cicdJobId, trigger, conn));
        }
        return parentIds;
    }

    private void deleteCICDJobRunTriggers(final String company, final UUID cicdJobRunId, final Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(String.format("DELETE FROM %s.cicd_job_run_triggers WHERE cicd_job_run_id = ?::uuid", company))) {
            ps.setString(1, cicdJobRunId.toString());
            ps.executeUpdate();
        }
    }
    // endregion

    // region Job Run Params
    private void insertCICDJobRunParams(PreparedStatement insertCICDJobRunParamsPstmt, UUID cicdJobRunId, List<CICDJobRun.JobRunParam> params) throws SQLException {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        for (CICDJobRun.JobRunParam p : params) {
            insertCICDJobRunParamsPstmt.setObject(1, cicdJobRunId);
            insertCICDJobRunParamsPstmt.setString(2, p.getType());
            insertCICDJobRunParamsPstmt.setString(3, p.getName());
            insertCICDJobRunParamsPstmt.setString(4, p.getValue());

            insertCICDJobRunParamsPstmt.addBatch();
            insertCICDJobRunParamsPstmt.clearParameters();
        }
        insertCICDJobRunParamsPstmt.executeBatch();
    }

    private void deleteCICDJobRunParams(PreparedStatement deleteCICDJobRunParamsPstmt, UUID cicdJobRunId) throws SQLException {
        deleteCICDJobRunParamsPstmt.setObject(1, cicdJobRunId);
        deleteCICDJobRunParamsPstmt.executeUpdate();
    }
    // endregion

    // region insert
    private Optional<UUID> checkExisting(PreparedStatement checkExistingPstmt, CICDJobRun t) throws SQLException {
        checkExistingPstmt.setObject(1, t.getCicdJobId());
        checkExistingPstmt.setObject(2, t.getJobRunNumber());
        try (ResultSet rs = checkExistingPstmt.executeQuery()) {
            if (rs.next()) {
                UUID existingId = (UUID) rs.getObject(1);
                return Optional.ofNullable(existingId);
            }
            return Optional.empty();
        }
    }

    @Override
    public String insert(String company, CICDJobRun jobRun) throws SQLException {
        UUID cicdJobRunId = null;
        String checkExistingSql = String.format(CHECK_EXISTING_SQL_FORMAT, company);
        String insertSql = String.format("INSERT INTO " + company + ".cicd_job_runs" +
                " (cicd_job_id, job_run_number, status, start_time, duration, end_time, cicd_user_id, scm_commit_ids, source, reference_id, log_gcspath, ci, cd, metadata) " +
                " VALUES " +
                " (?,?,?,?,greatest(?,0),?,?,?,?,?,?,?,?,?::jsonb) " +
                " RETURNING id");
        String cicdJobRunParamsInsertSql = String.format(CICD_JOB_RUN_PARAMS_INSERT_SQL, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkExistingPstmt = conn.prepareStatement(checkExistingSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertCICDJobRunParamsPstmt = conn.prepareStatement(cicdJobRunParamsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            Optional<UUID> optionalExistingId = checkExisting(checkExistingPstmt, jobRun);
            if (optionalExistingId.isPresent()) {
                return optionalExistingId.get().toString();
            }
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                insertPstmt.setObject(1, jobRun.getCicdJobId());
                insertPstmt.setLong(2, jobRun.getJobRunNumber());
                insertPstmt.setString(3, jobRun.getStatus());
                insertPstmt.setTimestamp(4, jobRun.getStartTime() != null ? Timestamp.from(jobRun.getStartTime()) : null);
                insertPstmt.setInt(5, jobRun.getDuration() != null ? jobRun.getDuration() : 0);
                insertPstmt.setTimestamp(6, jobRun.getEndTime() != null ? Timestamp.from(jobRun.getEndTime()) : null);
                insertPstmt.setString(7, jobRun.getCicdUserId());
                insertPstmt.setObject(8, conn.createArrayOf("varchar", ListUtils.emptyIfNull(jobRun.getScmCommitIds()).toArray()));
                insertPstmt.setString(9, (jobRun.getSource() != null) ? jobRun.getSource().toString() : null);
                insertPstmt.setString(10, jobRun.getReferenceId());
                insertPstmt.setObject(11, jobRun.getLogGcspath());
                insertPstmt.setObject(12, jobRun.getCi());
                insertPstmt.setObject(13, jobRun.getCd());
                insertPstmt.setString(14, ParsingUtils.serialize(mapper, "metadata", jobRun.getMetadata(), "{}"));

                int affectedRows = insertPstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create ticket!");
                }
                // get the ID back
                try (ResultSet rs = insertPstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create cicd job run!");
                    }
                    cicdJobRunId = (UUID) rs.getObject(1);
                }

                insertCICDJobRunParams(insertCICDJobRunParamsPstmt, cicdJobRunId, jobRun.getParams());
                insertCICDJobRunTriggers(company, cicdJobRunId, jobRun.getTriggers(), conn);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return cicdJobRunId.toString();
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, CICDJobRun jobRun) throws SQLException {
        boolean success = true;
        String updateSql = "" +
                " UPDATE " + company + ".cicd_job_runs " +
                " SET" +
                "   status = ?," +
                "   start_time = ?," +
                "   duration = greatest(?,0)," +
                "   end_time = ?, " +
                "   cicd_user_id = ?," +
                "   scm_commit_ids = ?," +
                "   source = ?," +
                "   reference_id = ?," +
                "   log_gcspath = ?," +
                "   ci = ?," +
                "   cd = ?," +
                "   metadata = ?::jsonb," +
                "   updated_at = now()" +
                " WHERE id = ?";
        String cicdJobRunParamsDeleteSql = String.format(CICD_JOB_RUN_PARAMS_DELETE_SQL, company);
        String cicdJobRunParamsInsertSql = String.format(CICD_JOB_RUN_PARAMS_INSERT_SQL, company);
        UUID cicdJobRunId = jobRun.getId();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement updatePstmt = conn.prepareStatement(updateSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement deleteCICDJobRunParamsPstmt = conn.prepareStatement(cicdJobRunParamsDeleteSql);
             PreparedStatement insertCICDJobRunParamsPstmt = conn.prepareStatement(cicdJobRunParamsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                updatePstmt.setString(1, jobRun.getStatus());
                updatePstmt.setTimestamp(2, Timestamp.from(jobRun.getStartTime()));
                updatePstmt.setInt(3, jobRun.getDuration());
                updatePstmt.setTimestamp(4, jobRun.getEndTime() != null ? Timestamp.from(jobRun.getEndTime()) : null);
                updatePstmt.setString(5, jobRun.getCicdUserId());
                updatePstmt.setArray(6, conn.createArrayOf("varchar", jobRun.getScmCommitIds().toArray()));
                updatePstmt.setString(7, (jobRun.getSource() != null) ? jobRun.getSource().toString() : null);
                updatePstmt.setString(8, jobRun.getReferenceId());
                updatePstmt.setObject(9, jobRun.getLogGcspath());
                updatePstmt.setObject(10, jobRun.getCi());
                updatePstmt.setObject(11, jobRun.getCd());
                updatePstmt.setObject(12, ParsingUtils.serialize(mapper, "metadata", jobRun.getMetadata(), "{}"));
                updatePstmt.setObject(13, cicdJobRunId);

                int affectedRows = updatePstmt.executeUpdate();
                if (affectedRows > 0) {
                    deleteCICDJobRunParams(deleteCICDJobRunParamsPstmt, cicdJobRunId);
                    insertCICDJobRunParams(insertCICDJobRunParamsPstmt, cicdJobRunId, jobRun.getParams());
                    deleteCICDJobRunTriggers(company, cicdJobRunId, conn);
                    insertCICDJobRunTriggers(company, cicdJobRunId, jobRun.getTriggers(), conn);
                } else {
                    success = false;
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return success;
    }

    public int updateJobCiCdUserId(String company, CICDJobRun jobRun) throws SQLException {
        String updateSql = "UPDATE " + company + ".cicd_job_runs SET cicd_user_id = ?, updated_at = now() WHERE job_run_number = ? and cicd_job_id = ?";
        boolean success;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, jobRun.getCicdUserId());
            pstmt.setObject(2, jobRun.getJobRunNumber());
            pstmt.setObject(3, jobRun.getCicdJobId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return affectedRows;
        }
    }

    // endregion

    // region get and list commons
    public Set<CICDJobTrigger> getJobRunTriggers(String company, Set<UUID> triggerDBIds, Connection conn) throws SQLException {
        if (triggerDBIds == null) {
            return Set.of();
        }
        String SQL = String.format("SELECT * FROM %s.cicd_job_run_triggers WHERE id = ANY(?::uuid[])", company);
        try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", triggerDBIds.toArray()));
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                // should never happen..
                return null;
            }
            var parentIds = new HashSet<>(Arrays.asList((UUID[]) rs.getArray("direct_parent_triggers").getArray()));
            var parents = getJobRunTriggers(company, parentIds, conn);
            return Set.of(CICDJobTrigger.builder()
                    .id(rs.getString("trigger_id"))
                    .buildNumber(rs.getString("job_run_number"))
                    .type(rs.getString("type"))
                    .directParents(parents)
                    .build());
        }
    }

    public CICDJobTrigger getJobRunTrigger(String company, UUID jobRunId) throws SQLException {
        String SQL = String.format("SELECT * FROM %s.cicd_job_run_triggers WHERE cicd_job_run_id = ?::uuid", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, jobRunId.toString());
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                // shoudl never happen..
                return null;
            }
            return CICDJobTrigger.builder()
                    .id(rs.getString("trigger_id"))
                    .buildNumber(String.valueOf(rs.getInt("job_run_number")))
                    .type(rs.getString("type"))
                    .directParents(null)
                    .build();
        }
    }

    public Map<UUID, Set<CICDJobTrigger>> getJobRunTriggers(String company, List<UUID> jobRunIds) throws SQLException {
        String SQL = String.format("SELECT * FROM %s.cicd_job_run_triggers WHERE cicd_job_run_id = ANY(?::uuid[])", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", jobRunIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();

            Map<UUID, Set<CICDJobTrigger>> results = new HashMap<>();
            while (rs.next()) {
                UUID jobRunId = (UUID) rs.getObject("cicd_job_run_id");
                Set<CICDJobTrigger> triggers = results.get(jobRunId);
                if (triggers == null) {
                    triggers = new HashSet<>();
                    results.put(jobRunId, triggers);
                }
                var parentIds = rs.getArray("direct_parent_triggers");
                if (parentIds != null) {
                    // var dbIds = (UUID[]) parentIds.getArray();
                    // todo: dbIds to build the direct parents relation
                    getJobRunTriggers(company, Arrays.asList());
                }
                triggers.add(CICDJobTrigger.builder()
                        .id(rs.getString("trigger_id"))
                        .buildNumber(rs.getString("job_run_number"))
                        .type(rs.getString("type"))
                        .directParents(null)
                        .build()
                );
            }
            return results;
        }
    }

    public List<UUID> getParentJobRunIds(String company, List<UUID> jobRunIds) throws SQLException {
        List<UUID> resultList = new ArrayList<>();
        String sql = String.format("SELECT * FROM %s.cicd_job_run_triggers WHERE cicd_job_run_id = ANY(?::uuid[])", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", jobRunIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID jobRunId = (UUID) rs.getObject("cicd_job_run_id");
                resultList.add(jobRunId);
                var parentIds = rs.getArray("direct_parent_triggers");
                var dbIds = (UUID[]) parentIds.getArray();
                if (CollectionUtils.isNotEmpty(List.of(dbIds))) {
                    populateAncestorJobRunIds(company, resultList, Arrays.asList(dbIds));
                }
            }
            return resultList;
        }
    }

    public void populateAncestorJobRunIds(String company, List<UUID> result, List<UUID> dbIds) throws SQLException {
        UUID jobRunId;
        String sql = String.format("SELECT * FROM %s.cicd_job_run_triggers WHERE id = ANY(?::uuid[])", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", dbIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                var parentIds = rs.getArray("direct_parent_triggers");
                jobRunId = (UUID) rs.getObject("cicd_job_run_id");
                result.add(jobRunId);
                if (parentIds != null) {
                    var dbParentIds = (UUID[]) parentIds.getArray();
                    populateAncestorJobRunIds(company, result, Arrays.asList(dbParentIds));
                }
            }
        }
    }

    public Map<UUID, List<CICDJobRun.JobRunParam>> getJobRunParams(String company, List<UUID> jobRunIds) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".cicd_job_run_params WHERE cicd_job_run_id = ANY(?::uuid[])";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", jobRunIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();

            Map<UUID, List<CICDJobRun.JobRunParam>> result = new HashMap<>();
            while (rs.next()) {
                UUID jobRunId = (UUID) rs.getObject("cicd_job_run_id");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = rs.getString("value");
                if (!result.containsKey(jobRunId)) {
                    result.put(jobRunId, new ArrayList<>());
                }
                result.get(jobRunId).add(CICDJobRun.JobRunParam.builder()
                        .type(type)
                        .name(name)
                        .value(value)
                        .build()
                );
            }
            return result;
        }
    }

    private String formatCriterea(String criterea, List<Object> values, String newCriterea) {
        String result = criterea + ((values.size() == 0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }

    private DbListResponse<CICDJobRun> getJobRunsBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobIds, List<Long> jobRunNumbers) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_job_runs ";

        String orderBy = " ORDER BY job_run_number DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(cicdJobIds)) {
            criteria = formatCriterea(criteria, values, "cicd_job_id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", cicdJobIds));
        }
        if (CollectionUtils.isNotEmpty(jobRunNumbers)) {
            criteria = formatCriterea(criteria, values, "job_run_number = ANY(?::bigint[]) ");
            values.add(new ArrayWrapper<>("bigint", jobRunNumbers));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" + selectSqlBase + criteria + ") AS counted";

        List<CICDJobRun> retval = new ArrayList<>();
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
                CICDJobRun aggregationRecord = CiCdJobRunConverters.buildCiCdJobRun(mapper, rs);
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

    private List<CICDJobRun> mergeJobRunsParamAndTriggers(final List<CICDJobRun> jobRuns, final Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams, final Map<UUID, Set<CICDJobTrigger>> triggers) {
        return jobRuns.stream()
                .map(jobRun -> {
                    CICDJobRun.CICDJobRunBuilder bldr = jobRun.toBuilder();
                    if (jobRunParams.containsKey(jobRun.getId())) {
                        bldr.params(jobRunParams.get(jobRun.getId()));
                    }
                    var tmp = triggers.get(jobRun.getId());
                    if (tmp != null) {
                        bldr.triggers(tmp);
                    }
                    return bldr.build();
                })
                .collect(Collectors.toList());
    }

    private DbListResponse<CICDJobRun> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobIds, List<Long> jobRunNumbers) throws SQLException {
        DbListResponse<CICDJobRun> cicdJobRunDbListResponse = getJobRunsBatch(company, pageNumber, pageSize, ids, cicdJobIds, jobRunNumbers);
        List<CICDJobRun> jobRuns = cicdJobRunDbListResponse.getRecords();
        List<UUID> jobRunIds = jobRuns.stream()
                .map(CICDJobRun::getId)
                .collect(Collectors.toList());
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = getJobRunParams(company, jobRunIds);
        Map<UUID, Set<CICDJobTrigger>> jobRunTriggers = getJobRunTriggers(company, jobRunIds);
        List<CICDJobRun> fullJobRuns = mergeJobRunsParamAndTriggers(jobRuns, jobRunParams, jobRunTriggers);
        return DbListResponse.of(fullJobRuns, cicdJobRunDbListResponse.getTotalCount());
    }
    // endregion

    // region get
    @Override
    public Optional<CICDJobRun> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private Set<PathSegment> getSegment(String company, String triggerId, Integer triggerRunNumber, Map<String, Map<String, Object>> map) {
        if (Strings.isBlank(triggerId) || triggerRunNumber == null || MapUtils.isEmpty(map)) {
            return Set.of();
        }
        var key = triggerId + "::" + triggerRunNumber;
        var node = map.get(key);
        if (MapUtils.isEmpty(node)) {
            node = getJobRunWithTrigger(company, triggerId, triggerRunNumber);
            map.put(key, node);
            if (node == null) {
                return Set.of();
            }
        }
        Set<PathSegment> fullPath = new HashSet<>();
        if (node.get("trigger_id") != null) {
            fullPath.addAll(
                    getSegment(company, node.get("trigger_id").toString(), Integer.valueOf(node.get("trigger_run_number").toString()), map)
                    // .stream()
                    // .map(item -> item.toBuilder()
                    //     .position(item.getPosition() + fullPath.size())
                    //     .build())
                    // .collect(Collectors.toSet())
            );
        }
        fullPath.add(PathSegment.builder()
                .id(node.get("id").toString())
                .name(node.get("name").toString())
                .position(fullPath.size())
                .type(SegmentType.CICD_JOB)
                .build());
        return fullPath;
    }

    private Map<String, Object> getJobRunWithTrigger(String company, String jobFullName, Integer jobRunNumber) {
        var jobSql = MessageFormat.format(GET_JOBRUN_BY_NAME_SQL_FORMAT, company);
        var params = Map.of("fullName", jobFullName, "runNumber", jobRunNumber);
        return template.queryForMap(jobSql, params);
    }

    private Map<String, Object> getJobRunWithTrigger(String company, String jobRunId) {
        var jobSql = MessageFormat.format(GET_JOBRUN_SQL_FORMAT, company);
        var params = Map.of("id", jobRunId);
        return template.queryForMap(jobSql, params);
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<JobRunSegment> getSubjobs(String company, String jobRunId, QueryFilter filters, Pair<Set<String>, SortingOrder> sorting, int page, int pageSize) {
        // get the job run and job name
        var jobRun = getJobRunWithTrigger(company, jobRunId);

        var queryFilter = "";
        var resultsFilter = filters != null ? (Collection<String>) filters.getStrictMatches().get("result") : null;
        if (CollectionUtils.isNotEmpty(resultsFilter)) {
            queryFilter = String.format("WHERE result = ANY('{%s}'::text[]) \n", resultsFilter.stream().collect(Collectors.joining(",")));
        }
        var baseSql = MessageFormat.format(GET_SUBJOBS_PARENTS_SQL_FORMAT, company, jobRun.get("full_name"), jobRun.get("run_number").toString());
        var order = "ORDER BY " + (sorting == null || CollectionUtils.isNotEmpty(sorting.getLeft())
                ? DEFAULT_SORTING
                : String.format("%s %s \n", sorting.getLeft().stream().collect(Collectors.joining(", ")), sorting.getRight()));

        var pagination = String.format("LIMIT %s OFFSET %s ", pageSize, (page * pageSize));
        var querySql = baseSql + "SELECT * FROM full_tree " + queryFilter + order + pagination;
        var params = Map.of("id", jobRunId);
        var subJobs = template.queryForList(querySql, params);
        var count = subJobs.size();
        if (subJobs.stream().anyMatch(item -> item.get("id").toString().equals(jobRunId))) {
            count--;
        }
        if (count == pageSize) {
            var countSql = baseSql + "SELECT COUNT(*) FROM full_tree " + queryFilter;
            count = template.queryForObject(countSql, params, Integer.class);
        }

        var map = subJobs.stream().collect(Collectors.toMap(parent -> parent.get("full_name").toString() + "::" + parent.get("run_number").toString(), parent -> parent));
        map.put(jobRun.get("full_name").toString() + "::" + jobRun.get("run_number").toString(), jobRun);

        var results = subJobs.stream().filter(subJob -> !subJob.get("id").toString().equals(jobRunId)).map(subJob -> {
                    var id = (UUID) subJob.get("id");
                    Set<PathSegment> fullPath = new HashSet<>();
                    var triggerId = (String) subJob.get("trigger_id");
                    if (Strings.isNotBlank(triggerId)) {
                        fullPath.addAll(getSegment(company, triggerId, Integer.valueOf(subJob.get("trigger_run_number").toString()), map));
                    }
                    // adding the job itself as the last element in the path
                    fullPath.add(PathSegment.builder()
                            .id(id.toString())
                            .name(subJob.get("name").toString())
                            .type(SegmentType.CICD_JOB)
                            .position(fullPath.size())
                            .build());
                    return JobRunSegment.builder()
                            .id(id)
                            .jobNumber(subJob.get("run_number").toString())
                            .name(subJob.get("name").toString())
                            .result(subJob.get("result").toString())
                            .logs((String) subJob.get("logs"))
                            .duration(Integer.valueOf(subJob.get("duration").toString()))
                            .startTime(Instant.ofEpochMilli(Long.valueOf(subJob.get("start_time").toString())))
                            .url(getFullUrl(company, id))
                            .fullPath(fullPath)
                            .build();
                })
                .collect(Collectors.toSet());
        return DbListResponse.of(List.copyOf(results), count);
    }

    public String getFullUrl(String company, UUID jobRunId) {
        try {
            if (Strings.isBlank(company) || jobRunId == null) {
                return "";
            }
            // Get instance url and full name
            var query = "SELECT j.id, j.job_full_name, i.url, r.job_run_number "
                    + "FROM {0}.cicd_job_runs r, {0}.cicd_jobs j, {0}.cicd_instances i "
                    + "WHERE j.id = r.cicd_job_id AND j.cicd_instance_id = i.id AND r.id = ''{1}''::uuid "
                    + "GROUP BY j.id, i.url, r.job_run_number";
            final Set<String> urls = new HashSet<>(1);
            template.query(MessageFormat.format(query, company, jobRunId.toString()), (rs) -> {
                // get url
                urls.add(CiCdJobsDatabaseService.getFullUrl(company, rs.getString("url"), rs.getString("job_full_name"), rs.getInt("job_run_number"), null, null));
            });
            return urls.iterator().next();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Retrieves the log locations (gcs path) for logs in the job, in stages or in steps.
     *
     * @param company  tenant
     * @param jobRunId the levelops id of the build
     * @return
     */
    public Set<String> getLogs(final String company, final String jobRunId) {
        return getLogs(company, jobRunId, false);
    }

    /**
     * Retrieves the log locations (gcs path) for logs in the job, in stages or in steps.
     *
     * @param company  tenant
     * @param jobRunId the levelops id of the build
     * @return
     */
    public Set<String> getLogs(final String company, final String jobRunId, final boolean skipStages) {
        // get direct log
        // get stages logs
        // get steps logs
        var logs = new HashSet<String>();
        var query = MessageFormat.format(GET_ALL_LOGS_FORMAT, company, TABLE_NAME, CiCdJobRunStageDatabaseService.TABLE_NAME, CiCdJobRunStageStepsDatabaseService.TABLE_NAME, jobRunId);
        template.query(query, (rs) -> {
            logs.addAll(ParsingUtils.parseSet("job_logs", String.class, rs.getArray("job_logs")));
            if (!skipStages) {
                logs.addAll(ParsingUtils.parseSet("stage_logs", String.class, rs.getArray("stage_logs")));
            }
            logs.addAll(ParsingUtils.parseSet("step_logs", String.class, rs.getArray("step_logs")));
        });
        return logs;
    }
    // endregion

    // region list
    @Override
    public DbListResponse<CICDJobRun> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }

    public DbListResponse<CICDJobRun> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobIds, List<Long> jobRunNumbers) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, cicdJobIds, jobRunNumbers);
    }
    //endregion

    //region GetJobRunCommits
    /**
     * @param company
     * @param startTime  in milliseconds
     * @param endTime    in milliseconds
     * @param pageNumber
     * @param pageSize
     * @return
     * @throws SQLException
     */
    public DbListResponse<CICDJobRunCommits> getJobRunsCommits(String company, Long startTime, Long endTime, Integer pageNumber, Integer pageSize) throws SQLException {
        long timeDiffInMinutes = TimeUnit.MILLISECONDS.toMinutes(Instant.now().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - startTime);
        log.debug("Searching for job runs in the last {} minutes", timeDiffInMinutes);
        String selectSqlBase = "SELECT id, scm_commit_ids FROM " + company + ".cicd_job_runs WHERE (array_length(scm_commit_ids,1) > 0) AND (start_time > TIMESTAMP 'now' - interval '" + timeDiffInMinutes + " minutes') ORDER BY start_time";
        String selectSql = selectSqlBase + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        List<CICDJobRunCommits> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String[] scmCommitIds = rs.getArray("scm_commit_ids") != null
                        ? (String[]) rs.getArray("scm_commit_ids").getArray()
                        : new String[0];
                CICDJobRunCommits aggregationRecord = CICDJobRunCommits.builder()
                        .id(id)
                        .scmCommitIds(scmCommitIds.length > 0 ? Arrays.asList(scmCommitIds) : Collections.emptyList())
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

    public List<CICDJobRunCommits> getJobRunsCommitsV2(final String company, Long updatedAtStart, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        String selectSqlBase = null;
        if (updatedAtStart != null) {
            selectSqlBase = "SELECT r.id, r.scm_commit_ids, j.id as job_id, j.scm_url as job_scm_url, r.updated_at as job_run_updated_at FROM " + company + ".cicd_job_runs as r JOIN " + company + ".cicd_jobs as j on j.id=r.cicd_job_id WHERE (array_length(r.scm_commit_ids,1) > 0) AND r.updated_at > to_timestamp(:updated_at_start) ORDER BY r.updated_at";
            params.put("updated_at_start", updatedAtStart);
        } else {
            selectSqlBase = "SELECT r.id, r.scm_commit_ids, j.id as job_id, j.scm_url as job_scm_url, r.updated_at as job_run_updated_at FROM " + company + ".cicd_job_runs as r JOIN " + company + ".cicd_jobs as j on j.id=r.cicd_job_id WHERE (array_length(r.scm_commit_ids,1) > 0) ORDER BY r.updated_at";
        }
        String selectSql = selectSqlBase + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<CICDJobRunCommits> cicdJobRunCommits = template.query(selectSql, params, CICDJobRunCommitsConverters.rowMapper());
        return cicdJobRunCommits;
    }

    public List<CICDJobRunCommits> getJobRunsCommitsForCommitShas(final String company, List<String> commitShas) {
        List<String> sanitizedCommitShas = CollectionUtils.emptyIfNull(commitShas).stream().filter(s -> StringUtils.isNotBlank(s)).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sanitizedCommitShas)) {
            return Collections.emptyList();
        }
        String sql = "SELECT r.id, r.scm_commit_ids, j.id as job_id, j.scm_url as job_scm_url, r.updated_at as job_run_updated_at FROM " + company + ".cicd_job_runs as r JOIN " + company + ".cicd_jobs as j on j.id=r.cicd_job_id WHERE r.scm_commit_ids && ARRAY[ :scm_commit_shas ]::varchar[] ORDER BY r.updated_at";
        Map<String, Object> params = Map.of("scm_commit_shas", sanitizedCommitShas);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<CICDJobRunCommits> cicdJobRunCommits = template.query(sql, params, CICDJobRunCommitsConverters.rowMapper());
        return cicdJobRunCommits;
    }

    // endregion

    //region Latest Job Run UpdatedAt
    private static final String SELECT_LATEST_JOB_RUN_UPDATED_AT = "SELECT * from %s.cicd_job_runs ORDER BY updated_at DESC LIMIT 1";
    public Optional<CICDJobRun> getLatestUpdatedJobRun(final String company) {
        String sql = String.format(SELECT_LATEST_JOB_RUN_UPDATED_AT, company);
        List<CICDJobRun> cicdJobRuns = template.query(sql, Map.of(), CiCdJobRunConverters.rowMapper(mapper));
        if(CollectionUtils.isEmpty(cicdJobRuns)) {
            return Optional.empty();
        }
        return Optional.ofNullable(cicdJobRuns.get(0));
    }
    //endregion

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

    // region aggs
    @SuppressWarnings("unchecked")
    public DbListResponse<Map<String, Object>> getTriageGridAggs(final String company, final QueryFilter filters, OUConfiguration ouConfig, final int pageNumber, Integer pageSize) {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        List<String> jobTableConditions = new ArrayList<>();
        populateConditions(company, filters, conditions, params, ouConfig);
        populateConditionsForJobs(filters, jobTableConditions, params);
        String where = "";
        var parentFilter = "";
        var parentIds = filters.getStrictMatches().get("parent_job_ids") == null
                ? ""
                : String.join(",", ((Collection<String>) filters.getStrictMatches().get("parent_job_ids"))
                .stream()
                .filter(Strings::isNotBlank)
                .collect(Collectors.toSet()));

        if (Strings.isNotBlank(parentIds)) {
            parentFilter = MessageFormat.format(AGGS_BY_STATUS_SQL_FILTER_BY_PARENT_FORMAT, company, "'{" + parentIds + "}'");
        }
        if (conditions.size() > 0) {
            // the parent filter includes conditions already
            where = (Strings.isBlank(parentFilter) ? "WHERE " : " AND ") + String.join(" AND ", conditions) + " \n";
        }
        if (Strings.isNotBlank(parentFilter) || conditions.size() > 0) {
            where += " AND start_time IS NOT NULL";
        } else {
            where = "WHERE start_time IS NOT NULL";
        }

        String jenkinsTableCondition = !where.contains("jrh.") ? StringUtils.EMPTY : MessageFormat.format(JENKINS_RULE_HITS_TABLE, company);

        String initialField = "cicd_job_id,";
        String aggsField = "a.cicd_job_id,";

        String jobWhereClause = String.join(" AND ", jobTableConditions) + " \n";
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        String innerQuery = MessageFormat.format(AGGS_BY_STATUS_INNER_SQL_FORMAT, company, parentFilter, where, aggsField, initialField, jenkinsTableCondition);
        String baseQuery = MessageFormat.format(AGGS_BY_STATUS_SQL_FORMAT, company, innerQuery, jobWhereClause);
        log.debug("JobRun Aggs Base Query: {}", baseQuery);
        var records = template.query(baseQuery + limit, params, (rs, row) -> {
            return convertDBAgg(rs);
        });
        var totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) FROM ({0}) as a", baseQuery), params, Integer.class);

        String totalsQuery = MessageFormat.format("SELECT json_object_agg(start_day, results) AS totals FROM {0}.cicd_jobs j, (" + AGGS_BY_STATUS_INNER_SQL_FORMAT + ") as r", company, parentFilter, where, "", "", jenkinsTableCondition);
        var totalsResults = template.query(totalsQuery, params, (rs, row) -> {
            return ParsingUtils.<String, Object>parseMap(mapper, "totals", String.class, Object.class, rs.getString("totals"));
        });
        return DbListResponse.of(records, totalCount, totalsResults.get(0));
    }

    private Map<String, Object> convertDBAgg(final ResultSet rs) throws SQLException {
        return Map.of(
                "id", rs.getString("id"),
                "name", rs.getString("name"),
                "full_name", rs.getString("full_name"),
                "aggs", ParsingUtils.parseMap(mapper, "aggs", String.class, Object.class, rs.getString("aggs")).entrySet().stream()
                        .map(entry -> Map.of("key", entry.getKey(), "totals", entry.getValue()))
                        .collect(Collectors.toList()));
    }

    private void populateConditions(final String company, QueryFilter filters, @NonNull List<String> conditions, @NonNull MapSqlParameterSource params, OUConfiguration ouConfig) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            if (!allowedAggsFilters.contains(entry.getKey()) || filters.getStrictMatches().get(entry.getKey()) == null) {
                continue;
            }
            if ("job_ids".equalsIgnoreCase(entry.getKey()) || "results".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                var field = "job_ids".equalsIgnoreCase(entry.getKey()) ? "cicd_job_id" : "status";
                var fieldType = "job_ids".equalsIgnoreCase(entry.getKey()) ? "::uuid[]" : "::text[]";
                conditions.add(MessageFormat.format("c.{0} = ANY({2}{1})", field, fieldType, "'{" + String.join(",", collection) + "}'"));
                continue;
            }
            if ("triage_rule_ids".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                conditions.add("jrh.job_run_id = c.id");
                conditions.add(MessageFormat.format("jrh.rule_id = ANY({0}::uuid[])", "'{" + String.join(",", collection) + "}'"));
                continue;
            }
            if ("cicd_user_ids".equalsIgnoreCase(entry.getKey()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) {
                var columnName = "c.cicd_user_id";
                if (OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) {
                    var tmpParams = new HashMap<String, Object>();
                    var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, tmpParams, IntegrationType.getCICDIntegrationTypes());
                    if (StringUtils.isNotBlank(usersSelect)) {
                        conditions.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", columnName, usersSelect));
                        params.addValues(tmpParams);
                    }
                } else {
                    Set<String> collection = getValueAsSet(entry);
                    conditions.add(MessageFormat.format(columnName + " = ANY({0}::varchar[])", "'{" + String.join(",", collection) + "}'"));
                }
                continue;
            }
            if ("start_time".equalsIgnoreCase(entry.getKey())) {
                conditions.add(MessageFormat.format("c.start_time >= to_timestamp(:{0})", entry.getKey()));
                params.addValue(entry.getKey(), Long.valueOf(entry.getValue().toString()));
            } else if ("end_time".equalsIgnoreCase(entry.getKey())) {
                conditions.add(MessageFormat.format("c.start_time <= to_timestamp(:{0})", entry.getKey()));
                params.addValue(entry.getKey(), Long.valueOf(entry.getValue().toString()));
            }
        }
    }
    // endregion

    private void populateConditionsForJobs(QueryFilter filters, @NonNull List<String> conditions, MapSqlParameterSource params) {
        conditions.add("WHERE r.cicd_job_id = j.id \n");
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            if (!allowedAggsFilters.contains(entry.getKey()) || filters.getStrictMatches().get(entry.getKey()) == null) {
                continue;
            }
            if ("job_names".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                conditions.add(MessageFormat.format("job_name = ANY({0}::varchar[])", "'{" + String.join(",", collection) + "}'"));
                continue;
            }
            if ("job_normalized_full_names".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                conditions.add(MessageFormat.format("job_normalized_full_name = ANY({0}::varchar[])", "'{" + String.join(",", collection) + "}'"));
                continue;
            }
            if ("project_names".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                conditions.add(MessageFormat.format("project_name = ANY({0}::varchar[])", "'{" + String.join(",", collection) + "}'"));
                continue;
            }
            if ("cicd_instance_ids".equalsIgnoreCase(entry.getKey())) {
                Set<String> collection = getValueAsSet(entry);
                conditions.add(MessageFormat.format("cicd_instance_id = ANY({0}::uuid[])", "'{" + String.join(",", collection) + "}'"));
            }
        }
        if (filters.getPartialMatches() != null && MapUtils.isNotEmpty(filters.getPartialMatches())) {
            for (Map.Entry<String, Object> filter : filters.getPartialMatches().entrySet()) {
                if (PARTIAL_MATCH_COLUMNS.contains(filter.getKey()) && filter.getValue() instanceof String) {
                    String containsCondition = filter.getKey() + " SIMILAR TO :" + filter.getKey() + "_contains ";
                    conditions.add(containsCondition);
                    params.addValue(filter.getKey() + "_contains", "%" + filter.getValue() + "%");
                }
            }
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private Set<String> getValueAsSet(Map.Entry<String, Object> entry) {
        return ((Collection<String>) entry.getValue())
                .stream()
                .filter(Strings::isNotBlank)
                .collect(Collectors.toSet());
    }

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.cicd_job_runs(\n" +
                        "    id UUID                  PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    cicd_job_id              UUID NOT NULL REFERENCES {0}.cicd_jobs(id) ON DELETE CASCADE,\n" +
                        "    job_run_number           BIGINT NOT NULL,\n" +
                        "    status                   VARCHAR,\n" +
                        "    start_time               TIMESTAMP WITH TIME ZONE, \n" +
                        "    duration                 INTEGER,\n" +
                        "    end_time                 TIMESTAMP WITH TIME ZONE, \n" +
                        "    cicd_user_id             VARCHAR,\n" +
                        "    source                   VARCHAR,\n" +
                        "    reference_id             VARCHAR,\n" +
                        "    log_gcspath              VARCHAR,\n" +
                        "    scm_commit_ids           VARCHAR[]," +
                        "    ci                       BOOLEAN," +
                        "    cd                       BOOLEAN," +
                        "    metadata                 JSONB," +
                        "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",

                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_cicd_job_runs_cicd_job_id_job_run_number_idx on {0}.cicd_job_runs (cicd_job_id,job_run_number)",
                "CREATE INDEX IF NOT EXISTS cicd_job_runs_updated_at_idx on {0}.cicd_job_runs (updated_at)",

                "CREATE TABLE IF NOT EXISTS {0}.cicd_job_run_params(\n" +
                        "    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    cicd_job_run_id   UUID NOT NULL REFERENCES {0}.cicd_job_runs(id) ON DELETE CASCADE,\n" +
                        "    type              VARCHAR NOT NULL,\n" +
                        "    name              VARCHAR NOT NULL,\n" +
                        "    value             VARCHAR NOT NULL\n" +
                        ")",

                "CREATE TABLE IF NOT EXISTS {0}.cicd_job_run_triggers(\n" +
                        "    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    cicd_job_run_id         UUID REFERENCES {0}.cicd_job_runs(id) ON DELETE CASCADE,\n" + // the job_run_id of the job that inserted the record
                        "    trigger_id              VARCHAR NOT NULL,\n" +
                        "    type                    VARCHAR NOT NULL,\n" +
                        "    job_run_number          BIGINT,\n" +
                        "    direct_parent_triggers  UUID[]\n" + // db ids from the this same table
                        ")",

                "CREATE UNIQUE INDEX IF NOT EXISTS cicd_job_run_triggers_cicd_job_run_id_trigger_id_idx on {0}.cicd_job_run_triggers (cicd_job_run_id,trigger_id,type)");

        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    public Long getOldestJobRunStartTimeInMillis(String company, int integrationId) {

        Map<String,String> params = new HashMap<>();

        String selectSQl =
                " SELECT EXTRACT(EPOCH FROM JR.START_TIME) AS FIRST_JOB_RUN_START_TIME\n" +
                " FROM "+company+".CICD_JOB_RUNS JR\n" +
                " INNER JOIN "+company+".CICD_JOBS J ON J.ID = JR.CICD_JOB_ID\n" +
                " INNER JOIN "+company+".CICD_INSTANCES I ON I.ID = J.CICD_INSTANCE_ID\n" +
                " WHERE INTEGRATION_ID = "+ integrationId+" \n" +
                " ORDER BY JR.START_TIME \n" +
                " LIMIT 1";

        var oldestjobRunstartTimeInSeconds = template.queryForList(selectSQl, params, Long.class);

        return CollectionUtils.isNotEmpty(oldestjobRunstartTimeInSeconds) ? (oldestjobRunstartTimeInSeconds.get(0)) * 1000 : null;
    }
    // endregion
}
