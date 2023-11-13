package io.levelops.lql;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.levelops.lql.exceptions.LqlLexicalException;
import io.levelops.lql.models.LqlToken;
import io.levelops.lql.models.LqlTokenType;
import lombok.extern.log4j.Log4j2;

import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static io.levelops.lql.LqlConstants.AND;
import static io.levelops.lql.LqlConstants.CONTAINS;
import static io.levelops.lql.LqlConstants.EMPTY;
import static io.levelops.lql.LqlConstants.EQ;
import static io.levelops.lql.LqlConstants.GT;
import static io.levelops.lql.LqlConstants.GTE;
import static io.levelops.lql.LqlConstants.IN;
import static io.levelops.lql.LqlConstants.IS;
import static io.levelops.lql.LqlConstants.LT;
import static io.levelops.lql.LqlConstants.LTE;
import static io.levelops.lql.LqlConstants.NCONTAINS;
import static io.levelops.lql.LqlConstants.NEQ;
import static io.levelops.lql.LqlConstants.NIN;
import static io.levelops.lql.LqlConstants.NOT;
import static io.levelops.lql.LqlConstants.NULL;
import static io.levelops.lql.LqlConstants.OR;

@Log4j2
public class LqlLexer {

    private static final Set<String> OPERATORS = Sets.newHashSet(
            EQ, NEQ, IN, NIN, LT, GT, LTE, GTE, CONTAINS, NCONTAINS);
//            "eq", "neq", "lt", "gt", "lte", "gte", "contains", );
    private static final Set<String> KEYWORDS = Sets.newHashSet(IS, NOT, EMPTY, NULL, AND, OR);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z@_][\\w._]*$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d*(\\.\\d*)?$");
    private static final Set<Character> SEPARATORS = Sets.newHashSet('(', ')', '"', '[', ']', ',');

    private static final Pattern RUNBOOK_VARIABLE_EXTERNAL_PATTERN = Pattern.compile("(\\$\\{[\\w\\d-_.]+})"); // to look for variables in a string

    /**
     * Lexical analysis (lexer): String -> Tokens
     */
    protected static List<LqlToken> tokenize(String lql) throws LqlLexicalException {

        List<LqlToken> tokens = Lists.newArrayList();
        int lastIndex = -1;
        for (int i = 0; i < lql.length(); ) {

            // infinite loop detection
            if (i == lastIndex) {
                throw new LqlLexicalException("Parsing failure", lql, i);
            }
            lastIndex = i;

            char c = lql.charAt(i);
            log.trace("char={}", c);
            switch (c) {
                case '(':
                    tokens.add(new LqlToken(LqlTokenType.LPAREN, "("));
                    i++;
                    break;
                case ')':
                    tokens.add(new LqlToken(LqlTokenType.RPAREN, ")"));
                    i++;
                    break;
                case '[':
                    tokens.add(new LqlToken(LqlTokenType.LBRACKET, "["));
                    i++;
                    break;
                case ']':
                    tokens.add(new LqlToken(LqlTokenType.RBRACKET, "]"));
                    i++;
                    break;
                case ',':
                    tokens.add(new LqlToken(LqlTokenType.COMMA, ","));
                    i++;
                    break;
                default:
                    if (Character.isWhitespace(c)) {
                        i++;
                    } else {
                        String value = readToken(lql, i);
                        if (Strings.isEmpty(value)) {
                            throw new LqlLexicalException(String.format("Unexpected start of token '%s'", c), lql, i);
                        }
                        Optional<LqlToken> token = parseToken(value);
                        if (!token.isPresent()) {
                            throw new LqlLexicalException(String.format("Unrecognized token '%s'", value), lql, i);
                        }
                        tokens.add(token.get());
                        i += value.length();
                    }
                    break;
            }
        }

        return tokens;
    }

    protected static String readToken(String s, int i) {
        int j = i;
        boolean insideQuotes = false;
        boolean escaped = false;
        for (; j < s.length(); j++) {
            char c = s.charAt(j);
            log.trace("> char={} - isLetter={} isSep={} escaped={} j={}", c, Character.isLetter(c), isSeparator(c), escaped, j);
            if (c == '"') {
                if (i == j) {
                    insideQuotes = true;
                } else if (insideQuotes && !escaped) {
                    j++; // take it and exit
                    break;
                } else if (!insideQuotes) {
                    break; // " is a separator for a regular token
                }
            } else if (!insideQuotes && isSeparator(c)) {
                // separator - leave it and exit
                break;
            }
            escaped = !escaped && (c == '\\'); // if last char was escape, then current char is not escaping anything
        }
        return s.substring(i, j);
    }

    protected static Optional<LqlToken> parseToken(String rawValue) {
        String lowerCaseValue = rawValue.toLowerCase();
        if (OPERATORS.contains(lowerCaseValue)) {
            return Optional.of(new LqlToken(LqlTokenType.OPERATOR, lowerCaseValue, rawValue));
        } else if (KEYWORDS.contains(lowerCaseValue)) {
            return Optional.of(new LqlToken(LqlTokenType.KEYWORD, lowerCaseValue, rawValue));
        } else if (isIdentifier(lowerCaseValue)) {
            return Optional.of(new LqlToken(LqlTokenType.IDENTIFIER, rawValue));
        } else if (isNumberLiteral(rawValue)) {
            return Optional.of(new LqlToken((LqlTokenType.LITERAL), rawValue));
        } else if (isStringLiteral(rawValue)) {
            return Optional.of(new LqlToken(LqlTokenType.LITERAL, parseStringLiteral(rawValue), rawValue));
        } else {
            // default to literal (this will help on unquoted strings...)
            return Optional.of(new LqlToken(LqlTokenType.LITERAL, rawValue, rawValue));
        }
        // TODO more literal types?
//        return Optional.empty();
    }

    protected static boolean isSeparator(char c) {
        return Character.isWhitespace(c) || SEPARATORS.contains(c);
    }

    protected static boolean isIdentifier(String value) {
        return IDENTIFIER_PATTERN.matcher(value).matches() || RUNBOOK_VARIABLE_EXTERNAL_PATTERN.matcher(value).matches();
    }

    private static boolean isStringLiteral(String value) {
        return value.length() > 2 && value.startsWith("\"") && value.endsWith("\""); // TODO improve?
    }

    protected static String parseStringLiteral(String rawValue) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = 1; i < rawValue.length() - 1; i++) {
            char c = rawValue.charAt(i);
            if (c == '"') {
                if (escaped) {
                    builder.append(c);
                }
            } else if (c == '\\') {
                if (escaped) {
                    builder.append(c);
                }
            } else {
                builder.append(c);
            }
            escaped = !escaped && (c == '\\'); // if last char was escape, then current char is not escaping anything
        }
        return builder.toString();
    }

    private static boolean isNumberLiteral(String value) {
        return NUMBER_PATTERN.matcher(value).matches();
    }
}
