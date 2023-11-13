package io.levelops.runbooks.models;

public class RunbookClientException extends Exception{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    public RunbookClientException(){
        super();
    }
    
    public RunbookClientException(String message){
        super(message);
    }
    
    public RunbookClientException(Throwable t){
        super(t);
    }
    
    public RunbookClientException(String message, Throwable t){
        super(message, t);
    }
}