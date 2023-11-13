package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ProductsDatabaseService extends DatabaseService<DBOrgProduct> {
    public final static String ORG_PRODUCTS_TABLE_NAME = "org_products";
    public final static String ORG_PRODUCTS_INTEGRATIONS_TABLE_NAME = "org_product_integrations";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of(
        "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // product
        "    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    name           VARCHAR(150) UNIQUE NOT NULL," +
        "    description    VARCHAR(150) NOT NULL," +
        "    created_at     BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
        "    updated_at     BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())" +
        ");",

        // "CREATE UNIQUE INDEX IF NOT EXISTS {1}_email_idx ON {0}.{1}(lower(email))",
        "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
        "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

        "CREATE TABLE IF NOT EXISTS {0}.{2} (" + // product integrations
        "    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    filters            JSONB NOT NULL," +
        "    org_product_id     UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
        "    integration_id     INTEGER REFERENCES {0}.integrations(id) ON DELETE CASCADE," +
        "    created_at         BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
        "    updated_at         BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())" +
        ");",

        "CREATE INDEX IF NOT EXISTS {2}_created_at_idx ON {0}.{1}(created_at)",
        "CREATE INDEX IF NOT EXISTS {2}_updated_at_idx ON {0}.{1}(updated_at)");

    //#region queries

    private final static String BASE_FROM = 
        " FROM  " +
        "    {0}.{1} p" +
        "    {3}" +
        " {4} ";

    private final static String BASE_SELECT =
        "SELECT " +
        "    p.id," +
        "    p.name," +
        "    p.description," +
        "    to_json(ARRAY(SELECT row_to_json(u) FROM (SELECT pi.integration_id, i.application, i.name, pi.filters FROM {0}.{2} pi, {0}.integrations i WHERE p.id = pi.org_product_id AND i.id = pi.integration_id) AS u)) integrations" +
        BASE_FROM +
        " {5}";

    private final static String FILTERS_SELECT =
        "SELECT " +
        "    p.id," +
        "    to_json(ARRAY(SELECT row_to_json(u) FROM (SELECT pi.integration_id, pi.filters FROM {0}.{1} p, {0}.{2} pi WHERE p.id = pi.org_product_id) AS u)) integrations" +
        BASE_FROM +
        " {5}";

    private final static String INSERT_PRODUCT_INTEGRATIONS_SQL_FORMAT = 
        "INSERT INTO {0}.{1}(filters, integration_id, org_product_id, updated_at) "
        + "VALUES(:filters::jsonb, :integrationId, :orgProductId, EXTRACT(epoch FROM now()))";
    private final static String INSERT_PRODUCT_SQL_FORMAT = 
        "INSERT INTO {0}.{1}(name, description, updated_at) " 
        + "VALUES(:name, :description, EXTRACT(epoch FROM now())) " 
        + "ON CONFLICT (name) DO UPDATE SET (description, updated_at) = (EXCLUDED.description, EXTRACT(epoch FROM now()))";
    
    private final static String DELETE_PRODUCTS_SQL_FORMAT = 
        "DELETE FROM {0}.{1} as c WHERE {2}";

    private static final Set<String> allowedFilters = Set.of("product_id", "name", "updated_at", "created_at", "integration_id", "integration_type");
    private static final int DEFAULT_PAGE_SIZE = 10;

    //#endregion

    public ProductsDatabaseService(final DataSource dataSource, final ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DBOrgProduct item) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();

        int count = this.template.update(
            MessageFormat.format(INSERT_PRODUCT_SQL_FORMAT, company, ORG_PRODUCTS_TABLE_NAME),
            new MapSqlParameterSource()
                .addValue("name", item.getName())
                .addValue("description", item.getDescription()),
            keyHolder,
            new String[]{"id"}
        );
        var id = count == 0 ? null : (UUID) keyHolder.getKeys().get("id");
        // insert/update usernames
        insertIntegrations(company, item, id);
        return id.toString();
    }

    public void insertIntegrations(String company, DBOrgProduct item, final UUID orgProductId) throws SQLException {
        MapSqlParameterSource[] params = item.getIntegrations().stream()
            .map(integ -> {
                try {
                    return new MapSqlParameterSource()
                        .addValue("filters", mapper.writeValueAsString(integ.getFilters()))
                        .addValue("integrationId", integ.getIntegrationId())
                        .addValue("orgProductId", orgProductId);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .filter(m -> m != null)
            .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);

        this.template.batchUpdate(MessageFormat.format(INSERT_PRODUCT_INTEGRATIONS_SQL_FORMAT, company, ORG_PRODUCTS_INTEGRATIONS_TABLE_NAME), params);
    }

    @Override
    public Boolean update(String company, DBOrgProduct t) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<DBOrgProduct> get(String company, String prodcutId) throws SQLException {
        return get(company, UUID.fromString(prodcutId));
    }

    public Optional<DBOrgProduct> get(String company, UUID productId) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("product_id", productId).build(), 0, 1).getRecords();
        if(results.size() > 0){
            return Optional.of(results.get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<DBOrgProduct> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<DBOrgProduct> filter(
        final String company,
        final QueryFilter filters,
        final Integer pageNumber,
        Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        Set<String> extraTables = new HashSet<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";
        if (extraConditions.contains("integ.")) {
            extraTables.add("{0}.integrations integ");
        }
        if (extraConditions.contains("pi.")) {
            extraTables.add("{0}." + ORG_PRODUCTS_INTEGRATIONS_TABLE_NAME + " pi");
        }
        var extraFrom = extraTables.size() > 0 ? ", " + MessageFormat.format(String.join(",", extraTables), company) : "";
        var groupBy = extraTables.size() > 0 ? " GROUP BY p.id" : "";

        var sql = MessageFormat.format(BASE_SELECT, company, ORG_PRODUCTS_TABLE_NAME, ORG_PRODUCTS_INTEGRATIONS_TABLE_NAME, extraFrom, extraConditions, groupBy);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
        List<DBOrgProduct> items = template.query(sql + limit, params, (rs, row) -> {
            List<Map<String, Object>> integrationsTmp = ParsingUtils.parseJsonList(mapper, "integrations", rs.getString("integrations"));
            return DBOrgProduct.builder()
                .id((UUID)rs.getObject("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .integrations(integrationsTmp.stream()
                    .map(item -> DBOrgProduct.Integ.builder()
                        .integrationId(Integer.valueOf(item.get("integration_id").toString()))
                        .name(item.get("name").toString())
                        .type(item.get("application").toString())
                        .filters((Map<String, Object>) item.get("filters"))
                        .build())
                    .collect(Collectors.toSet()) )
                .build();
        });
        var total = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") as l", params, Integer.class);
        return DbListResponse.of(items, total);
    }

    private void populateConditions(
        final String company,
        final QueryFilter filters,
        final @NonNull List<String> conditions,
        final @NonNull MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!allowedFilters.contains(item)
                || filters.getStrictMatches().get(item) == null) {
                continue;
            }
            if ("integration_type".equalsIgnoreCase(item)){
                processCondition("integ", "application", values, conditions, params);
                conditions.add("integ.id = pi.integration_id");
                conditions.add("p.id = pi.org_product_id");
                continue;
            }
            if ("integration_id".equalsIgnoreCase(item)){
                processCondition("pi", "integration_id", values, conditions, params);
                conditions.add("p.id = pi.org_product_id");
                continue;
            }
            if ("product_id".equalsIgnoreCase(item)){
                processCondition("p", "id", values, conditions, params);
                continue;
            }
            var prefix = "ingested_at".equalsIgnoreCase(item) || "model".equalsIgnoreCase(item) ? "r" : "i";
            processCondition(prefix, item, values, conditions, params);
        }
    }

    @SuppressWarnings("unchecked")
    private void processCondition(
        final String prefix,
        final String item,
        final Object values,
        final @NonNull List<String> conditions,
        final @NonNull MapSqlParameterSource params) {
        if (item.equalsIgnoreCase("ingested_at")) {
            Map<String, String> range = (Map<String, String>) values;
            String gt = range.get("$gt");
            if (gt != null) {
                conditions.add(MessageFormat.format("{0}.{1} > :ingested_at_gt", prefix, item));
                params.addValue("ingested_at_gt", NumberUtils.toInt(gt));
            }
            String lt = range.get("$lt");
            if (lt != null) {
                conditions.add(MessageFormat.format("{0}.{1} < :ingested_at_lt", prefix, item));
                params.addValue("ingested_at_lt", NumberUtils.toInt(lt));
            }
            return;
        }
        if (values instanceof Collection) {
            var collection = ((Collection<Object>) values)
                        .stream()
                        .filter(ObjectUtils::isNotEmpty)
                        .map(Object::toString)
                        .map(s -> s.toLowerCase())
                        .collect(Collectors.toSet());
            var tmp = MessageFormat.format("{0}.{1} = ANY({2})", prefix, item, "'{" + String.join(",", collection) + "}'");
            log.debug("filter: {}", tmp);
            conditions.add(tmp);
            return;
        }
        if (values instanceof UUID) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::uuid", prefix, item));
        }
        else if (values instanceof Integer) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
        }
        else if (values instanceof Long) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::bigint", prefix, item));
        }
        else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return delete(company, Set.of(UUID.fromString(id)));
    }

    public Boolean delete(String company, Set<UUID> ids) throws SQLException {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        processCondition("c", "id", ids, conditions, params);
        var sql = MessageFormat.format(DELETE_PRODUCTS_SQL_FORMAT, company, ORG_PRODUCTS_TABLE_NAME, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if(ids.size() != updates){
            log.warn("Request to delete {} records ended up deleting {} records (by ids)", ids.size(), updates);
        }
        return true;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item -> template.getJdbcTemplate()
            .execute(MessageFormat.format(
                item,
                company,
                ORG_PRODUCTS_TABLE_NAME,
                ORG_PRODUCTS_INTEGRATIONS_TABLE_NAME)));
        return true;
    }
}
