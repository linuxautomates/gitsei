package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Log4j2
@Service
public class EventTypesDatabaseService extends DatabaseService<EventType> {

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;
    private final ComponentsDatabaseService componentsService;

    private final static String bootstrapValues;
    private final static String bootstrapValuesLocation = "db/default_data/event_types.value";

    static {
        // Loading resource in static block because when Spring autowires this class, the class loader is different
        String values = null;
        try {
            values = ResourceUtils.getResourceAsString(bootstrapValuesLocation);
        } catch (IOException e) {
            log.error("Unable to load the default values for event types, new create table calls will fail! Data location: {}", bootstrapValuesLocation, e);
        } 
        bootstrapValues = values;
    }

    protected EventTypesDatabaseService(final DataSource dataSource, final ObjectMapper objectMapper,
            final ComponentsDatabaseService componentsService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
        this.componentsService = componentsService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ComponentsDatabaseService.class);
    }

    @Override
    public String insert(String company, EventType eventType) throws SQLException {
        String data;
        try {
            data = objectMapper.writeValueAsString(eventType.getData());
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to insert record due to errors processing the field 'data' in the eventType being inserted.",e);
        }
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("type", eventType.toString().toUpperCase())
                .addValue("description", eventType.getDescription())
                .addValue("component_id", eventType.getComponent().getId())
                .addValue("data", data);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int count = template.update(
                "INSERT INTO " + company + ".event_types(type, description, component_id, data) "
                + "VALUES(:type, :description, :component_id, :data::jsonb)",
                params, 
                keyHolder, 
                new String[] { "id" });
        return count == 0 ? null : keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, EventType eventType) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<EventType> get(String company, String eventType) throws SQLException {
        DbListResponse<EventType> response = list(company, 0, 1, QueryFilter.builder().strictMatch("type", eventType).build(), null);
        if (response.getTotalCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(response.getRecords().get(0));
    }

    @Override
    public DbListResponse<EventType> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return list(company, pageNumber, pageSize, null, null);
    }

    public DbListResponse<EventType> list(
            final String company, 
            final Integer pageNumber, 
            final Integer pageSize,
            final QueryFilter filters, 
            final Map<String, SortingOrder> sorting) throws SQLException {
        String conditions = "";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (filters != null) {
            List<String> filterConditions = new ArrayList<>();
            if (filters.getStrictMatches() != null && !filters.getStrictMatches().isEmpty()) {
                for (Map.Entry<String, Object> filter : filters.getStrictMatches().entrySet()) {
                    filterConditions.add(String.format("%s = :%s", filter.getKey(), filter.getKey()));
                    if (filter.getKey().equalsIgnoreCase("type")) {
                        params.addValue(filter.getKey(), ((String) filter.getValue()).toUpperCase());
                    } else {
                        params.addValue(filter.getKey(), filter.getValue() instanceof String ? ((String) filter.getValue()).toLowerCase() : filter.getValue());
                    }
                }
            }
            if (filters.getPartialMatches() != null && !filters.getPartialMatches().isEmpty()) {
                for (Map.Entry<String, Object> filter : filters.getPartialMatches().entrySet()) {
                    if (filter.getValue() instanceof List) {
                        filterConditions.add(String.format("%s = ANY(:%s)", filter.getKey(), filter.getKey()));
                    } else {
                        filterConditions.add(String.format("%s = :%s", filter.getKey(), filter.getKey()));
                    }
                    params.addValue(filter.getKey(), filter.getValue() instanceof String ? ((String) filter.getValue()).toLowerCase() : filter.getValue());
                }
            }
            if (filterConditions.size() > 0) {
                conditions = "WHERE " + String.join(" AND ", filterConditions) + " ";
            }
        }
        String baseQuery = String.format("FROM %s.event_types %s", company, conditions);
        String sortBy = getSortBy(sorting);
        int limit = MoreObjects.firstNonNull(pageSize, 50);
        int skip = MoreObjects.firstNonNull(pageNumber, 0) * limit;
        String offSet = "LIMIT " + limit + " OFFSET " + skip;
        String listQuery = "SELECT * " + baseQuery + sortBy + offSet;
        List<EventType> records = template.query(listQuery, params, (rs, row) -> buildEventTypeFromDBRow(company, rs));
        if (records.size() == 0) {
            return DbListResponse.of(records, 0);
        }
        int totalCount = template.queryForObject("SELECT COUNT(*) AS count " + baseQuery, params, Integer.class);
        return DbListResponse.of(records, totalCount);
    }

    private EventType buildEventTypeFromDBRow(String company, ResultSet rs) throws SQLException {
        Map<String, KvField> data;
        try {
            data = objectMapper.readValue(rs.getString("data"), objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, KvField.class));
        } catch (Exception e) {
            data = Collections.emptyMap();
            log.error("Unable to parse the fields data for the event type '{}'", rs.getString("type"), e);
        }
        return EventType.builder()
                .description(rs.getString("description"))
                .type(rs.getString("type"))
                .component(componentsService.getById(company, (UUID) rs.getObject("component_id")).orElse(Component.builder().type(ComponentType.UNKNOWN).build()))
                .data(data)
                .build();
    }

    private String getSortBy(Map<String, SortingOrder> sorting) {
        return String.format(" ORDER BY %s ", (sorting == null || sorting.size() < 1 
                ? "type DESC"
                : String.join(", ", sorting.keySet().stream().map(key -> String.format("%s %s", key, sorting.get(key))).collect(Collectors.toList()))));
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.event_types ("
                    + "id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),"
                    + "type             VARCHAR(60) NOT NULL,"
                    + "component_id     UUID NOT NULL REFERENCES {0}.components(id) ON DELETE CASCADE,"
                    + "description      TEXT NOT NULL," 
                    + "data             JSONB NOT NULL,"
                    + "unique           (type, component_id)" 
            + ");",
            "CREATE UNIQUE INDEX IF NOT EXISTS event_types_type_component_idx ON {0}.event_types (lower(type), component_id)",
            "INSERT INTO {0}.event_types(type, component_id, description, data) VALUES " + bootstrapValues + " "
            + "ON CONFLICT(type, component_id) DO UPDATE SET data = EXCLUDED.data, description = EXCLUDED.description;");
        ddl.stream().map(item -> MessageFormat.format(item, company)).forEach(template.getJdbcTemplate()::execute);
        return null;
    }
    
}