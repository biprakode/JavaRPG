package model.error;

public class MonsterAlreadyDeadException extends RuntimeException {
    public MonsterAlreadyDeadException(String message) {
        super(message);
    }
}
