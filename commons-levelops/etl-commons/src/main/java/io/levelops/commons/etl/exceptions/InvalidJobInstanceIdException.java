package io.levelops.commons.etl.exceptions;

public class InvalidJobInstanceIdException extends Exception {
    public InvalidJobInstanceIdException(String jobInstanceId) {
        super("Invalid job instance id: " + jobInstanceId);
    }
}
