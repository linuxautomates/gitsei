package io.levelops.commons.databases.services.jira.conditions;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraCustomFieldConditionsBuilder {

    NamedParameterJdbcTemplate template;
    private final JiraFieldService jiraFieldService;
    private final IntegrationService configService;

    @Autowired
    public JiraCustomFieldConditionsBuilder(DataSource dataSource,
                                            JiraFieldService jiraFieldService,
                                            IntegrationService configService) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.jiraFieldService = jiraFieldService;
        this.configService = configService;
    }

    @SuppressWarnings("unchecked")
    public void createCustomFieldConditions(String company,
                                            Map<String, Object> params,
                                            String paramPrefix,
                                            List<String> integrationIds,
                                            Map<String, Object> customFields,
                                            List<String> issueTblConditions,
                                            boolean include,
                                            String issueTblQualifier) {
        createCustomFieldConditions(company, params, paramPrefix, integrationIds, customFields, issueTblConditions, include, issueTblQualifier, List.of());
    }
    public void createCustomFieldConditions(String company,
                                            Map<String, Object> params,
                                            String paramPrefix,
                                            List<String> integrationIds,
                                            Map<String, Object> customFields,
                                            List<String> issueTblConditions,
                                            boolean include,
                                            String issueTblQualifier,
                                            List<String> customStacks) {
        createCustomFieldConditions(company, params, paramPrefix, integrationIds, customFields, issueTblConditions, include, issueTblQualifier, List.of(),false);

    }

    @SuppressWarnings("unchecked")
    public void createCustomFieldConditions(String company,
                                            Map<String, Object> params,
                                            String paramPrefix,
                                            List<String> integrationIds,
                                            Map<String, Object> customFields,
                                            List<String> issueTblConditions,
                                            boolean include,
                                            String issueTblQualifier,
                                            List<String> customStacks, boolean stacking) {
        int fieldNumber = 0;
        for (String key : customFields.keySet()) {
            if (StringUtils.isEmpty(key)
                    || !DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(key).matches()) {
                continue;
            }
            String fieldRef = paramPrefix + (include ? "customfield" : "not_customfield") + fieldNumber;
            String customFieldType = "";
            try {
                customFieldType = getCustomFieldType(company, key, integrationIds);
            } catch (SQLException e) {
                log.error("SQL exception while querying jira fields");
            }

            if (customFields.get(key) instanceof Map && isRangeFilterSupported(customFieldType)) {
                Map<String, String> timeRange = Map.class.cast(customFields.get(key));
                final Long timeRangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
                final Long timeRangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;

                if (timeRangeStart != null) {
                    fieldRef = fieldRef + "_start";
                    String condition = "(" + issueTblQualifier + "custom_fields->>'" + key + "')::float8 >= :" + fieldRef;
                    issueTblConditions.add(condition);
                    if (isDateCustomField(customFieldType)) {
                        params.put(fieldRef, TimeUnit.SECONDS.toMillis(timeRangeStart));
                    } else {
                        params.put(fieldRef, timeRangeStart);
                    }
                }

                if (timeRangeEnd != null) {
                    fieldRef = fieldRef + "_end";
                    String condition = "(" + issueTblQualifier + "custom_fields->>'" + key + "')::float8 <= :" + fieldRef;
                    issueTblConditions.add(condition);
                    if (isDateCustomField(customFieldType)) {
                        params.put(fieldRef, TimeUnit.SECONDS.toMillis(timeRangeEnd));
                    } else {
                        params.put(fieldRef, timeRangeEnd);
                    }
                }

            } else if (customFields.get(key) instanceof List) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;

                if (customFieldType.equalsIgnoreCase("array")) {
                    List<String> values = List.class.cast(customFields.get(key));
                    if (CollectionUtils.isEmpty(values)) {
                        continue;
                    }
                    String condition = (include ? " " : " ( NOT ")
                            + issueTblQualifier + "custom_fields->'" + key + "' @> ANY(ARRAY[ :";
                    if(stacking)
                         fieldRef = fieldRef + "sval" + (valNum);
                    else
                        fieldRef = fieldRef + "val" + (valNum);
                    conditionBuilder.append(condition).append(fieldRef).append(" ]::jsonb[])");
                    if (!include) {
                        // if custom stacks is the same as the key then do not include tickets without the custom_field
                        // because we are interested in tickets with the field only..
                        if (CollectionUtils.isEmpty(customStacks) || !customStacks.contains(key)) {
                            conditionBuilder.append(" OR NOT " + issueTblQualifier + "custom_fields ?? '").append(key).append("' ");
                        }
                        conditionBuilder.append(") ");
                    }
                    params.put(fieldRef, values.stream().map(val -> "[\"" + StringEscapeUtils.escapeJson(val) + "\"]")
                            .collect(Collectors.toList()));
                } else {
                    List<String> values = List.class.cast(customFields.get(key));
                    if (CollectionUtils.isEmpty(values)) {
                        continue;
                    }
                    String condition = (include ? "" : "NOT ") + " " + issueTblQualifier + "custom_fields @> :";
                    for (String value : values) {
                        if(stacking)
                            fieldRef = fieldRef + "sval" + (valNum++);
                        else
                            fieldRef = fieldRef + "val" + (valNum++);
                        if (valNum > 1) {
                            conditionBuilder.append(" OR ");
                        }
                        conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                        if (List.of("datetime", "date").contains(customFieldType)) {
                            params.put(fieldRef, "{\"" + key + "\":" + value + "}");
                        } else {
                            params.put(fieldRef, "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}");
                        }
                    }
                }
                issueTblConditions.add(conditionBuilder.append(")").toString());
            }
            fieldNumber += 1;
        }
    }

    private static boolean isRangeFilterSupported(String customFieldType) {
        return customFieldType.equalsIgnoreCase("datetime")
                || customFieldType.equalsIgnoreCase("date") || customFieldType.equalsIgnoreCase("number");
    }

    private static boolean isDateCustomField(String customFieldType) {
        return customFieldType.equalsIgnoreCase("datetime")
                || customFieldType.equalsIgnoreCase("date");
    }

    public String getCustomFieldType(String company, String customField, List<String> integrationIds) throws SQLException {
        final DbListResponse<DbJiraField> response = jiraFieldService.listByFilter(company,
                integrationIds, true, null, null,
                List.of(customField), 0, 1);
        final Optional<DbJiraField> dbJiraFieldOpt = response.getCount() > 0 ?
                response.getRecords().stream().findFirst() : Optional.empty();
        if (dbJiraFieldOpt.isEmpty()) {
            return "";
        }
        if ("array".equalsIgnoreCase(dbJiraFieldOpt.get().getFieldType())
                || isCustomFieldWithDelimiter(company, customField, integrationIds)) {
            return "array";
        }

        return dbJiraFieldOpt.get().getFieldType();
    }

    public boolean isCustomFieldWithDelimiter(String company, String customField, List<String> integrationIds) {
        Set<String> customArrayFields = ListUtils.emptyIfNull(configService
                        .listConfigs(company, integrationIds, 0, 10000)
                        .getRecords())
                .stream()
                .map(IntegrationConfig::getConfig)
                .filter(Objects::nonNull)
                .map(conf -> conf.get("agg_custom_fields"))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(configEntry -> StringUtils.isNotEmpty(configEntry.getDelimiter()))
                .map(IntegrationConfig.ConfigEntry::getKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        return customArrayFields.contains(customField);
    }

}
