package io.levelops.aggregations_shared.exceptions;

public class InvalidJobInstanceIdException extends Exception {
    public InvalidJobInstanceIdException(String jobInstanceId) {
        super("Invalid job instance id: " + jobInstanceId);
    }
}
