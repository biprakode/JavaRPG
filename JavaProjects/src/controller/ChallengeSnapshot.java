package controller;

import model.ChallengeDifficulty;
import model.ChallengeState;
import model.ChallengeType;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ChallengeSnapshot(
        String id,
        ChallengeType type,
        ChallengeState state,
        String prompt,
        String desc,
        String expectAnswer,
        ChallengeDifficulty difficulty,
        double baseRewardXP,
        int timeLimit,
        int maxAttempts,
        int attemptsRemaining,
        List<String> playerResponses,
        int hintsUsed,
        Map<String, String> metadata,
        boolean isCompleted,
        boolean wasSuccessful,
        String finalFeedback,
        long startTime,
        long endTime
) implements Serializable { }
