package model;

public enum MonsterDifficulty {
    EASY, MEDIUM, HARD;

    public String getDifficulty() {
        return switch (this) {
            case EASY -> "Easy";
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
        };
    }
}
