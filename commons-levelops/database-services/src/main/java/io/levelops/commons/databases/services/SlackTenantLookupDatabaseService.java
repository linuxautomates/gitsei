package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.SlackTenantLookupConverters;
import io.levelops.commons.databases.models.database.SlackTenantLookup;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class SlackTenantLookupDatabaseService extends DatabaseService<SlackTenantLookup>{
    private final NamedParameterJdbcTemplate template;

    private static final String SELECT_SQL_FORMAT = "SELECT * from %s.slack_tenant_lookups where team_id = :team_id and tenant_name = :tenant_name LIMIT 1";
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.slack_tenant_lookups (team_id,tenant_name) VALUES(:team_id,:tenant_name) \n" +
            "RETURNING id";
    private static final String LOOKUP_SQL_FORMAT = "SELECT * from %s.slack_tenant_lookups where team_id = :team_id";

    @Autowired
    public SlackTenantLookupDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public boolean isTenantSpecific() {
        return false;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.LEVELOPS_INVENTORY_SCHEMA;
    }

    @Override
    public String insert(String company, SlackTenantLookup t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }

    @Override
    public Boolean update(String company, SlackTenantLookup t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }

    @Override
    public Optional<SlackTenantLookup> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<SlackTenantLookup> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }

    public String upsert(SlackTenantLookup t) throws SQLException {
        Validate.notNull(t, "input cannot be null.");
        Validate.notBlank(t.getTeamId(), "team id cannot be null or empty.");
        Validate.notBlank(t.getTenantName(), "tenant name cannot be null or empty.");

        String selectSql = String.format(SELECT_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);
        String upsertSql = String.format(INSERT_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("team_id", t.getTeamId());
        params.addValue("tenant_name", t.getTenantName());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            List<SlackTenantLookup> results = template.query(selectSql, params,SlackTenantLookupConverters.rowMapper());
            if(CollectionUtils.isNotEmpty(results)) {
                log.info("slack tenant lookup exists for teamId {} and tenantName {}", t.getTeamId(), t.getTenantName());
                return results.get(0).getId().toString();
            }
            log.info("slack tenant lookup does not exist for teamId {} and tenantName {}", t.getTeamId(), t.getTenantName());
            int updatedRows = template.update(upsertSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to upsert slack tenant lookup!" +  t.toString());
            }
        } catch (DuplicateKeyException e) {
            throw new SQLException("Failed to upsert slack tenant lookup!" +  t.toString(), e);
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public List<SlackTenantLookup> lookup(String teamId) throws SQLException {
        Validate.notBlank(teamId, "team id cannot be null or empty.");
        String lookUpSql = String.format(LOOKUP_SQL_FORMAT, LEVELOPS_INVENTORY_SCHEMA);
        try {
            List<SlackTenantLookup> results = template.query(lookUpSql, Map.of("team_id", teamId),
                    SlackTenantLookupConverters.rowMapper());
            return results;
        } catch (DataAccessException e) {
            throw new SQLException("Failed to lookup tenant for team id " +  teamId, e);
        }
    }

    @Override
    public Boolean ensureTableExistence(String schema) throws SQLException {
        ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + LEVELOPS_INVENTORY_SCHEMA + ".slack_tenant_lookups " +
                        "(" +
                        "  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  team_id     TEXT NOT NULL," +
                        "  tenant_name TEXT NOT NULL," +
                        "  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",

                //Only one entry can be present for a team_id
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_slack_tenant_lookups_team_id_tenant_name_idx on " + LEVELOPS_INVENTORY_SCHEMA + ".slack_tenant_lookups (team_id,tenant_name)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
