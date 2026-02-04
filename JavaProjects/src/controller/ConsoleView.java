package controller;

import model.Challenge;
import model.ChallengeResult;

public interface ConsoleView {
    void displayMessage(String message);
    void displayChallenge(Challenge challenge);
    void displayResult(Challenge challenge, ChallengeResult result);

    void displayHint(String hint);
    void displayTimer(int secondsRemaining);
}
