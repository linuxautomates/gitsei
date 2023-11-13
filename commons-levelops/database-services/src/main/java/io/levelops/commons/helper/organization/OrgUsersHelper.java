package io.levelops.commons.helper.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.database.FilterConditionParser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Log4j2
public class OrgUsersHelper {

    private final OrgUsersDatabaseService usersService;
    private final OrgVersionsDatabaseService versionsService;
    private final OrgUsersLockService orgUsersLockService;

    public OrgUsersHelper(
            final OrgUsersDatabaseService usersService,
            final OrgVersionsDatabaseService versionsService,
            final OrgUsersLockService orgUsersLockService) {
        this.usersService = usersService;
        this.versionsService = versionsService;
        this.orgUsersLockService = orgUsersLockService;
    }

    // NOTE: This function assumes that any org -> integration user mappings that have been created were manually added
    public Set<Integer> insertNewVersionUsers(final String company, final Stream<DBOrgUser> users) {
        var userIds = new HashSet<Integer>();
        var errors = new HashSet<String>();
        if (!orgUsersLockService.lock(company, 1)) {
            throw new RuntimeException("Failed to obtain org user lock for tenantId=" + company);
        }
        try {
            // create new version
            var versionId = versionsService.insert(company, OrgAssetType.USER);
            // insert users 
            users.filter(user -> user != null).forEach(user -> {
                try {
                    userIds.add(Integer.valueOf(usersService.insert(company, user)));
                } catch (SQLException e) {
                    errors.add(user.getEmail());
                    log.error("Error encountered when inserting user {}. [{}] ", user, company, e);
                }
            });
            // activate new version only if there are no errors
            if (errors.size() < 1) {
                activateVersion(company, versionId);
            } else {
                log.error("Not activating new user version for company {} due " +
                        "to errors encountered when inserting users: {}", company, errors);
            }
            // TODO: return errors
        } catch (SQLException e) {
            log.error("[{}] ", company, e);
        } finally {
            orgUsersLockService.unlock(company);
        }
        return userIds;
    }

    public void updateUsers(final String company, final Stream<DBOrgUser> users) {
        if (!orgUsersLockService.lock(company, 1)) {
            throw new RuntimeException("Failed to obtain org user lock for tenantId=" + company);
        }
        try {

            // create new version
            var versionId = versionsService.insert(company, OrgAssetType.USER);
            // set of ref_ids for the new entries, these users will already have the new directory version and so, 
            // the old records matching this ref_ids should not be updated to be included in the new version of the directory
            final var excludeRefIds = new HashSet<Integer>();
            // insert users
            users.forEach(user -> {
                try {
                    excludeRefIds.add(Integer.valueOf(usersService.insert(company, user)));
                } catch (SQLException e) {
                    log.error("[{}] ", company, e);
                }
            });
            // update other users
            usersService.upgradeUsersVersion(company, versionId, excludeRefIds);
            // activate new version
            activateVersion(company, versionId);
        } catch (SQLException e) {
            log.error("[{}] ", company, e);
        } finally {
            orgUsersLockService.unlock(company);
        }
    }

    public void deleteUsers(final String company, Set<Integer> userIdsToDelete) {
        if (!orgUsersLockService.lock(company, 1)) {
            throw new RuntimeException("Failed to obtain org user lock for tenantId=" + company);
        }
        try {
            // create new version
            var versionId = versionsService.insert(company, OrgAssetType.USER);
            // add new version to the users from the current active version excluding the deleted users
            usersService.upgradeUsersVersion(company, versionId, userIdsToDelete);
            // activate new version
            activateVersion(company, versionId);
        } catch (SQLException e) {
            log.error("[{}] ", company, e);
        } finally {
            orgUsersLockService.unlock(company);
        }
    }

