package controller;

import model.ChallengeResult;

public class ChallengeEvaluatorImpl implements ChallengeEvaluator {
    public ChallengeEvaluatorImpl() {}

    @Override
    public ChallengeResult parseEvaluation(String llmEvaluation) {
        // Expected input
//        {
//            "isCorrect": true,
//                "confidence": 0.95,
//                "reasoning": "The player's answer matches the expected answer semantically",
//                "effectiveness": "FULL"
//        }

        if(llmEvaluation == null || llmEvaluation.isEmpty()) {
            return new ChallengeResult(false , "No evaluation result received");
        }

        String cleaned = this.cleanResponse(llmEvaluation);
        boolean success = extractBoolean(cleaned , "isCorrect");
        Double confidence = extractDouble(cleaned);
        String reasoning = extractString(cleaned , "reasoning");
        String effect = extractString(cleaned , "effectiveness");
        int effectRating = mapEffect(effect , confidence);

        String feedback = reasoning != null ? reasoning : (success ? "Correct!" : "Incorrect.");
        return new ChallengeResult(success , feedback).withEffectiveness(effectRating);
    }

    private String extractRawValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex + pattern.length());
        if (colonIndex == -1) return null;

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && json.charAt(valueEnd) != ','
                && json.charAt(valueEnd) != '}') {
            valueEnd++;
        }

        return json.substring(valueStart, valueEnd).trim().replace("\"", "");
    }

    private int mapEffect(String effect , Double conf) {
        if(effect == null) {
            return (int) (conf * 100);
        }

        return switch (effect.toUpperCase()) {
            case "FULL" -> (int) (100 * Math.max(conf , 0.8));
            case "PARTIAL" -> (int) (50 * Math.max(conf , 0.5));
            case "NONE" -> 0;
            default -> (int) (conf * 100);
        };
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex + pattern.length());
        if (colonIndex == -1) return null;

        int quoteStart = json.indexOf("\"", colonIndex + 1);
        if (quoteStart == -1) return null;

        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return null;

        return json.substring(quoteStart + 1, quoteEnd);

    }

    private Double extractDouble(String cleaned) {
        String value = extractRawValue(cleaned, "confidence");
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private boolean extractBoolean(String cleaned, String key) {
        String value = extractRawValue(cleaned , key);
        return "true".equalsIgnoreCase(value);
    }

    private String cleanResponse(String llmEvaluation) {
        String cleaned = llmEvaluation.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}') + 1;

        if (start == -1 || end == 0) {
            return cleaned;
        }

        return cleaned.substring(start, end);
    }
}
