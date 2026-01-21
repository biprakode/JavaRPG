package model.error;

public class PlayerAlreadyDeadException extends GameRuntimeException {
    public PlayerAlreadyDeadException(String message) {
        super(message);
    }
}
