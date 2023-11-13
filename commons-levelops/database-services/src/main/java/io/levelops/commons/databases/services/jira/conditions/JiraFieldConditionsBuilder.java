package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.services.JiraFieldService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.FIELD_SIZE_COLUMNS;
import static java.util.stream.Collectors.groupingBy;

@Service
public class JiraFieldConditionsBuilder {


    private final JiraFieldService jiraFieldService;

    @Autowired
    public JiraFieldConditionsBuilder(JiraFieldService jiraFieldService) {
        this.jiraFieldService = jiraFieldService;
    }

    public void createFieldSizeFilter(Map<String, Map<String, String>> size, List<String> issueTblConditions,
                                      String company, List<String> integrationIds, Map<String, Object> params, String issueTblQualifier) {

        ArrayList<String> keys = new ArrayList<>(size.keySet());
        Map<String, List<DbJiraField>> jiraFieldMap = null;
        try {
            List<DbJiraField> dbJiraFields = getDbJiraFields(company, integrationIds, keys);
            jiraFieldMap = dbJiraFields.stream().collect(groupingBy(DbJiraField::getFieldKey));
        } catch (SQLException e) {
            throw new RuntimeException("Issue while listing jira fields. Reason: " + e.getMessage());
        }

        for (Map.Entry<String, Map<String, String>> sizeEntry : size.entrySet()) {
            String key = sizeEntry.getKey();
            Map<String, String> value = sizeEntry.getValue();
            String ltStr = value.get("$lt");
            String gtStr = value.get("$gt");

            Integer lt = ltStr != null ? Integer.valueOf(ltStr) : null;
            Integer gt = gtStr != null ? Integer.valueOf(gtStr) : null;
            if (lt != null || gt != null) {
                StringBuilder conditionBuilder = new StringBuilder();

                if (DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(key).matches()) {
                    boolean isArray = false;
                    List<DbJiraField> dbJiraFields = jiraFieldMap.get(key);
                    if (CollectionUtils.isNotEmpty(dbJiraFields)) {
                        DbJiraField dbJiraField = dbJiraFields.get(0);
                        if (dbJiraField.getFieldType().equalsIgnoreCase("array")
                                && dbJiraField.getFieldItems().equalsIgnoreCase("string")) {
                            isArray = true;
                        } else if (!dbJiraField.getFieldType().equalsIgnoreCase("string")) {
                            continue;
                        }
                        if (isArray) {
                            String condition="jsonb_array_length(" + issueTblQualifier + "custom_fields->'" + key +"') =";
                            condition = condition + "( select count(*) from jsonb_array_elements_text(" + issueTblQualifier + "custom_fields->'" +key+ "') as custom_value where " ;
                            condition = condition + JiraConditionUtils.getRangeCondition("custom_value", lt, gt, params, issueTblQualifier) +")";
                            if(gt==0){
                                condition="(" + condition + " OR NOT custom_fields ??| array[ '" +sizeEntry.getKey()+ "' ] )";
                            }
                            conditionBuilder.append(condition);
                        } else {
                            key = "custom_fields->>'" + key + "'";
                            String condition= JiraConditionUtils.getRangeCondition(key, lt, gt, params, issueTblQualifier);
                            if(gt==0){
                                condition="(" + condition + " OR NOT custom_fields ??| array[ '" +sizeEntry.getKey()+ "' ] )";
                            }
                            conditionBuilder.append(condition);
                            }
                        issueTblConditions.add(conditionBuilder.toString());
                    }
                } else {
                    conditionBuilder.append(JiraConditionUtils.getRangeCondition(key, lt, gt, params, issueTblQualifier));
                    issueTblConditions.add(conditionBuilder.toString());
                }
            }
        }
    }


    /*
    Check given list of fields present as built in fields or custom fields. Returns first not found field or null if
    all fields are present.
     */
    public String checkFieldsPresent(String company, List<String> integrationIds, List<String> fields) throws SQLException {
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }

        Set<String> jiraFieldKeys = getDbJiraFields(company, integrationIds, fields).stream()
                .map(DbJiraField::getFieldKey).collect(Collectors.toSet());
        for (String field : fields) {
            if (!FIELD_SIZE_COLUMNS.contains(field) && !jiraFieldKeys.contains(field)) {
                return field;
            }
        }
        return null;
    }

    public List<DbJiraField> getDbJiraFields(String company, List<String> integrationIds, List<String> keys) throws SQLException {
        return jiraFieldService.listByFilter(company,
                integrationIds, true, null, null,
                keys, 0, 1000).getRecords();
    }

    public List<String> getMissingFieldsClause(
            Map<JiraIssuesFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
            Map<String, Boolean> missingCustomFields, Map<String, Object> params, String issueTblQualifier) {
        List<String> missingFieldConditions = new ArrayList<>();
        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingFieldConditions.addAll(missingBuiltinFields.entrySet().stream()
                    .map(missingBuiltinField -> {
                        String clause;
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingBuiltinField.getValue());
                        switch (missingBuiltinField.getKey()) {
                            case priority:
                                clause = shouldBeMissing ? " " + issueTblQualifier + "priority = '_UNPRIORITIZED_' " : " " + issueTblQualifier + "priority IS NOT NULL ";
                                break;
                            case status:
                                clause = shouldBeMissing ? " " + issueTblQualifier + "status = '_UNKNOWN_' " : " " + issueTblQualifier + "status IS NOT NULL ";
                                break;
                            case assignee:
                                clause = shouldBeMissing ? " " + issueTblQualifier + "assignee = '_UNASSIGNED_' " : " " + issueTblQualifier + "assignee_id IS NOT NULL ";
                                break;
                            case reporter:
                                clause = shouldBeMissing ? " " + issueTblQualifier + "reporter = '_UNKNOWN_' " : " " + issueTblQualifier + "reporter_id IS NOT NULL ";
                                break;
                            case component:
                                clause = " array_length(" + issueTblQualifier + "components, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case label:
                                clause = " array_length(" + issueTblQualifier + "labels, 1) IS " + (shouldBeMissing ? " NULL " : "NOT NULL ");
                                break;
                            case fix_version:
                                clause = " array_length(" + issueTblQualifier + "fix_versions, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case version:
                                clause = " array_length(" + issueTblQualifier + "versions, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case epic:
                                clause = " " + issueTblQualifier + "epic IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case project:
                                clause = " " + issueTblQualifier + "project IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case first_assignee:
                                clause = " " + issueTblQualifier + "first_assignee_id IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            default:
                                return null;
                        }
                        return clause;
                    })
                    .collect(Collectors.toList()));
        }
        if (MapUtils.isNotEmpty(missingCustomFields)) {
            List<String> emptyFields = new ArrayList<>();
            List<String> nonEmptyFields = new ArrayList<>();
            missingCustomFields.forEach((field, shouldBeMissing) -> {
                final String fieldStr = "'" + field + "'";
                if (shouldBeMissing) {
                    emptyFields.add(fieldStr);
                } else {
                    nonEmptyFields.add(fieldStr);
                }
            });
            if (CollectionUtils.isNotEmpty(emptyFields)) {
                missingFieldConditions.add("not " + issueTblQualifier + "custom_fields ??| array[" + String.join(",", emptyFields) + "]");
            }
            if (CollectionUtils.isNotEmpty(nonEmptyFields)) {
                missingFieldConditions.add(issueTblQualifier + "custom_fields ??& array[" + String.join(",", nonEmptyFields) + "]");
            }
        }
        return missingFieldConditions;
    }

}
