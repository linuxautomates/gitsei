package io.levelops.commons.databases.services.organization;

import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;

import lombok.extern.log4j.Log4j2;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class OrgVersionsDatabaseService extends DatabaseService<OrgVersion> {
    public final static String ORG_VERSIONS_TABLE_NAME = "org_version_counter";

    private final NamedParameterJdbcTemplate template;

    private final static List<String> ddl = List.of(
        "CREATE TABLE IF NOT EXISTS {0}.{1} (\n" +
            "   id           UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n" +
            "   type         VARCHAR NOT NULL,\n" +
            "   version        INTEGER NOT NULL,\n" +
            "   active       BOOL NOT NULL DEFAULT false,\n" +
            "   created_at   TIMESTAMP NOT NULL DEFAULT now(),\n" +
            "   updated_at   TIMESTAMP NOT NULL DEFAULT now()\n" +
        ");");

    public OrgVersionsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(final String company, final OrgVersion version) throws SQLException {
        return insert(company, version.getType()).toString();
    }

    public UUID insert(final String company, OrgVersion.OrgAssetType type) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        template.update(
            MessageFormat.format("INSERT INTO {0}.{1} (type, version) VALUES(:type, COALESCE((SELECT MAX(version)+1 FROM {0}.{1} WHERE type = :type),1))", company, ORG_VERSIONS_TABLE_NAME),
            new MapSqlParameterSource().addValue("type", type.toString()),
            keyHolder,
            new String[]{"id"}
        );
        return (UUID) keyHolder.getKeys().get("id");
    }

    @Override
    public Boolean update(final String company, final OrgVersion version) throws SQLException {
        return update(company, version.getId(), version.isActive());
    }

    public Boolean update(final String company, final OrgAssetType type, final Integer version, final Boolean active) throws SQLException {
        var count = template.update(
            MessageFormat.format("UPDATE {0}.{1} SET active = :active, updated_at = now() WHERE type = :type AND version = :version", company, ORG_VERSIONS_TABLE_NAME),
            new MapSqlParameterSource().addValue("type", type.toString()).addValue("version", version).addValue("active", active)
        );
        log.info("Updated org version: {}, active: {} - count {}", version, active, count);
        return count != 0;
    }

    public Boolean update(final String company, final UUID versionId, final Boolean active) throws SQLException {
        var count = template.update(
            MessageFormat.format("UPDATE {0}.{1} SET active = :active, updated_at = now() WHERE id = :id", company, ORG_VERSIONS_TABLE_NAME),
            new MapSqlParameterSource().addValue("id", versionId).addValue("active", active)
        );
        log.info("Updated org version: {}, active: {} - count {}", versionId, active, count);
        return count != 0;
    }

    @Override
    public Optional<OrgVersion> get(final String company, final String versionId) throws SQLException {
        return get(company, UUID.fromString(versionId));
    }

    public Optional<OrgVersion> get(final String company, final UUID versionId) throws SQLException {
        try{
            return Optional.ofNullable(this.template.queryForObject(
                MessageFormat.format("SELECT * FROM {0}.{1} WHERE id = :id", company, ORG_VERSIONS_TABLE_NAME),
                Map.of("id", versionId), getOrgVersionRowMapper()));
        }
        catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<OrgVersion> get(final String company, final OrgAssetType type, final Integer version) throws SQLException {
        try{
            return Optional.ofNullable(this.template.queryForObject(
                MessageFormat.format("SELECT * FROM {0}.{1} WHERE type = :type AND version = :version", company, ORG_VERSIONS_TABLE_NAME),
                Map.of("type", type.toString(),"version", version), getOrgVersionRowMapper()));
        }
        catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private RowMapper<OrgVersion> getOrgVersionRowMapper(){
        return (rs, row) -> {
            return OrgVersion.builder()
                .id((UUID) rs.getObject("id"))
                .type(OrgAssetType.fromString(rs.getString("type")))
                .version(rs.getInt("version"))
                .active(rs.getBoolean("active"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
        };
    }

    public Optional<OrgVersion> getLatest(final String company, final OrgAssetType type) throws SQLException {
        try {
            return Optional.ofNullable(this.template.queryForObject(
                MessageFormat.format("SELECT * FROM {0}.{1} WHERE type = :type ORDER BY version DESC LIMIT 1", company, ORG_VERSIONS_TABLE_NAME),
                Map.of("type", type.toString()),
                getOrgVersionRowMapper()));
        }
        catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<OrgVersion> getActive(final String company, final OrgAssetType type) throws SQLException {
        try {
            return Optional.ofNullable(this.template.queryForObject(
                MessageFormat.format("SELECT * FROM {0}.{1} WHERE type = :type AND active = true LIMIT 1", company, ORG_VERSIONS_TABLE_NAME),
                Map.of("type", type.toString()),
                getOrgVersionRowMapper()));
        }
        catch(EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<OrgVersion> list(final String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public DbListResponse<OrgVersion> filter(final String company, final OrgAssetType type, Integer pageNumber, Integer pageSize) throws SQLException {
        var whereClause = type == null ? "" : "WHERE type = :type";
        var baseSelect = MessageFormat.format("SELECT * FROM {0}.{1} {2}", company, ORG_VERSIONS_TABLE_NAME, whereClause);
        var params = new MapSqlParameterSource().addValue("type", type.toString());
        var records = this.template.query(
            MessageFormat.format("{0} LIMIT {1} OFFSET {2}", baseSelect, pageSize, (pageNumber * pageSize)),
            params,
            getOrgVersionRowMapper()
        );
        var count = records.size() < pageSize ? records.size() : this.template.queryForObject(MessageFormat.format("SELECT COUNT(*) FROM ({0}) AS c", baseSelect), params, Integer.class);
        return DbListResponse.of(records, count);
    }

    @Override
    public Boolean delete(final String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(final String company) throws SQLException {
        ddl.forEach(item -> template.getJdbcTemplate()
                .execute(MessageFormat.format(
                        item,
                        company,
                        ORG_VERSIONS_TABLE_NAME)));
        return true;
    }
    
}
