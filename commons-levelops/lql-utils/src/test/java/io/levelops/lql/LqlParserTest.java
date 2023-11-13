package io.levelops.lql;

import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlToken;
import io.levelops.lql.models.LqlTokenType;
import io.levelops.lql.utils.LqlParsingContext;
import org.assertj.core.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LqlParserTest {

    @Test
    public void testParsingContext() throws LqlSyntaxException {
        LqlParsingContext context = new LqlParsingContext(Lists.newArrayList(
                new LqlToken(LqlTokenType.IDENTIFIER, "code"),
                new LqlToken(LqlTokenType.OPERATOR, "="),
                new LqlToken(LqlTokenType.LITERAL, "\"s3\"")
        ));
        assertThat(context.hasNext()).isTrue();
        assertThat(context.peekNextToken().getValue()).isEqualTo("code");
        assertThat(context.nextToken().getValue()).isEqualTo("code");

        assertThat(context.hasNext()).isTrue();
        assertThat(context.nextToken().getValue()).isEqualTo("=");
        assertThat(context.getCurrentToken().getValue()).isEqualTo("=");

        assertThat(context.hasNext()).isTrue();
        assertThat(context.nextToken().getValue()).isEqualTo("\"s3\"");
        assertThat(context.getCurrentToken().getValue()).isEqualTo("\"s3\"");

        assertThat(context.hasNext()).isFalse();
    }

    @Test
    public void testParseTermOperator() throws LqlSyntaxException {
        LqlToken codeToken = new LqlToken(LqlTokenType.IDENTIFIER, "code");
        LqlToken eqToken = new LqlToken(LqlTokenType.OPERATOR, "=");
        LqlToken s3Token = new LqlToken(LqlTokenType.LITERAL, "\"s3\"");
        LqlParsingContext context = new LqlParsingContext(Lists.newArrayList(
                codeToken, eqToken, s3Token));

        LqlAst ast = LqlParser.parseTerm(context);
        assertThat(ast.getToken()).isEqualTo(eqToken);
        assertThat(ast.getChildren()).hasSize(2);

        assertThat(ast.getChildren().get(0).getToken()).isEqualTo(codeToken);
        assertThat(ast.getChildren().get(0).getChildren()).hasSize(0);
        assertThat(ast.getChildren().get(1).getToken()).isEqualTo(s3Token);
        assertThat(ast.getChildren().get(1).getChildren()).hasSize(0);
    }

    @Test
    public void testParseTermOperatorUnquoted() throws LqlSyntaxException {
        LqlToken codeToken = new LqlToken(LqlTokenType.IDENTIFIER, "code");
        LqlToken eqToken = new LqlToken(LqlTokenType.OPERATOR, "=");
        LqlToken unquotedToken = new LqlToken(LqlTokenType.LITERAL, "s3@abc.com");
        LqlParsingContext context = new LqlParsingContext(Lists.newArrayList(
                codeToken, eqToken, unquotedToken));

        LqlAst ast = LqlParser.parseTerm(context);
        assertThat(ast.getToken()).isEqualTo(eqToken);
        assertThat(ast.getChildren()).hasSize(2);

        assertThat(ast.getChildren().get(0).getToken()).isEqualTo(codeToken);
        assertThat(ast.getChildren().get(0).getChildren()).hasSize(0);
        assertThat(ast.getChildren().get(1).getToken()).isEqualTo(unquotedToken);
        assertThat(ast.getChildren().get(1).getChildren()).hasSize(0);
    }

    @Test
    public void testParseTermOperatorUnquotedId() throws LqlSyntaxException {
        LqlToken codeToken = new LqlToken(LqlTokenType.IDENTIFIER, "code");
        LqlToken eqToken = new LqlToken(LqlTokenType.OPERATOR, "=");
        LqlToken unquotedToken = new LqlToken(LqlTokenType.LITERAL, "abc");
        LqlParsingContext context = new LqlParsingContext(Lists.newArrayList(
                codeToken, eqToken, unquotedToken));

        LqlAst ast = LqlParser.parseTerm(context);
        assertThat(ast.getToken()).isEqualTo(eqToken);
        assertThat(ast.getChildren()).hasSize(2);

        assertThat(ast.getChildren().get(0).getToken()).isEqualTo(codeToken);
        assertThat(ast.getChildren().get(0).getChildren()).hasSize(0);
        assertThat(ast.getChildren().get(1).getToken()).isEqualTo(unquotedToken);
        assertThat(ast.getChildren().get(1).getChildren()).hasSize(0);
    }

    @Test
    public void testMalformed() throws LqlSyntaxException, LqlLexicalException {
        String lql = "x = a b and y = c";
        assertThatThrownBy(() -> LQL.parse(lql))
                .isExactlyInstanceOf(LqlSyntaxException.class)
                .hasMessageContaining("Malformed expression: unexpected token");
    }
}