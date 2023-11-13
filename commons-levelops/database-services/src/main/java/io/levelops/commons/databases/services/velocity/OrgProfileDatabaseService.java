package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.velocity.OrgProfile;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class OrgProfileDatabaseService extends DatabaseService<OrgProfile> {

    public final static String TABLE_OU_PROFILES = "ou_profiles";

    private final static String INSERT_SQL = "INSERT INTO {0}." + TABLE_OU_PROFILES + " {1} VALUES {2}";
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;

    private final ObjectMapper mapper;

    public OrgProfileDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.mapper = mapper;
    }

    @Override
    public String insert(String company, OrgProfile profile) throws SQLException {
        return null;
    }

    public int insert(String company, List<OrgProfile> profiles) throws SQLException {
        String columns = "(ou_ref_id, profile_id, profile_type, is_completed)";
        String value = "(:ou_ref_id_{0}, :profile_id_{0}, :profile_type_{0}, :is_completed_{0})";
        List<String> values = new ArrayList<>();
        int i = 0;
        MapSqlParameterSource params = new MapSqlParameterSource();
        for (OrgProfile profile : ListUtils.emptyIfNull(profiles)) {
            values.add(MessageFormat.format(value, i));
            params.addValue("ou_ref_id_" + i, profile.getOuRefId())
                    .addValue("profile_id_" + i, profile.getProfileId())
                    .addValue("is_completed_" + i, true)
                    .addValue("profile_type_" + i, profile.getProfileType().toString());
            i++;
        }
        String sql = MessageFormat.format(INSERT_SQL, company, columns, String.join(",", values)) + " ON CONFLICT DO NOTHING";
        return template.update(sql, params);
    }

    @Override
    public Boolean update(String company, OrgProfile t) throws SQLException {
        return null;
    }

    public Boolean updateProfileOUMappings(String company, UUID profileId, List<String> ouRefIds) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            deleteByProfileId(company, profileId);
            List<OrgProfile> workflows = new ArrayList<>();
            ListUtils.emptyIfNull(ouRefIds).forEach(ouRefId -> {
                workflows.add(OrgProfile.builder()
                        .profileId(profileId)
                        .profileType(OrgProfile.ProfileType.WORKFLOW)
                        .ouRefId(Integer.valueOf(ouRefId))
                        .build());
            });
            if (CollectionUtils.isNotEmpty(workflows)) {
                insert(company, workflows);
            }
            transactionManager.commit(txStatus);
            return true;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    @Override
    public Optional<OrgProfile> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    public Optional<List<String>> getByProfileId(String company, String profileId, OrgProfile.ProfileType profileType) {
        String sql = "SELECT ou_ref_id FROM " + company + "." + TABLE_OU_PROFILES +
                " WHERE profile_id = :profile_id AND profile_type = :profile_type";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("profile_id", UUID.fromString(profileId))
                .addValue("profile_type", profileType.toString());
        List<String> ouRefId = template.query(sql, params, (rs, row) -> {
            return rs.getString("ou_ref_id");
        });
        return ouRefId.size() > 0 ? Optional.of(ouRefId) : Optional.empty();
    }

    @Override
    public DbListResponse<OrgProfile> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    public Boolean deleteByProfileId(String company, UUID profileId) throws SQLException {
        String sql = "DELETE FROM " + company + "." + TABLE_OU_PROFILES + " WHERE profile_id = :profile_id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("profile_id", profileId);
        template.update(sql, params);
        return true;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + TABLE_OU_PROFILES + " (" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  ou_ref_id        INTEGER NOT NULL," +
                        "  profile_id       UUID NOT NULL," +
                        "  profile_type     VARCHAR(100) NOT NULL," +
                        "  is_completed     BOOLEAN NOT NULL DEFAULT false" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TABLE_OU_PROFILES + "_ou_ref_id_profile_type_uniq_idx ON " + company + "." + TABLE_OU_PROFILES + "(ou_ref_id,profile_type);"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}