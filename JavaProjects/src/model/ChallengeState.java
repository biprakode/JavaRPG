package model;

public enum ChallengeState {
    NONE,
    GENERATING,
    ACTIVE,
    EVALUATING ,
    HINT_OFFERED,
    COMPLETED,
    RESOLVED;

    public boolean canAcceptInput() {
        return switch (this) {
            case HINT_OFFERED,ACTIVE -> true;
            case GENERATING,NONE,GENERATING,COMPLETED,RESOLVED -> false;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return switch (this) {
            case RESOLVED -> true;
            default -> false;
        };
    }
}
