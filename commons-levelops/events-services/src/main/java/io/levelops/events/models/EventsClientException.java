package io.levelops.events.models;

public class EventsClientException extends Exception{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public EventsClientException() {
        super();
    }

    public EventsClientException(final String message) {
        super(message);
    }

    public EventsClientException(final Throwable t) {
        super(t);
    }

    public EventsClientException(final String message, final Throwable t) {
        super(message, t);
    }
}