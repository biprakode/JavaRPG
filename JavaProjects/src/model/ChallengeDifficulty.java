package model;

public enum ChallengeDifficulty {
    EASY, MEDIUM, HARD, ULTRA;

    public String getDifficulty() {
        return switch (this) {
            case EASY -> "Easy";
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
            case ULTRA -> "Ultra";
        };
    }

    double getrewardXP() {
        return (double) switch (this) {
            case EASY -> 1.5;
            case MEDIUM -> 1.75;
            case HARD -> 2.0;
            case ULTRA -> 2.5;
        };
    }
}
