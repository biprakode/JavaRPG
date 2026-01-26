package controller;

public enum Action {
    MOVE, TAKE, USE, ATTACK, EXAMINE, TALK, INVENTORY, HELP, SAVE, QUIT, LOAD, STATS, UNKNOWN;

    public boolean isSystemCommand() {
        return this == HELP || this == SAVE || this == LOAD || this == QUIT;
    }
    public boolean isGameAction() {
        return this == MOVE || this == ATTACK || this == EXAMINE || this == TAKE || this == USE || this == TALK;
    }
}
