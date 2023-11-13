package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.database.jira.DbJiraField;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.JiraIssueService.PARTIAL_MATCH_ARRAY_COLUMNS;
import static io.levelops.commons.databases.services.JiraIssueService.PARTIAL_MATCH_COLUMNS;
import static java.util.stream.Collectors.groupingBy;

@Log4j2
@Service
public class JiraPartialMatchConditionsBuilder {

    NamedParameterJdbcTemplate template;
    private final JiraFieldConditionsBuilder fieldConditionsBuilder;
    private final JiraCustomFieldConditionsBuilder customFieldConditionsBuilder;

    @Autowired
    public JiraPartialMatchConditionsBuilder(DataSource dataSource,
                                             JiraFieldConditionsBuilder fieldConditionsBuilder,
                                             JiraCustomFieldConditionsBuilder customFieldConditionsBuilder) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.fieldConditionsBuilder = fieldConditionsBuilder;
        this.customFieldConditionsBuilder = customFieldConditionsBuilder;
    }

    public void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap,
                                         List<String> issueTblConditions, List<String> sprintTableConditions,
                                         Map<String, Object> params, String company, List<String> integrationIds, String issueTableQualifier) {
        ArrayList<String> keys = new ArrayList<>(partialMatchMap.keySet());
        Map<String, List<DbJiraField>> jiraFieldMap = null;
        try {
            List<DbJiraField> dbJiraFields = fieldConditionsBuilder.getDbJiraFields(company, integrationIds, keys);
            jiraFieldMap = dbJiraFields.stream().collect(groupingBy(DbJiraField::getFieldKey));
        } catch (SQLException e) {
            throw new RuntimeException("Issue while listing jira fields. Reason: " + e.getMessage());
        }

        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (StringUtils.firstNonEmpty(begins, ends, contains) != null) {
                if (DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(key).matches()) {
                    List<DbJiraField> dbJiraFields = jiraFieldMap.get(key);
                    if (CollectionUtils.isNotEmpty(dbJiraFields)) {
                        DbJiraField dbJiraField = dbJiraFields.get(0);
                        if (dbJiraField.getFieldType().equalsIgnoreCase(DbJiraField.TYPE_ARRAY)
                                || customFieldConditionsBuilder.isCustomFieldWithDelimiter(company, key, integrationIds)) {
                            key = "custom_fields->'" + key + "'";
                            createPartialMatchConditionJsonArray(issueTblConditions, params, key, begins, ends, contains, issueTableQualifier);
                        } else if (dbJiraField.getFieldType().equalsIgnoreCase(DbJiraField.TYPE_STRING)
                                || dbJiraField.getFieldType().equalsIgnoreCase(DbJiraField.TYPE_NUMBER)
                                || dbJiraField.getFieldType().equalsIgnoreCase(DbJiraField.TYPE_OPTION)) {
                            key = "custom_fields->>'" + key + "'";
                            createPartialMatchCondition(issueTblConditions, sprintTableConditions, params, key, begins, ends, contains, issueTableQualifier);
                        }
                    }
                } else {
                    if (PARTIAL_MATCH_ARRAY_COLUMNS.contains(key)) {
                        createPartialMatchConditionArray(issueTblConditions, params, key, begins, ends, contains, issueTableQualifier);
                    } else if (PARTIAL_MATCH_COLUMNS.contains(key)) {
                        createPartialMatchCondition(issueTblConditions, sprintTableConditions, params, key, begins, ends, contains, issueTableQualifier);
                    }
                }
            }
        }
    }

    private void createPartialMatchConditionJsonArray(List<String> issueTblConditions, Map<String, Object> params,
                                                      String key, String begins, String ends, String contains, String issueTblQualifier) {
        key = issueTblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_begins )";
            params.put(keyName + "_begins", begins + "%");
            issueTblConditions.add(beingsCondition);
        }

        if (ends != null) {
            String endsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_ends )";
            params.put(keyName + "_ends", "%" + ends);
            issueTblConditions.add(endsCondition);
        }

        if (contains != null) {
            String containsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_contains )";
            params.put(keyName + "_contains", "%" + contains + "%");
            issueTblConditions.add(containsCondition);
        }
    }

    private void createPartialMatchConditionArray(List<String> issueTblConditions, Map<String, Object> params,
                                                  String key, String begins, String ends, String contains, String issueTblQualifier) {
        key = issueTblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_begins )";
            params.put(keyName + "_begins", begins + "%");
            issueTblConditions.add(beingsCondition);
        }

        if (ends != null) {
            String endsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_ends )";
            params.put(keyName + "_ends", "%" + ends);
            issueTblConditions.add(endsCondition);
        }

        if (contains != null) {
            String containsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_contains )";
            params.put(keyName + "_contains", "%" + contains + "%");
            issueTblConditions.add(containsCondition);
        }
    }

    private void createPartialMatchCondition(List<String> issueTblConditions, List<String> sprintTableConditions,
                                             Map<String, Object> params, String key, String begins, String ends, String contains, String issueTblQualifier) {
        key = issueTblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        boolean isSprintName = key.equalsIgnoreCase("sprint_name") || key.equalsIgnoreCase("sprint_full_name") || key.equalsIgnoreCase("sprint_full_names");
        if (begins != null) {
            String beingsCondition = key + " SIMILAR TO :" + keyName + "_begins ";
            params.put(keyName + "_begins", begins + "%");
            if (isSprintName) {
                beingsCondition = "name SIMILAR TO :" + keyName + "_begins ";
                sprintTableConditions.add(beingsCondition);
            } else {
                issueTblConditions.add(beingsCondition);
            }
        }

        if (ends != null) {
            String endsCondition = key + " SIMILAR TO :" + keyName + "_ends ";
            params.put(keyName + "_ends", "%" + ends);
            if (isSprintName) {
                endsCondition = "name SIMILAR TO :" + keyName + "_ends ";
                sprintTableConditions.add(endsCondition);
            } else {
                issueTblConditions.add(endsCondition);
            }
        }

        if (contains != null) {
            String containsCondition = key + " SIMILAR TO :" + keyName + "_contains ";
            params.put(keyName + "_contains", "%" + contains + "%");
            if (isSprintName) {
                containsCondition = "name SIMILAR TO :" + keyName + "_contains ";
                sprintTableConditions.add(containsCondition);
            } else {
                issueTblConditions.add(containsCondition);
            }
        }
    }

}
