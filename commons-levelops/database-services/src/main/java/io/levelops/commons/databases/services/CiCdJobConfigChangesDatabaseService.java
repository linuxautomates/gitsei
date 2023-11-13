package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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

@Log4j2
@Service
public class CiCdJobConfigChangesDatabaseService extends DatabaseService<CICDJobConfigChange> {
    private static final String CHECK_EXISTING_SQL_FORMAT = "SELECT id from %s.cicd_job_config_changes where cicd_job_id = ? AND change_time = ? AND change_type = ?  AND cicd_user_id = ?";
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.cicd_job_config_changes (cicd_job_id,change_time,change_type,cicd_user_id) VALUES(?,?,?,?) RETURNING id";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_job_config_changes WHERE id = ?";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public CiCdJobConfigChangesDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class);
    }
    // endregion

    // region insert
    private Optional<UUID> checkExisting(PreparedStatement checkExistingPstmt, CICDJobConfigChange t) throws SQLException {
        checkExistingPstmt.setObject(1, t.getCicdJobId());
        checkExistingPstmt.setTimestamp(2, Timestamp.from(t.getChangeTime()));
        checkExistingPstmt.setString(3, t.getChangeType());
        checkExistingPstmt.setString(4, t.getCicdUserId());
        try (ResultSet rs = checkExistingPstmt.executeQuery()) {
            if (rs.next()) {
                UUID existingId = (UUID) rs.getObject(1);
                return Optional.ofNullable(existingId);
            }
            return Optional.empty();
        }
    }
    @Override
    public String insert(String company, CICDJobConfigChange t) throws SQLException {
        UUID cicdJobConfigChangeId = null;
        String checkExistingSql = String.format(CHECK_EXISTING_SQL_FORMAT, company);
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkExistingPstmt = conn.prepareStatement(checkExistingSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            Optional<UUID> optionalExistingId = checkExisting(checkExistingPstmt, t);
            if(optionalExistingId.isPresent()){
                return optionalExistingId.get().toString();
            }
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                insertPstmt.setObject(1, t.getCicdJobId());
                insertPstmt.setTimestamp(2, Timestamp.from(t.getChangeTime()));
                insertPstmt.setString(3, t.getChangeType());
                insertPstmt.setString(4, t.getCicdUserId());

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
                    cicdJobConfigChangeId = (UUID) rs.getObject(1);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return cicdJobConfigChangeId.toString();
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, CICDJobConfigChange t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }
    // endregion

    // region get and list commons
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }

    private DbListResponse<CICDJobConfigChange> getJobConfigChangesBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids) throws SQLException {
        //String selectSqlBase = "SELECT * FROM " + company + ".aggregations";
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_job_config_changes ";

        String orderBy = " ORDER BY change_time DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<CICDJobConfigChange> retval = new ArrayList<>();
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
                UUID cicdJobId = (UUID) rs.getObject("cicd_job_id");
                Instant changeTime = DateUtils.toInstant(rs.getTimestamp("change_time"));
                String changeType = rs.getString("change_type");
                String cicdUserId = rs.getString("cicd_user_id");

                CICDJobConfigChange aggregationRecord = CICDJobConfigChange.builder()
                        .id(id)
                        .cicdJobId(cicdJobId)
                        .changeTime(changeTime)
                        .changeType(changeType)
                        .cicdUserId(cicdUserId)
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

    private DbListResponse<CICDJobConfigChange> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids) throws SQLException {
        DbListResponse<CICDJobConfigChange> cicdJobRunDbListResponse = getJobConfigChangesBatch(company, pageNumber, pageSize, ids);
        return cicdJobRunDbListResponse;
    }
    // endregion
    // region get
    @Override
    public Optional<CICDJobConfigChange> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id))).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<CICDJobConfigChange> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null);
    }
    public DbListResponse<CICDJobConfigChange> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids);
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
        List<String> sqlStatements = new ArrayList<>();
        String sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".cicd_job_config_changes(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    cicd_job_id UUID NOT NULL REFERENCES " + company + ".cicd_jobs(id) ON DELETE CASCADE,\n" +
                "    change_time TIMESTAMP WITH TIME ZONE NOT NULL, \n" +
                "    change_type VARCHAR NOT NULL,\n" +
                "    cicd_user_id VARCHAR\n" +
                ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_cicd_job_config_changes_cicd_job_id_change_time_change_type_cicd_user_id_idx on " + company + ".cicd_job_config_changes (cicd_job_id,change_time,change_type,cicd_user_id)";
        sqlStatements.add(sqlStatement);

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqlStatements) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
    // endregion
}
