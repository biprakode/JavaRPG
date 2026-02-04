package controller;

import model.ChallengeDifficulty;
import model.ChallengeType;

import java.net.http.HttpClient;
import java.util.Map;

public class LLMServiceImpl implements LLMService{
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final HttpClient httpClient;
    private final int timeout; // seconds
    private final int maxRetries;

    public LLMServiceImpl(String apiKey, String endpoint, String model, HttpClient httpClient, int timeout, int maxRetries) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.httpClient = httpClient;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
    }

    String generateChallenge(ChallengeType type, ChallengeDifficulty difficulty, String context) {

    }


    @Override
    public String generateChallenge(String prompt) {
        return "";
    }

    @Override
    public String evaluateResponse(String playerResponse, String expectedPattern, String challengeContext) {
        return "";
    }

    @Override
    public String generateHint(String challengePrompt, int hintLevel) {
        return "";
    }

    @Override
    public Map<String, String> parseJsonResponse(String llmResponse) {
        return Map.of();
    }
}
