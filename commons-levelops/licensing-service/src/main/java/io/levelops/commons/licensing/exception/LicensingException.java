package io.levelops.commons.licensing.exception;

public class LicensingException extends Exception{

    public LicensingException() {}

    public LicensingException(String message) {
        super(message);
    }

    public LicensingException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicensingException(Throwable cause) {
        super(cause);
    }

}
