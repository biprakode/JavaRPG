package model;

public enum ChallengeType {
    RIDDLE,
    COMBAT_CREATIVE,
    COMBAT_STANDARD,
    NEGOTIATION,
    PUZZLE,
    MORAL_DILEMMA;

    public boolean requiresLLM() {
        return switch (this) {
            case RIDDLE , COMBAT_CREATIVE , NEGOTIATION , PUZZLE , MORAL_DILEMMA -> true;
            case COMBAT_STANDARD -> false;
        };
    }
    public int getBaseTimeout() {
        return switch (this) {
            case RIDDLE,PUZZLE,MORAL_DILEMMA -> 120;
            case COMBAT_CREATIVE -> 60;
            case COMBAT_STANDARD -> 10;
            case NEGOTIATION -> 90;
        };
    }
    public int getMaxAttempts() {
        return switch (this) {
            case RIDDLE,PUZZLE,MORAL_DILEMMA -> 3;
            case COMBAT_CREATIVE , COMBAT_STANDARD,NEGOTIATION -> 2;
        };
    }
}