    public void activateVersion(final String company, final UUID versionId) throws SQLException {
        var activeVersion = versionsService.getActive(company, OrgAssetType.USER);
        var updateNewVersionResult = versionsService.update(company, versionId, true);
        log.info("Activated user version {}, result {}", versionId, updateNewVersionResult);
        if (activeVersion.isPresent()) {
            var updatedResult = versionsService.update(company, activeVersion.get().getId(), false);
            log.info("Deactivate old version {}, result {}", activeVersion.get().getVersion(), updatedResult);
        }
        log.info("Activated user version {} for company {}", versionId, company);
    }

    public void activateVersion(final String company, final Integer version) throws SQLException {
        var activeVersion = versionsService.getActive(company, OrgAssetType.USER);
        var updateNewVersionResult = versionsService.update(company, OrgAssetType.USER, version, true);
        log.info("Activated user version {}, result {}", version, updateNewVersionResult);
        if (activeVersion.isPresent()) {
            var updatedResult = versionsService.update(company, activeVersion.get().getId(), false);
            log.info("Deactivate old version {}, result {}", activeVersion.get().getVersion(), updatedResult);
        }
        log.info("Activated user version {} for company {}", version, company);
    }

    /**
     * Returns a query to get id and ref_id for the users that match the conditions passed in the map. <br /><br />
     * The conditions must be exact match at this point.<br /><br />
     * <br />
     * <br />
     * Supports exact match and the following special matchers:
     * <ul>
     *  <li>$begins</li>
     *  <li>$ends</li>
     *  <li>$contains</li>
     *  <li>$age</li>
     *  <li>$gt</li>
     *  <li>$gte</li>
     *  <li>$lt</li>
     *  <li>$lte</li>
     * </ul>
     * <br />
     * <br />
     * Examples:<br /><br />
     * <br /><br />
     * <code>{"email": "user@domain.com", "custom_field_location": "USA"}</code>
     * <code>{"email": "user@domain.com", "custom_field_designation": {"$begins": "Senior"}, "custom_field_deparment": {"$contains": "marketing"}}</code>
     *
     * @param company
     * @param userSelectors conditions to select the users, this can be mandatory fields like "email" and "full_name" or anything in the custom fields (defined by the schema).
     * @param params        Map where the variables for the query will be placed
     * @return the query to get (id, ref_id, versions) from the org_users table when executed by a {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate}
     * @throws JsonProcessingException
     */
    @SuppressWarnings("unchecked")
    public static String getOrgUsersSelectQuery(final String company, final Map<String, ? extends Object> userSelectors, final Map<String, Object> params) throws JsonProcessingException {
        // strict match
        //   string
        //   numbers
        //   dates
        //      ranges
        //      greater than
        //      age
        //      IN
        //      1:1
        //      timestamp
        //      epoch seconds
        //      format
        //   json
        // partial match
        //   string
        //   json

        // =
        // $gt $lt
        // $age
        // partial_match: {"custom_field": ""}
        // first handle special fields email and full_name

        var conditions = getOrgUsersSelectConditions(company, userSelectors, params);
        var query = MessageFormat.format("SELECT id, ref_id, versions FROM {0}.org_users o_u WHERE {1}", company, String.join(" AND ", conditions));
        return query;
    }

    public static List<String> getOrgUsersSelectConditions(final String company, final Map<String, ? extends Object> userSelectors, final Map<String, Object> params) throws JsonProcessingException {

        var conditions = getConditions(company, userSelectors, params);
        conditions.add(MessageFormat.format("o_u.versions @> ARRAY(SELECT version FROM {0}.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)", company));
        Collections.sort(conditions);
        return conditions;
    }

    public static String getOrgUsersSelectQueryWithOr(final String company, final Map<String, ? extends Object> userSelectors, final Map<String, Object> params) {

        var conditions = getConditions(company, userSelectors, params);
        Collections.sort(conditions);
        var queryConditions = String.join(" OR ", conditions);
        String orConditionStart = "{1}";
        String orConditionEnd = "";
        if (queryConditions != null && !queryConditions.isEmpty()) {
            orConditionStart = " ( {1} ";
            orConditionEnd = " ) AND ";
        }

        queryConditions += MessageFormat.format(orConditionEnd + " o_u.versions @> ARRAY(SELECT version FROM {0}.org_version_counter WHERE type = :org_user_selection_version_type AND active = true) ", company);
        var query = MessageFormat.format("SELECT id, ref_id, versions FROM {0}.org_users o_u WHERE " + orConditionStart, company, queryConditions);
        return query;
    }

