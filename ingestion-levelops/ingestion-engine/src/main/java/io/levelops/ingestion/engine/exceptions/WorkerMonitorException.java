package io.levelops.ingestion.engine.exceptions;

public class WorkerMonitorException extends Exception {
    public WorkerMonitorException() {
    }

    public WorkerMonitorException(String message) {
        super(message);
    }

    public WorkerMonitorException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkerMonitorException(Throwable cause) {
        super(cause);
    }
}
