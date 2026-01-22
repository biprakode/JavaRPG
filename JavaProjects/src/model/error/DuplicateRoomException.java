package model.error;

public class DuplicateRoomException extends RuntimeException {
    public DuplicateRoomException(String message) {
        super(message);
    }
}
