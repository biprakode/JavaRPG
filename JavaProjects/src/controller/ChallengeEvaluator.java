package controller;

import model.ChallengeResult;

public interface ChallengeEvaluator {
    // Rule-based evaluation (regex/keyword matching)
    ChallengeResult evaluateWithRules(String response, String expectedPattern);
    // Parse LLM evaluation response into ChallengeResult
    ChallengeResult parseEvaluation(String llmEvaluation);
}