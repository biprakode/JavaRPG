package model.error;

public abstract class GameRuntimeException extends RuntimeException {
    public GameRuntimeException(String message) {
        super(message);
    }
}
