package io.levelops.commons.databases.services.jira;

import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.utils.TransactionCallback;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;

@Log4j2
@Service
public class JiraIssueUserService {

    private final NamedParameterJdbcTemplate template;

    public JiraIssueUserService(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public boolean insertJiraUser(String company, DbJiraUser user) {
        return BooleanUtils.isTrue(template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //insert user
            String sql = "INSERT INTO " + company + "." + USERS_TABLE + " AS users " +
                    " (jira_id, integ_id, display_name, account_type, active)" +
                    " VALUES" +
                    " (?,?,?,?,?) " +
                    " ON CONFLICT (integ_id, jira_id) DO UPDATE SET" +
                    " (display_name, account_type, active) = " +
                    " (EXCLUDED.display_name, EXCLUDED.account_type, EXCLUDED.active) " +
                    " WHERE " +
                    " (users.display_name, users.account_type, users.active) " +
                    " IS DISTINCT FROM " +
                    " (EXCLUDED.display_name, EXCLUDED.account_type, EXCLUDED.active)";

            try (PreparedStatement insertUser = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertUser.setObject(i++, user.getJiraId());
                insertUser.setObject(i++, NumberUtils.toInt(user.getIntegrationId()));
                insertUser.setObject(i++, user.getDisplayName());
                insertUser.setObject(i++, user.getAccountType());
                insertUser.setObject(i, user.getActive());

                return insertUser.executeUpdate() > 0;
            }
        })));
    }

    public List<DbJiraUser> listJiraUsers(String company, List<Integer> integrationIds, List<String> displayNames) {
        Validate.notEmpty(displayNames, "Display Names cannot be empty.");
        Validate.notEmpty(integrationIds, "integration Ids cannot be empty.");
        Map<String, Object> params = new HashMap<>();
        params.put("displayNames", displayNames);
        params.put("integIds", integrationIds);

        String where = " WHERE display_name IN (:displayNames) AND integ_id IN (:integIds)";
        String sql = "SELECT * FROM " + company + "." + USERS_TABLE + where;
        return template.query(sql, params, DbJiraIssueConverters.listJiraUsersMapper());
    }

}
