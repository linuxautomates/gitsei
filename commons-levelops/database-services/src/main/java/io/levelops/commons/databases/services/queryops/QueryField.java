package io.levelops.commons.databases.services.queryops;

import com.google.common.base.Preconditions;
import org.springframework.lang.NonNull;

import javax.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.List;

public class QueryField {
    final private List<String> parents;
    final private String fieldName;
    final private FieldType type;
    final private Operation op;
    final private Object value;

    /**
     * Short hand constructor to queries by String fields with exact match.
     * @param fieldName
     * @param value
     */
    public QueryField(@NotBlank String fieldName, @NotBlank String value) {
        this.fieldName = fieldName;
        this.value = value;
        this.type = FieldType.STRING;
        this.op = Operation.EXACT_MATCH;
        this.parents = Collections.emptyList();
    }

    public QueryField(@NotBlank String fieldName, @NonNull FieldType type,
                      @NonNull Operation op, Object value) {
        //for jsonb array fields we cross join on parent so it MUST exist
        Preconditions.checkArgument(type != FieldType.JSONB_ARRAY_FIELD, "JSONB_ARRAY_FIELD field type should include parents.");
        Preconditions.checkArgument(type != FieldType.NUMBER ||
                (!List.of(Operation.PREFIX, Operation.SUFFIX, Operation.CONTAINS, Operation.REGEX).contains(op)
                        && (value == null || value instanceof Number)));
        this.fieldName = fieldName;
        this.value = value;
        this.type = type;
        this.op = op;
        this.parents = Collections.emptyList();
    }

    public QueryField(@NotBlank String fieldName, @NonNull FieldType type,
                      @NonNull Operation op, @NonNull List<String> parents, Object value) {
        //for jsonb array fields we cross join on parent so it MUST exist
        Preconditions.checkArgument(type != FieldType.JSONB_ARRAY_FIELD || parents.size() > 0);
        Preconditions.checkArgument(type != FieldType.NUMBER ||
                (!List.of(Operation.PREFIX, Operation.SUFFIX, Operation.CONTAINS, Operation.REGEX).contains(op)
                        && (value == null || value instanceof Number)));
        this.fieldName = fieldName;
        this.value = value;
        this.type = type;
        this.op = op;
        this.parents = parents;
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldType getType() {
        return type;
    }

    public Operation getOp() {
        return op;
    }

    public String getFullJoin() {
        if (type == FieldType.STRING || type == FieldType.NUMBER) // we only need join for nested array fields
        {
            return "";
        }
        //this is not super safe as if fieldname is duplicated in the query, it will cause duplication issues
        //finally it converts the jsonb to text
        StringBuilder joinFieldName = new StringBuilder();
        for (String parent : parents) {
            if (joinFieldName.length() > 0) {
                joinFieldName.append("->");
            }
            joinFieldName.append("'").append(parent).append("'");
        }

        if (type == FieldType.STRING_ARRAY) {
            if (joinFieldName.length() > 0) {
                joinFieldName.append("->");
            }
            joinFieldName.append("'").append(fieldName).append("'");
            return "LEFT JOIN jsonb_array_elements_text(_levelops_data_field->" + joinFieldName.toString() + ") "
                    + fieldName + "(" + fieldName + ") ON TRUE"; // on true ensures we always expand it
        }
        String columnName = parents.get(parents.size() - 1);
        //jsonb array field means that the parent is the jsonb array.
        return "LEFT JOIN jsonb_array_elements(_levelops_data_field->" + joinFieldName.toString() + ") " + columnName
                + "(" + columnName + ") ON TRUE";
    }

    public Object getValueForQuery() {
        switch (op) {
            case EXACT_MATCH:
            case REGEX:
            case NOT_EQUALS:
            case GREATER_THAN:
            case LESS_THAN:
                return value;
            case PREFIX:
                return value + "%";
            case CONTAINS:
                return "%" + value + "%";
            case SUFFIX:
                return "%" + value;
            case NULL_CHECK:
            case NON_NULL_CHECK:
                return null;
            default:
                throw new UnsupportedOperationException(); //should never get here.
        }
    }

    //the brackets help cast to int or something else
    public String getQueryWithCriteria() {
        //integer operations need to be casted
        if (value instanceof Number) {
            return "(" + getQueryFieldName() + ")::bigint " + op.getOpWithCriteria();
        } else {
            return "(" + getQueryFieldName() + ") " + op.getOpWithCriteria();
        }
    }

    public String getQueryFieldName() {
        if (type == FieldType.STRING) {
            StringBuilder fName = new StringBuilder("_levelops_data_field->");
            for (String parent : parents) {
                fName.append("'").append(parent).append("'").append("->");
            }
            //the extra '>' converts it to text
            fName.append(">").append("'").append(fieldName).append("'");
            return fName.toString();
        }
        if (type == FieldType.NUMBER) {
            StringBuilder fName = new StringBuilder("_levelops_data_field->");
            for (String parent : parents) {
                fName.append("'").append(parent).append("'").append("->");
            }
            fName.append("'").append(fieldName).append("'");
            return fName.toString();
        }

        //because we do a join for the two array types, we dont need to append parents
        //The joined fields are now full columns with the parentname or the fieldname

        if (type == FieldType.STRING_ARRAY) {
            return fieldName;
        }
        //for jsonb array field, we convert to text before query.
        return parents.get(parents.size() - 1) + "->>'" + fieldName + "'";
    }

    public enum Operation {
        NON_NULL_CHECK("IS NOT NULL", ""),
        NULL_CHECK("IS NULL", ""),
        EXACT_MATCH("=", " ?"),
        NOT_EQUALS("<>", " ?"),
        LESS_THAN("<", " ?"),
        GREATER_THAN(">", " ?"),
        PREFIX("LIKE", " ?"),
        SUFFIX("LIKE", " ?"),
        CONTAINS("LIKE", " ?"),
        REGEX("~", " ?");

        private String operator;
        private String valuePlaceholder;

        Operation(String operator, String valuePlaceholder) {
            this.operator = operator;
            this.valuePlaceholder = valuePlaceholder;
        }

        public String getOpWithCriteria() {
            return operator + valuePlaceholder;
        }
    }

    public enum FieldType {
        STRING_ARRAY,
        JSONB_ARRAY_FIELD,
        NUMBER,
        STRING
    }
}
