package io.levelops.aggregations.functions;

import io.levelops.commons.databases.services.queryops.QueryField;
import lombok.extern.log4j.Log4j2;

import java.util.List;

import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.NUMBER;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING;
import static io.levelops.commons.databases.services.queryops.QueryField.FieldType.STRING_ARRAY;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.EXACT_MATCH;
import static io.levelops.commons.databases.services.queryops.QueryField.Operation.NON_NULL_CHECK;


@Log4j2
@SuppressWarnings("unused")
public class GithubAggQueries {
    public static QueryField NON_NULL_PR_CREATORS = new QueryField("creator_name",
            STRING, NON_NULL_CHECK, List.of(), null);
    public static QueryField NON_NULL_COMMIT_AUTHORS = new QueryField("author_name",
            STRING, NON_NULL_CHECK, List.of(), null);
    public static QueryField NON_NULL_FILENAMES = new QueryField("file_name",
            STRING, NON_NULL_CHECK, List.of(), null);
    public static QueryField NON_NULL_REPOS = new QueryField("repo_name",
            STRING, NON_NULL_CHECK, List.of(), null);

    public static QueryField getQueryFieldForRepo(String repoName) {
        return new QueryField("repo_name", STRING, EXACT_MATCH, List.of(), repoName);
    }

    public static List<QueryField> getQueryForApprovedMergedReviews() {
        return List.of(
                new QueryField("reviews", STRING_ARRAY, EXACT_MATCH, List.of(),
                        "APPROVED"),
                new QueryField("merged_date", NUMBER, NON_NULL_CHECK, List.of(),
                        null));
    }

    public static List<QueryField> getQueryForTotalMergedReviews() {
        return List.of(
                new QueryField("merged_date", NUMBER, NON_NULL_CHECK, List.of(),
                        null));
    }

    public static List<QueryField> getQueryForPRCreators(String text) {
        return List.of(
                new QueryField("creator_name",
                        STRING, EXACT_MATCH, List.of(), text));
    }

    public static List<QueryField> getQueryForCommitAuthors(String text) {
        return List.of(
                new QueryField("author_name",
                        STRING, EXACT_MATCH, List.of(), text));
    }

    public static List<QueryField> getQueryForFilesWithName(String fname) {
        return List.of(
                new QueryField("file_name",
                        STRING, EXACT_MATCH, List.of(), fname));
    }

}
