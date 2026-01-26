package controller;

import java.util.List;

public class Command {
    private Action action;
    private String target;
    private String originalInput;
    private List<String> tokens;

    public Command(Action type, String target) {
        this.action = type;
        this.target = target;
    }

    public boolean hasTarget() {
        return target != null && !target.isEmpty();
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return String.format("Command[type=%s, target='%s']", action, target);
    }
}
