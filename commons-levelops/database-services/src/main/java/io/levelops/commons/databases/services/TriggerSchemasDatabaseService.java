package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
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
public class TriggerSchemasDatabaseService extends DatabaseService<TriggerSchema> {

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    private final static String bootstrapValues;
    private final static String bootstrapValuesLocation = "db/default_data/trigger_schemas.value";

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
    
    protected TriggerSchemasDatabaseService(final DataSource dataSource, final ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Collections.emptySet();
    }

    @Override
    public String insert(String company, TriggerSchema triggerSchema) throws SQLException {
        String examples = "{}";
        try {
            if(triggerSchema.getExamples() != null && !triggerSchema.getExamples().isEmpty()){
                examples = objectMapper.writeValueAsString(triggerSchema.getExamples());
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to insert record due to errors processing the field 'examples' in the triggerSchema being inserted.", e);
        }
        String fields = "{}";
        try {
            if(triggerSchema.getFields() != null && !triggerSchema.getFields().isEmpty()){
                fields = objectMapper.writeValueAsString(triggerSchema.getFields());
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to insert record due to errors processing the field 'fields' in the triggerSchema being inserted.", e);
        }
        SqlParameterSource params = new MapSqlParameterSource().addValue("trigger_type", triggerSchema.getTriggerType().toString().toLowerCase())
                                    .addValue("description", triggerSchema.getDescription())
                                    .addValue("fields", fields)
                                    .addValue("examples", examples);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int count = template.update("INSERT INTO " + company + ".trigger_schemas(trigger_type, description, fields, examples) " 
                                        + "VALUES(:trigger_type, :description, :fields::jsonb, :examples::jsonb)", 
                                    params, 
                                    keyHolder, 
                                    new String[]{"id"});
        return count == 0 ? null: keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, TriggerSchema t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TriggerSchema> get(String company, String triggerType) throws SQLException {
        DbListResponse<TriggerSchema> response = list(company, 0, 1, QueryFilter.builder().strictMatch("trigger_type", triggerType).build(), null);
        if(response.getTotalCount() == 0){
            return Optional.empty();
        }
        return Optional.of(response.getRecords().get(0));
    }

    public Optional<TriggerSchema> getById(String company, UUID id) throws SQLException {
        DbListResponse<TriggerSchema> response = list(company, 0, 1, QueryFilter.builder().strictMatch("id", id).build(), null);
        if(response.getTotalCount() == 0){
            return Optional.empty();
        }
        return Optional.of(response.getRecords().get(0));
    }

    public List<TriggerType> getTriggerTypes(String company) throws SQLException {
        return template.query("SELECT DISTINCT(trigger_type) AS trigger_type FROM " + company + ".trigger_schemas", 
                new MapSqlParameterSource(), 
                (rs, row) -> TriggerType.fromString(rs.getString("trigger_type"))
                );
    }

    @Override
    public DbListResponse<TriggerSchema> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, pageNumber, pageSize, null, null);
    }

    public DbListResponse<TriggerSchema> list(
        final String company, 
        final Integer pageNumber, 
        final Integer pageSize, 
        final QueryFilter filters, 
        final Map<String, SortingOrder> sorting)
    throws SQLException {
        String conditions = "";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if(filters != null){
            List<String> filterConditions = new ArrayList<>();
            if(filters.getStrictMatches() != null && !filters.getStrictMatches().isEmpty()){
                for(Map.Entry<String, Object> filter:filters.getStrictMatches().entrySet()) {
                    filterConditions.add(String.format("%s = :%s", filter.getKey(), filter.getKey()));
                    params.addValue(filter.getKey(), filter.getValue());
                }
            }
            if(filters.getPartialMatches() != null && !filters.getPartialMatches().isEmpty()){
                for(Map.Entry<String, Object> filter:filters.getPartialMatches().entrySet()) {
                    if(filter.getValue() instanceof List){
                        filterConditions.add(String.format("%s = ANY(:%s)", filter.getKey(), filter.getKey()));
                    }
                    else{
                        filterConditions.add(String.format("%s = :%s", filter.getKey(), filter.getKey()));
                    }
                    params.addValue(filter.getKey(), filter.getValue() instanceof String? ((String)filter.getValue()).toLowerCase() : filter.getValue());
                }
            }
            if(filterConditions.size() > 0){
                conditions = "WHERE " + String.join(" AND ", filterConditions) + " "; 
            }
        }
        String baseQuery = String.format("FROM %s.trigger_schemas %s", company, conditions);
        String sortBy = getSortBy(sorting);
        int limit = MoreObjects.firstNonNull(pageSize, 50);
        int skip = MoreObjects.firstNonNull(pageNumber, 0) * limit;
        String offSet = "LIMIT " + limit + " OFFSET " + skip;
        String listQuery = "SELECT * " + baseQuery + sortBy + offSet;
        List<TriggerSchema> records = template.query(listQuery, params, (rs, row) -> buildTriggerSchemaFromDBRow(company, rs));
        if(records.size() == 0){
            return DbListResponse.of(records, 0);
        }
        int totalCount = template.queryForObject("SELECT COUNT(*) AS count " + baseQuery, params, Integer.class);
        return DbListResponse.of(records, totalCount);
    }

    private TriggerSchema buildTriggerSchemaFromDBRow(String company, ResultSet rs) throws SQLException {
        Map<String, KvField> fields;
        try{
            fields = objectMapper.readValue(rs.getString("fields"), objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, KvField.class));
        }
        catch(Exception e){
            fields = Collections.emptyMap();
            log.error("Unable to parse the fields 'fields' for the trigger schema of type '{}'", rs.getString("trigger_type"), e);
        }
        Map<String, Object> examples;
        try{
            examples = objectMapper.readValue(rs.getString("fields"), objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, Object.class));
        }
        catch(Exception e){
            examples = Collections.emptyMap();
            log.error("Unable to parse the field 'examples' for the trigger schema of type '{}'", rs.getString("trigger_type"), e);
        }
        return TriggerSchema.builder()
                        .id((UUID)rs.getObject("id"))
                        .triggerType(TriggerType.fromString(rs.getString("trigger_type")))
                        .description(rs.getString("description"))
                        .fields(fields)
                        .examples(examples)
                        .build();
    }

    private String getSortBy(Map<String, SortingOrder> sorting) {
        return String.format(" ORDER BY %s ", ( sorting == null || sorting.size() < 1
                        ? "trigger_type DESC"
                        : String.join(", ", sorting.keySet().stream().map(key -> String.format("%s %s", key, sorting.get(key))).collect(Collectors.toList()))));
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.trigger_schemas ("
                + "id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),"
                + "trigger_type     VARCHAR(60) NOT NULL UNIQUE,"
                + "description      text NOT NULL,"
                + "fields           JSONB NOT NULL,"
                + "examples         JSONB"
            + ");",
            "INSERT INTO {0}.trigger_schemas(trigger_type, description, fields) VALUES {1} "
            + "ON CONFLICT(trigger_type) DO UPDATE SET fields = EXCLUDED.fields, description = EXCLUDED.description;");
        ddl.stream().map(item -> MessageFormat.format(item, company, bootstrapValues)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}