package io.levelops.commons.inventory.exceptions;

public class InventoryException extends Exception {

    public InventoryException() {
    }

    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public InventoryException(Throwable cause) {
        super(cause);
    }
}
