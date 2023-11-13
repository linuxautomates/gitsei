package io.levelops.lql;

import io.levelops.lql.eval.LqlStringTermEvaluator;
import io.levelops.lql.exceptions.LqlEvalException;
import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlAst;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LqlInterpreterTest {

    @Test
    public void testOr() throws LqlSyntaxException, LqlLexicalException, LqlEvalException {
        HashMap<LqlAst, Boolean> env = new HashMap<>();
        env.put(LQL.parse("code = \"test\""), true);
        env.put(LQL.parse("var = \"a\""), false);
        Boolean eval = LqlInterpreter.evaluate(LQL.parse("code = \"test\" or var = \"a\""), env);
        assertThat(eval).isTrue();
    }

    @Test
    public void testAnd() throws LqlSyntaxException, LqlLexicalException, LqlEvalException {
        HashMap<LqlAst, Boolean> env = new HashMap<>();
        env.put(LQL.parse("code = \"test\""), true);
        env.put(LQL.parse("var = \"a\""), false);
        Boolean eval = LqlInterpreter.evaluate(LQL.parse("code = \"test\" and var = \"a\""), env);
        assertThat(eval).isFalse();
    }

    @Test
    public void testAndOr() throws LqlSyntaxException, LqlLexicalException, LqlEvalException {
        HashMap<LqlAst, Boolean> env = new HashMap<>();
        env.put(LQL.parse("a = 1"), false);
        env.put(LQL.parse("b = 1"), true);
        Boolean eval = (Boolean) LqlInterpreter.evaluate(LQL.parse("a = 1 or (b = 1 and b = 1) or a = 1"), env);

        assertThat(eval).isTrue();
    }

    @Test
    public void testNot() throws LqlSyntaxException, LqlLexicalException, LqlEvalException {
        HashMap<LqlAst, Boolean> env = new HashMap<>();
        env.put(LQL.parse("a = 1"), false);
        Boolean eval = (Boolean) LqlInterpreter.evaluate(LQL.parse("not a = 1"), env);

        assertThat(eval).isTrue();
    }

    @Test
    public void testExtractTerms() throws LqlSyntaxException, LqlLexicalException {
        List<LqlAst> terms = LqlInterpreter.extractTerms(LQL.parse("a = 1 or (b = 1 and b = 1) or a = 1"));
        System.out.println(terms);
        assertThat(terms).hasSize(4);
    }

    @Test
    public void testEvaluator() throws LqlSyntaxException, LqlLexicalException, LqlEvalException {
        LqlAst ast = LQL.parse("issue.title = \"abc\"");

        LqlStringTermEvaluator evaluator = LqlStringTermEvaluator.builder()
                .assignment("issue.title", "abc")
                .build();

        assertThat(evaluator.accepts(ast)).isTrue();

        Boolean eval = LqlInterpreter.evaluate(ast, Lists.newArrayList(evaluator));

        assertThat(eval).isTrue();
    }

}