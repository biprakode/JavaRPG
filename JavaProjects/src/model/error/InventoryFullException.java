package model.error;

public class InventoryFullException extends RuntimeException {
    public InventoryFullException(String message) {
        super(message);
    }
}