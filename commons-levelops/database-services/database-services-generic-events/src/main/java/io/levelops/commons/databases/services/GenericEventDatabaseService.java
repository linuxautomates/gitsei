package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.GenericEventConverters;
import io.levelops.commons.models.DbListResponse;
import io.propelo.commons.generic_events.models.Component;
import io.propelo.commons.generic_events.models.GenericEventRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class GenericEventDatabaseService extends DatabaseService<GenericEventRequest> {
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.generic_events(component, key, secondary_key, event_type, event_time) VALUES (:component, :key, :secondary_key, :event_type, :event_time) RETURNING id";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.generic_events WHERE id = :id";


    private final NamedParameterJdbcTemplate template;

    public GenericEventDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    // region insert upsert common
    private void validateInput(String company, GenericEventRequest t) {
        Validate.notBlank(company, "Company cannot be null or empty!");

        Validate.notNull(t, "Input Generic Event Request cannot be null!");
        Validate.notNull(t.getComponent(),"Component can not be null");
        Validate.notBlank(t.getKey(),"Key can not be null or empty!");
        Validate.notBlank(t.getEventType(),"Event Type can not be null or empty!");
        Validate.notNull(t.getEventTime(),"Event time can not be null");
    }
    private MapSqlParameterSource constructParameterSourceForReport(GenericEventRequest t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("component", t.getComponent().toString());
        params.addValue("key", t.getKey());
        params.addValue("secondary_key", t.getSecondaryKey());
        params.addValue("event_type", t.getEventType());
        params.addValue("event_time", Timestamp.from(Instant.ofEpochSecond(t.getEventTime())));
        return params;
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, GenericEventRequest t) throws SQLException {
        validateInput(company, t);
        MapSqlParameterSource params = constructParameterSourceForReport(t, null);
        String insertReportSql = String.format(INSERT_SQL_FORMAT, company);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertReportSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert dev productivity report record!! " + t.toString());
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id.toString();
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, GenericEventRequest t) throws SQLException {
        throw new NotImplementedException();
    }
    // endregion

    // region get
    @Override
    public Optional<GenericEventRequest> get(String company, String param) throws SQLException {
        var results = getBatch(company, 0, 10, null, null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region list
    @Override
    public DbListResponse<GenericEventRequest> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null);
    }
    private DbListResponse<GenericEventRequest> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Component> components, List<String> eventTypes, List<String> keys) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(components)) {
            criterias.add("component in (:components)");
            params.put("components", components.stream().map(Objects::toString).collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(eventTypes)) {
            criterias.add("event_type in (:event_types)");
            params.put("event_types", eventTypes);
        }
        if(CollectionUtils.isNotEmpty(keys)) {
            criterias.add("key in (:keys)");
            params.put("keys", keys);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".generic_events";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<GenericEventRequest> results = template.query(selectSql, params, GenericEventConverters.rowMapper());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, GenericEventConverters.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    public DbListResponse<GenericEventRequest> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<Component> components, List<String> eventTypes, List<String> keys) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, components, eventTypes, keys);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".generic_events\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    component VARCHAR NOT NULL,\n" +
                        "    key VARCHAR NOT NULL,\n" +
                        "    secondary_key VARCHAR,\n" +
                        "    event_type VARCHAR NOT NULL,\n" +
                        "    event_time TIMESTAMPTZ NOT NULL,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS generic_events_component_event_type_idx on " + company + ".generic_events (component, event_type)",
                "CREATE INDEX IF NOT EXISTS generic_events_key_idx on " + company + ".generic_events (key)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
