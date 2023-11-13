package io.levelops.triggers.clients;

public class TriggersRESTClientException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    public TriggersRESTClientException(){
        super();
    }

    public TriggersRESTClientException(String message){
        super(message);
    }

    public TriggersRESTClientException(Throwable t){
        super(t);
    }

    public TriggersRESTClientException(String message, Throwable t){
        super(message, t);
    }
}