    private static ArrayList<String> getConditions(String company, Map<String, ?> userSelectors, Map<String, Object> params) {

        var conditions = new ArrayList<String>();
        params.put("org_user_selection_version_type", "USER");

        Consumer<Map.Entry<String, ? extends Object>> conditionParserNonCustom = selector -> {
            var key = selector.getKey();
            var paramName = "org_user_" + key;
            var paramValue = selector.getValue();
            params.put(paramName, paramValue);
            if (paramValue instanceof String && StringUtils.isNotBlank((String) paramValue)) {
                conditions.add("o_u." + key + " ILIKE :org_user_" + key);
            } else {
                conditions.add("o_u." + key + " IN (:org_user_" + key + ")");
            }
        };

        Consumer<Map.Entry<String, ? extends Object>> conditionParserCustom = selector -> {
            var key = selector.getKey().substring(13);
            var paramName = "org_user_condition_key_" + FilterConditionParser.sanitizeParamName(key);
            var field = MessageFormat.format("o_u.custom_fields->>:{0}", paramName);
            params.put(paramName, key);
            var condition = FilterConditionParser.parseCondition(field, "o_u_c_", selector.getValue(), params);
            if (StringUtils.isNotBlank(condition)) {
                conditions.add(condition);
            }
        };

        Consumer<Map.Entry<String, ? extends Object>> excludeConditionParserCustom = selector -> {
            var key = selector.getKey().substring(13);
            var paramName = "org_user_condition_key_" + FilterConditionParser.sanitizeParamName(key);
            var field = MessageFormat.format("o_u.custom_fields->>:{0}", paramName);
            params.put(paramName, key);
            var condition = FilterConditionParser.parseCondition(field, "o_u_c_", selector.getValue(), params);
            condition = "NOT (" + condition + ")";
            if (StringUtils.isNotBlank(condition)) {
                conditions.add(condition);
            }
        };

        //Non-custom fields like full_name,email
        userSelectors.entrySet()
                .stream()
                .filter(selector -> !selector.getKey().equals("partial_match") && !selector.getKey().startsWith("custom_field_") && !selector.getKey().equals("exclude"))
                .forEach(conditionParserNonCustom);

        //Custom-fields
        userSelectors.entrySet()
                .stream()
                .filter(selector -> selector.getKey().startsWith("custom_field_"))
                .forEach(conditionParserCustom);

        //exclude Custom-fields
        userSelectors.entrySet().stream()
                .filter(k -> k.getKey().startsWith("exclude"))
                .flatMap(k -> ((Map<String, ? extends Object>) k.getValue()).entrySet().stream())
                .filter(selector -> selector.getKey().startsWith("custom_field_"))
                .forEach(excludeConditionParserCustom);

        //partial-match
        var partial = (Map<String, Object>) userSelectors.get("partial_match");
        if (MapUtils.isNotEmpty(partial)) {
            partial.entrySet()
                    .stream()
                    .filter(selector -> selector.getKey().startsWith("custom_field_"))
                    .forEach(conditionParserCustom);

            partial.entrySet()
                    .stream()
                    .filter(selector -> selector.getKey().equals("email") || selector.getKey().equals("full_name"))
                    .forEach(selector -> {
                        String key = selector.getKey();
                        var field = "o_u." + key;
                        var condition = FilterConditionParser.parseCondition(field, "o_u_", partial.get(key), params);
                        if (StringUtils.isNotBlank(condition)) {
                            conditions.add(condition);
                        }
                    });
        }

        return conditions;
    }
}
