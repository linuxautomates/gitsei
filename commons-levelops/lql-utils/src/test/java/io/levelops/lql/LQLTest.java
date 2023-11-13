package io.levelops.lql;

import io.levelops.lql.eval.LqlDateTermEvaluator;
import io.levelops.lql.eval.LqlStringTermEvaluator;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlAst;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class LQLTest {

    @Test
    public void testParse() throws LqlSyntaxException, LqlLexicalException {
        String lql = "Integration = \"github\" AND ((documentation.type = \"Confluence\" AND document.title ~ \"Design\" ) OR (code ~ \"s3\"))";
        System.out.println(lql);
        System.out.println(LqlLexer.tokenize(lql));
        System.out.println((LQL.parse(lql).toPrettyString()));

        LqlAst ast = LQL.parse("var in [\"a\", \"b\", \"c\"]");
        System.out.println((ast.toPrettyString()));
    }

    @Test
    public void testParseUnquoted() throws LqlSyntaxException, LqlLexicalException {
        String lql = "Integration = github AND ((documentation.type = Confluence AND document.title ~ \"Design\" ) OR (code ~ \"s3\"))";
        System.out.println(lql);
        System.out.println(LqlLexer.tokenize(lql));
        System.out.println((LQL.parse(lql).toPrettyString()));

        LqlAst ast = LQL.parse("var in [a, b, c]");
        System.out.println((ast.toPrettyString()));
    }

    @Test
    public void testEval() throws LqlException {
        boolean output = LQL.eval("issue.title = \"abc\" and issue.content ~ \"Design\" and issue.created < \"2010-01-02T00:00:00Z\"",
                Lists.newArrayList(
                        LqlStringTermEvaluator.builder()
                                .assignment("issue.title", "abc")
                                .assignment("issue.content", "something Design something")
                                .build(),
                        LqlDateTermEvaluator.builder()
                                .assignment("issue.created", Instant.parse("2000-01-01T00:00:00Z"))
                                .build()));
        assertThat(output).isTrue();
    }

    @Test
    public void testEvalUnquoted() throws LqlException {
        boolean output = LQL.eval("issue.title = abc and issue.content ~ Design@abc and issue.created < 2010-01-02T00:00:00Z",
                Lists.newArrayList(
                        LqlStringTermEvaluator.builder()
                                .assignment("issue.title", "abc")
                                .assignment("issue.content", "something Design@abc something")
                                .build(),
                        LqlDateTermEvaluator.builder()
                                .assignment("issue.created", Instant.parse("2000-01-01T00:00:00Z"))
                                .build()));
        assertThat(output).isTrue();
    }

    @Test
    public void testEvalMany() {
        LQL.EvalManyResult result = LQL.evalMany(Lists.newArrayList("a = 1", "b = 1", "c = 1"), Lists.newArrayList(LqlStringTermEvaluator.builder()
                .assignment("a", "0")
                .assignment("b", "1")
                .build()));
        System.out.println(result);
        assertThat(result.isGlobalMatch()).isTrue();
        assertThat(result.getResults()).containsEntry("a = 1", false);
        assertThat(result.getResults()).containsEntry("b = 1", true);
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    public void testTransform() throws LqlSyntaxException, LqlLexicalException {
        // ${1.ticket.state} = "open" and ${2.ticket.state} = "open"
        LqlAst ast = LQL.parse("not ${1.ticket.state} = \"12 ${b} 34\" and (${1.x} = \"${2.y}\" or ${1.x} = 123 or a = b)");

        LqlInterpreter.transformLiterals(ast, value -> "**" + value.toUpperCase() + "**");

        System.out.println(ast.toInlineString());

        assertThat(ast.toInlineString()).isEqualTo("((not (${1.ticket.state} = **\"12 ${B} 34\"**)) and (((${1.x} = **\"${2.Y}\"**) or (${1.x} = **123**)) or (a = b)))");
    }
}