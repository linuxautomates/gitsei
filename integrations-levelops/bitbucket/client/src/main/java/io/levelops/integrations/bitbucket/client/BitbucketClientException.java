package io.levelops.integrations.bitbucket.client;

import io.levelops.ingestion.exceptions.FetchException;

public class BitbucketClientException extends FetchException  {
    public BitbucketClientException() {
    }

    public BitbucketClientException(String message) {
        super(message);
    }

    public BitbucketClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitbucketClientException(Throwable cause) {
        super(cause);
    }
}
