package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.PagerDutyFilterParserCommons;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@org.springframework.stereotype.Service
public class ServicesDatabaseService extends DatabaseService<Service> {

    public static final String TABLE_NAME = "services";
    private static final List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (\n"
        + "   id                 uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n"
        + "   name               text,\n"
        + "   type               varchar(20),\n"
        + "   created_at         bigint NOT NULL DEFAULT extract(epoch from now()),\n"
        + "   updated_at         bigint NOT NULL DEFAULT extract(epoch from now()),\n"
        + "   CONSTRAINT {1}_unique_service UNIQUE(name,type)"
        + ");",
            
        "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_name_idx ON {0}.{1}(name)",
        
        "CREATE INDEX IF NOT EXISTS {1}_type_idx ON {0}.{1}(type)");


    private static final String INSERT_SQL_FORMAT = "INSERT INTO {0}.{1}(id, name, type, created_at, updated_at) "
                                            + "VALUES(:id, :name, :type, :createdAt, :updatedAt) "
                                            + "ON CONFLICT(name, type) DO UPDATE SET updated_at = EXCLUDED.updated_at";
    
    private static final Set<String> allowedFilters = Set.of("id", "name", "type", "created_at", "updated_at");

    private static final int DEFAULT_PAGE_SIZE = 10;
    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;
    private PagerDutyFilterParserCommons filterParserCommons;
    private ProductsDatabaseService productsDatabaseService;

    @Autowired
    public ServicesDatabaseService(final ObjectMapper mapper, DataSource dataSource) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        this.filterParserCommons = new PagerDutyFilterParserCommons(productsDatabaseService);
    }

    @Override
    public String insert(String company, Service service) throws SQLException {
        UUID id = service.getId() != null ? service.getId() : UUID.randomUUID();

        var keyHolder = new GeneratedKeyHolder();
        this.template.update(
            MessageFormat.format(INSERT_SQL_FORMAT, company,TABLE_NAME),
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", service.getName())
                .addValue("type", service.getType().toString())
                .addValue("createdAt", service.getCreatedAt().getEpochSecond())
                .addValue("updatedAt", (service.getUpdatedAt() != null ? service.getUpdatedAt() : Instant.now()).getEpochSecond()),
            keyHolder,
            new String[]{"id"}
        );
        return keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, Service t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Service> get(String company, String id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("id", id).build(), 0, 1, null);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<Service> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, pageNumber, pageSize, null);
    }

    public DbListResponse<Service> list(String company, QueryFilter filters, Integer pageNumber, Integer pageSize, Set<UUID> orgProductUuids)
            throws SQLException {
        var params = new MapSqlParameterSource();
        String queryStmt = getQueryStmt(company, filters, params, orgProductUuids);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        var records = template.query(MessageFormat.format(queryStmt + limit, company, TABLE_NAME), params, (rs, row) -> Service.builder()
                .id((UUID) rs.getObject("id"))
                .type(Service.Type.fromString(rs.getString("type")))
                .name(rs.getString("name"))
                .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")))
                .updatedAt(Instant.ofEpochSecond(rs.getLong("updated_at")))
                .build());
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) from ( " + queryStmt + ") as count", company, TABLE_NAME), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    private String getQueryStmt(String company, QueryFilter filters, MapSqlParameterSource params,
                                Set<UUID> orgProductUuids)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        Map<Integer, Map<String, Object>> productFilters = null;
        if (orgProductUuids != null) {
            try {
                productFilters = filterParserCommons.getProductFilters(company, orgProductUuids);
            } catch (SQLException e) {
                log.error("Error while getting products...{0}" + e.getMessage(), e);
                throw e;
            }
        }
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            populateConditions(filters, conditions, params);
            return getUnionSql(conditions);
        }
        Map<Integer, Map<String, Object>> finalProductFilters = productFilters;
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            QueryFilter queryFilter = QueryFilter.fromRequestFilters(finalProductFilters.get(integ));
            populateConditions(queryFilter, conditions, params);
            return getUnionSql(conditions);
        }).collect(Collectors.toList());
        return String.join(" UNION ", queryList);
    }

    private String getUnionSql(List<String> conditions) {
        String where = "";
        if (conditions.size() > 0) {
            where = "WHERE " + String.join(" AND ", conditions) + " ";
        }
        String baseStatement = "FROM {0}.{1} c " + where;
        return "SELECT * " + baseStatement;
    }

    private void populateConditions(QueryFilter filters, @NonNull List<String> conditions, @NonNull MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
            if (!allowedFilters.contains(entry.getKey()) || filters.getStrictMatches().get(entry.getKey()) == null) {
                continue;
            }
            if (entry.getValue() instanceof Collection) {
                var collection = ((Collection<String>) entry.getValue())
                            .stream()
                            .filter(Strings::isNotBlank)
                            .map(s -> s.toUpperCase())
                            .collect(Collectors.toSet());
                var tmp = MessageFormat.format("c.{0} = ANY({1})", entry.getKey(), "'''{" + String.join(",", collection) + "}'''");
                log.debug("filter: {}", tmp);
                conditions.add(tmp);
                continue;
            }
            if (entry.getValue() instanceof UUID) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::uuid", entry.getKey()));
            }
            else {
                conditions.add(MessageFormat.format("c.{0} = :{0}", entry.getKey()));
            }
            params.addValue(entry.getKey(), entry.getValue().toString());
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String delete = "DELETE FROM {0}.{1} WHERE id = :id::uuid";
        var count = template.update(MessageFormat.format(delete, company, TABLE_NAME), Map.of("id", id));
        return count > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.stream()
            .map(st -> MessageFormat.format(st, company, TABLE_NAME))
            .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}
