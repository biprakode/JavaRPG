package controller;

import model.ChallengeDifficulty;
import model.ChallengeType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class LLMServiceImpl implements LLMService {
    private final String endpoint;
    private final String model;
    private final HttpClient httpClient;
    private final int timeout; // seconds
    private final int maxRetries;

    public LLMServiceImpl(String endpoint, String model, int timeout, int maxRetries) {
        this.endpoint = endpoint;
        this.model = model;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.httpClient = HttpClient.newHttpClient();
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        return String.format(
                "{\"model\": \"%s\", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"%s\"}," +
                        "{\"role\": \"user\", \"content\": \"%s\"}" +
                        "], \"temperature\": 0.7}",
                model,
                systemPrompt.replace("\"", "\\\""),
                userPrompt.replace("\"", "\\\"")
        );
    }

    private String sendRequest(String systemPrompt, String userPrompt) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(systemPrompt, userPrompt)))
                .timeout(Duration.ofSeconds(timeout))
                .build();

        int attempts = 0;
        while (attempts < maxRetries) {
            attempts++;
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    System.err.println("LLM server responded with error code: " + response.statusCode());
                    if (attempts < maxRetries) {
                        System.err.println("Retrying... (" + attempts + "/" + maxRetries + ")");
                    }
                }
            } catch (java.net.ConnectException e) {
                System.err.println("Could not connect to LLM server at " + endpoint + ". Is llama-server running?");
                break; // no point retrying connection refused
            } catch (IOException | InterruptedException e) {
                System.err.println("LLM request failed: " + e.getMessage());
                if (attempts < maxRetries) {
                    System.err.println("Retrying... (" + attempts + "/" + maxRetries + ")");
                }
            }
        }
        return null;
    }

    private String extractJsonFromResponse(String responseBody) {
        try {
            int contentStart = responseBody.indexOf("\"content\":") + 10;
            // skip whitespace and opening quote
            int quoteStart = responseBody.indexOf("\"", contentStart);
            String contentValue = responseBody.substring(quoteStart + 1);

            // Unescape the content first
            String unescaped = contentValue.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");

            // Find the JSON object within the unescaped content
            int firstBrace = unescaped.indexOf("{");
            int lastBrace = unescaped.lastIndexOf("}");

            if (firstBrace == -1 || lastBrace == -1) {
                return "{\"error\": \"No JSON object found in LLM response\"}";
            }

            return unescaped.substring(firstBrace, lastBrace + 1);
        } catch (Exception e) {
            return "{\"error\": \"Failed to parse LLM output\"}";
        }
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            int contentStart = responseBody.indexOf("\"content\":") + 10;
            // skip to the opening quote of the content value
            int quoteStart = responseBody.indexOf("\"", contentStart);
            int quoteEnd = responseBody.indexOf("\"", quoteStart + 1);
            String text = responseBody.substring(quoteStart + 1, quoteEnd);

            return text.replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return "Look closer at the details.";
        }
    }

    @Override
    public String generateChallenge(ChallengeType type, ChallengeDifficulty difficulty, String context) {
        String systemPrompt = getSystemPromptChallenge(type, difficulty);
        String userPrompt = getUserPromptChallenge(type, difficulty, context);

        String responseBody = sendRequest(systemPrompt, userPrompt);
        if (responseBody == null) {
            return null;
        }
        return extractJsonFromResponse(responseBody);
    }

    @Override
    public String generateChallenge(String challengePrompt) {
        String systemPrompt = "You are a creative game master generating challenges for a text-based RPG. " +
                "Return ONLY valid JSON with fields: prompt, correctAnswer, hint1, hint2, hint3, " +
                "expectedAnswerPattern, desc, alternateAnswers.";

        String responseBody = sendRequest(systemPrompt, challengePrompt);
        if (responseBody == null) {
            return null;
        }
        return extractJsonFromResponse(responseBody);
    }

    private static String getUserPromptChallenge(ChallengeType type, ChallengeDifficulty difficulty, String context) {
        String difficultyGuidelines = switch (difficulty) {
            case EASY -> "Simple, obvious answers";
            case MEDIUM -> "Requires thought, multiple steps";
            case HARD -> "Complex, requires creativity";
            case ULTRA -> "Extremely difficult, obscure knowledge";
        };

        return String.format(
                """
                        Generate a %s challenge with these requirements:
                        Difficulty: %s (%s)
                        Context: %s
                        Format: Return JSON with fields: prompt, correctAnswer, hint1, hint2, hint3, \
                        expectedAnswerPattern, desc, alternateAnswers""",
                type, difficulty, difficultyGuidelines, context);
    }

    private static String getSystemPromptChallenge(ChallengeType type, ChallengeDifficulty difficulty) {
        String challengeNature = switch (type) {
            case RIDDLE -> "knowledge-based";
            case COMBAT_CREATIVE, NEGOTIATION, MORAL_DILEMMA -> "creative";
            case COMBAT_STANDARD, PUZZLE -> "solvable";
        };

        return String.format(
                "You are a creative game master generating challenges for a text-based RPG. " +
                "Create a %s challenge at %s difficulty level. " +
                "The challenge should be %s based on type. " +
                "Return ONLY valid JSON with no additional text.", type, difficulty, challengeNature);
    }

    @Override
    public String evaluateResponse(String playerResponse, String expectedPattern, String challengeContext) {
        String systemPrompt = "You are an RPG Game Master. Evaluate if the player's response matches " +
                "the correct answer semantically. " +
                "Return ONLY JSON with fields: isCorrect (bool), confidence (float), " +
                "reasoning (string), effectiveness (FULL/PARTIAL/NONE).";

        String userPrompt = String.format(
                "Challenge: %s\nExpected Answer: %s\nPlayer's Response: %s\n",
                challengeContext, expectedPattern, playerResponse
        );

        String responseBody = sendRequest(systemPrompt, userPrompt);
        if (responseBody == null) {
            return null;
        }
        return extractJsonFromResponse(responseBody);
    }

    @Override
    public String generateHint(String challengePrompt, String expectedAnswer, int hintLevel) {
        String levelDescription = switch (hintLevel) {
            case 1 -> "Level 1: Very subtle, a cryptic nudge or thematic clue.";
            case 2 -> "Level 2: More direct, narrows down possibilities.";
            case 3 -> "Level 3: Very obvious, almost gives the answer away.";
            default -> "Level 1: Very subtle.";
        };

        String systemPrompt = "You are a creative RPG Game Master. Provide a hint for a challenge. " +
                "Return ONLY the hint text. No JSON, no quotes, no conversational filler.";

        String userPrompt = String.format(
                "Challenge: %s\nAnswer: %s\nTarget Intensity: %s\nGenerate the hint now:",
                challengePrompt, expectedAnswer, levelDescription);

        String responseBody = sendRequest(systemPrompt, userPrompt);
        if (responseBody == null) {
            return null;
        }
        return extractTextFromResponse(responseBody);
    }

    @Override
    public Map<String, String> parseJsonResponse(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        // Strip markdown code fences if present
        String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```", "").trim();

        // Extract key-value pairs from JSON manually (no external deps)
        String[] keys = {"prompt", "correctAnswer", "hint1", "hint2", "hint3",
                "expectedAnswerPattern", "desc", "alternateAnswers",
                "isCorrect", "confidence", "reasoning", "effectiveness"};

        for (String key : keys) {
            String value = extractJsonValue(cleaned, key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex + pattern.length());
        if (colonIndex == -1) return null;

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            // String value
            int valueEnd = json.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else if (firstChar == '[') {
            // Array value — return as-is
            int depth = 0;
            for (int i = valueStart; i < json.length(); i++) {
                if (json.charAt(i) == '[') depth++;
                if (json.charAt(i) == ']') depth--;
                if (depth == 0) return json.substring(valueStart, i + 1);
            }
        } else {
            // Boolean/number — read until comma, brace, or bracket
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ','
                    && json.charAt(valueEnd) != '}' && json.charAt(valueEnd) != ']') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
        return null;
    }

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.replace("/chat/completions", "/models")))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
