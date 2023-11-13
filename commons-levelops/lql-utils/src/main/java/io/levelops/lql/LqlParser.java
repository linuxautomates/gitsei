package io.levelops.lql;

import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlToken;
import io.levelops.lql.models.LqlTokenType;
import io.levelops.lql.utils.LqlParsingContext;

import java.util.List;

import static io.levelops.lql.utils.LqlParsingUtils.checkToken;
import static io.levelops.lql.utils.LqlParsingUtils.checkTokenType;
import static io.levelops.lql.utils.LqlParsingUtils.expectToken;
import static io.levelops.lql.utils.LqlParsingUtils.expectTokenType;

public class LqlParser {

    /**
     * Syntactic analysis (parser): Tokens -> AST
     */
    public static LqlAst parseTokens(List<LqlToken> tokens) throws LqlSyntaxException {
        LqlParsingContext context = new LqlParsingContext(tokens);
        LqlAst ast = parseStatement(context);

        // check that all the tokens have been consumed
        if (context.hasNext()) {
            context.nextToken(); // move context to unexpected token
            context.raiseException("Malformed expression: unexpected token(s)");
        }
        return ast;
    }

    protected static LqlAst parseStatement(LqlParsingContext context) throws LqlSyntaxException {
        // STMT ← AND_STMT (KEYWORD<or> AND_STMT)*

        LqlAst node = parseAndStatement(context);

        while (context.hasNext()) {
            LqlToken currentToken = context.peekNextToken();
            if (!checkToken(currentToken, LqlTokenType.KEYWORD, "or")) {
                break;
            }
            context.nextToken();

            LqlAst cdr = parseAndStatement(context);
            node = LqlAst.builder()
                    .token(currentToken)
                    .child(node)
                    .child(cdr)
                    .build();
        }

        return node;
    }


    protected static LqlAst parseAndStatement(LqlParsingContext context) throws LqlSyntaxException {
        // AND_STMT ← EXPR (KEYWORD<and> AND_STMT)*

        LqlAst node = parseExpr(context);

        while (context.hasNext()) {
            LqlToken currentToken = context.peekNextToken();
            if (!checkToken(currentToken, LqlTokenType.KEYWORD, "and")) {
                break;
            }
            context.nextToken();

            LqlAst cdr = parseAndStatement(context);
            node = LqlAst.builder()
                    .token(currentToken)
                    .child(node)
                    .child(cdr)
                    .build();
        }

        return node;
    }

    protected static LqlAst parseExpr(LqlParsingContext context) throws LqlSyntaxException {
        // EXPR <- KEYWORD<not>? EXPR
        // EXPR <- TERM | (LPAREN STMT RPAREN)

        LqlToken nextToken = context.peekNextToken();
        boolean not = checkToken(nextToken, LqlTokenType.KEYWORD, "not");
        if (not) {
            context.nextToken();
            LqlAst expr = parseExpr(context);
            return LqlAst.builder()
                    .token(nextToken)
                    .child(expr)
                    .build();
        }

        boolean lparen = checkTokenType(nextToken, LqlTokenType.LPAREN);
        if (lparen) {
            context.nextToken();
            LqlAst statement = parseStatement(context);
            nextToken = context.nextToken();
            expectTokenType(context, nextToken, LqlTokenType.RPAREN);
            return statement;
        }

        return parseTerm(context);
    }

    protected static LqlAst parseTerm(LqlParsingContext context) throws LqlSyntaxException {
//        TERM ← IDENTIFIER (OPERATOR<in> | OPERATOR<nin>) LBRACKET (LITERAL COMMA)* LITERAL RBRACKET
//        TERM ← IDENTIFIER OPERATOR LITERAL
//        TERM ← IDENTIFIER KEYWORD<is> (KEYWORD<not>)? (KEYWORD<null> | KEYWORD<empty>)

        LqlToken identifier = context.nextToken();
        expectTokenType(context, identifier, LqlTokenType.IDENTIFIER);

        LqlToken operatorOrKeyword = context.nextToken();
        expectTokenType(context, operatorOrKeyword, LqlTokenType.OPERATOR, LqlTokenType.KEYWORD);

        if (checkToken(operatorOrKeyword, LqlTokenType.KEYWORD, "is")) {
            // TERM ← IDENTIFIER KEYWORD<is> (KEYWORD<not>)? (KEYWORD<null> | KEYWORD<empty>)

            LqlToken firstToken = context.nextToken();
            boolean not = checkToken(firstToken, LqlTokenType.KEYWORD, "not");
            if (not) {
                LqlToken secondToken = context.nextToken();
                expectToken(context, secondToken, LqlTokenType.KEYWORD, "null", "empty");
                return LqlAst.operator(operatorOrKeyword, identifier, firstToken, secondToken);
            } else {
                expectToken(context, firstToken, LqlTokenType.KEYWORD, "null", "empty");
                return LqlAst.operator(operatorOrKeyword, identifier, firstToken);
            }

        } else if (checkToken(operatorOrKeyword, LqlTokenType.OPERATOR, "in", "nin")) {
            //  TERM ← IDENTIFIER (OPERATOR<in> | OPERATOR<nin>) LBRACKET (LITERAL COMMA)* LITERAL RBRACKET
            expectTokenType(context, context.nextToken(), LqlTokenType.LBRACKET);

            LqlAst.LqlAstBuilder arrayBuilder = LqlAst.builder()
                    .token(operatorOrKeyword)
                    .child(LqlAst.leaf(identifier));

            LqlToken currentToken;
            do {
                currentToken = context.nextToken();
                // expect LITERAL (adding IDENTIFIER for unquoted strings)
                expectTokenType(context, currentToken, LqlTokenType.LITERAL, LqlTokenType.IDENTIFIER);
                arrayBuilder.child(LqlAst.leaf(currentToken));

                currentToken = context.nextToken();
            } while (checkTokenType(currentToken, LqlTokenType.COMMA));

            expectTokenType(context, currentToken, LqlTokenType.RBRACKET);

            return arrayBuilder.build();
        } else {
            // TERM ← IDENTIFIER OPERATOR LITERAL
            expectTokenType(context, operatorOrKeyword, LqlTokenType.OPERATOR);
            LqlToken literal = context.nextToken();
            // expect LITERAL (adding IDENTIFIER for unquoted strings)
            expectTokenType(context, literal, LqlTokenType.LITERAL, LqlTokenType.IDENTIFIER);
            return LqlAst.operator(operatorOrKeyword, identifier, literal);
        }
    }

}
