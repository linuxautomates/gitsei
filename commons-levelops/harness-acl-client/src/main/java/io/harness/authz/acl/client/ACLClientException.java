package io.harness.authz.acl.client;

public class ACLClientException  extends Exception {

    public ACLClientException(){
    }

    public ACLClientException(String message){
        super(message);
    }

    public ACLClientException(String message, Throwable cause){
        super(message, cause);
    }

    public ACLClientException(Throwable cause){
        super(cause);
    }
}
