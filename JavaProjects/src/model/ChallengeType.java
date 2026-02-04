package model;

public enum ChallengeType {
    RIDDLE,
    COMBAT_CREATIVE,
    COMBAT_STANDARD,
    NEGOTIATION,
    PUZZLE,
    MORAL_DILEMMA;

    public String getChallengeType() {
        return switch (this) {
            case RIDDLE -> "Riddle";
            case COMBAT_CREATIVE -> "Creative Combat";
            case COMBAT_STANDARD -> "Standard Combat";
            case NEGOTIATION -> "Negotiation";
            case PUZZLE -> "Puzzle";
            case MORAL_DILEMMA -> "Moral Dilemma";
        };
    }

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
