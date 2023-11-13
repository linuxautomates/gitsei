package io.levelops.aggregations.exceptions;

public class AggregationFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -898868578218844469L;

    public AggregationFailedException(String message) {
        super(message);
    }
    
    public AggregationFailedException(String message, Exception e) {
        super(message, e);
    }

}