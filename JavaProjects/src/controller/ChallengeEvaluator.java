package controller;

import model.ChallengeResult;

public interface ChallengeEvaluator {
    // Parse LLM evaluation response into ChallengeResult
    ChallengeResult parseEvaluation(String llmEvaluation);
}
