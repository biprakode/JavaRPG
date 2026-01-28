package model;

public enum Directions {
    NORTH("north", "n"),
    SOUTH("south", "s"),
    EAST("east", "e"),
    WEST("west", "w");

    private final String shortname;
    private final String fullname;

    Directions(String s , String f) {
        shortname = s;
        fullname = f;
    }

    public String getFullName() {
        return fullname;
    }

    public String getShortName() {
        return shortname;
    }

    public Directions getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

}
