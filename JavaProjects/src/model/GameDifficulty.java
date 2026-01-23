package model;

public enum GameDifficulty {
    EASY, MEDIUM, HARD, ULTRA;
    public String getDescription() {
        return switch (this) {
            case EASY -> "Forgiving - 4 lives, weaker enemies";
            case MEDIUM -> "Balanced - 3 lives, normal enemies";
            case HARD -> "Challenging - 2 lives, tough enemies";
            case ULTRA -> "Permadeath - 1 life, brutal enemies";
        };
    }
}
