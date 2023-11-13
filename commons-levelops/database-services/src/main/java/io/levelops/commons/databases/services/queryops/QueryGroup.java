package io.levelops.commons.databases.services.queryops;

import com.google.common.base.Preconditions;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.List;

public class QueryGroup {
    final private List<QueryField> queryFields;
    final private GroupOperator groupOperator;

    public QueryGroup(List<QueryField> queryFields, @NonNull GroupOperator op) {
        //for jsonb array fields we cross join on parent so it MUST exist
        Preconditions.checkArgument(queryFields != null && queryFields.size() > 0);
        this.groupOperator = op;
        this.queryFields = queryFields;
    }

    public static QueryGroup or(QueryField... fields) {
        return new QueryGroup(Arrays.asList(fields), GroupOperator.OR);
    }

    public static QueryGroup and(QueryField... fields) {
        return new QueryGroup(Arrays.asList(fields), GroupOperator.AND);
    }

    public List<QueryField> getQueryFields() {
        return queryFields;
    }

    public GroupOperator getGroupOperator() {
        return groupOperator;
    }

    public enum GroupOperator {
        AND(" AND "),
        OR(" OR ");

        private String op;

        GroupOperator(String op) {
            this.op = op;
        }

        public String getOp() {
            return op;
        }
    }
}
