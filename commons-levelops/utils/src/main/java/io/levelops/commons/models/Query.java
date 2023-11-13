package io.levelops.commons.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Query {
    private List<SelectField> selectFields;
    private List<String> distinctOnFields;
    private List<FromField> fromFields;
    private QueryConditions criteria;
    private Condition condition;
    private List<GroupByField> groupByFields;
    private List<SortByField> orderByFields;
    private int limit;
    private int offset;

    private Query(QueryBuilder builder) {
        this.selectFields = builder.selectFields;
        this.distinctOnFields = builder.distinctOnFields;
        this.fromFields = builder.fromFields;
        this.criteria = builder.criteria;
        this.condition = builder.condition;
        this.groupByFields = builder.groupByFields;
        this.orderByFields = builder.orderByFields;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    public String toSql() {
        String select = getSelectStmt(distinctOnFields, selectFields);
        String from = getFromStmt(fromFields);
        String where = getWhereStmt(criteria, condition);
        String groupBy = getGroupByStmt(groupByFields);
        String orderBy = getOrderByStmt(orderByFields);
        String limit = getLimitStmt(this.limit);
        String offset = getOffsetStmt(this.offset);

        return String
                .join(" ", select, from, where, groupBy, orderBy, offset, limit)
                .trim().replaceAll(" +", " ");
    }

    public static QueryBuilder builder() {
        return new QueryBuilder();
    }
    public static class QueryBuilder {
        private List<SelectField> selectFields = new ArrayList<>();
        private List<String> distinctOnFields = new ArrayList<>();
        private List<FromField> fromFields = new ArrayList<>();
        private QueryConditions criteria = new QueryConditions();
        private Condition condition;
        private List<GroupByField> groupByFields = new ArrayList<>();
        private List<SortByField> orderByFields = new ArrayList<>();
        private int limit;
        private int offset;

        public QueryBuilder select(List<SelectField> selectFields) {
            this.selectFields = selectFields;
            return this;
        }

        public QueryBuilder select(SelectField... selectFields) {
            this.selectFields = Arrays.asList(selectFields);
            return this;
        }

        public QueryBuilder distinctOn(List<String> distinctOnFields) {
            this.distinctOnFields = distinctOnFields;
            return this;
        }

        public QueryBuilder distinctOn(String... distinctOnFields) {
            this.distinctOn(Arrays.asList(distinctOnFields));
            return this;
        }

        public QueryBuilder from(List<FromField> fromFields) {
            this.fromFields = fromFields;
            return this;
        }

        public QueryBuilder from(FromField... fromFields) {
            this.fromFields = Arrays.asList(fromFields);
            return this;
        }

        public QueryBuilder where(QueryConditions queryConditions, Condition condition) {
            this.criteria = queryConditions;
            this.condition = condition;
            return this;
        }

        public QueryBuilder groupBy(List<GroupByField> groupByFields) {
            this.groupByFields = groupByFields;
            return this;
        }

        public QueryBuilder groupBy(GroupByField... groupByFields) {
            this.groupByFields = Arrays.asList(groupByFields);
            return this;
        }

        public QueryBuilder orderBy(List<SortByField> orderByFields) {
            this.orderByFields = orderByFields;
            return this;
        }

        public QueryBuilder orderBy(SortByField... orderByFields) {
            this.orderByFields = Arrays.asList(orderByFields);
            return this;
        }

        public QueryBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public QueryBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Query build() {
            return new Query(this);
        }
    }

    public String getSelectStmt(List<String> distinctOnFields, List<SelectField> selectFields) {
        if (CollectionUtils.isEmpty(selectFields)) {
            return "";
        }
        List<String> selectValues = selectFields.stream().map(selectField -> {
            String selectStmt = " " + selectField.getField() + " ";
            if (StringUtils.isNotEmpty(selectField.getAlias())) {
                selectStmt += " as " + selectField.getAlias() + " ";
            }
            return selectStmt;
        }).distinct().collect(Collectors.toList());
        String command = "SELECT";
        if (CollectionUtils.isNotEmpty(distinctOnFields)) {
            command = "SELECT DISTINCT ON (" + String.join(", ", distinctOnFields) + ")";
        }
        return getSqlStatement(command, selectValues, ", ");
    }

    public String getFromStmt(List<FromField> tables) {
        if (CollectionUtils.isNotEmpty(tables)) {
            List<String> tableValues = tables.stream().map(table -> {
                String tableStmt = " " + table.getTable() + " ";
                if (StringUtils.isNotEmpty(table.getAlias())) {
                    tableStmt += " " + table.getAlias() + " ";
                }
                return tableStmt;
            }).collect(Collectors.toList());
            return getSqlStatement("FROM", tableValues, ", ");
        }
        return "";
    }

    public String getWhereStmt(QueryConditions queryConditions, Condition condition) {
        if (queryConditions != null && CollectionUtils.isNotEmpty(queryConditions.getConditions())) {
            return getSqlStatement("WHERE", queryConditions.getConditions(),
                    " " + condition.getValue() + " ");
        }
        return "";
    }

    public String getGroupByStmt(List<GroupByField> groupByFields) {
        if (CollectionUtils.isNotEmpty(groupByFields)) {
            List<String> groupByValues = groupByFields.stream().map(GroupByField::getField).collect(Collectors.toList());
            return getSqlStatement("GROUP BY", groupByValues, ", ");
        }
        return "";
    }

    public String getOrderByStmt(List<SortByField> sortByFields) {
        if(CollectionUtils.isNotEmpty(sortByFields)) {
            List<String> sortByValues = sortByFields.stream()
                    .map(sortByField ->
                            sortByField.getField() + " " + sortByField.getOrder()).collect(Collectors.toList());
            return getSqlStatement("ORDER BY", sortByValues, ", ");
        }
        return "";
    }

    public String getLimitStmt(int limit) {
        return (limit > 0) ? " LIMIT " + limit : "";
    }

    public String getOffsetStmt(int offset) {
        return (offset > 0) ? " OFFSET " + offset : "";
    }

    public String getSqlStatement(String command, List<String> values, String separator) {
        String result = "";
        if (CollectionUtils.isNotEmpty(values)) {
            result = command + " " + String.join(separator, values);
        }
        return " " + result + " ";
    }

    public static SelectField selectField(String field, String alias) {
        return new SelectField(field, alias);
    }

    public static SelectField selectField(String field) {
        return selectField(field, null);
    }
    @Getter
    @AllArgsConstructor
    public static class SelectField {
        String field;
        String alias;

        public String getAliasOrField() {
            return StringUtils.isNotEmpty(alias) ? alias : field;
        }
    }

    public static FromField fromField(String field, String alias) {
        return new FromField(field, alias);
    }

    public static FromField fromField(String field) {
        return fromField(field, null);
    }

    @Getter
    @AllArgsConstructor
    public static class FromField {
        String table;
        String alias;
    }

    public static GroupByField groupByField(String field) {
        return new GroupByField(field);
    }
    @Getter
    @AllArgsConstructor
    public static class GroupByField {
        String field;
    }

    public static SortByField sortByField(String field, String order, boolean nullsLast) {
        return new SortByField(field, order, nullsLast);
    }

    @Getter
    @AllArgsConstructor
    public static class SortByField {
        String field;
        String order;
        boolean nullsLast;
    }

    public static QueryConditions conditions(List<String> conditions, Map<String, Object> queryParams) {
        return new QueryConditions(conditions, queryParams);
    }

    public static QueryConditions conditions(List<String> conditions) {
        return new QueryConditions(conditions, new HashMap<>());
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QueryConditions {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();
    }

    public enum Condition {
        AND("AND"),
        OR("OR"),
        EQ("="),
        NOT_EQ("!=");

        private final String value;

        Condition(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }
}
