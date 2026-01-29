package model;

public enum monsterType {
    BEAST, UNDEAD, CONSTRUCT, HUMANOID;

    public String getType() {
        return switch (this) {
            case BEAST -> "Beast";
            case UNDEAD -> "Undead";
            case CONSTRUCT -> "Construct";
            case HUMANOID -> "Humanoid";
        };
    }
}
