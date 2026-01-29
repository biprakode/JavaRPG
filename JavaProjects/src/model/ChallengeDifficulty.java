package model;

public enum ChallengeDifficulty {
    EASY, MEDIUM, HARD, ULTRA;

    double getrewardXP() {
        return (double) switch (this) {
            case EASY -> 1.5;
            case MEDIUM -> 1.75;
            case HARD -> 2.0;
            case ULTRA -> 2.5;
        };
    }
}
