package io.levelops.integrations.bitbucket_server.client;

import io.levelops.ingestion.exceptions.FetchException;

public class BitbucketServerClientException extends FetchException {

    public BitbucketServerClientException() {
    }

    public BitbucketServerClientException(String message) {
        super(message);
    }

    public BitbucketServerClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitbucketServerClientException(Throwable cause) {
        super(cause);
    }
}
