package io.levelops.commons.databases.models.database.jira.parsers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.jira.converters.JiraCustomFieldConverter;
import io.levelops.integrations.jira.models.JiraIssue;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.database.jira.DbJiraField.CUSTOM_TYPE_TEAM;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.TYPE_DATE;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.TYPE_DATETIME;
import static io.levelops.commons.databases.models.database.jira.DbJiraField.TYPE_OPTION_WITH_CHILD;

@Log4j2
public class JiraIssueCustomFieldParser {

    public static Map<String, Object> parseCustomFields(JiraIssue jiraIssue,
                                                        @Nullable List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                                        @Nullable List<DbJiraField> customFieldProperties) {
        Map<String, Object> customFields = new HashMap<>();

        if (CollectionUtils.isEmpty(customFieldConfig)
                || MapUtils.isEmpty(jiraIssue.getFields().getDynamicFields())) {
            return customFields;
        }
        for (IntegrationConfig.ConfigEntry entry : customFieldConfig) {
            if (entry.getKey() == null) {
                continue;
            }

            Object parsedValue = parseCustomField(jiraIssue, customFieldProperties, entry);
            if (parsedValue != null) {
                customFields.put(entry.getKey(), parsedValue);
            }
        }

        return customFields;
    }


    @Nullable
    public static Object parseCustomField(JiraIssue jiraIssue,
                                          @Nullable List<DbJiraField> customFieldProperties,
                                          IntegrationConfig.ConfigEntry entry) {

        Optional<DbJiraField> fieldOpt = ListUtils.emptyIfNull(customFieldProperties)
                .stream()
                .filter(c -> c.getFieldKey().equalsIgnoreCase(entry.getKey()))
                .findFirst();
        log.debug("field = {}", fieldOpt);

        Object val = jiraIssue.getFields().getDynamicFields().get(entry.getKey());
        log.debug("val = {}", val);
        if (val == null) {
            return null;
        }

        // -- try to handle special cases first
        if (fieldOpt.isPresent()) {
            DbJiraField field = fieldOpt.get();
            String fieldType = StringUtils.trimToEmpty(field.getFieldType()).toLowerCase();
            log.debug("field.get().getFieldType() = {}", fieldType);

            switch (fieldType) {
                case TYPE_DATE:
                    try {
                        return JiraCustomFieldConverter.parseDate(val);
                    } catch (ParseException e) {
                        log.debug("Unable to parse date field: '{}'", val, e);
                    }
                    break;
                case TYPE_DATETIME:
                    try {
                        return JiraCustomFieldConverter.parseDateTime(val);
                    } catch (ParseException e) {
                        log.debug("Unable to parse datetime field: '{}'", val, e);
                    }
                    break;
                case TYPE_OPTION_WITH_CHILD:
                    try {
                        return JiraCustomFieldConverter.parseOptionWithChild((Map<?, ?>) val);
                    } catch (JsonProcessingException e) {
                        log.debug("Unable to parse option-with-child field: '{}'", val, e);
                    }
                    break;
                case CUSTOM_TYPE_TEAM:
                    try {
                        return JiraCustomFieldConverter.parseTeam((Map<?, ?>) val);
                    } catch (Exception e) {
                        log.debug("Unable to parse custom team field: '{}'", val, e);
                    }
                    break;
            }
        }

        // -- parse lists
        if (val instanceof List && !((List<?>) val).isEmpty()) {
            log.debug("True - val instanceof List && !((List) val).isEmpty()");
            return ((List<?>) val).stream()
                    .map(JiraCustomFieldConverter::parseValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // -- parse delimiters
        log.debug("False - val instanceof List && !((List) val).isEmpty()");
        if (StringUtils.isNotEmpty(entry.getDelimiter())) {
            //For Custom Field with delimitation, parse field without truncation
            String value = JiraCustomFieldConverter.parseValueWithoutTruncation(val);
            if (StringUtils.isEmpty(value)) {
                return null;
            }
            switch (entry.getDelimiter()) {
                case "html_list":
                    Document doc = Jsoup.parse(value);
                    Elements list = doc.select("body > ul > li");
                    return list.stream()
                            .map(element -> element.ownText())
                            .collect(Collectors.toList());
                default:
                    return Stream.of(value.split(entry.getDelimiter()))
                            .map(JiraCustomFieldConverter::parseValue)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            }
        }

        // -- catch all parsing
        String value = JiraCustomFieldConverter.parseValue(val);
        log.debug("value = {}", value);
        return value;
    }

}
