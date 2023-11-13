package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ProductService extends DatabaseService<Product> {

    private final NamedParameterJdbcTemplate template;
    private static final Pattern DUPLICATE_KEY_ERROR = Pattern.compile("ERROR: duplicate key value violates unique constraint \"uniq_products_key_idx\"");
    private static final Pattern DUPLICATE_NAME_ERROR = Pattern.compile("ERROR: duplicate key value violates unique constraint \"uniq_products_name_idx\"");
    private static final String DEFAULT_ORG_IDENTIFIER = "defaultOrg";

    private final List<String> sqlStatements = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.products(\n" +
                    "        id            INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                    "        name          VARCHAR NOT NULL, \n" +
                    "        key           VARCHAR NOT NULL, \n" +
                    "        orgIdentifier VARCHAR NOT NULL, \n" +
                    "        owner_id      INTEGER REFERENCES {0}.users(id) ON DELETE SET NULL," +
                    "        description   VARCHAR, \n" +
                    "        updatedat     BIGINT DEFAULT extract(epoch from now()),\n" +
                    "        createdat     BIGINT DEFAULT extract(epoch from now()),\n" +
                    "        bootstrapped  BOOLEAN NOT NULL DEFAULT false,\n" +
                    "        immutable     BOOLEAN NOT NULL DEFAULT false,\n" +
                    "        disabled      BOOLEAN NOT NULL DEFAULT false,\n" +
                    "        demo     BOOLEAN NOT NULL DEFAULT false\n" +
                    "    )",

            "CREATE UNIQUE INDEX IF NOT EXISTS uniq_products_name_orgidentifier_idx on {0}.products (name, orgIdentifier)",
            "CREATE UNIQUE INDEX IF NOT EXISTS uniq_products_key_orgidentifier_idx on {0}.products (key, orgIdentifier)",
            "CREATE INDEX IF NOT EXISTS products_updated_at_idx on {0}.products (updatedat)",
            "CREATE INDEX IF NOT EXISTS products_created_at_idx on {0}.products (createdat)",

            // PROP-469: New tenants created do not have the default categories... using api method instead of DB insert
            // "INSERT INTO {0}.products(name,key,description,bootstrapped,immutable) " +
            //         "SELECT ''Default Product'', ''DEFAULT'', ''Default Product'', true, true " +
            //         "WHERE NOT EXISTS (SELECT * FROM {0}.products WHERE bootstrapped = true AND immutable = true);",

            // "DO $$\n" +
            //         "DECLARE s RECORD;\n" +
            //         "BEGIN\n" +
            //         "FOR s IN EXECUTE ''SELECT CAST(id AS varchar) as pid from {0}.products''\n" +
            //         "    LOOP\n" +
            //         "        EXECUTE ''CREATE SEQUENCE IF NOT EXISTS {0}.sequence_product_id_'' || s.pid;\n" +
            //         "    END LOOP;\n" +
            //         "END;\n" +
            //         "$$;",

            "CREATE TABLE IF NOT EXISTS {0}.workspace_integrations (" +
                    "    workspace_id      INTEGER NOT NULL REFERENCES {0}.products(id) ON DELETE CASCADE," +
                    "    integration_id    SMALLINT NOT NULL REFERENCES {0}.integrations(id) ON DELETE CASCADE," +
                    "    created_at        TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC''),\n" +
                    "    CONSTRAINT worspace_integrations_pkey PRIMARY KEY (workspace_id,integration_id)" +
                    ");"
    );

    private final String INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT = "INSERT INTO {0}.workspace_integrations(workspace_id,integration_id) VALUES(:workspaceId, :integrationId)";

    @Autowired
    public ProductService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public static String generateSequenceName(final String company, final Integer productId) {
        return company + ".sequence_product_id_" + productId.toString();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(UserService.class);
    }

    @Override
    public String insert(String company, Product product) throws SQLException {
        var result = insertForId(company, product);
        return result.isEmpty() ? null : result.get().toString();
    }

    public Optional<Integer> insertForId(final String company, final Product product) {
        String SQL = "INSERT INTO {0}.products(name,description,owner_id,key,orgIdentifier,bootstrapped,immutable,demo)" +
                " VALUES(:name,:description,:ownerId,:key,:orgIdentifier,:bootstrapped,:immutable,:demo)";
        var keyHolder = new GeneratedKeyHolder();
        int count = -1;

        try {
            count = this.template.update(
                    MessageFormat.format(SQL, company),
                    new MapSqlParameterSource()
                            .addValue("name", product.getName().trim())
                            .addValue("description", StringUtils.isEmpty(product.getDescription()) ?
                                    "" : product.getDescription().trim())
                            .addValue("ownerId", (StringUtils.isNotEmpty(product.getOwnerId()) && NumberUtils.toInt(product.getOwnerId(), -1) != -1)
                                    ? NumberUtils.toInt(product.getOwnerId())
                                    : null)
                            .addValue("key", product.getKey().toUpperCase().trim())
                            .addValue("orgIdentifier", StringUtils.isNotBlank(product.getOrgIdentifier()) ? product.getOrgIdentifier() : DEFAULT_ORG_IDENTIFIER)
                            .addValue("bootstrapped", BooleanUtils.isTrue(product.getBootstrapped()))
                            .addValue("immutable", BooleanUtils.isTrue(product.getImmutable()))
                            .addValue("demo", BooleanUtils.isTrue(product.getDemo())),
                    keyHolder,
                    new String[]{"id"}
            );
        } catch (DataAccessException e) {
            String msg = e.getMessage();
            Matcher matcher = DUPLICATE_KEY_ERROR.matcher(msg);
            if (matcher.find()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Duplicate key exists");
            }
            Matcher nameMatcher = DUPLICATE_NAME_ERROR.matcher(msg);
            if (nameMatcher.find()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Duplicate name exists");
            }
            throw e;
        }
        if (count < 1) {
            return Optional.empty();
        }
        var id = (Integer) keyHolder.getKeys().get("id");
        this.template.getJdbcTemplate().execute("CREATE SEQUENCE IF NOT EXISTS " + generateSequenceName(company, id));
        if (CollectionUtils.isEmpty(product.getIntegrationIds())) {
            return Optional.of(id);
        }
        // Insert integration mappings
        var values = new ArrayList<Map<String, Object>>();
        product.getIntegrationIds().forEach(integrationId -> {
            values.add(Map.of(
                    "workspaceId", id,
                    "integrationId", integrationId)
            );
        });
        int[] count2 = this.template.batchUpdate(
                MessageFormat.format(INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT, company),
                values.toArray(new Map[0])
        );
        if (Arrays.stream(count2).anyMatch(r -> r < 1)) {
            log.warn("[{}] Unable to insert all workspace - integrations.. {} -> {}", company, id, product.getIntegrationIds());
        }
        return Optional.of(id);
    }

    @Override
    public Boolean update(String company, Product product) throws SQLException {
        String SQL = "UPDATE " + company + ".products SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        //empty string is NOT a valid name so we skip it
        if (StringUtils.isNotEmpty(product.getName())) {
            updates = "name = ?";
            values.add(product.getName().trim());
        }
        //empty string is valid description
        if (product.getDescription() != null) {
            updates = StringUtils.isEmpty(updates) ? "description = ?" : updates + ", description = ?";
            values.add(product.getDescription());
        }
        if (StringUtils.isNotBlank(product.getOwnerId())) {
            updates = StringUtils.isEmpty(updates) ? "owner_id = ?" : updates + ", owner_id = ?";
            values.add(NumberUtils.toInt(product.getOwnerId()));
        }
        if (StringUtils.isNotBlank(product.getOrgIdentifier())) {
            updates = StringUtils.isEmpty(updates) ? "orgIdentifier = ?" : updates + ", orgIdentifier = ?";
            values.add(product.getOrgIdentifier());
        }
        if (StringUtils.isNotBlank(product.getKey())) {
            updates = StringUtils.isEmpty(updates) ? "key = ?" : updates + ", key = ?";
            values.add(product.getKey().toUpperCase().trim());
        }
        if (product.getBootstrapped() != null) {
            updates = StringUtils.isEmpty(updates) ? "bootstrapped = ?" : updates + ", bootstrapped = ?";
            values.add(product.getBootstrapped());
        }
        if (product.getImmutable() != null) {
            updates = StringUtils.isEmpty(updates) ? "immutable = ?" : updates + ", immutable = ?";
            values.add(product.getImmutable());
        }
        if (product.getDisabled() != null) {
            updates = StringUtils.isEmpty(updates) ? "disabled = ?" : updates + ", disabled = ?";
            values.add(product.getDisabled());
        }
        if (product.getDemo() != null) {
            updates = StringUtils.isEmpty(updates) ? "demo = ?" : updates + ", demo = ?";
            values.add(product.getDemo());
        }

        //nothing to update.
        if (values.size() == 0 && CollectionUtils.isEmpty(product.getIntegrationIds())) {
            return false;
        }

        if (values.size() > 0) {
            updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
            values.add(Instant.now().getEpochSecond());

            SQL = SQL + updates + condition;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(SQL,
                         Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 1; i <= values.size(); i++) {
                    pstmt.setObject(i, values.get(i - 1));
                }
                pstmt.setInt(values.size() + 1, Integer.parseInt(product.getId()));
                pstmt.executeUpdate();
            }
        }

        if (CollectionUtils.isNotEmpty(product.getIntegrationIds())) {
            var mapValues = new ArrayList<Map<String, Object>>();
            product.getIntegrationIds().forEach(integrationId -> {
                mapValues.add(Map.of(
                        "workspaceId", Integer.parseInt(product.getId()),
                        "integrationId", integrationId)
                );
            });
            // remove previous integrations
            var result = template.update(MessageFormat.format("DELETE FROM {0}.workspace_integrations WHERE workspace_id = :workspaceId", company), Map.of("workspaceId", Integer.parseInt(product.getId())));
            log.debug("[{}] deleted '{}' previous integrations.", company, result);
            // insert new integrations
            int[] count2 = this.template.batchUpdate(
                    MessageFormat.format(INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT + " ON CONFLICT(workspace_id, integration_id) DO NOTHING", company),
                    mapValues.toArray(new Map[0])
            );
            if (Arrays.stream(count2).anyMatch(r -> r < 1)) {
                log.warn("[{}] Unable to insert all workspace - integrations.. {} -> {}", company, product.getId(), product.getIntegrationIds());
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<Product> get(String company, String productId) {
        String SQL = "SELECT id,name,key,orgIdentifier,owner_id,description,updatedat,createdat,bootstrapped,immutable,disabled,demo,"
                + "(SELECT array_agg(integration_id::int) FROM {0}.workspace_integrations WHERE workspace_id = w.id) integration_ids,"
                + "(SELECT json_agg(row_to_json(l)) FROM (SELECT i.id::int, i.name, i.application FROM {0}.workspace_integrations wi, {0}.integrations i WHERE wi.workspace_id = w.id AND i.id = wi.integration_id) as l) integrations"
                + " FROM {0}.products w WHERE id = :productId ";
        try {
            return Optional.of(template.queryForObject(MessageFormat.format(SQL, company), Map.of("productId", Integer.parseInt(productId)), mapper()));
        } catch (EmptyResultDataAccessException e) {
            log.debug("", e);
            return Optional.empty();
        }
    }

    public Boolean deleteOuIntegration(final String company, final Integer workspaceId, List<Integer> integrationIds) throws SQLException {
        return template.update("DELETE FROM " + company + ".ou_content_sections WHERE ou_id IN (select ou.id from " + company + ".ous ou left join " + company + ".ou_categories ouc on ou.ou_category_id=ouc.id where workspace_id=:workspace_id) and integration_id IN(:integration_id)", Map.of("workspace_id", workspaceId, "integration_id", integrationIds)) > 0;
    }


    private RowMapper<Product> mapper() {
        return (rs, i) -> {
            var integrationsArray = ParsingUtils.parseSet(DefaultObjectMapper.get(), "integrations", Map.class, rs.getString("integrations"));
            var integrationIdsArray = rs.getArray("integration_ids");
            return Product.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .key(rs.getString("key"))
                    .orgIdentifier(rs.getString("orgIdentifier"))
                    .ownerId(rs.getString("owner_id"))
                    .updatedAt(rs.getLong("updatedat"))
                    .createdAt(rs.getLong("createdat"))
                    .integrationIds(integrationIdsArray == null ? Set.of() : Arrays.asList((Integer[]) integrationIdsArray.getArray()).stream()
                            .map(o -> (Integer) o).collect(Collectors.toSet()))
                    .integrations(integrationsArray == null ? Set.of() : integrationsArray.stream()
                            .map(o -> Integration.builder()
                                    .application((String) o.get("application"))
                                    .name((String) o.get("name"))
                                    .id(o.get("id").toString())
                                    .build()).collect(Collectors.toSet()))
                    .bootstrapped(rs.getBoolean("bootstrapped"))
                    .immutable(rs.getBoolean("immutable"))
                    .disabled(rs.getBoolean("disabled"))
                    .demo(rs.getBoolean("demo"))
                    .build();
        };
    }

    public List<Product> getSystemImmutableProducts(String company) throws SQLException {
        DbListResponse<Product> dbListResponse = listByFilter(company, null, null, true, true, 0, 1000);
        if (dbListResponse == null) {
            throw new SQLException("Could not fetch Bootstrapped and Immutable Products");
        }
        return dbListResponse.getRecords();
    }

    public DbListResponse<Product> listByFilter(String company, String name, Set<Integer> productIds,
                                                Boolean bootstrapped, Boolean immutable, Integer pageNumber,
                                                Integer pageSize) throws SQLException {
        return listByFilter(company, name, productIds, null, null, null, null, null, null, bootstrapped, immutable, null, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<Product> listByFilter(String company, String name, Set<Integer> productIds, Set<Integer> integrationIds, Set<String> integrationType,
                                                Set<String> category, Set<String> key, Set<String> orgIdentifier, Set<String> ownerId, Boolean bootstrapped, Boolean immutable, Long updatedAtStart,
                                                Long updatedAtEnd, Boolean disabled, Boolean demo, Integer pageNumber, Integer pageSize) throws SQLException {
        String criteria = " WHERE ";
        var values = new MapSqlParameterSource();
        var specialCriteria = false;
        if (StringUtils.isNotEmpty(name)) {
            criteria += "name ILIKE :name ";
            values.addValue("name", "%" + name + "%");
        }

        if (CollectionUtils.isNotEmpty(productIds)) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " w.id = ANY('{" + String.join(",", productIds.stream().map(o -> o.toString()).collect(Collectors.toSet())) + "}'::int[]) ";
            // values.addValue("productIds", productIds);
            specialCriteria = true;
        }
        if (CollectionUtils.isNotEmpty(key)) {
            key = key.stream().map(k -> k.toUpperCase()).collect(Collectors.toSet());
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " key in ( :key ) ";
            specialCriteria = true;
            values.addValue("key", key);
        }
        if (CollectionUtils.isNotEmpty(orgIdentifier)) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") +  " orgIdentifier in (:orgIdentifier) ";
            values.addValue("orgIdentifier", orgIdentifier);
        }
        if (CollectionUtils.isNotEmpty(ownerId)) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " owner_id = ANY('{" + String.join(",", ownerId) + "}'::int[]) ";
            specialCriteria = true;
            // values.addValue("ownerId", ownerId);
        }
        if (bootstrapped != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " bootstrapped = :bootstrapped ";
            values.addValue("bootstrapped", bootstrapped);
        }
        if (immutable != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " immutable = :immutable ";
            values.addValue("immutable", immutable);
        }
        if (updatedAtStart != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : "AND ") + "updatedat > :updatedAtStart ";
            values.addValue("updatedAtStart", updatedAtStart);
        }
        if (updatedAtEnd != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : "AND ") + "updatedat < :updatedAtEnd ";
            values.addValue("updatedAtEnd", updatedAtEnd);
        }
        if (demo != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : "AND ") + "demo = :demo ";
            values.addValue("demo", demo);
        }
        if (disabled != null) {
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : "AND ") + "disabled = :disabled ";
            values.addValue("disabled", disabled);
        } else {
            // By default we only get non-disabled rows
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : "AND ") + "disabled = false ";
            values.addValue("disabled", disabled);
        }

        // by associations
        var extraFrom = "";
        // workspace_integrations + integrations
        if (CollectionUtils.isNotEmpty(integrationType)) {
            extraFrom += ", {0}.workspace_integrations wi, {0}.integrations i ";
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " wi.workspace_id = w.id AND wi.integration_id = i.id AND i.application = ANY('{" + String.join(",", integrationType) + "}'::text[]) ";
            specialCriteria = true;
        }

        // workspace_integrations
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            extraFrom += ", {0}.workspace_integrations wi ";
            criteria += (values.getValues().size() == 0 && !specialCriteria ? "" : " AND") + " wi.integration_id = ANY('{" + String.join(",", integrationIds.stream().map(o -> o.toString()).collect(Collectors.toSet())) + "}'::int[]) ";
            specialCriteria = true;
            // values.addValue("productIds", productIds);
        }

        // ou_categories
        if (CollectionUtils.isNotEmpty(category)) {
            extraFrom += ", {0}.ou_categories oc ";
            criteria += (values.getValues().size() == 0 && !specialCriteria
                    ? ""
                    : CollectionUtils.isNotEmpty(productIds)
                    ? " OR"
                    : " AND")
                    + " (oc.workspace_id = w.id AND oc.name = ANY('{" + String.join(",", category) + "}'::int[]) )";
            // values.addValue("productIds", productIds);
            specialCriteria = true;
        }

        if (values.getValues().size() == 0 && !specialCriteria) {
            criteria = "";
        }
        String SQL = MessageFormat.format("SELECT "
                + "w.id,"
                + "w.name,"
                + "w.owner_id,"
                + "w.description,"
                + "w.updatedat,"
                + "w.createdat,"
                + "w.key,"
                + "w.orgIdentifier,"
                + "w.bootstrapped,"
                + "w.immutable,"
                + "w.disabled,"
                + "w.demo,"
                + "(SELECT array_agg(integration_id::int) FROM {0}.workspace_integrations WHERE workspace_id = w.id) integration_ids,"
                + "(SELECT json_agg(row_to_json(l)) FROM (SELECT i.id::int, i.name, i.application FROM {0}.workspace_integrations wi, {0}.integrations i WHERE wi.workspace_id = w.id AND i.id = wi.integration_id) as l) integrations"
                + " FROM {0}.products w " + extraFrom, company)
                + criteria + "ORDER BY updatedat DESC LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        List<Product> retval = template.query(SQL, values, mapper());
        // List<Product> retval = new ArrayList<>();
        Integer totCount = 0;
        if (retval.size() > 0) {
            totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (retval.size() == pageSize) {
                String countSQL = "SELECT COUNT(*) FROM ( SELECT id,name FROM " + company + ".products " + criteria + ") AS d";
                totCount = template.queryForObject(countSQL, values, Integer.class);
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    public DbListResponse<Integration> listIntegrationsByFilter(String company,
                                                                String name,
                                                                boolean strictMatch,
                                                                List<String> applications,
                                                                Boolean satellite,
                                                                List<Integer> tagIds,
                                                                Integer productId,
                                                                Integer pageNumber,
                                                                Integer pageSize) {
        int limit = MoreObjects.firstNonNull(pageSize, 25);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        HashMap<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        IntegrationService.createWhereCondition(name, strictMatch, applications, satellite, null, tagIds,
                null, null, null, params, conditions);
        String whereCondStr = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String intrSql = "SELECT i.*, t.tags FROM "
                + company + ".integrations as i"
                + " JOIN " + company + ".workspace_integrations as wi"
                + " ON i.id = wi.integration_id"
                + " JOIN ("
                + " SELECT id FROM " + company + ".products WHERE id = " + productId + ") as p"
                + " ON p.id = wi.workspace_id"
                + " LEFT OUTER JOIN ("
                + " SELECT array_remove(array_agg(ti.tagid), NULL)::text[] as tags,"
                + " ti.itemid::int FROM " + company + ".tagitems as ti"
                + " WHERE ti.itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'"
                + " GROUP BY ti.itemid ) as t ON t.itemid = i.id";
        String SQL = intrSql
                + whereCondStr
                + " ORDER BY i.updatedat desc "
                + " LIMIT " + limit
                + " OFFSET " + skip;
        List<Integration> integrations = template.query(SQL, params, IntegrationService.ROW_MAPPER);
        Integer count = 0;
        if (CollectionUtils.isNotEmpty(integrations)) {
            String countSQL = "SELECT COUNT(*)"
                    + " FROM ("
                    + intrSql
                    + whereCondStr
                    + " ) AS counting";
            count = template.queryForObject(countSQL, params, Integer.class);
        }
        return DbListResponse.of(integrations, count);
    }

    @Override
    public DbListResponse<Product> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".products WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, Integer.parseInt(id));
                if (pstmt.executeUpdate() > 0) {
                    String deleteSequenceSql = "DROP SEQUENCE IF EXISTS " + generateSequenceName(company, Integer.parseInt(id));
                    try (PreparedStatement deleteSequencePstmt = conn.prepareStatement(deleteSequenceSql)) {
                        deleteSequencePstmt.execute();
                    }
                    return true;
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return false;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        String SQL = "DELETE FROM " + company + ".products WHERE id IN (:ids)";
        int rowsDeleted = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            rowsDeleted = template.update(SQL, params);
            if (rowsDeleted > 0) {
                ids.forEach(id -> {
                    String deleteSequenceSql = "DROP SEQUENCE IF EXISTS " + generateSequenceName(company, Integer.parseInt(id));
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement deleteSequencePstmt = conn.prepareStatement(deleteSequenceSql)) {
                        deleteSequencePstmt.execute();
                    } catch (SQLException e) {
                        log.error("Unable to delete the product with id: " + id + ". " + e.getMessage());
                    }
                });
            }
        }
        return rowsDeleted;
    }

    public Optional<Product> getProduct(String company, String orgIdentifier, String key){

        String sql = "SELECT id,name,key,orgIdentifier,owner_id,description,updatedat,createdat,bootstrapped,immutable,disabled,demo,"
                + "(SELECT array_agg(integration_id::int) FROM {0}.workspace_integrations WHERE workspace_id = w.id) integration_ids,"
                + "(SELECT json_agg(row_to_json(l)) FROM (SELECT i.id::int, i.name, i.application FROM {0}.workspace_integrations wi, {0}.integrations i WHERE wi.workspace_id = w.id AND i.id = wi.integration_id) as l) integrations"
                + " FROM {0}.products w WHERE key = :key AND orgIdentifier = :orgIdentifier ";

        log.info("Get workspace for company {}, projectIdentifier {} and orgIdentifier {}", company, key, orgIdentifier);

        try {
            return Optional.of(template.queryForObject(MessageFormat.format(sql, company), Map.of("key", key.toUpperCase(), "orgIdentifier", orgIdentifier), mapper()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        sqlStatements.forEach(statement -> template.getJdbcTemplate()
                .execute(MessageFormat.format(statement, company)));
        return true;
    }
}