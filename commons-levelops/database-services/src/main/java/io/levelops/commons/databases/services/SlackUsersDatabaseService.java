package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

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
public class SlackUsersDatabaseService extends DatabaseService<SlackUser> {
    private static final String UPSERT_SQL_FORMAT = "INSERT INTO %s.slack_users (team_id,user_id,real_name_normalized,username,email) VALUES(:team_id,:user_id,:real_name_normalized,:username,:email) "
            + "ON CONFLICT(team_id, user_id) DO UPDATE SET (real_name_normalized,username,email,updated_at) = (EXCLUDED.real_name_normalized,EXCLUDED.username,EXCLUDED.email,now()) "
            + " RETURNING id";
    private static final String LOOKUP_SQL_FORMAT = "SELECT * from %s.slack_users where team_id = :team_id AND user_id = :user_id";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public SlackUsersDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, SlackUser t) throws SQLException {
        throw new NotImplementedException("insert not implemented!");
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, SlackUser t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }
    // endregion

    // region get
    @Override
    public Optional<SlackUser> get(String company, String param) throws SQLException {
        throw new NotImplementedException("not implemented!");
    }
    // endregion

    // region list
    @Override
    public DbListResponse<SlackUser> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new NotImplementedException("not implemented!");
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException("not implemented!");
    }
    // endregion

    // region insert
    public String upsert(String company, SlackUser t) throws SQLException {
        Validate.notNull(t, "input cannot be null.");
        Validate.notBlank(t.getTeamId(), "team id cannot be null or empty.");
        Validate.notBlank(t.getUserId(), "user id cannot be null or empty.");

        String upsertSql = String.format(UPSERT_SQL_FORMAT, company);

        MapSqlParameterSource params = new MapSqlParameterSource();
        //:team_id,:user_id,:real_name_normalized,:username,:email
        params.addValue("team_id", t.getTeamId());
        params.addValue("user_id", t.getUserId());
        params.addValue("real_name_normalized", t.getRealNameNormalized());
        params.addValue("username", t.getUsername());
        params.addValue("email", t.getEmail());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int updatedRows = template.update(upsertSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to upsert slack user!" +  t.toString());
            }
        } catch (DuplicateKeyException e) {
            throw new SQLException("Failed to upsert slack slack user!" +  t.toString(), e);
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }
    // endregion

    public List<SlackUser> lookup(String company, String teamId, String userId) throws SQLException {
        Validate.notBlank(teamId, "company cannot be null or empty.");
        Validate.notBlank(teamId, "team id cannot be null or empty.");
        Validate.notBlank(teamId, "user id cannot be null or empty.");
        String lookUpSql = String.format(LOOKUP_SQL_FORMAT, company);
        try {
            List<SlackUser> results = template.query(lookUpSql, Map.of("team_id", teamId,
                    "user_id", userId),
                    SlackUserConverters.rowMapper());
            return results;
        } catch (DataAccessException e) {
            throw new SQLException("Failed to lookup tenant for team id " +  teamId, e);
        }
    }

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".slack_users( \n" +
                        "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                        "        team_id VARCHAR NOT NULL, \n" +
                        "        user_id VARCHAR NOT NULL, \n" +
                        "        real_name_normalized VARCHAR NOT NULL, \n" +
                        "        username VARCHAR, \n" +
                        "        email VARCHAR, \n" +
                        "        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                        "        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        "    )",

                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_slack_users_team_id_user_id_idx on " + company + ".slack_users (team_id,user_id)");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
