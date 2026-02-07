package controller;

import controller.error.ChallengeAlreadyComplete;
import controller.error.ChallengeNotActive;
import model.*;

import java.util.*;

public class ChallengeController {
    private LLMService llmService;
    private ChallengeEvaluator challengeEvaluator;
    private ConsoleView view;
    private GameState gameState;

    private Challenge activeChallenge;
    private ChallengeContext currentContext;
    private Timer challengeTimer;

    private List<Challenge> completedChallenges;
    private Queue<ChallengeType> recentTypes;

    private boolean allowHints;

    public ChallengeController(LLMService llm, ConsoleView v, GameState st) {
        llmService = llm;
        view = v;
        gameState = st;
        if(st.getDifficulty() != GameDifficulty.ULTRA) {
            allowHints = true;
        }
    }

    public void initiateChallenge(Room room, ChallengeType type) {
        if (!llmService.isAvailable()) {
            view.displayError("LLM server is offline. Go touch some grass.");
            return;
        }
        if(activeChallenge != null && activeChallenge.isChallengeCompleted()) {
            throw new ChallengeAlreadyComplete("Cannot initiate already completed challenge");
        }
        if(type == null) {
            type = switch (room.getRoomtype()) {
                case NORMAL -> ChallengeType.RIDDLE;
                case BOSS -> ChallengeType.COMBAT_STANDARD;
                case SAFE -> ChallengeType.PUZZLE;
            };
        }
        currentContext = buildContext(room , type);
        generateChallenge(currentContext);
        if (activeChallenge == null) {
            view.displayError("LLM failed to generate a challenge. Go touch some grass.");
            return;
        }
        activeChallenge.setChallengeState(ChallengeState.ACTIVE);
    }

    public void submitResponse(String response) {
        if(activeChallenge.getChallengeState() != ChallengeState.ACTIVE) {
            throw new ChallengeNotActive("Cannot respond to not Active challenge");
        }
        if(activeChallenge.hasTimedOut()) {
            view.displayMessage("Challenge opportunity has passed");
            handleTimeout();
            return;
        }
        activeChallenge.recordResponse(response);
        evaluateResponse(response);
    }

    public void requiresHint(int level) {
        if(!allowHints) {
            view.displayMessage("Sorry, no hints allowed");
            return;
        }
        int cost = calculateHintCost(level);

        String hint = generateHint(level);
        view.displayHint(hint , cost);
        activeChallenge.incrementHints();

        double deduct = switch (gameState.getDifficulty()) {
            case EASY -> 0.05;
            case MEDIUM -> 0.08;
            case HARD -> 0.12;
            case ULTRA -> 0.18;
        };
        double xp = activeChallenge.getbaseXP();
        activeChallenge.setbaseXP(xp - xp * deduct);

    }

    //Challenge Context Building for LLM
    private ChallengeContext buildContext(Room room, ChallengeType type) {
        ChallengeContext context = new ChallengeContext()
                .withPlayer(gameState.getPlayer())
                .withRoom(room)
                .withChallengeType(type)
                .withDifficulty(mapGameToChallengeDifficulty(gameState.getDifficulty()));
        if (room.hasMonster()) {
            context.withMonster(room.getMonster());
        }
        enrichContextWithHistory(context);
        return context;
    }

    private void enrichContextWithHistory(ChallengeContext context) {
        List<ChallengeType> recent = getRecentChallengeTypes(5);
        if (!recent.isEmpty()) {
            context.withHistory(recent);
        }
    }

    //ChallengeLifeCycle
    private void generateChallenge(ChallengeContext context) {
        String llmPrompt = context.buildLLMPrompt();
        String llmResponse = llmService.generateChallenge(llmPrompt);
        // Parse JSON response
        Map<String, String> parsed = llmService.parseJsonResponse(llmResponse);
        activeChallenge = new Challenge(
                context.getChallengeType(),
                parsed.get("prompt"),
                mapGameToChallengeDifficulty(gameState.getDifficulty())
        );
        activeChallenge.addMetaData("hint1", parsed.get("hint1"));
        activeChallenge.addMetaData("hint2", parsed.get("hint2"));
        activeChallenge.addMetaData("hint3", parsed.get("hint3"));
        activeChallenge.addMetaData("expectedPattern", parsed.get("expectedAnswerPattern"));
        activeChallenge.addMetaData("desc", parsed.get("desc"));

        presentChallenge(activeChallenge);
        startChallengeTimer(activeChallenge.getTimeLimit());
    }

