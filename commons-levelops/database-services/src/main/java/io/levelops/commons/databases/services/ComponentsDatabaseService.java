package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ComponentsDatabaseService extends DatabaseService<Component> {

    private final NamedParameterJdbcTemplate template;

    protected ComponentsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Collections.emptySet();
    }

    @Override
    public String insert(String company, Component component) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int count = template.update("INSERT INTO " + company + ".components(type, name) VALUES(:type, :name)", 
                                new MapSqlParameterSource()
                                    .addValue("type", component.getType().toString())
                                    .addValue("name", component.getName().toLowerCase()), 
                                keyHolder,
                                new String[]{"id"});
        return count == 0 ? null : keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, Component t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a component by id (string).
     */
    @Override
    public Optional<Component> get(String company, String id) throws SQLException {
        return getById(company, UUID.fromString(id));
    }

    public Optional<Component> getByTypeName(final String company, final ComponentType type, final String name) throws SQLException {
        DbListResponse<Component> response = list(company, 0, 1, QueryFilter.builder()
            .strictMatch("type", type.toString())
            .strictMatch("name", name.trim().toLowerCase())
            .build(), null);
        if(response.getTotalCount() == 0){
            return Optional.empty();
        }
        return Optional.of(response.getRecords().get(0));
    }

    public Optional<Component> getById(String company, UUID id) throws SQLException {
        DbListResponse<Component> response = list(company, 0, 1, QueryFilter.builder().strictMatch("id", id).build(), null);
        if(response.getTotalCount() == 0){
            return Optional.empty();
        }
        return Optional.of(response.getRecords().get(0));
    }

    public List<ComponentType> getComponentTypes(String company) throws SQLException {
        return template.query("SELECT DISTINCT(type) AS type FROM " + company + ".components", 
                new MapSqlParameterSource(), 
                (rs, row) -> ComponentType.fromString(rs.getString("type"))
        );
    }

    @Override
    public DbListResponse<Component> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return list(company, pageNumber, pageSize, null, null);
    }

    public DbListResponse<Component> list(
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
                    params.addValue(filter.getKey(), filter.getValue() instanceof String? ((String)filter.getValue()).trim().toLowerCase(): filter.getValue());
                }
            }
            if(filterConditions.size() > 0){
                conditions = "WHERE " + String.join(" AND ", filterConditions) + " "; 
            }
        }
        String baseQuery = String.format("FROM %s.components %s", company, conditions);
        String sortBy = getSortBy(sorting);
        int limit = MoreObjects.firstNonNull(pageSize, 50);
        int skip = MoreObjects.firstNonNull(pageNumber, 0) * limit;
        String offSet = "LIMIT " + limit + " OFFSET " + skip;
        String listQuery = "SELECT * " + baseQuery + sortBy + offSet;
        List<Component> records = template.query(listQuery, params, (rs, row) -> buildComponentFromDBRow(rs));
        if(records.size() == 0){
            return DbListResponse.of(records, 0);
        }
        int totalCount = template.queryForObject("SELECT COUNT(*) AS count " + baseQuery, params, Integer.class);
        return DbListResponse.of(records, totalCount);
    }

    private Component buildComponentFromDBRow(ResultSet rs) throws SQLException {
        return Component.builder()
                .id((UUID) rs.getObject("id"))
                .type(ComponentType.fromString(rs.getString("type")))
                .name(rs.getString("name"))    
                .build();
    }
    
    private String getSortBy(Map<String, SortingOrder> sorting) {
        return String.format(" ORDER BY %s ", ( sorting == null || sorting.size() < 1
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
            "CREATE TABLE IF NOT EXISTS {0}.components ("
                + "id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),"
                + "type             VARCHAR(60) NOT NULL,"
                + "name             VARCHAR(120) NOT NULL,"
                + "sub_components   UUID[],"
                + "unique           (type, name)"
            + ");",
            "CREATE UNIQUE INDEX IF NOT EXISTS components_type_name_idx ON {0}.components (type, lower(name));",
            "INSERT INTO {0}.components(type, name) VALUES"
                    + "(''integration'', ''jira''),"
                    + "(''integration'', ''snyk''),"
                    + "(''integration'', ''github''),"
                    + "(''integration'', ''pagerduty''),"
                    + "(''plugin_result'', ''Praetorian Report''),"
                    + "(''plugin_result'', ''NCC Group Report''),"
                    + "(''plugin_result'', ''Microsoft Threat Modeling Tool Report''),"
                    + "(''smart_ticket'', ''smart ticket''),"
                    + "(''assessment'', ''assessment''),"
                    + "(''plugin_result'', ''Jenkins Plugin''), "
                    + "(''plugin_result'', ''CSV''), "
                    + "(''triage_rules_matched'', ''jenkins''), "
                    + "(''integration'', ''sonarqube''), "
                    + "(''custom'', ''trigger'') ,"
                    + "(''integration'', ''coverity''),"
                    + "(''integration'', ''cxsast'') "
                    + "ON CONFLICT(type, lower(name)) DO NOTHING;");
        ddl.stream().map(item -> MessageFormat.format(item, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}