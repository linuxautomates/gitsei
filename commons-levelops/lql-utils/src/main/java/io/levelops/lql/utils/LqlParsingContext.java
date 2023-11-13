package io.levelops.lql.utils;

import io.levelops.lql.exceptions.LqlSyntaxException;
import io.levelops.lql.models.LqlToken;

import java.util.List;

public class LqlParsingContext {

    private static final int ERROR_CONTEXT_WIDTH = 4;

    private final List<LqlToken> tokens;
    private int currentTokenIndex = -1;

    public LqlParsingContext(List<LqlToken> tokens) {
        this.tokens = tokens;
    }

    public LqlToken getCurrentToken() {
        if (currentTokenIndex < 0) {
            return null;
        }
        return tokens.get(currentTokenIndex);
    }

    public boolean hasNext() {
        return currentTokenIndex < tokens.size() - 1;
    }

    public LqlToken peekNextToken() throws LqlSyntaxException {
        if (!hasNext()) {
            raiseException("Unexpected end of input");
        }
        return getNextToken(1);
    }

    public LqlToken nextToken() throws LqlSyntaxException {
        LqlToken token = peekNextToken();
        currentTokenIndex++;
        return token;
    }

    private LqlToken getPreviousToken(int nth) {
        return currentTokenIndex >= nth ? tokens.get(currentTokenIndex - nth) : null;
    }

    private LqlToken getNextToken(int nth) {
        return currentTokenIndex < tokens.size() - nth ? tokens.get(currentTokenIndex + nth) : null;
    }

    private String getContextString(int width) {
        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        for (int i = 1; i < width + 1; ++i) {
            LqlToken previousToken = getPreviousToken(width + 1 - i);
            if (previousToken != null) {
                before.append(previousToken.getValue()).append(" ");
            }
            LqlToken nextToken = getNextToken(i);
            if (nextToken != null) {
                after.append(nextToken.getValue()).append(" ");
            }
        }
        return before.toString() + ">>>" + getCurrentToken().getValue() + "<<< " + after.toString();
    }

    public void raiseException(String message) throws LqlSyntaxException {
        String context = String.format(", at '%s' in '%s'",
                getCurrentToken(),
                getContextString(ERROR_CONTEXT_WIDTH));
        throw new LqlSyntaxException(message + context);
    }
}