    private void presentChallenge(Challenge challenge) {
        view.displayChallenge(challenge);
        challenge.activate();
    }

    private void evaluateResponse(String response) {
        activeChallenge.startEval();
        ChallengeResult result = determineEvaluationMethod(response);
        if (result.isSuccess()) {
            applyConsequences(result);
            completeChallenge();
        } else {
            activeChallenge.decrementAttempts();
            view.displayResult(activeChallenge , result);
            if (!activeChallenge.hasAttemptsRemaining()) {
                result = new ChallengeResult(false, "No attempts remaining").withDamage(0, calculateDamage(activeChallenge, 0));
                applyConsequences(result);
                completeChallenge();
            }
        }
    }

    private void applyConsequences(ChallengeResult result) {
        Player player = gameState.getPlayer();
        if(result.getXpAwarded() > 0) {
            player.addXP(result.getXpAwarded());
        }
        if (result.getDamageTaken() > 0) {
            player.takeDamage(result.getDamageTaken());
        }
        if(result.getItemsAwarded() != null && !result.getItemsAwarded().isEmpty()) {
            for(Item item : result.getItemsAwarded()) {
                player.addInventory(item);
            }
        }
        view.displayResult(activeChallenge , result);
    }

    private void completeChallenge() {
        cancelTimer();
        activeChallenge.resolve();
        recordCompletedChallenge(activeChallenge);
        activeChallenge = null;
        currentContext = null;
    }

    public void abortChallenge() {
        if (activeChallenge == null) {
            return;
        }
        cancelTimer();
        ChallengeResult abortResult = new ChallengeResult(false, "Challenge abandoned")
                .withDamage(0, calculateDamage(activeChallenge, 0) / 2);
        applyConsequences(abortResult);
        activeChallenge.complete(false, "Aborted by player");
        completeChallenge();
    }

    private ChallengeResult determineEvaluationMethod(String response) {
        return evaluateWithLLM(response);
    }

    private ChallengeResult evaluateWithLLM(String response) {
        String expectedPattern = activeChallenge.getMetaData("expectedPattern");
        String challengePrompt = activeChallenge.getPrompt();

        String llmEval = llmService.evaluateResponse(response, expectedPattern, challengePrompt);
        if (llmEval == null) {
            view.displayError("LLM evaluation failed. Challenge cannot be scored.");
            return new ChallengeResult(false, "LLM offline — challenge evaluation unavailable");
        }
        ChallengeResult result = challengeEvaluator.parseEvaluation(llmEval);

        int effectiveness = result.getEffectivenessRating();
        return result.withXP(calculateXPReward(activeChallenge, effectiveness))
                .withDamage(calculateDamage(activeChallenge, effectiveness), 0)
                .withItems(determineItemRewards(activeChallenge));
    }

    // Reward Calculation
    private int calculateXPReward(Challenge challenge , int effectiveness) {
        double baseXP = challenge.getbaseXP();
        double effectivenessMultiplier = effectiveness / 100.0;
        int hintsUsed = challenge.getHintsUsed();
        double hintPenalty = 1.0 - (hintsUsed * 0.15); // 15 % penalty per hint
        return (int) (baseXP * effectivenessMultiplier * Math.max(hintPenalty , 0.4));
    }

    private int calculateDamage(Challenge challenge , int effectiveness) { // dealt to monster or taken on failure
        int baseDamage = switch (challenge.getDifficulty()) {
            case EASY -> 10;
            case MEDIUM -> 20;
            case HARD -> 35;
            case ULTRA -> 50;
        };
        return (int) (baseDamage * (effectiveness / 100.0));
    }

    private List<Item> determineItemRewards(Challenge challenge) {
        List<Item> rewards = new ArrayList<>();
        if(!challenge.getWasSuccesful()) {
            return rewards; // return empty list on failure
        }
        double dropchance = switch (challenge.getDifficulty()) {
            case EASY -> 0.1;
            case MEDIUM -> 0.2;
            case HARD -> 0.35;
            case ULTRA -> 0.5;
        };
        if(Math.random() < dropchance) {
            Item reward = generateRewardItem(challenge.getType());
            if (reward != null) {
                rewards.add(reward);
            }
        }
        return rewards;
    }

