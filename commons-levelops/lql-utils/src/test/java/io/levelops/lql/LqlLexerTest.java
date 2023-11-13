package io.levelops.lql;

import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.models.LqlTokenType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LqlLexerTest {

    // TODO add test for arrays

    @Test
    public void testIsSeparator() {
        assertThat(LqlLexer.isSeparator('a')).isFalse();
        assertThat(LqlLexer.isSeparator('1')).isFalse();
        assertThat(LqlLexer.isSeparator('.')).isFalse();

        assertThat(LqlLexer.isSeparator(' ')).isTrue();
        assertThat(LqlLexer.isSeparator('(')).isTrue();
        assertThat(LqlLexer.isSeparator(')')).isTrue();
        assertThat(LqlLexer.isSeparator('"')).isTrue();
    }

    @Test
    public void testIsIdentifier() {
        assertThat(LqlLexer.isIdentifier("abc")).isTrue();
        assertThat(LqlLexer.isIdentifier("abc123")).isTrue();
        assertThat(LqlLexer.isIdentifier("@abc123")).isTrue();
        assertThat(LqlLexer.isIdentifier("a_b_c")).isTrue();
        assertThat(LqlLexer.isIdentifier("a.b.c")).isTrue();
        assertThat(LqlLexer.isIdentifier("_a")).isTrue();

        assertThat(LqlLexer.isIdentifier(".a")).isFalse();
        assertThat(LqlLexer.isIdentifier("123abc")).isFalse();
        assertThat(LqlLexer.isIdentifier("a!")).isFalse();

        assertThat(LqlLexer.isIdentifier("${1.ticket.state}")).isTrue();
    }

    @Test
    public void testReadAtom() {
        assertThat(LqlLexer.readToken("abc", 0)).isEqualTo("abc");
        assertThat(LqlLexer.readToken("abc", 1)).isEqualTo("bc");
        assertThat(LqlLexer.readToken("abc", 2)).isEqualTo("c");
        assertThat(LqlLexer.readToken("abc", 3)).isEqualTo("");

        assertThat(LqlLexer.readToken("abc def", 0)).isEqualTo("abc");
        assertThat(LqlLexer.readToken("a.b_c", 0)).isEqualTo("a.b_c");
        assertThat(LqlLexer.readToken("-1.04f", 0)).isEqualTo("-1.04f");
        assertThat(LqlLexer.readToken("abc\"def", 0)).isEqualTo("abc");
        assertThat(LqlLexer.readToken("\"abc\"def", 0)).isEqualTo("\"abc\"");
        assertThat(LqlLexer.readToken("\" () \"", 0)).isEqualTo("\" () \"");
        assertThat(LqlLexer.readToken("\" \\\" \"", 0)).isEqualTo("\" \\\" \"");
        assertThat(LqlLexer.readToken("\" \\\\\"      \"...", 0)).isEqualTo("\" \\\\\"");
    }

    @Test
    public void testParseStringLiteral() {
        assertThat(LqlLexer.parseStringLiteral("\"abc\"")).isEqualTo("abc");
        assertThat(LqlLexer.parseStringLiteral("\" () \"")).isEqualTo(" () ");
        assertThat(LqlLexer.parseStringLiteral("\" \\\" \"")).isEqualTo(" \" ");
        assertThat(LqlLexer.parseStringLiteral("\" \\\\ \"")).isEqualTo(" \\ ");
    }

    @Test
    public void testParseToken() {
        assertThat(LqlLexer.parseToken("\"abc\"").get().getType()).isEqualTo(LqlTokenType.LITERAL);
        assertThat(LqlLexer.parseToken("123").get().getType()).isEqualTo(LqlTokenType.LITERAL);
        assertThat(LqlLexer.parseToken("test@abc.com").get().getType()).isEqualTo(LqlTokenType.LITERAL);
        assertThat(LqlLexer.parseToken("ab.c").get().getType()).isEqualTo(LqlTokenType.IDENTIFIER);
        assertThat(LqlLexer.parseToken("@abc").get().getType()).isEqualTo(LqlTokenType.IDENTIFIER);
    }

    @Test
    public void test() throws LqlLexicalException {
        System.out.println((LqlLexer.tokenize("")));
        System.out.println((LqlLexer.tokenize("var in [ab,cd,ef]")));
        System.out.println((LqlLexer.tokenize("Integration eq any AND ((documentation.type eq \"Confluence\" AND document.title contains \"Design\" ) OR (code contains s3))")));
    }
}