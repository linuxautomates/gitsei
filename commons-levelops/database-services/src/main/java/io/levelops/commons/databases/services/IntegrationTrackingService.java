package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.IntegrationTrackerEx;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class IntegrationTrackingService extends DatabaseService<IntegrationTracker> {

    private static final RowMapper<IntegrationTracker> TRACKER_ROW_MAPPER = (rs, rowNumber) ->
            IntegrationTracker.builder()
                    .id(rs.getString("id"))
                    .integrationId(rs.getString("integration_id"))
                    .latestIngestedAt(rs.getLong("latest_ingested_at"))
                    .lastAggStartedAt(rs.getLong("last_agg_started_at"))
                    .lastAggEndedAt(rs.getLong("last_agg_ended_at"))
                    .latestAggregatedAt(rs.getLong("latest_aggregated_at"))
                    .latestESIndexedAt(rs.getLong("latest_es_indexed_at"))
                    .build();
    private static final RowMapper<IntegrationTrackerEx> TRACKER_EX_ROW_MAPPER = (rs, rowNumber) ->
            IntegrationTrackerEx.builder()
                    .id(rs.getString("id"))
                    .integrationId(rs.getInt("integration_id"))
                    .latestIngestedAt(rs.getLong("latest_ingested_at"))
                    .lastAggStartedAt(rs.getLong("last_agg_started_at"))
                    .lastAggEndedAt(rs.getLong("last_agg_ended_at"))
                    .latestAggregatedAt(rs.getLong("latest_aggregated_at"))
                    .latestESIndexedAt(rs.getLong("latest_es_indexed_at"))
                    .integrationStatus(rs.getString("status"))
                    .application(rs.getString("application"))
                    .build();

    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public IntegrationTrackingService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, IntegrationTracker tracker) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Boolean upsert(String company, IntegrationTracker tracker) {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            String SQL = "INSERT INTO " + company + ".integration_tracker(integration_id,latest_ingested_at,last_agg_started_at,last_agg_ended_at) "
                    + "VALUES(?,?,?,?) ON CONFLICT (integration_id) DO UPDATE SET (latest_ingested_at,last_agg_started_at,last_agg_ended_at) =" +
                    " (EXCLUDED.latest_ingested_at,EXCLUDED.last_agg_started_at,EXCLUDED.last_agg_ended_at)";

            try (PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                pstmt.setObject(++i, Integer.parseInt(tracker.getIntegrationId()));
                pstmt.setObject(++i, tracker.getLatestIngestedAt());
                pstmt.setObject(++i, tracker.getLastAggStartedAt());
                pstmt.setObject(++i, tracker.getLastAggEndedAt());
                return pstmt.execute();
            }
        }));
    }

    public void upsertJiraWIDBAggregatedAt(String company, Integer integrationId, Long latestAggregatedAt) throws SQLException {
        String sql = "INSERT INTO " + company + ".integration_tracker(integration_id,latest_aggregated_at) VALUES(:integration_id, :latest_aggregated_at) ON CONFLICT (integration_id) DO UPDATE SET latest_aggregated_at = EXCLUDED.latest_aggregated_at";
        Map<String, Object> params = Map.of("latest_aggregated_at", latestAggregatedAt, "integration_id", integrationId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        int updatedRows = template.update(sql, params);
        if (updatedRows <= 0) {
            String error = String.format("Failed to upsert Jira or WI latest db aggregated at!! company %s, integrationId %d, latestAggregatedAt %d", company, integrationId, latestAggregatedAt);
            throw new SQLException(error);
        }
        return;
    }

    public boolean updateESIndexedAt(String company, Long latestESIndexedAt) throws SQLException {
        String sql = "UPDATE " + company + ".integration_tracker SET latest_es_indexed_at=:latest_es_indexed_at, latest_ingested_at=:latest_es_indexed_at WHERE latest_aggregated_at=:latest_es_indexed_at AND integration_id IN (SELECT id FROM " + company + ".integrations where application in ('jira', 'azure_devops'))";
        Map<String, Object> params = Map.of("latest_es_indexed_at", latestESIndexedAt);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        int updatedRows = template.update(sql, params);
        return (updatedRows > 0);
    }

    public int updateLastAggStarted(String company, Integer integrationId, Long lastAggStartedAt) {
        String sql = "UPDATE " + company + ".integration_tracker SET last_agg_started_at = :last_agg_started_at where integration_id=:integration_id";
        Map<String, Object> params = Map.of("last_agg_started_at", lastAggStartedAt, "integration_id", integrationId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        int affectedRows = template.update(sql, params);
        log.info("updateLastAggStarted, company = {}, integrationId = {}, lastAggStartedAt = {}, affectedRows = {}", company, integrationId, lastAggStartedAt, affectedRows);
        return affectedRows;
    }

    @Override
    public Boolean update(String company, IntegrationTracker tracker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationTracker> get(String company, String integrationId) {
        Validate.notNull(integrationId, "Missing integrationId.");
        List<IntegrationTracker> data = template.query(
                "SELECT * FROM " + company + ".integration_tracker WHERE integration_id = :integid",
                Map.of("integid", NumberUtils.toInt(integrationId)),
                TRACKER_ROW_MAPPER);
        return data.stream().max(Comparator.comparing(IntegrationTracker::getLatestIngestedAt));
    }

    @Override
    public DbListResponse<IntegrationTracker> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    private static final String SELECT_DB_ES_MISMATCHED_TRACKERS = "select t.*, i.status, i.application FROM %s.integration_tracker AS t JOIN %s.integrations AS i ON i.id = t.integration_id WHERE (latest_aggregated_at IS NOT null) AND ((latest_es_indexed_at IS NULL) OR (latest_aggregated_at != latest_es_indexed_at)) AND (latest_aggregated_at>=:latest_aggregated_at_min) order by t.integration_id";

    public List<IntegrationTrackerEx> getDBESMismatchedTrackers (String company, Instant latestAggregatedAtMin) {
        String sql = String.format(SELECT_DB_ES_MISMATCHED_TRACKERS, company, company);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("latest_aggregated_at_min", latestAggregatedAtMin.getEpochSecond());
        List<IntegrationTrackerEx> integrationTrackers = template.query(sql, params, TRACKER_EX_ROW_MAPPER);
        return integrationTrackers;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".integration_tracker(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    last_agg_started_at BIGINT DEFAULT extract(epoch from now()),\n" +
                "    last_agg_ended_at BIGINT ,\n" +
                "    latest_ingested_at BIGINT ,\n" +
                "    latest_aggregated_at BIGINT ,\n" +
                "    latest_es_indexed_at BIGINT ,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    UNIQUE(integration_id)" +
                ")";

        template.getJdbcTemplate().execute(sql);
        return true;
    }
}