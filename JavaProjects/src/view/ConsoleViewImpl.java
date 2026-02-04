package view;

import controller.ConsoleView;
import model.Challenge;
import model.ChallengeResult;
import model.Item;

import java.util.List;

public class ConsoleViewImpl implements ConsoleView {

    @Override
    public void displayMessage(String message) {
        System.out.println(message + '\n');
    }

    @Override
    public void displayChallenge(Challenge challenge) {
        String type = challenge.getType().toString().toLowerCase();
        String difficulty = challenge.getDifficulty().toString();
        String prompt = challenge.getPrompt();
        String timeLimit = String.valueOf(challenge.getTimeLimit());
        int attemptsRemaining = challenge.getAttemptsRemaining();

        String challengeString = visualSeperator() +
                "CHALLENGE: " + type + " (" + difficulty + ")\n" +
                visualSeperator() +
                prompt + "\n" +
                "Time Limit: " + timeLimit + " | Attempts: " + attemptsRemaining + " remaining\n" +
                visualSeperator();

        displayMessage(challengeString);
    }

    @Override
    public void displayResult(Challenge challenge, ChallengeResult result) {
        boolean success = result.isSuccess();
        StringBuilder sb = new StringBuilder();
        String message = result.getFeedback();

        if (success) {
            int xpAwarded = result.getXpAwarded();
            List<Item> items = result.getItemsAwarded();

            sb.append("CORRECT!\n");
            sb.append("You earned ").append(xpAwarded).append(" XP!\n");
            sb.append(message).append("\n");

            sb.append("Item Rewarded: ");
            if (items == null || items.isEmpty()) {
                sb.append("No items rewarded");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    sb.append(items.get(i).getName());
                    if (i < items.size() - 1) sb.append(", ");
                }
            }
        } else {
            String type = challenge.getType().toString().toLowerCase();
            int damageTaken = result.getDamageTaken();
            int attemptsRemaining = challenge.getAttemptsRemaining();

            sb.append("INCORRECT\n");
            sb.append("You take ").append(damageTaken).append(" damage from the ").append(type).append("!\n");
            sb.append(message).append("\n");
            sb.append(attemptsRemaining).append(" attempts remaining.");
        }

        String resultString = sb.toString();
        displayMessage(resultString);
    }

    @Override
    public void displayHint(String hint, int xpCost) {
        StringBuilder sb = new StringBuilder();

        sb.append("HINT (-").append(xpCost).append(" XP)\n");
        sb.append(visualSeperator());
        sb.append(hint).append("\n");
        sb.append(visualSeperator());

        String hintString = sb.toString();
        displayMessage(hintString);
    }

    @Override
    public void displayTimer(int secondsRemaining) {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String YELLOW = "\u001B[33m";
        String CYAN = "\u001B[36m";

        StringBuilder sb = new StringBuilder();
        sb.append("\r");

        if (secondsRemaining <= 10) {
            // Urgent State: Red text with alert icon
            sb.append(RED).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining! ").append(RESET);
        } else if (secondsRemaining <= 20) {
            // Warning State: Yellow text
            sb.append(YELLOW).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining  ").append(RESET);
        } else {
            // Normal State: Cyan text
            sb.append(CYAN).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining  ").append(RESET);
        }

        System.out.print(sb.toString());
        System.out.flush();
    }

    private String visualSeperator() {
        return "══════════════════════════════════════════════════════════\n";
    }
}
