package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class IntegrationService extends DatabaseService<Integration> {

    private static final boolean DO_NAME_PARTIAL_MATCH = false;
    public static final String UNLINK_CREDENTIALS = "-1";

    private static final String NULL = "null";
    static final RowMapper<Integration> ROW_MAPPER = (rs, rowNumber) ->
            Integration.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .url(rs.getString("url"))
                    .status(rs.getString("status"))
                    .description(rs.getString("description"))
                    .application(rs.getString("application"))
                    .satellite(rs.getBoolean("satellite"))
                    .tags(Arrays.asList(rs.getArray("tags") != null ?
                            (String[]) rs.getArray("tags").getArray() : new String[0]))
                    .metadata(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(), "integration",
                            rs.getString("metadata")))
                    .authentication(Integration.Authentication.fromString(rs.getString("authentication")))
                    .linkedCredentials(rs.getString("linked_credentials"))
                    .updatedAt(rs.getLong("updatedat"))
                    .createdAt(rs.getLong("createdat"))
                    .build();

    private static final RowMapper<IntegrationConfig> CONFIG_ROW_MAPPER = (rs, rowNumber) ->
            IntegrationConfig.builder()
                    .id(rs.getString("id"))
                    .integrationId(rs.getString("integration_id"))
                    .config(ParsingUtils.parseMap(
                            DefaultObjectMapper.get(),
                            "integration_config",
                            DefaultObjectMapper.get().getTypeFactory().constructType(String.class),
                            DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, IntegrationConfig.ConfigEntry.class),
                            rs.getString("config")))
                    .repoConfig(ParsingUtils.parseList(
                            DefaultObjectMapper.get(),
                            "integration_repo_config",
                            IntegrationConfig.RepoConfigEntry.class,
                            rs.getString("repository_config")))
                    .customHygieneList(ParsingUtils.parseList(
                            DefaultObjectMapper.get(),
                            "integration_hygiene",
                            IntegrationConfig.CustomHygieneEntry.class,
                            rs.getString("custom_hygienes")))
                    .metadata(ParsingUtils.parseObject(DefaultObjectMapper.get(), "metadata", IntegrationConfig.Metadata.class, rs.getString("metadata")))
                    .createdAt(rs.getLong("createdat"))
                    .build();
    protected final NamedParameterJdbcTemplate template;

    @Autowired
    public IntegrationService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public static void createWhereCondition(String name, boolean strictMatch, List<String> applications,
                                            Boolean satellite, List<Integer> integrationIds, List<Integer> tagIds,
                                            Long updatedAtStart, Long updatedAtEnd, String linkedCredentialsIntegrationId,
                                            HashMap<String, Object> params,
                                            List<String> conditions) {
        if (StringUtils.isNotEmpty(name)) {
            conditions.add("name ILIKE :name");
            if (strictMatch) {
                params.put("name", name);
            } else {
                params.put("name", "%" + name + "%");
            }
        }
        if (CollectionUtils.isNotEmpty(tagIds)) {
            conditions.add(":tagids::text[] && t.tags");
            params.put("tagids", DatabaseUtils.toSqlArray(tagIds));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("i.id IN (:idlist)");
            params.put("idlist", integrationIds);
        }
        if (CollectionUtils.isNotEmpty(applications)) {
            conditions.add("application IN (:applications)");
            params.put("applications", applications);
        }
        if (satellite != null) {
            conditions.add("satellite = :satellite");
            params.put("satellite", satellite);
        }
        if (updatedAtStart != null) {
            conditions.add("updatedat > :updatedAtStart");
            params.put("updatedAtStart", updatedAtStart);
        }
        if (updatedAtEnd != null) {
            conditions.add("updatedat < :updatedAtEnd");
            params.put("updatedAtEnd", updatedAtEnd);
        }
        if (linkedCredentialsIntegrationId != null) {
            conditions.add("linked_credentials = :linked_credentials");
            params.put("linked_credentials", NumberUtils.toInt(linkedCredentialsIntegrationId));
        }
    }

    @Override
    public String insert(String company, Integration integration) throws SQLException {

        String SQL = "INSERT INTO " + company + ".integrations " +
                "(name,url,status,application,description,satellite, metadata, authentication, linked_credentials) " +
                " VALUES " +
                "(:name, :url, :status, :application, :description, :satellite, to_json(:metadata::jsonb), :authentication, :linked_credentials)";
        MapSqlParameterSource params = new MapSqlParameterSource(Map.of(
                "name", integration.getName(),
                "url", StringUtils.defaultString(integration.getUrl()),
                "status", integration.getStatus(),
                "application", integration.getApplication(),
                "description", StringUtils.defaultString(integration.getDescription()),
                "satellite", BooleanUtils.toBooleanDefaultIfNull(integration.getSatellite(), false),
                "metadata", getJsonString(MapUtils.emptyIfNull(integration.getMetadata())),
                "authentication", MoreObjects.firstNonNull(integration.getAuthentication(), Integration.Authentication.UNKNOWN).toString()));
        params.addValue("linked_credentials", integration.getLinkedCredentials() != null ? NumberUtils.toInt(integration.getLinkedCredentials()) : null);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(SQL, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public Boolean insertConfig(String company, IntegrationConfig config) throws SQLException {

        String SQL = "INSERT INTO " + company + ".integration_configs" +
                " (integration_id, config, repository_config, custom_hygienes, metadata)" +
                " VALUES" +
                " (:integration_id, to_json(:config::json), to_json(:repository_config::json), to_json(:custom_hygienes::json), to_json(:metadata::json))" +
                " ON CONFLICT (integration_id) " +
                " DO UPDATE SET " +
                " (config, repository_config, custom_hygienes, metadata) =" +
                " (EXCLUDED.config, EXCLUDED.repository_config, EXCLUDED.custom_hygienes, EXCLUDED.metadata)";
        Map<String, Object> params = null;
        try {
            params = Map.of(
                    "integration_id", Integer.parseInt(config.getIntegrationId()),
                    "config", DefaultObjectMapper.get().writeValueAsString(MapUtils.emptyIfNull(config.getConfig())),
                    "repository_config", DefaultObjectMapper.get().writeValueAsString(ListUtils.emptyIfNull(config.getRepoConfig())),
                    "custom_hygienes", DefaultObjectMapper.get().writeValueAsString(ListUtils.emptyIfNull(config.getCustomHygieneList())),
                    "metadata", ParsingUtils.serializeOrThrow(config.getMetadata(), "{}"));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize config obj.");
        }

        int updatedRows = template.update(SQL, new MapSqlParameterSource(params));
        return updatedRows > 0;
    }

    private String getJsonString(@NonNull Map<String, Object> metadata) {
        try {
            return DefaultObjectMapper.get().writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("getJsonString: error converting metadata to string: " + e.getMessage(), e);
            return NULL;
        }
    }

    @Override
    public Boolean update(String company, Integration integration) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(integration, "integration cannot be null.");
        Validate.notBlank(integration.getId(), "integration.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();

        // -- name
        if (StringUtils.isNotEmpty(integration.getName())) {
            updates.add("name = :name");
            params.put("name", integration.getName());
        }
        // -- url
        if (StringUtils.isNotEmpty(integration.getUrl())) {
            updates.add("url = :url");
            params.put("url", integration.getUrl());
        }
        // -- status
        if (StringUtils.isNotEmpty(integration.getStatus())) {
            updates.add("status = :status");
            params.put("status", integration.getStatus());
        }
        // -- description
        if (StringUtils.isNotEmpty(integration.getDescription())) {
            updates.add("description = :description");
            params.put("description", integration.getDescription());
        }
        // -- satellite
        if (integration.getSatellite() != null) {
            updates.add("satellite = :satellite");
            params.put("satellite", BooleanUtils.toBooleanDefaultIfNull(integration.getSatellite(), false));
        }
        // -- metadata
        if (integration.isAppendMetadata() && integration.getMetadata() != null) {
            updates.add("metadata = metadata || to_jsonb(:metadata::jsonb)");
            params.put("metadata", getJsonString(MapUtils.emptyIfNull(integration.getMetadata())));
        } else if (integration.getMetadata() != null) {
            updates.add("metadata = to_json(:metadata::json)");
            params.put("metadata", getJsonString(MapUtils.emptyIfNull(integration.getMetadata())));
        }
        // -- authentication
        if (integration.getAuthentication() != null) {
            updates.add("authentication = :authentication");
            params.put("authentication", integration.getAuthentication().toString());
        }
        // -- linked credentials
        if (integration.getLinkedCredentials() != null) {
            int linkedCredentials = NumberUtils.toInt(integration.getLinkedCredentials());
            if (linkedCredentials >= 0) {
                updates.add("linked_credentials = :linked_credentials");
                params.put("linked_credentials", linkedCredentials);
            } else {
                updates.add("linked_credentials = null");
            }
        }

        // no updates
        if (updates.isEmpty()) {
            return false;
        }

        // -- updated at
        updates.add("updatedat = :updatedat");
        params.put("updatedat", Instant.now().getEpochSecond());

        String condition = "id = :id";
        params.put("id", Integer.parseInt(integration.getId()));

        String SQL = "UPDATE " + company + ".integrations " +
                " SET " + String.join(" , ", updates) +
                " WHERE " + condition;
        return template.update(SQL, params) > 0;
    }

    @Override
    public Optional<Integration> get(String company, String integrationId) {
        String SQL = "SELECT i.*, array_remove(array_agg(t.tagid), NULL)::text[] as tags"
                + " FROM " + company + ".integrations as i "
                + " LEFT OUTER JOIN ("
                + "   SELECT ti.tagid, ti.itemid::int"
                + "   FROM " + company + ".tagitems as ti"
                + "   WHERE ti.itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'"
                + ") as t ON t.itemid = i.id"
                + " WHERE i.id = :id"
                + " GROUP BY i.id";

        Map<String, Object> params = Map.of("id", Integer.parseInt(integrationId));

        try {
            var v = IterableUtils.getFirst(template.query(SQL, params, ROW_MAPPER));
            return v;
        } catch (DataAccessException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<Integration> listByFilter(String company,
                                                    String name,
                                                    List<String> applications,
                                                    Boolean satellite,
                                                    List<Integer> integrationIds,
                                                    List<Integer> tagIds,
                                                    Integer pageNumber,
                                                    Integer pageSize) throws SQLException {
        return listByFilter(company, name, DO_NAME_PARTIAL_MATCH, applications, satellite, integrationIds, tagIds, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<Integration> listByFilter(String company,
                                                    String name,
                                                    boolean strictMatch,
                                                    List<String> applications,
                                                    Boolean satellite,
                                                    List<Integer> integrationIds,
                                                    List<Integer> tagIds,
                                                    Integer pageNumber,
                                                    Integer pageSize) throws SQLException {
        return listByFilter(company, name, strictMatch, applications, satellite, integrationIds, tagIds, null, null, null, pageNumber, pageSize);
    }

    public Stream<Integration> stream(String company,
                                      String name,
                                      boolean strictMatch,
                                      List<String> applications,
                                      Boolean satellite,
                                      List<Integer> integrationIds,
                                      List<Integer> tagIds,
                                      Long updatedAtStart,
                                      Long updatedAtEnd,
                                      String linkedCredentialsIntegrationId) throws SQLException {
        return PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(page ->
                listByFilter(company, name, strictMatch, applications, satellite, integrationIds, tagIds, updatedAtStart, updatedAtEnd, linkedCredentialsIntegrationId, page, 100).getRecords()));
    }

    public DbListResponse<Integration> listByFilter(String company,
                                                    String name,
                                                    boolean strictMatch,
                                                    List<String> applications,
                                                    Boolean satellite,
                                                    List<Integer> integrationIds,
                                                    List<Integer> tagIds,
                                                    Long updatedAtStart,
                                                    Long updatedAtEnd,
                                                    String linkedCredentialsIntegrationId,
                                                    Integer pageNumber,
                                                    Integer pageSize) throws SQLException {
        int limit = MoreObjects.firstNonNull(pageSize, 25);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        HashMap<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        createWhereCondition(name, strictMatch, applications, satellite, integrationIds, tagIds, updatedAtStart,
                updatedAtEnd, linkedCredentialsIntegrationId, params, conditions);
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String SQL = "SELECT i.*, t.tags FROM "
                + company + ".integrations as i"
                + " LEFT OUTER JOIN ("
                + " SELECT array_remove(array_agg(ti.tagid), NULL)::text[] as tags,"
                + " ti.itemid::int FROM " + company + ".tagitems as ti"
                + " WHERE ti.itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'"
                + " GROUP BY ti.itemid ) as t ON t.itemid = i.id"
                + where
                + " ORDER BY updatedat desc "
                + " LIMIT " + limit
                + " OFFSET " + skip;
        List<Integration> integrations = template.query(SQL, params, ROW_MAPPER);

        Integer count = 0;
        if (CollectionUtils.isNotEmpty(integrations)) {
            String countSQL = "SELECT COUNT(*)"
                    + " FROM ( SELECT"
                    + " i.id, t.tags FROM "
                    + company + ".integrations as i"
                    + " LEFT OUTER JOIN ("
                    + " SELECT array_remove(array_agg(ti.tagid), NULL)::text[] as tags,"
                    + " ti.itemid::int FROM " + company + ".tagitems as ti"
                    + " WHERE ti.itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'"
                    + " GROUP BY ti.itemid ) as t ON t.itemid = i.id"
                    + where
                    + " ) AS counting";
            count = template.queryForObject(countSQL, params, Integer.class);
        }

        return DbListResponse.of(integrations, count);
    }

    @Override
    public DbListResponse<Integration> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByFilter(company, null, DO_NAME_PARTIAL_MATCH, null, null, null, null, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<IntegrationConfig> listConfigs(String company,
                                                         List<String> integrationIds,
                                                         Integer pageNumber,
                                                         Integer pageSize) {
        int limit = MoreObjects.firstNonNull(pageSize, 25);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        HashMap<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:idlist)");
            params.put("idlist", integrationIds.stream().map(Integer::valueOf).collect(Collectors.toList()));
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String SQL = "SELECT * FROM " + company + ".integration_configs"
                + where + " LIMIT " + limit + " OFFSET " + skip;
        List<IntegrationConfig> integrations = template.query(SQL, params, CONFIG_ROW_MAPPER);

        Integer count = 0;
        if (CollectionUtils.isNotEmpty(integrations)) {
            String countSQL = "SELECT COUNT(*) FROM " + company + ".integration_configs" + where;
            count = template.queryForObject(countSQL, params, Integer.class);
        }

        return DbListResponse.of(integrations, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".integrations WHERE id = :id";
        String SQL2 = "DELETE FROM " + company + ".tagitems WHERE itemid::int = :id " +
                "AND itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'";
        Boolean deleted = template.update(SQL, Map.of("id", Integer.parseInt(id))) > 0;
        if (deleted) {
            template.update(SQL2, Map.of("id", Integer.parseInt(id)));
        }
        return deleted;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        if (CollectionUtils.isNotEmpty(ids)) {
            String SQL = "DELETE FROM " + company + ".integrations WHERE id IN (:ids)";
            String SQL2 = "DELETE FROM " + company + ".tagitems WHERE itemid::int IN (:ids) " +
                    "AND itemtype = '" + TagItemMapping.TagItemType.INTEGRATION + "'";
            Map<String, Object> params = Map.of("ids", ids.stream().map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            int rowsDeleted = template.update(SQL, params);
            if (rowsDeleted > 0) {
                template.update(SQL2, params);
            }
            return rowsDeleted;
        }
        return 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS " + company + ".integrations(" +
                        "        id                 INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
                        "        name               VARCHAR(50) UNIQUE NOT NULL, " +
                        "        description        VARCHAR(140), " +
                        "        status             VARCHAR(50) NOT NULL, " +
                        "        application        VARCHAR(50) NOT NULL, " +
                        "        url                VARCHAR NOT NULL, " +
                        "        satellite          BOOLEAN NOT NULL DEFAULT false, " +
                        "        metadata           JSONB, " +
                        "        authentication     VARCHAR(50) NOT NULL DEFAULT 'unknown', " +
                        "        linked_credentials INTEGER REFERENCES " + company + ".integrations(id), " +
                        "        updatedat          BIGINT DEFAULT extract(epoch from now()), " +
                        "        createdat          BIGINT DEFAULT extract(epoch from now())" +
                        "    )",

                "CREATE INDEX IF NOT EXISTS integrations_updatedat_idx ON " + company + ".integrations(updatedat)",

                "CREATE TABLE IF NOT EXISTS " + company + ".integration_configs(" +
                        "        id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(), " +
                        "        integration_id    INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE, \n" +
                        "        config            JSONB, " +
                        "        repository_config JSONB, " +
                        "        custom_hygienes   JSONB, " +
                        "        metadata          JSONB, " +
                        "        createdat         BIGINT DEFAULT extract(epoch from now())," +
                        "        UNIQUE(integration_id)" +
                        "    )");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}