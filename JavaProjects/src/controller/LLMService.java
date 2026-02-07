package controller;

import model.ChallengeDifficulty;
import model.ChallengeType;

import java.util.Map;

public interface LLMService {
    String generateChallenge(ChallengeType type, ChallengeDifficulty difficulty, String context);
    String generateChallenge(String challengePrompt);
    String evaluateResponse(String playerResponse, String expectedPattern, String challengeContext);
    String generateHint(String challengePrompt, String expectedAnswer, int hintLevel);
    Map<String, String> parseJsonResponse(String json);
    boolean isAvailable();
}
