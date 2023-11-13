package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TestRailsCustomCaseFieldConditionsBuilder {

    private final TestRailsCaseFieldDatabaseService caseFieldDatabaseService;
    private final List<CaseField.FieldType> stringCaseFieldTypes = List.of(CaseField.FieldType.SCENARIO,
            CaseField.FieldType.STEPS,
            CaseField.FieldType.STRING,
            CaseField.FieldType.TEXT,
            CaseField.FieldType.DROPDOWN,
            CaseField.FieldType.USER,
            CaseField.FieldType.URL);

    @Autowired
    public TestRailsCustomCaseFieldConditionsBuilder(TestRailsCaseFieldDatabaseService caseFieldDatabaseService) {
        this.caseFieldDatabaseService = caseFieldDatabaseService;
    }

    public void createCustomCaseFieldConditions(String company,
                                                Map<String, Object> params,
                                                String paramPrefix,
                                                List<String> integrationIds,
                                                Map<String, Object> customCaseFields,
                                                List<String> criteriaConditions,
                                                boolean include) {

        int fieldNumber = 0;
        Map<String, CaseField.FieldType> customFieldTypes = new HashMap<>();
        try {
            customFieldTypes = getCustomCaseFieldsType(company, new ArrayList<>(customCaseFields.keySet()), integrationIds);
        } catch (SQLException e) {
            log.error("SQL exception while querying testrails custom case fields");
            throw new RuntimeException(e);
        } catch (BadRequestException e) {
            throw new RuntimeException(e);
        }
        for (String key : customCaseFields.keySet()) {
            if (StringUtils.isEmpty(key) || !key.startsWith("custom_")) {
                continue;
            }
            String fieldRef = paramPrefix + (include ? "custom" : "not_custom") + fieldNumber;
            CaseField.FieldType customFieldType = customFieldTypes.get(key);
            if(customFieldType == null){
                throw new RuntimeException("Failed to get type of custom case field for " + key);
            }
            if (customFieldType.equals(CaseField.FieldType.CHECKBOX)) {
                Boolean booleanValue = Boolean.valueOf(customCaseFields.get(key).toString());
                fieldRef = fieldRef + "_boolean" + fieldNumber;
                StringBuilder conditionBuilder = new StringBuilder("(");
                String condition = (include ? booleanValue ? "" : "(" : " ( NOT ") + "custom_case_fields @> :" + fieldRef + " :: jsonb";
                conditionBuilder.append(condition);
                if (!include || Boolean.FALSE.equals(booleanValue)) {
                    conditionBuilder.append(" OR NOT custom_case_fields ?? '").append(key).append("' ");
                    conditionBuilder.append(")");
                }
                conditionBuilder.append(")");
                criteriaConditions.add(conditionBuilder.toString());
                params.put(fieldRef, "{\"" + key + "\":" + booleanValue + "}");
            } else if (customCaseFields.get(key) instanceof List && (customFieldType.equals(CaseField.FieldType.INTEGER) || customFieldType.equals(CaseField.FieldType.MILESTONE))) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;
                List<String> stringValues = List.class.cast(customCaseFields.get(key));
                List<Integer> values = stringValues.stream().map(Integer::parseInt).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                String condition = (include ? "" : "NOT ") + "custom_case_fields @> :";
                for (int intValue : values) {
                    fieldRef = fieldRef + "_intVal" + (valNum++);
                    if (valNum > 1) {
                        conditionBuilder.append(include ? " OR " : " AND ");
                    }
                    conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                    params.put(fieldRef, "{\"" + key + "\":" + intValue + "}");
                }
                criteriaConditions.add(conditionBuilder.append(")").toString());
            } else if (customCaseFields.get(key) instanceof Map && customFieldType.equals(CaseField.FieldType.DATE)) {
                Map<String, String> timeRange = Map.class.cast(customCaseFields.get(key));
                final Long timeRangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
                final Long timeRangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;

                if (timeRangeStart != null) {
                    fieldRef = fieldRef + "_start";
                    String condition = "TO_DATE(custom_case_fields->>'" + key + "', 'MM/DD/YYYY') >= to_timestamp(:" + fieldRef + ")";
                    criteriaConditions.add(condition);
                    params.put(fieldRef, TimeUnit.SECONDS.toSeconds(timeRangeStart));
                }

                if (timeRangeEnd != null) {
                    fieldRef = fieldRef + "_end";
                    String condition = "TO_DATE(custom_case_fields->>'" + key + "', 'MM/DD/YYYY') <= to_timestamp(:" + fieldRef + ")";
                    criteriaConditions.add(condition);
                    params.put(fieldRef, TimeUnit.SECONDS.toSeconds(timeRangeEnd));
                }
            } else if (customCaseFields.get(key) instanceof List && customFieldType.equals(CaseField.FieldType.MULTI_SELECT)) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;
                List<String> values = List.class.cast(customCaseFields.get(key));
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                String condition = (include ? "" : " ( NOT ") + "custom_case_fields->'" + key + "' @> ANY(ARRAY[ :";
                fieldRef = fieldRef + "_val" + (valNum);
                conditionBuilder.append(condition).append(fieldRef).append(" ]::jsonb[])");
                if (!include) {
                    conditionBuilder.append(" OR NOT custom_case_fields ?? '").append(key).append("' ");
                    conditionBuilder.append(")");
                }
                params.put(fieldRef, values.stream().map(val -> "[\"" + StringEscapeUtils.escapeJson(val) + "\"]")
                        .collect(Collectors.toList()));
                conditionBuilder.append(")");
                criteriaConditions.add(conditionBuilder.toString());
            } else if (customCaseFields.get(key) instanceof List && stringCaseFieldTypes.contains(customFieldType)) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;
                List<String> values = List.class.cast(customCaseFields.get(key));
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                String condition = (include ? "" : "NOT ") + "custom_case_fields @> :";
                for (String value : values) {
                    fieldRef = fieldRef + "_val" + (valNum++);
                    if (valNum > 1) {
                        conditionBuilder.append(include ? " OR " : " AND ");
                    }
                    conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                    params.put(fieldRef, "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}");
                }
                criteriaConditions.add(conditionBuilder.append(")").toString());
            }
            fieldNumber += 1;
        }
    }

    public Map<String, CaseField.FieldType> getCustomCaseFieldsType(String company, List<String> customCaseFieldKeys, List<String> integrationIds) throws SQLException, BadRequestException {
        final DbListResponse<DbTestRailsCaseField> response = caseFieldDatabaseService.listByFilter(company, TestRailsCaseFieldFilter.builder().integrationIds(integrationIds).systemNames(customCaseFieldKeys).needAssignedFieldsOnly(true).isActive(true).build(), 0, customCaseFieldKeys.size());

        return response.getRecords().stream().collect(Collectors.toMap(DbTestRailsCaseField::getSystemName, value -> CaseField.FieldType.fromString(value.getType())));
    }
}
