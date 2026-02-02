package controller;

import java.util.Map;

public interface LLMService {
    String generateChallenge(String prompt);
    String evaluateResponse(String playerResponse, String expectedPattern, String challengeContext);
    String generateHint(String challengePrompt, int hintLevel);
    Map<String, String> parseJsonResponse(String llmResponse);
}