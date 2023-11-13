package io.levelops.aggregations.functions;

import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.services.queryops.QueryField;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.regex.Pattern;

import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.JSONB_ARRAY_FIELD;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.NUMBER;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING_ARRAY;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.CONTAINS;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.EXACT_MATCH;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.GREATER_THAN;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.LESS_THAN;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NON_NULL_CHECK;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NULL_CHECK;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.REGEX;

@Log4j2
@SuppressWarnings("unused")
public class JiraAggQueries {
    public static String DOCUMENTATION_SEARCH_TEXT = "docs.google.com";
    public static List<String> VULNERABILITY_SEARCH_TEXT = List.of("CVE-", "CWE-");
    public static List<Pattern> VULNERABILITY_SEARCH_REGEXES = List.of(Pattern.compile("CVE-\\d{4,7}(?!-)"),
            Pattern.compile("CVE-\\d{4}-\\d{4,7}"), Pattern.compile("CWE-\\d{3,5}"));

    public static QueryField NON_NULL_ISSUE_TYPES = new QueryField("name",
            STRING, NON_NULL_CHECK, List.of("issuetype"),
            null);
    public static QueryField NON_NULL_RELEASES = new QueryField("name",
            JSONB_ARRAY_FIELD, NON_NULL_CHECK, List.of("fixVersions"),
            null);

    public static QueryField getQueryFieldForProduct(DbJiraField product, String regexMatch) {
        if (product == null) {
            return null;
        }
        switch (product.getFieldType()) {
            case "project":
                return new QueryField("name",
                        STRING, REGEX, List.of("project"),
                        regexMatch);
            case "string":
                return new QueryField(product.getFieldKey(),
                        STRING, REGEX, List.of(),
                        regexMatch);
            case "components":
                return new QueryField("name",
                        JSONB_ARRAY_FIELD, REGEX, List.of("components"),
                        regexMatch);
            case "array":
            case "labels":
                //we assume its a string array
                return new QueryField(product.getFieldKey(),
                        STRING_ARRAY, REGEX, List.of(),
                        regexMatch);
            default:
                return null;
        }
    }

    public static List<QueryField> getQueryForIssueByType(String issueType) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"),
                        issueType));
    }

    public static List<QueryField> getQueryWithDescContains(String text) {
        return List.of(
                new QueryField("description_text",
                        STRING, CONTAINS, List.of(), text));
    }

    public static List<QueryField> getQueryWithSummaryContains(String text) {
        return List.of(
                new QueryField("summary",
                        STRING, CONTAINS, List.of(), text));
    }

    public static List<QueryField> getQueryForIssueWithLargeDescription(String issueType) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField("description_length",
                        NUMBER, GREATER_THAN, List.of(), 600)); //greater than 600 chars
    }

    public static List<QueryField> getQueryForIssueContainingTextInDescription(String issueType, String containsText) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField("description_text",
                        STRING, CONTAINS, List.of(), containsText));
    }

    //use this to subtract dupes to prevent double counting
    public static List<QueryField> getQueryForIssueWithLargeDescriptionAndContainsText(String issueType, String containsText) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField("description_length",
                        NUMBER, GREATER_THAN, List.of(), 600),
                new QueryField("description_text",
                        STRING, CONTAINS, List.of(), containsText));
    }

    public static List<QueryField> getQueryForIssuesWithStorypoints(String issueType, DbJiraField storyPointField) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, NON_NULL_CHECK, List.of(), null));
    }

    public static List<QueryField> getQueryForIssuesWithoutStorypoints(String issueType, DbJiraField storyPointField) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, NULL_CHECK, List.of(), null));
    }

    public static List<QueryField> getQueryForLargeIssues(String issueType, DbJiraField storyPointField) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, GREATER_THAN, List.of(), 30));
    }

    public static List<QueryField> getQueryForMediumIssues(String issueType, DbJiraField storyPointField) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, GREATER_THAN, List.of(), 15),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, LESS_THAN, List.of(), 31));
    }

    public static List<QueryField> getQueryForSmallIssues(String issueType, DbJiraField storyPointField) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField(storyPointField.getFieldKey(),
                        NUMBER, LESS_THAN, List.of(), 16));
    }

    public static List<QueryField> getQueryForNullFixVersionAndIssueType(String issueType) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField("name",
                        JSONB_ARRAY_FIELD, NULL_CHECK, List.of("fixVersions"),
                        null));
    }

    public static List<QueryField> getQueryForFixVersionAndIssueType(String issueType, String fixVersion) {
        return List.of(
                new QueryField("name",
                        STRING, EXACT_MATCH, List.of("issuetype"), issueType),
                new QueryField("name",
                        JSONB_ARRAY_FIELD, EXACT_MATCH, List.of("fixVersions"),
                        fixVersion));
    }
}
