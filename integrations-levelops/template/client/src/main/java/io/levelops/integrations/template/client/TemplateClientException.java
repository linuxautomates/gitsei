package io.levelops.integrations.template.client;

import io.levelops.ingestion.exceptions.FetchException;

public class TemplateClientException extends FetchException{

    /**
     *
     */
    private static final long serialVersionUID = 1370860759010040266L;

    public TemplateClientException() {
    }

    public TemplateClientException(String message) {
        super(message);
    }

    public TemplateClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TemplateClientException(Throwable cause) {
        super(cause);
    }

}