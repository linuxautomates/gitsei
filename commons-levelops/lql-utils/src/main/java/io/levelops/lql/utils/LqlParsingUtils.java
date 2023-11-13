package io.levelops.lql.utils;

import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlToken;
import io.levelops.lql.models.LqlTokenType;

import java.util.Arrays;

public class LqlParsingUtils {

    public static boolean checkTokenType(LqlToken token, LqlTokenType... types) {
        for (LqlTokenType type : types) {
            if (token.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static void expectTokenType(LqlParsingContext context, LqlToken token, LqlTokenType... types) throws LqlSyntaxException {
        if (!checkTokenType(token, types)) {
            context.raiseException(String.format("Unexpected token (wanted types=%s)", Arrays.toString(types)));
        }
    }

    public static boolean checkTokenValue(LqlToken token, String... values) {
        for (String value : values) {
            if (token.getValue().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkToken(LqlToken token, LqlTokenType type, String... values) {
        return checkTokenType(token, type) && checkTokenValue(token, values);
    }

    public static void expectToken(LqlParsingContext context, LqlToken token, LqlTokenType type, String... values) throws LqlSyntaxException {
        if (!checkToken(token, type, values)) {
            context.raiseException(String.format("Unexpected token (wanted type=%s, values=%s)", type, Arrays.toString(values)));
        }
    }
}
