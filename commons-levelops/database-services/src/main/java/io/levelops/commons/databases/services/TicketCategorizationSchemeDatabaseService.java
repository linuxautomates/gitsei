package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.TicketCategorizationSchemeConverters;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.TicketCategorization;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class TicketCategorizationSchemeDatabaseService extends FilteredDatabaseService<TicketCategorizationScheme, TicketCategorizationSchemeDatabaseService.TicketCategorizationSchemeFilter> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    @VisibleForTesting
    protected boolean populateData = true;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public TicketCategorizationSchemeDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        this.transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Override
    public String insert(String company, TicketCategorizationScheme t) throws SQLException {
        Validate.notBlank(t.getName(), "name cannot be null or empty.");

        //exclude categories from config before inserting scheme
        Collection<TicketCategorization> categories = t.getConfig().getCategories().values();
        t = t.toBuilder().config(t.getConfig().toBuilder().categories(null).build()).build();

        String sql = "INSERT INTO " + company + ".ticket_categorization_schemes " +
                "(name, default_scheme, config)" +
                " VALUES " +
                "(:name, :default_scheme, :config::jsonb)";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "name", t.getName(),
                    "default_scheme", false,
                    "config", (t.getConfig() != null) ? objectMapper.writeValueAsString(t.getConfig()) : "{}"
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize ticket categorization scheme to JSON", e);
        }
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        String id;
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                return null;
            }

            id = String.valueOf(keyHolder.getKeys().get("id"));

            //insert all categories
            for(TicketCategorization tc : categories){
                insertTicketCategorization(company, id, tc);
            }

            if (BooleanUtils.isTrue(t.getDefaultScheme())) {
                    update(company, TicketCategorizationScheme.builder()
                            .id(id)
                            .defaultScheme(true)
                            .build());
            }
            transactionManager.commit(txStatus);
        }catch(Exception e){
            transactionManager.rollback(txStatus);
            throw e;
        }
        return id;
    }

    private String insertTicketCategorization(String company, String schemeId, TicketCategorization tc) throws SQLException {
        String sql = "INSERT INTO " + company + ".ticket_categorizations " +
                "(config,scheme_id)" +
                " VALUES " +
                "(:config::jsonb,:scheme_id::UUID)";
        Map<String, Object> params;
        try {
            params = Map.of(
                    "config", (tc != null) ? objectMapper.writeValueAsString(tc) : "{}",
                    "scheme_id",schemeId
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize ticket categorization to JSON", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert ticket categorization");
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, TicketCategorizationScheme t) throws SQLException {
        Boolean updated = false;
        Validate.notBlank(t.getId(), "id cannot be null or empty.");
        String schemeId = t.getId();

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", schemeId);

        boolean unsetCurrentDefaultScheme = false;
        try {
            // -- name
            if (t.getName() != null) {
                updates.add("name = :name");
                params.put("name", t.getName());
            }

            // -- config
            if (t.getConfig() != null) {
                updates.add("config = :config::jsonb");
                params.put("config", objectMapper.writeValueAsString(t.getConfig()));
            }

            // -- default (can only be set to true, not unset)
            if (BooleanUtils.isTrue(t.getDefaultScheme())) {
                unsetCurrentDefaultScheme = true;
                updates.add("default_scheme = true");
            }

        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize data", e);
        }

        if (updates.isEmpty()) {
            return true;
        }

        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try{
            if(t.getConfig() != null && t.getConfig().getCategories() != null){
                //list1 -> fetch existing categories for the scheme (based on id) & list2 -> t.getConfig().getCategories()
                //For the categories with no id in list2, create a new category with schemeId
                //For the categories in both list1 & list2 (same id) - update the body
                //For the categories (list1 - list2), delete from categories table
                Optional<TicketCategorizationScheme> existingSchOpt = get(company,schemeId);
                TicketCategorizationScheme existingSch = existingSchOpt.orElse(null);
                if(existingSch != null){
                    Collection<TicketCategorization> existingCategories = existingSch.getConfig().getCategories().values();
                    Collection<TicketCategorization> newCategories = t.getConfig().getCategories().values();
                    Set<String> existingCategoryIds = existingCategories.stream().map(c -> c.getId()).collect(Collectors.toSet());
                    Set<String> newCategoryIds = newCategories.stream().map(c -> c.getId()).collect(Collectors.toSet());
                    Map<String,TicketCategorization> existingCategoryMap = existingCategories.stream()
                            .collect(Collectors.toMap(c -> c.getId(), c -> c));
                    Map<String,TicketCategorization> newCategoryMap = newCategories.stream()
                            .collect(Collectors.toMap(c -> c.getId(), c -> c));
                    for(String categoryId : newCategoryIds){
                        if(existingCategoryIds.contains(categoryId))
                            updateTicketCategorization(company,schemeId,newCategoryMap.get(categoryId));
                        else
                            insertTicketCategorization(company,schemeId,newCategoryMap.get(categoryId));
                    }
                    Set<String> toBeDeleted = existingCategoryIds.removeAll(newCategoryIds) ? existingCategoryIds : Set.of();
                    for(String categoryId : toBeDeleted){
                        deleteTicketCategorizarion(company,categoryId);
                    }
                }
            }

            updates.add("updated_at = now()");
            String sql = "";
            if (unsetCurrentDefaultScheme) {
                // first the default scheme flag needs to be unset
                // (this will be part of the same transaction)
                sql += "UPDATE " + company + ".ticket_categorization_schemes " +
                        " SET default_scheme = false " +
                        " WHERE default_scheme = true; ";
            }
            sql += "UPDATE " + company + ".ticket_categorization_schemes " +
                    " SET " + String.join(", ", updates) +
                    " WHERE id = :id::uuid ";

            updated =  template.update(sql, params) > 0;
            transactionManager.commit(txStatus);
        }catch(Exception e){
            transactionManager.rollback(txStatus);
            throw e;
        }
        return updated;
    }

    private Boolean deleteTicketCategorizarion(String company, String id) {
        String sql = "DELETE FROM " + company + ".ticket_categorizations " +
                " WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    private Boolean updateTicketCategorization(String company, String schemeId, TicketCategorization c) throws SQLException {
        Validate.notBlank(c.getId(),"category id cannot be null");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", c.getId());

        updates.add("config = :config::jsonb");
        try {
            params.put("config", objectMapper.writeValueAsString(c));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize data", e);
        }

        updates.add("scheme_id = :scheme_id::uuid");
        params.put("scheme_id",schemeId);

        String sql = "UPDATE " + company + ".ticket_categorizations " +
                " SET " + String.join(", ", updates) +
                " WHERE id = :id::uuid ";

        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<TicketCategorizationScheme> get(String company, String id) {
        String sql = "SELECT * FROM " + company + ".ticket_categorization_schemes " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        TicketCategorizationScheme res = null;
        try {
            List<TicketCategorizationScheme> results = template.query(sql, Map.of("id", id),
                    TicketCategorizationSchemeConverters.rowMapper(objectMapper));
            res = IterableUtils.getFirst(results).orElse(null);
            if(res != null){
                TicketCategorizationScheme.TicketCategorizationConfig newConfig = res.getConfig().toBuilder().
                        categories(getCategoriesBySchemeId(company,res.getId())).build();
                res = res.toBuilder().config(newConfig).build();
            }
            else return Optional.empty();
        } catch (DataAccessException e) {
            log.warn("Failed to get ticket categorization scheme for id={}", id, e);
            return Optional.empty();
        }
        return Optional.of(res);
    }

    public Optional<TicketCategorization> getCategoryById(String company, String categoryId) {
        String sql = "SELECT * FROM " + company + ".ticket_categorizations " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        TicketCategorization res = null;
        List<TicketCategorization> results = template.query(sql,Map.of("id",categoryId),TicketCategorizationSchemeConverters.categoryRowMapper(objectMapper));
        res = IterableUtils.getFirst(results).orElse(null);
        return Optional.of(res);
    }

    private Map<String,TicketCategorization> getCategoriesBySchemeId(String company, String schemeId) {
        String sql = "SELECT * FROM " + company + ".ticket_categorizations " +
                " WHERE scheme_id = :scheme_id::uuid ";
        List<TicketCategorization> categories = template.query(sql, Map.of("scheme_id", schemeId),
                TicketCategorizationSchemeConverters.categoryRowMapper(objectMapper));
        return categories.stream().collect(Collectors.toMap(c -> c.getIndex().toString(),c -> c));
    }

    public Optional<TicketCategorizationScheme> getDefaultScheme(String company) {
        String sql = "SELECT * FROM " + company + ".ticket_categorization_schemes " +
                " WHERE default_scheme = true " +
                " LIMIT 1 ";
        TicketCategorizationScheme res = null;
        try {
            List<TicketCategorizationScheme> results = template.query(sql,Map.of(),
                    TicketCategorizationSchemeConverters.rowMapper(objectMapper));
            res = IterableUtils.getFirst(results).orElse(null);
            if(res != null){
                TicketCategorizationScheme.TicketCategorizationConfig newConfig = res.getConfig().toBuilder().
                        categories(getCategoriesBySchemeId(company,res.getId())).build();
                res = res.toBuilder().config(newConfig).build();
            } else return Optional.empty();
        } catch (DataAccessException e) {
            log.warn("Failed to load default categorization scheme", e);
            return Optional.empty();
        }
        return Optional.of(res);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketCategorizationSchemeFilter.TicketCategorizationSchemeFilterBuilder.class)
    public static class TicketCategorizationSchemeFilter {
        @JsonProperty("ids")
        List<String> ids;
        @JsonProperty("name")
        String name;
        @JsonProperty("partial_name")
        String partialName;
        @JsonProperty("default_scheme")
        Boolean defaultScheme;
    }

    @Override
    public DbListResponse<TicketCategorizationScheme> filter(Integer pageNumber, Integer pageSize, String company, TicketCategorizationSchemeFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : TicketCategorizationSchemeFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- name
        if (StringUtils.isNotEmpty(filter.getName())) {
            conditions.add("name ILIKE :name");
            params.put("name", filter.getName());
        }
        // -- partial name
        else if (StringUtils.isNotEmpty(filter.getPartialName())) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + filter.getPartialName() + "%");
        }
        // -- ids
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", filter.getIds());
        }
        // -- default_scheme
        if (filter.getDefaultScheme() != null) {
            conditions.add("default_scheme = :default_scheme::boolean");
            params.put("default_scheme", filter.getDefaultScheme());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".ticket_categorization_schemes " +
                where +
                " ORDER BY name ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<TicketCategorizationScheme> results = template.query(sql, params, TicketCategorizationSchemeConverters.rowMapper(objectMapper));
        results = results.stream().map((res) -> {
            TicketCategorizationScheme.TicketCategorizationConfig newConfig = res.getConfig().toBuilder().
                    categories(getCategoriesBySchemeId(company,res.getId())).build();
            return res.toBuilder().config(newConfig).build();
        }).collect(Collectors.toList());
        String countSql = "SELECT count(*) FROM " + company + ".ticket_categorization_schemes " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String defaultSchemeId = getDefaultScheme(company)
                .map(TicketCategorizationScheme::getId)
                .orElse(null);
        if (StringUtils.equals(defaultSchemeId, id)) {
            return false;
        }
        String sql = "DELETE FROM " + company + ".ticket_categorization_schemes " +
                " WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".ticket_categorization_schemes " +
                        "(" +
                        "   id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   default_scheme BOOLEAN NOT NULL DEFAULT false," +
                        "   name           TEXT NOT NULL," +
                        "   config         JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   created_at     TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",
                // make sure there can only be one default scheme:
                "CREATE UNIQUE INDEX IF NOT EXISTS ticket_categorization_schemes_default_scheme_index " +
                        " ON " + company + ".ticket_categorization_schemes (default_scheme) " +
                        " WHERE default_scheme = true;",
                // make sure name is unique (case insensitive)
                "CREATE UNIQUE INDEX IF NOT EXISTS ticket_categorization_schemes_name_index " +
                        " ON " + company + ".ticket_categorization_schemes (UPPER(name));",
                "CREATE TABLE IF NOT EXISTS " + company + ".ticket_categorizations " +
                        "(" +
                        "   id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   config         JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   scheme_id      UUID NOT NULL REFERENCES " + company + ".ticket_categorization_schemes(id) ON DELETE CASCADE," +
                        "   created_at     TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);

        if (populateData) {
            populateDefaultData(company);
        }

        return true;
    }

    protected void populateDefaultData(String company) throws SQLException {
        boolean defaultAlreadyExists = getDefaultScheme(company).isPresent();
        if (defaultAlreadyExists) {
            // for now, we will not recreate the scheme if there is already a default scheme
            return;
        }
        TicketCategorizationScheme defaultScheme = TicketCategorizationScheme.builder()
                .name("System Ticket Categorization Profile")
                .defaultScheme(!defaultAlreadyExists) // if there is no default, set this to default
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc")
                        .integrationType("jira")
                        .uncategorized(TicketCategorizationScheme.Uncategorized.builder().build())
                        .categories(Map.of(
                                "1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("1")
                                        .name("New Features")
                                        .index(10)
                                        .filter(Map.of("issue_types", List.of("NEW FEATURE", "STORY")))
                                        .build(),
                                "2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("2")
                                        .name("Bugs")
                                        .index(20)
                                        .filter(Map.of("issue_types", List.of("BUG")))
                                        .build(),
                                "3", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("3")
                                        .name("Technical Tasks")
                                        .index(30)
                                        .filter(Map.of("issue_types", List.of("TASK")))
                                        .build()
                        ))
                        .build())
                .build();
        boolean alreadyExists = filter(0, 1, company, TicketCategorizationSchemeFilter.builder()
                .name(defaultScheme.getName())
                .build())
                .getCount() > 0;
        if (alreadyExists) {
            log.info("Not inserting default data for ticket categorization schemes: already exists");
            return;
        }
        try {
            String id = insert(company, defaultScheme);
            log.info("Inserted default data for ticket categorization schemes ({} rows)", (id != null) ? 1 : 0);
        } catch (DuplicateKeyException e) {
            log.warn("Could not insert default ticket categorization scheme: already exists (won't overwrite)");
        }
    }

}
