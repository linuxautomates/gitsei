package io.levelops.lql.eval;

import com.google.common.collect.Iterables;
import io.levelops.lql.exceptions.LqlEvalException;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlTokenType;
import lombok.Builder;
import lombok.Singular;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static io.levelops.lql.LqlConstants.EQ;
import static io.levelops.lql.LqlConstants.GT;
import static io.levelops.lql.LqlConstants.GTE;
import static io.levelops.lql.LqlConstants.IN;
import static io.levelops.lql.LqlConstants.LT;
import static io.levelops.lql.LqlConstants.LTE;
import static io.levelops.lql.LqlConstants.NEQ;
import static io.levelops.lql.LqlConstants.NIN;
import static io.levelops.lql.utils.LqlParsingUtils.checkToken;

@Builder
public class LqlDateTermEvaluator implements LqlTermEvaluator {

    private static final String YESTERDAY = "yesterday";
    private static final String LAST_WEEK = "lastWeek";
    private static final String LAST_MONTH = "lastMonth";

    @Singular("assignment")
    Map<String, Instant> assignments;

    @Override
    public boolean accepts(LqlAst node) {
        return node.getChildren().size() > 1 &&
                checkToken(node.getChildren().get(0).getToken(), LqlTokenType.IDENTIFIER,
                        Iterables.toArray(assignments.keySet(), String.class)) &&
                checkToken(node.getToken(), LqlTokenType.OPERATOR,
                        EQ, NEQ, IN, NIN, GT, GTE, LT, LTE);
        // TODO is (not) null / empty
    }

    private Instant parseArg(String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case YESTERDAY:
                return Instant.now().minus(1, ChronoUnit.DAYS);
            case LAST_WEEK:
                return Instant.now().minus(1, ChronoUnit.WEEKS);
            case LAST_MONTH:
                return Instant.now().minus(1, ChronoUnit.MONTHS);
            default:
                return Instant.parse(value);
        }
    }

    @Override
    public Boolean evaluate(LqlAst node) throws LqlException {
        String identifier = node.getChildren().get(0).getToken().getValue();
        Instant value = assignments.get(identifier);
        Instant arg = parseArg(node.getChildren().get(1).getToken().getValue());
        String operator = node.getToken().getValue().toLowerCase();

        if (value == null || arg == null) {
            return false;
        }

        switch (operator) {
            case EQ:
                return value.equals(arg);
            case NEQ:
                return !value.equals(arg);
            case NIN:
                for (int i = 1; i < node.getChildren().size(); i++) {
                    Instant dateTime = parseArg(node.getChildren().get(i).getToken().getValue());
                    boolean isIn = value.equals(dateTime);
                    if (isIn) {
                        return false;
                    }
                }
                return true;
            case IN:
                for (int i = 1; i < node.getChildren().size(); i++) {
                    Instant dateTime = parseArg(node.getChildren().get(i).getToken().getValue());
                    boolean isIn = value.equals(dateTime);
                    if (isIn) {
                        return true;
                    }
                }
                return false;
            case LT:
                return value.isBefore(arg);
            case LTE:
                return value.isBefore(arg) || value.equals(arg);
            case GT:
                return value.isAfter(arg);
            case GTE:
                return value.isAfter(arg) || value.equals(arg);
        }
        throw new LqlEvalException("Unsupported operator='" + node.getToken().getValue() + "' for date identifier='" + identifier + "'");
    }
}