    // Hint System
    private String generateHint(int level) {
        String hintKey = "hint"+level;
        String storedHint = activeChallenge.getMetaData(hintKey);
        if(storedHint != null && !storedHint.isEmpty()) {
            return storedHint;
        }
        //generate hint through LLM
        String expectedAnswer = activeChallenge.getMetaData("expectedPattern");
        return llmService.generateHint(activeChallenge.getPrompt(), expectedAnswer, level);
    }

    private int calculateHintCost(int level) {
        int baseCost = switch (level) {
            case 1 -> 5;   // Subtle hint
            case 2 -> 15;  // Direct hint
            case 3 -> 30;  // Obvious hint
            default -> 10;
        };

        double multiplier = switch (gameState.getDifficulty()) {
            case EASY -> 0.5;
            case MEDIUM -> 1.0;
            case HARD -> 1.5;
            case ULTRA -> 2.0;
        };

        return (int) (baseCost * multiplier);
    }

    // TimeOut Handlings
    private void startChallengeTimer(int seconds) {
        cancelTimer(); // cancel existing timer
        challengeTimer = new Timer();
        challengeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleTimeout();
            }
        } , seconds * 1000L);
    }

    private void handleTimeout() {
        if (activeChallenge == null) {
            return;
        }
        view.displayMessage("Time's up!");

        ChallengeResult timeoutResult = new ChallengeResult(false, "Challenge timed out")
                .withDamage(0, calculateDamage(activeChallenge, 0));

        activeChallenge.complete(false, "Timed out");
        applyConsequences(timeoutResult);
        completeChallenge();
    }

    private void cancelTimer() {
        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }
    }

    public boolean hasActiveChallenge() {
        return activeChallenge != null &&
                activeChallenge.getChallengeState() == ChallengeState.ACTIVE;
    }

    public Challenge getActiveChallenge() {
        return activeChallenge;
    }

    public ChallengeState getCurrentState() {
        return activeChallenge != null ?
                activeChallenge.getChallengeState() :
                ChallengeState.NONE;
    }

    private void recordCompletedChallenge(Challenge challenge) {
        if (completedChallenges == null) {
            completedChallenges = new ArrayList<>();
        }
        completedChallenges.add(challenge);

        // Track recent types for diversity
        if (recentTypes == null) {
            recentTypes = new LinkedList<>();
        }
        recentTypes.add(challenge.getType());

        // Keep only last 10 types
        while (recentTypes.size() > 10) {
            recentTypes.poll();
        }
    }

    private List<ChallengeType> getRecentChallengeTypes(int count) {
        if (recentTypes == null || recentTypes.isEmpty()) {
            return Collections.emptyList();
        }

        return recentTypes.stream()
                .limit(count)
                .toList();
    }

    public ChallengeSnapshot saveState() {
        if (activeChallenge == null) {
            return null;
        }
        return activeChallenge.createSnapshot();
    }

    public void loadState(ChallengeSnapshot snapshot) {
        if (snapshot == null) {
            activeChallenge = null;
            return;
        }

        activeChallenge = Challenge.fromSnapshot(snapshot);

        if (activeChallenge.getChallengeState() == ChallengeState.ACTIVE) {
            long elapsed = activeChallenge.getElapsedTime();
            int remaining = activeChallenge.getTimeLimit() - (int) elapsed;
            if (remaining > 0) {
                startChallengeTimer(remaining);
            } else {
                handleTimeout();
            }
        }
    }

    // Helper method to map GameDifficulty to ChallengeDifficulty
    private ChallengeDifficulty mapGameToChallengeDifficulty(GameDifficulty gameDifficulty) {
        return switch (gameDifficulty) {
            case EASY -> ChallengeDifficulty.EASY;
            case MEDIUM -> ChallengeDifficulty.MEDIUM;
            case HARD -> ChallengeDifficulty.HARD;
            case ULTRA -> ChallengeDifficulty.ULTRA;
        };
    }

    // Generate reward item based on challenge type
    private Item generateRewardItem(ChallengeType type) {
        // TODO: Implement proper item generation, possibly via LLM
        // For now, return null - rewards can be added later
        return null;
    }
}