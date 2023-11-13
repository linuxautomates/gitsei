package io.levelops.commons.databases.utils;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TeamUtils {

    /**
     * Adds users condition based on the {@param ids}. If {@param ids} user ids and team ids, the generated
     * condition would have the following format
     *
     *   users in (:userIds) OR users in (:userid derived from the teams)
     *
     * @param company          company name
     * @param tableConditions  Condition is appended to the list
     * @param params           Named key/value map for sql params
     * @param column           Name of the column
     * @param columnParamName  named parameter key name
     * @param isArray          Specify whether column is of type array or not
     * @param ids              Filter values containing user ids and/or team ids
     * @param applicationTypes application type for given integrations
     */
    public static void addUsersCondition(String company, List<String> tableConditions, Map<String, Object> params,
                                         String column, String columnParamName, boolean isArray, List<String> ids,
                                         List<String> applicationTypes) {
        addUsersCondition(company, tableConditions, params, column, columnParamName, isArray, ids, applicationTypes, false);
    }

    public static void addUsersCondition(String company, List<String> tableConditions, Map<String, Object> params,
                                         String column, String columnParamName, boolean isArray, List<String> ids,
                                         List<String> applicationTypes, boolean excludeUsers) {
        Map<Boolean, List<String>> teamIdUsersSeg
                = ids.stream().collect(Collectors.groupingBy(assignee -> assignee.startsWith("team_id:")));
        List<String> teamIds = teamIdUsersSeg.get(true);
        List<String> userIds = teamIdUsersSeg.get(false);

        String usersCondition = getUsersCondition(userIds, params, column, columnParamName, isArray, excludeUsers);
        String teamUsersCondition = getTeamUsersCondition(company, teamIds, params, column, columnParamName,
                isArray, applicationTypes, excludeUsers);

        if (StringUtils.isNotEmpty(usersCondition) && StringUtils.isNotEmpty(teamUsersCondition)) {
            tableConditions.add("( " + usersCondition + (excludeUsers ? " AND " : " OR ") + teamUsersCondition + ") ");
        } else if (StringUtils.isNotEmpty(teamUsersCondition)) {
            tableConditions.add(teamUsersCondition);
        } else {
            tableConditions.add(usersCondition);
        }
    }

    private static String getUsersCondition(List<String> userIds, Map<String, Object> params,
                                            String column, String columnParamName, boolean isArray,
                                            boolean excludeUsers) {
        String usersCondition = "";
        if (CollectionUtils.isNotEmpty(userIds)) {
            if (isArray) {
                usersCondition = (excludeUsers ? " NOT " : "") + column + " && ARRAY[ :" + columnParamName + "" + " ]::varchar[] ";
            } else {
                usersCondition = column + (excludeUsers ? " NOT " : "") + " IN (:" + columnParamName + "" + ")";
                //usersCondition = excludeUsers ? "("+usersCondition+" OR "+column+" IS NULL )" : usersCondition;
            }
                params.put(columnParamName, userIds);
            }
        return usersCondition;
    }

    private static String getTeamUsersCondition(String company, List<String> teamIds, Map<String, Object> params,
                                                String column, String columnNameParam, boolean isArray, List<String> applicationTypes,
                                                boolean excludeUsers) {
        String teamUsersCondition = "";
        if (CollectionUtils.isNotEmpty(teamIds)) {
            Set<UUID> teamIdSet = teamIds.stream().filter(assignee -> assignee.startsWith("team_id"))
                    .map(teamId -> teamId.split(":")[1])
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());

            if (CollectionUtils.isNotEmpty(teamIds)) {
                if (isArray) {
                    teamUsersCondition = (excludeUsers ? " NOT " : "") + column + " && " + getUsersFromTeamAsArray(company, column, columnNameParam, teamIdSet,
                            applicationTypes, params);
                } else {
                    teamUsersCondition = (excludeUsers ? " NOT " : "") + column + " = ANY(" + getUsersFromTeamAsArray(company, column, columnNameParam, teamIdSet,
                            applicationTypes, params) + ")";
                }

            }
        }
        return teamUsersCondition;
    }

    private static String getUsersFromTeamAsArray(String company, String column, String columnNameParam, Set<UUID> teamIds,
                                                  List<String> applicationTypes, Map<String, Object> params) {
        String selectUsers = " SELECT iu.cloud_id "
                + "FROM   " + company + ".teams "
                + " INNER JOIN " + company + ".team_membership teammsp ON teammsp.team_id = teams.id " +
                " AND teams.id IN (:" + columnNameParam + "_team_ids) "
                + " INNER JOIN " + company + ".team_members teamm ON teammsp.team_member_id = teamm.id "
                + " INNER JOIN " + company + ".team_member_usernames teammu ON teammu.team_member_id = teamm.id "
                + " INNER JOIN " + company + ".integration_users iu ON teammu.integration_user_id = iu.id"
                + " INNER JOIN " + company + ".integrations ON iu.integration_id = integrations.id " +
                " AND integrations.application IN (:applicationType) ";

        String selectUsersAsArray = "ARRAY(" + selectUsers + ")";
        params.put(columnNameParam + "_team_ids", teamIds);
        params.put("applicationType", applicationTypes);
        return selectUsersAsArray;
    }
}
