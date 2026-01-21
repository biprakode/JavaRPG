package model.error;

public class InvalidCombatActionException extends RuntimeException{
    public InvalidCombatActionException(String message) {
        super(message);
    }
}
