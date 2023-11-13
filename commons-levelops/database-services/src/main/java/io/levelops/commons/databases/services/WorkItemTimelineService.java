package io.levelops.commons.databases.services;

import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringSubstitutor;
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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.converters.DbWorkItemHistoryConverters.mapToDbWorkItemHistory;
import static io.levelops.commons.databases.converters.DbWorkItemHistoryConverters.mapToDbWorkItemHistoryList;
import static io.levelops.commons.databases.converters.DbWorkItemHistoryConverters.workItemHistoricalMapper;

@Service
@Log4j2
public class WorkItemTimelineService extends DatabaseService<DbWorkItemHistory> {

    public static final String TABLE_NAME = "issue_mgmt_workitems_timeline";

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.issue_mgmt_workitems_timeline (" +
            "workitem_id, integration_id, field_type, field_value, start_date, end_date)"
            + " VALUES(?,?,?,?,?,?)\n" +
            "ON CONFLICT(workitem_id, integration_id, field_type, field_value, start_date) " +
            "DO NOTHING " +
            "RETURNING id";

    private static final String INSERT_FAKE_EVENT_FORMAT = "INSERT INTO %s.issue_mgmt_workitems_timeline (" +
            "workitem_id, integration_id, field_type, field_value, start_date, end_date)"
            + " VALUES(?,?,?,?,?,?)\n" +
            "ON CONFLICT(workitem_id, integration_id, field_type, field_value, start_date) " +
            "DO NOTHING " +
            "RETURNING id";

    private static final String FETCH_MAX_STARTTIME_ROW = "Select * FROM %s.issue_mgmt_workitems_timeline where" +
            " workitem_id = ? and integration_id = ? and field_type = ? ORDER BY start_date DESC LIMIT 1";

    private static final String FETCH_MIN_STARTTIME_ROW = "Select * FROM %s.issue_mgmt_workitems_timeline where" +
            " workitem_id = ? and integration_id = ? and field_type = ? ORDER BY start_date ASC LIMIT 1";

    private static final String FETCH_MIN_STARTTIME_ROWS = "Select DISTINCT ON (field_type) * FROM %s.issue_mgmt_workitems_timeline " +
            " where workitem_id = ? and integration_id = ? ORDER BY field_type,start_date ASC";

    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.issue_mgmt_workitems_timeline SET end_date = ?" +
            " WHERE workitem_id = ? and integration_id = ? and field_type = ?  and field_value = ? and start_date = ?";

    private static final String DELETE_EVENT_FORMAT = "DELETE FROM %s.issue_mgmt_workitems_timeline  WHERE id = ? ";

    private static final String FETCH_ALL_EVENTS = "Select * FROM %s.issue_mgmt_workitems_timeline WHERE" +
            " workitem_id = ? and integration_id = ? and field_type = ? ORDER BY start_date";


    private final NamedParameterJdbcTemplate template;

    @Autowired
    public WorkItemTimelineService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbWorkItemHistory dbWorkItemHistory) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID workItemHistoryJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItemHistory.getWorkItemId());
            pstmt.setObject(++i, NumberUtils.toInt(dbWorkItemHistory.getIntegrationId()));
            pstmt.setObject(++i, dbWorkItemHistory.getFieldType());
            pstmt.setObject(++i, dbWorkItemHistory.getFieldValue());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getStartDate());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getEndDate());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workItemHistoryJobId = (UUID) rs.getObject(1);
                    return workItemHistoryJobId.toString();
                }
            }
        }
        return null;
    }

    public Optional<String> upsert(String company, DbWorkItemHistory dbWorkItemHistory) throws SQLException {
        String insertSql = String.format("INSERT INTO %s.issue_mgmt_workitems_timeline " +
                " (workitem_id, integration_id, field_type, field_value, start_date, end_date)" +
                " VALUES(?,?,?,?,?,?) " +
                " ON CONFLICT(workitem_id, integration_id, field_type, field_value, start_date) " +
                " DO UPDATE SET " +
                "   end_date = EXCLUDED.end_date " +
                " RETURNING id ", company);
        UUID workItemHistoryJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItemHistory.getWorkItemId());
            pstmt.setObject(++i, NumberUtils.toInt(dbWorkItemHistory.getIntegrationId()));
            pstmt.setObject(++i, dbWorkItemHistory.getFieldType());
            pstmt.setObject(++i, dbWorkItemHistory.getFieldValue());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getStartDate());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getEndDate());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workItemHistoryJobId = (UUID) rs.getObject(1);
                    return Optional.ofNullable(workItemHistoryJobId.toString());
                }
            }
        }
        return Optional.empty();
    }

    public String insertFakeEvent(String company, DbWorkItemHistory dbWorkItemHistory) throws SQLException {
        String insertSql = String.format(INSERT_FAKE_EVENT_FORMAT, company);
        UUID workItemHistoryJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItemHistory.getWorkItemId());
            pstmt.setObject(++i, NumberUtils.toInt(dbWorkItemHistory.getIntegrationId()));
            pstmt.setObject(++i, dbWorkItemHistory.getFieldType());
            pstmt.setObject(++i, dbWorkItemHistory.getFieldValue());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getStartDate());
            pstmt.setTimestamp(++i, dbWorkItemHistory.getEndDate());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    workItemHistoryJobId = (UUID) rs.getObject(1);
                    return workItemHistoryJobId.toString();
                }
            }
        }
        return "";
    }

    public Optional<DbWorkItemHistory> getLastEvent(String company, Integer integrationId,
                                                    String fieldType, String workItemId) throws SQLException {
        String fetchRowSql = String.format(FETCH_MAX_STARTTIME_ROW, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            pstmt.setObject(3, fieldType);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapToDbWorkItemHistory(resultSet));
            }
        }
        return Optional.empty();
    }

    public Optional<DbWorkItemHistory> getLastEventBefore(String company, Integer integrationId,
                                                          String fieldType, String workItemId,
                                                          Timestamp before) throws SQLException {
        String fetchRowSql = String.format("SELECT * FROM %s.issue_mgmt_workitems_timeline" +
                " WHERE workitem_id = ? and integration_id = ? and field_type = ?" +
                " AND start_date < ? " +
                " ORDER BY start_date DESC " +
                " LIMIT 1 ", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            pstmt.setObject(3, fieldType);
            pstmt.setObject(4, before);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapToDbWorkItemHistory(resultSet));
            }
        }
        return Optional.empty();
    }

    public Optional<DbWorkItemHistory> getFirstEvent(String company, Integer integrationId,
                                                     String fieldType, String workItemId) throws SQLException {
        String fetchRowSql = String.format(FETCH_MIN_STARTTIME_ROW, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            pstmt.setObject(3, fieldType);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapToDbWorkItemHistory(resultSet));
            }
        }
        return Optional.empty();
    }

    public Optional<DbWorkItemHistory> getFirstEventAfter(String company, Integer integrationId,
                                                          String fieldType, String workItemId,
                                                          Timestamp after) throws SQLException {
        String fetchRowSql = String.format("SELECT * FROM %s.issue_mgmt_workitems_timeline " +
                " WHERE workitem_id = ? and integration_id = ? and field_type = ? " +
                " AND start_date > ? " +
                " ORDER BY start_date ASC " +
                " LIMIT 1", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            pstmt.setObject(3, fieldType);
            pstmt.setObject(4, after);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapToDbWorkItemHistory(resultSet));
            }
        }
        return Optional.empty();
    }

    public List<DbWorkItemHistory> getFirstEventOfEachType(String company, Integer integrationId,
                                                           String workItemId) throws SQLException {
        String fetchRowSql = String.format(FETCH_MIN_STARTTIME_ROWS, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            ResultSet resultSet = pstmt.executeQuery();
            return mapToDbWorkItemHistoryList(resultSet);
        }
    }

    public List<DbWorkItemHistory> getEvents(String company, Integer integrationId, String fieldType,
                                             String workItemId) throws SQLException {
        String fetchRowSql = String.format(FETCH_ALL_EVENTS, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, integrationId);
            pstmt.setObject(3, fieldType);
            ResultSet resultSet = pstmt.executeQuery();
            return mapToDbWorkItemHistoryList(resultSet);
        }
    }

    public void updateEndDate(String company, DbWorkItemHistory dbWorkItemHistory) throws SQLException {
        String fetchRowSql = String.format(UPDATE_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setTimestamp(1, dbWorkItemHistory.getEndDate());
            pstmt.setObject(2, dbWorkItemHistory.getWorkItemId());
            pstmt.setObject(3, NumberUtils.toInt(dbWorkItemHistory.getIntegrationId()));
            pstmt.setObject(4, dbWorkItemHistory.getFieldType());
            pstmt.setObject(5, dbWorkItemHistory.getFieldValue());
            pstmt.setObject(6, dbWorkItemHistory.getStartDate());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create WorkItem history!");
            }
        }
    }

    public int updateZeroStartDatesForWorkItem(String company, String integrationId, String workItemId, Timestamp newValue) throws SQLException {
        String fetchRowSql = String.format("UPDATE %s.issue_mgmt_workitems_timeline" +
                " SET start_date = ?" +
                " WHERE workitem_id = ? and integration_id = ? and start_date = ?", company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql)) {
            int i = 0;
            pstmt.setTimestamp(++i, newValue);
            pstmt.setObject(++i, workItemId);
            pstmt.setObject(++i, NumberUtils.toInt(integrationId));
            pstmt.setTimestamp(++i, new Timestamp(0));
            return pstmt.executeUpdate();
        }
    }

    public void updateAllZeroStartDates(String company, String integrationId, Long ingestedAt) {
        String sql = "" +
                " WITH j AS (" +
                "   SELECT t.id, t.workitem_id, t.integration_id, t.start_date, w.workitem_created_at, w.ingested_at " +
                "   FROM ${company}.issue_mgmt_workitems_timeline AS t " +
                "   LEFT JOIN ${company}.issue_mgmt_workitems AS w " +
                "   ON    w.integration_id = t.integration_id " +
                "   AND   w.workitem_id    = t.workitem_id " +
                "   WHERE start_date       = to_timestamp(0) " +
                "   AND   t.integration_id   = :integration_id " +
                "   AND   w.ingested_at      = :ingested_at " +
                " )" +
                " UPDATE ${company}.issue_mgmt_workitems_timeline AS upd " +
                " SET start_date = j.workitem_created_at" +
                " FROM j " +
                " WHERE upd.id = j.id ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        Map<String, Object> params = Map.of(
                "ingested_at", ingestedAt,
                "integration_id", Integer.valueOf(integrationId));
        template.update(sql, params);
    }

    public void deleteEvent(String company, String id) throws SQLException {
        String fetchRowSql = String.format(DELETE_EVENT_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fetchRowSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, UUID.fromString(id));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows <= 0) {
                throw new SQLException("Failed to delete WorkItem history!");
            }
        }
    }

    /**
     * It fetches the TimeLine data based on fieldTypes from the DB
     *
     * @return List of DbWorkItemHistory
     */
    public DbListResponse<DbWorkItemHistory> listByFilter(String company, List<String> workItemIds,
                                                          List<String> integrationIds, List<String> fieldTypes) {
        Validate.notEmpty(workItemIds, "workItems cannot be empty");
        Validate.notEmpty(integrationIds, "Integration ids cannot be empty");
        Validate.notEmpty(fieldTypes, "Field types cannot be empty");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME;
        String whereCondition = " WHERE field_type IN ( :field_types ) AND" +
                " workitem_id IN ( :workitem_ids ) AND integration_id::text IN ( :integration_ids )";
        sql += whereCondition;
        Map<String, Object> params = new HashMap<>();
        params.put("workitem_ids", workItemIds);
        params.put("integration_ids", integrationIds);
        params.put("field_types", fieldTypes);
        log.info("sql = " + sql);
        log.info("params: {}", params);

        List<DbWorkItemHistory> results = template.query(sql, params, workItemHistoricalMapper());
        return DbListResponse.of(results, results.size());
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }


    @Override
    public Boolean update(String company, DbWorkItemHistory t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbWorkItemHistory> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbWorkItemHistory> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_workitems_timeline(\n" +
                        "    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    workitem_id             VARCHAR NOT NULL,\n" +
                        "    integration_id          INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    field_type              VARCHAR NOT NULL,\n" +
                        "    field_value             VARCHAR NOT NULL,\n" +
                        "    start_date              TIMESTAMP WITH TIME ZONE,\n" +
                        "    end_date                TIMESTAMP WITH TIME ZONE,\n" +
                        "    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                        "    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_workitems_timeline_workitem_integration_type_value_start_idx on " +
                        "{0}.issue_mgmt_workitems_timeline (workitem_id, integration_id, field_type, field_value, start_date)"
        );
        ddl.stream()
                .map(statement -> MessageFormat.format(statement, company))
                .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
