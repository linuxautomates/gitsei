package io.levelops.api.exceptions;

import io.levelops.models.PreflightCheckResults;

public class PreflightCheckFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -1923601083222549017L;
    private final PreflightCheckResults preflightCheckResults;

    public PreflightCheckFailedException(PreflightCheckResults preflightCheckResults) {
        this.preflightCheckResults = preflightCheckResults;
    }

    public PreflightCheckResults getPreflightCheckResults() {
        return preflightCheckResults;
    }
}
