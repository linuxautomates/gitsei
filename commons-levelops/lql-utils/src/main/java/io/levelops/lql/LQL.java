package io.levelops.lql;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.lql.eval.LqlTermEvaluator;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlToken;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.List;
import java.util.Map;

public class LQL {

    /**
     * Parse source lql String (lexer + parser) into an AST
     */
    public static LqlAst parse(String lql) throws LqlLexicalException, LqlSyntaxException {
        List<LqlToken> tokens = LqlLexer.tokenize(lql);
        return LqlParser.parseTokens(tokens);
    }

    public static boolean eval(String lql, List<LqlTermEvaluator> evaluators) throws LqlException {
        return LqlInterpreter.evaluate(LQL.parse(lql), evaluators);
    }

    public static EvalManyResult evalMany(List<String> lqlList, List<LqlTermEvaluator> evaluators) {
        EvalManyResult.EvalManyResultBuilder builder = EvalManyResult.builder();
        MutableBoolean globalMatch = new MutableBoolean(false);
        lqlList.forEach(lql -> {
            try {
                boolean eval = LQL.eval(lql, evaluators);
                builder.result(lql, eval);
                globalMatch.setValue(globalMatch.getValue() || eval);
            } catch (LqlException e) {
                builder.error(lql, e);
            }
        });
        return builder
                .globalMatch(globalMatch.getValue())
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EvalManyResult.EvalManyResultBuilder.class)
    public static class EvalManyResult {
        boolean globalMatch; // logical OR (ignores errors)
        @Singular
        Map<String, Boolean> results;
        @Singular
        Map<String, LqlException> errors;
    }
}
