package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.CICDJobRunDetails;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
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

@Log4j2
@Service
public class CiCdJobRunDetailsDatabaseService extends DatabaseService<CICDJobRunDetails> {
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.cicd_job_run_details (cicd_job_run_id, gcs_path) VALUES(?,?)\n" +
            "ON CONFLICT(cicd_job_run_id) DO NOTHING\n" +
            "RETURNING id";
    private static final String UPSERT_SQL_FORMAT = "INSERT INTO %s.cicd_job_run_details (cicd_job_run_id, gcs_path) VALUES(?,?)\n" +
            "ON CONFLICT(cicd_job_run_id) DO UPDATE SET (gcs_path,updated_at) = (EXCLUDED.gcs_path,now())\n" +
            "RETURNING id";
    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.cicd_job_run_details SET gcs_path = ?, updated_at = now() WHERE id = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.cicd_job_run_details WHERE id = ?";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public CiCdJobRunDetailsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, CICDJobRunDetails t) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID id;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, t.getCicdJobRunId());
            pstmt.setString(2, t.getGcsPath());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd job run detail!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cicd job run detail!");
                }
                id = (UUID) rs.getObject(1);
                return id.toString();
            }
        }
    }
    // endregion

    // region upsert
    public String upsert(String company, CICDJobRunDetails t) throws SQLException {
        String upsertSql = String.format(UPSERT_SQL_FORMAT, company);
        UUID id;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, t.getCicdJobRunId());
            pstmt.setString(2, t.getGcsPath());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd job run detail!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cicd job run detail!");
                }
                id = (UUID) rs.getObject(1);
                return id.toString();
            }
        }
    }
    // endregion

    // region delete
    @Override
    public Boolean update(String company, CICDJobRunDetails t) throws SQLException {
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, t.getGcsPath());
            pstmt.setObject(2, t.getId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        }
    }
    // endregion

    // region get and list commons
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }
    private DbListResponse<CICDJobRunDetails> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobRunIds) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".cicd_job_run_details";
        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(cicdJobRunIds)) {
            criteria = formatCriterea(criteria, values, "cicd_job_run_id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", cicdJobRunIds));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<CICDJobRunDetails> retval = new ArrayList<>();
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
                UUID cicdJobRunId = (UUID) rs.getObject("cicd_job_run_id");
                String gcsPath = rs.getString("gcs_path");
                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));

                CICDJobRunDetails cicdJobRunDetails = CICDJobRunDetails.builder()
                        .id(id)
                        .cicdJobRunId(cicdJobRunId)
                        .gcsPath(gcsPath)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .build();

                retval.add(cicdJobRunDetails);
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

    // region get
    @Override
    public Optional<CICDJobRunDetails> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)),null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<CICDJobRunDetails> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null,null);
    }
    public DbListResponse<CICDJobRunDetails> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> cicdJobRunIds) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, cicdJobRunIds);
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
                "CREATE TABLE IF NOT EXISTS {0}.cicd_job_run_details(\n" +
                "    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    cicd_job_run_id         UUID REFERENCES {0}.cicd_job_runs(id) ON DELETE CASCADE,\n" + // the job_run_id of the job that inserted the record
                "    gcs_path                VARCHAR NOT NULL,\n" +
                "    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                "    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                ")",

                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_cicd_job_run_details_cicd_job_run_id_idx on {0}.cicd_job_run_details (cicd_job_run_id)"
        );

        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
