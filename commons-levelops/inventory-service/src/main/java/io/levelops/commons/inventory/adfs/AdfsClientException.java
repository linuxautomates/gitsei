package io.levelops.commons.inventory.adfs;

public class AdfsClientException extends Exception {

    public AdfsClientException() {
    }

    public AdfsClientException(String message) {
        super(message);
    }

    public AdfsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdfsClientException(Throwable cause) {
        super(cause);
    }

}
