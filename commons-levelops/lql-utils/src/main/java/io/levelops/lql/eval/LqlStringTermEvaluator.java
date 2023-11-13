package io.levelops.lql.eval;

import com.google.common.collect.Iterables;
import io.levelops.lql.exceptions.LqlEvalException;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlTokenType;
import lombok.Builder;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static io.levelops.lql.LqlConstants.CONTAINS;
import static io.levelops.lql.LqlConstants.EQ;
import static io.levelops.lql.LqlConstants.IN;
import static io.levelops.lql.LqlConstants.NCONTAINS;
import static io.levelops.lql.LqlConstants.NEQ;
import static io.levelops.lql.LqlConstants.NIN;
import static io.levelops.lql.utils.LqlParsingUtils.checkToken;

@Builder
public class LqlStringTermEvaluator implements LqlTermEvaluator {

    @Singular("assignment")
    Map<String, String> assignments;

    @Override
    public boolean accepts(LqlAst node) {
        return node.getChildren().size() > 1 &&
                checkToken(node.getChildren().get(0).getToken(), LqlTokenType.IDENTIFIER,
                        Iterables.toArray(assignments.keySet(), String.class)) &&
                checkToken(node.getToken(), LqlTokenType.OPERATOR,
                        EQ, NEQ, IN, NIN, CONTAINS, NCONTAINS);
        // TODO is (not) null / empty
    }

    @Override
    public Boolean evaluate(LqlAst node) throws LqlException {
        String identifier = node.getChildren().get(0).getToken().getValue();
        String value = StringUtils.lowerCase(assignments.get(identifier));
        String arg = StringUtils.lowerCase(node.getChildren().get(1).getToken().getValue());
        String operator = node.getToken().getValue().toLowerCase();

        if (value == null) {
            return false;
        }

        switch (operator) {
            case EQ:
                return value.equals(arg);
            case NEQ:
                return !value.equals(arg);
            case NIN:
                for (int i = 1; i < node.getChildren().size(); i++) {
                    boolean isIn = value.equals(StringUtils.lowerCase(node.getChildren().get(i).getToken().getValue()));
                    if (isIn) {
                        return false;
                    }
                }
                return true;
            case IN:
                for (int i = 1; i < node.getChildren().size(); i++) {
                    boolean isIn = value.equals(StringUtils.lowerCase(node.getChildren().get(i).getToken().getValue()));
                    if (isIn) {
                        return true;
                    }
                }
                return false;
            case CONTAINS:
                return arg != null && value.contains(arg);
            case NCONTAINS:
                return arg != null && !value.contains(arg);
        }
        throw new LqlEvalException("Unsupported operator='" + node.getToken().getValue() + "' for string identifier='" + identifier + "'");
    }
}
