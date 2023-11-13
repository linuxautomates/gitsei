package io.levelops.lql.eval;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.levelops.lql.exceptions.LqlEvalException;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlTokenType;
import lombok.Builder;
import lombok.Singular;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.levelops.lql.LqlConstants.CONTAINS;
import static io.levelops.lql.LqlConstants.EQ;
import static io.levelops.lql.LqlConstants.IN;
import static io.levelops.lql.LqlConstants.NCONTAINS;
import static io.levelops.lql.LqlConstants.NEQ;
import static io.levelops.lql.LqlConstants.NIN;
import static io.levelops.lql.utils.LqlParsingUtils.checkToken;

@Builder
public class LqlStringListTermEvaluator implements LqlTermEvaluator {

    @Singular("assignment")
    Map<String, List<String>> assignments;

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
        List<String> values = assignments.get(identifier);
        String arg = StringUtils.lowerCase(node.getChildren().get(1).getToken().getValue());
        String operator = node.getToken().getValue().toLowerCase();
        if (values == null) {
            return false;
        }

        var valuesStream = values.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase);

        switch (operator) {
            case EQ:
                return valuesStream.anyMatch(arg::equals);
            case NEQ:
                return valuesStream.noneMatch(arg::equals);
            case NIN: {
                Set<String> args = getArgs(node);
                return valuesStream.noneMatch(args::contains);
            }
            case IN: {
                Set<String> args = getArgs(node);
                return valuesStream.anyMatch(args::contains);
            }
            case CONTAINS:
                return arg != null && valuesStream.anyMatch(value -> value.contains(arg));
            case NCONTAINS:
                return arg != null && valuesStream.noneMatch(value -> value.contains(arg));
        }
        throw new LqlEvalException("Unsupported operator='" + node.getToken().getValue() + "' for string list identifier='" + identifier + "'");
    }

    private Set<String> getArgs(LqlAst node) {
        Set<String> args = Sets.newHashSet();
        for (int i = 1; i < node.getChildren().size(); i++) {
            String arg = node.getChildren().get(i).getToken().getValue();
            if (arg != null) {
                args.add(arg.toLowerCase());
            }
        }
        return args;
    }
}
