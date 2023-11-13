package io.levelops.integrations.confluence.client;

import java.util.List;

public class ConfluenceCqlBuilder {

    public static final String FIELD_TEXT = "text";
    public static final String FIELD_LAST_MODIFIED = "lastmodified";
    public static final String FIELD_CREATED = "created";

    private final StringBuilder stringBuilder;

    protected ConfluenceCqlBuilder() {
        stringBuilder = new StringBuilder();
    }

    public static ConfluenceCqlBuilder builder() {
        return new ConfluenceCqlBuilder();
    }

    public ConfluenceCqlBuilder op(String field, String op, String value) {
        stringBuilder.append(field).append(" ").append(op).append(" ").append(value).append(" ");
        return this;
    }

    public ConfluenceCqlBuilder equals(String field, String value) {
        return op(field, "=", value);
    }

    public ConfluenceCqlBuilder notEquals(String field, String value) {
        return op(field, "!=", value);
    }

    /**
     * Only the following fields can be used with contains:
     *     title
     *     text
     *     space.title
     *
     * Notable behaviors:
     *     text ~ "abc def"      -> search for text containing both "abc" AND "def"
     *     text ~ "abc AND def"  -> same as above
     *     text ~ "abc OR def"   -> search for text containing "abc" OR "def"
     */
    public ConfluenceCqlBuilder contains(String field, String value) {
        return op(field, "~", quote(value));
    }

    public ConfluenceCqlBuilder containsRaw(String field, String value) {
        return op(field, "~", value);
    }

    public ConfluenceCqlBuilder containsAny(String field, Iterable<String> keywords) {
        return contains(field, String.join(" OR ", keywords));
    }

    public ConfluenceCqlBuilder notContains(String field, String value) {
        return op(field, "!~", quote(value));
    }


    public ConfluenceCqlBuilder in(String field, String... values) {
        stringBuilder.append(field).append(" in (").append(String.join(", ", values)).append(") ");
        return this;
    }

    public ConfluenceCqlBuilder notIn(String field, String... values) {
        stringBuilder.append(field).append(" not in (").append(String.join(", ", values)).append(") ");
        return this;
    }

    public ConfluenceCqlBuilder greaterThan(String field, String value) {
        return op(field, ">", value);
    }

    public ConfluenceCqlBuilder greaterThanEquals(String field, String value) {
        return op(field, ">=", value);
    }

    public ConfluenceCqlBuilder since(String field, String timeDelta) {
        return greaterThanEquals(field, now(timeDelta));
    }

    public ConfluenceCqlBuilder sinceDays(String field, int days) {
        return greaterThanEquals(field, now("-" + days + "d"));
    }

    public ConfluenceCqlBuilder lowerThan(String field, String value) {
        return op(field, "<", value);
    }

    public ConfluenceCqlBuilder lowerThanEquals(String field, String value) {
        return op(field, "<=", value);
    }

    /**
     * Must be last.
     */
    public ConfluenceCqlBuilder orderBy(String... fields) {
        stringBuilder.append("order by ").append(String.join(", ", fields)).append(" ");
        return this;
    }

    public ConfluenceCqlBuilder orderByDesc(String... fields) {
        orderBy(fields);
        stringBuilder.append("desc ");
        return this;
    }

    public ConfluenceCqlBuilder orderByAsc(String... fields) {
        orderBy(fields);
        stringBuilder.append("asc ");
        return this;
    }

    public ConfluenceCqlBuilder and() {
        stringBuilder.append("and ");
        return this;
    }

    public ConfluenceCqlBuilder or() {
        stringBuilder.append("or ");
        return this;
    }

    public ConfluenceCqlBuilder orMany(List<String> cqlStatements) {
        stringBuilder.append(String.join(" or ", cqlStatements));
        return this;
    }


    public String build() {
        return stringBuilder.toString();
    }

    public static String now(String timeDelta) {
        return String.format("now(\"%s\")", timeDelta);
    }

    public static String quote(String value) {
        return "\"" + value + "\"";
    }
}

