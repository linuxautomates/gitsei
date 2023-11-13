package io.levelops.lql.exceptions;

import lombok.Getter;

@Getter
public class LqlLexicalException extends LqlException {

    private static final int CONTEXT_RADIUS = 5;
    private static final String ELLIPSIS = "…";

    private final String lql;
    private final int index;

    public LqlLexicalException(String message, String lql, int index) {
        super(generateMessage(message, lql, index));
        this.lql = lql;
        this.index = index;
    }

    private static String generateMessage(String message, String lql, int index) {
        return message + String.format(" at column %s (%s) in lql='%s'", index, context(lql, index), lql);
    }

    private static String context(String lql, int index) {
        return (index > CONTEXT_RADIUS ? ELLIPSIS : "") +
                lql.substring(index - CONTEXT_RADIUS, index + CONTEXT_RADIUS)
                + (index < lql.length() - CONTEXT_RADIUS ? ELLIPSIS : "");
    }
}
