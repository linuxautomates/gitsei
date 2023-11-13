package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraSprint;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraSprint.DbJiraSprintBuilder.class)
public class DbJiraSprint {
    public static final Pattern SPRINT_PATTERN = Pattern.compile("\\[(?<VALUE>.*)\\]", Pattern.CASE_INSENSITIVE);
    private static final String[] DATEFORMATS = {"YYYY-MM-DD", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"};
    private static final Pattern VALUE_KEY_PATTERN = Pattern.compile("(?<VALUE>.*),(?<KEY>.*)");
    private static final Pattern VALUE_KEY_PATTERN_EQUALS = Pattern.compile("(?<VALUE>.*?)=(?<KEY>.*)");
    private static final Pattern SPRINT_TOKENIZE_PATTERN = Pattern.compile(",(?<VALUE>[^ ]*?)=");

    @JsonProperty("id")
    String id; // internal id

    @JsonProperty("sprint_id")
    Integer sprintId;

    @JsonProperty("name")
    String name;

    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("state")
    String state;

    @JsonProperty("goal")
    String goal;

    @JsonProperty("start_date")
    Long startDate;

    @JsonProperty("end_date")
    Long endDate;

    @JsonProperty("complete_date")
    Long completedDate;

    @JsonProperty("updated_at")
    @EqualsAndHashCode.Exclude
    Long updatedAt;

    public static DbJiraSprint fromJiraSprint(JiraSprint sprint, String integrationId, Long updatedAt) {
        return DbJiraSprint.builder()
                .sprintId(NumberUtils.toInteger(sprint.getId()))
                .name(sprint.getName())
                .integrationId(NumberUtils.toInteger(integrationId))
                .state(sprint.getState())
                .goal(sprint.getGoal())
                .startDate(parseDate(sprint.getStartDate()))
                .endDate(parseDate(sprint.getEndDate()))
                .completedDate(parseDate(sprint.getCompleteDate()))
                .updatedAt(updatedAt)
                .build();
    }

    private static Long parseDate(String date) {
        if (StringUtils.isNotEmpty(date)) {
            try {
                return DateUtils.parseDate(date, DATEFORMATS).toInstant().getEpochSecond();
            } catch (ParseException e) {
                log.error("parseDate: Could not parse date from " + date, e);
            }
        }
        return null;
    }

    public static List<DbJiraSprint> fromJiraIssue(JiraIssue source,
                                                   String integrationId,
                                                   String sprintField) {
        if (sprintField == null || source.getFields() == null || source.getFields().getDynamicFields() == null) {
            return List.of();
        }
        Object sprintVal = source.getFields().getDynamicFields().get(sprintField);
        if (!(sprintVal instanceof Collection && CollectionUtils.isNotEmpty((Collection<?>) sprintVal))
                && !(sprintVal instanceof String && StringUtils.isNotBlank((String) sprintVal))) {
            return List.of();
        }
        Long issueUpdatedAt = source.getFields().getUpdated().toInstant().getEpochSecond();
        if (sprintVal instanceof String) {
            return List.of(DbJiraSprint.parseStringSprint((String) sprintVal, integrationId, issueUpdatedAt));
        }
        return ((Collection<?>) sprintVal).stream()
                .map(sprint -> {
                    if (sprint instanceof Map) {
                        return DbJiraSprint.parseValue((Map<?, ?>) sprint, integrationId, issueUpdatedAt);
                    } else if (sprint instanceof String) {
                        return DbJiraSprint.parseStringSprint((String) sprint, integrationId, issueUpdatedAt);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static DbJiraSprint parseValue(Map<?, ?> jiraSprint, String integrationId, Long updatedAt) {
        Long startDate = parseDate((String) jiraSprint.get("startDate"), jiraSprint, integrationId);
        Long activatedDate = parseDate((String) jiraSprint.get("activatedDate"), jiraSprint, integrationId);
        String endDate = (String) jiraSprint.get("endDate");
        String completedDate = (String) jiraSprint.get("completeDate");
        return DbJiraSprint.builder()
                .integrationId(NumberUtils.toInteger(integrationId))
                .state(StringUtils.upperCase((String) (isString(jiraSprint.get("state")) ? jiraSprint.get("state") : null)))
                .sprintId((Integer) (isInteger(jiraSprint.get("id")) ? jiraSprint.get("id") : null))
                .name((String) (isString(jiraSprint.get("name")) ? jiraSprint.get("name") : null))
                .goal((String) (isString(jiraSprint.get("goal")) ? jiraSprint.get("goal") : null))
                .startDate(ObjectUtils.firstNonNull(activatedDate, startDate))
                .endDate(endDate != null ? parseDate(endDate, jiraSprint, integrationId) : null)
                .completedDate(completedDate != null ? parseDate(completedDate, jiraSprint, integrationId) : null)
                .updatedAt(updatedAt)
                .build();
    }

    // Returns a list of tokens in the format key=value
    static List<String> tokenizeSprintString(String str){
        Matcher m = SPRINT_TOKENIZE_PATTERN.matcher(str);
        List<String> tokens = new ArrayList<>();
        var allMatches = m.results()
                .collect(Collectors.toList());

        int start = 0;
        for (MatchResult allMatch : allMatches) {
            tokens.add(str.substring(start, allMatch.start()));
            start = allMatch.start() + 1;
        }
        if (allMatches.size() > 1) {
            // add last token
            tokens.add(str.substring(allMatches.get(allMatches.size()-1).start()+1, str.length()));
        }
        if (allMatches.size() == 0 && str.length() > 0){
            tokens.add(str);
        }
        return tokens;
    }

    static List<Map.Entry<String, String>> parseStringSprintFields(String sprint) {
        if (StringUtils.isBlank(sprint)) {
            return List.of();
        }
        List<String> keysAndValues = new ArrayList<>();
        var tokens = tokenizeSprintString(sprint);
        for (int i = 0; i < tokens.size(); i++) {
            Matcher matcher = VALUE_KEY_PATTERN_EQUALS.matcher(tokens.get(i));
            if (matcher.find()) {
                keysAndValues.add(matcher.group("VALUE"));
                keysAndValues.add(matcher.group("KEY"));
            } else {
                log.error("Failed to parse sprint token: {}", tokens.get(i));
            }
        }
        if (keysAndValues.size() % 2 != 0) {
            log.error("Sprint Parsing has failed! sprint {}", sprint);
            return List.of();
        }
        List<Map.Entry<String, String>> result = new ArrayList<>();
        Iterator<String> it = keysAndValues.iterator();
        while (it.hasNext()) {
            result.add(Maps.immutableEntry(it.next(), it.next()));
        }
        return result;
    }

    public static DbJiraSprint parseStringSprint(String sprint, final String integrationId, Long updatedAt) {
        sprint = sprint.replaceAll("\r", " ").replaceAll("\n", " ");
        Matcher sprintMatcher = SPRINT_PATTERN.matcher(sprint);
        DbJiraSprintBuilder builder = DbJiraSprint.builder()
                .integrationId(NumberUtils.toInteger(integrationId));
        Long startDate = null;
        Long activatedDate = null;
        while (sprintMatcher.find()) {
            List<Map.Entry<String, String>> fields = parseStringSprintFields(sprintMatcher.group("VALUE"));
            for (Map.Entry<String, String> e : fields) {
                String field = e.getKey();
                String value = e.getValue();
                switch (field) {
                    case "completeDate":
                        builder.completedDate(parseDate(value, sprint, integrationId));
                        break;
                    case "activatedDate":
                        activatedDate = parseDate(value, sprint, integrationId);
                        break;
                    case "startDate":
                        startDate = parseDate(value, sprint, integrationId);
                        break;
                    case "endDate":
                        builder.endDate(parseDate(value, sprint, integrationId));
                        break;
                    case "state":
                        builder.state(StringUtils.upperCase(isString(value) ? value : null));
                        break;
                    case "name":
                        builder.name(isString(value) ? value : null);
                        break;
                    case "goal":
                        builder.goal(isString(value) ? value : null);
                        break;
                    case "id":
                        builder.sprintId(NumberUtils.toInteger(value));
                        break;
                }
            }
        }
        builder.startDate(ObjectUtils.firstNonNull(activatedDate, startDate));
        builder.updatedAt(updatedAt);
        return builder.build();
    }

    private static boolean isInteger(Object object) {
        return object instanceof Number;
    }

    private static boolean isString(Object object) {
        return object instanceof String && !"<null>".equals(object) && ObjectUtils.isNotEmpty(object);
    }

    private static Long parseDate(String date, String sprint, String integrationId) {
        try {
            return StringUtils.isNotEmpty(date) && !"<null>".equals(date) ?
                    DateUtils.parseDate(date, DATEFORMATS).toInstant().getEpochSecond() : null;
        } catch (ParseException parseException) {
            log.error("parseValue: unable to parse date from sprint field" + sprint
                    + " for integration " + integrationId, parseException);
            return null;
        }
    }

    @Nullable
    private static Long parseDate(@Nullable String date, Map<?, ?> sprint, String integrationId) {
        try {
            return StringUtils.isNotEmpty(date) && !"<null>".equals(date) ?
                    DateUtils.parseDate(date, DATEFORMATS).toInstant().getEpochSecond() : null;
        } catch (ParseException parseException) {
            log.error("parseValue: unable to parse date from sprint field" + sprint
                    + " for integration " + integrationId, parseException);
            return null;
        }
    }
}
