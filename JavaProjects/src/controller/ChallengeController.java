package controller;

import controller.error.ChallengeAlreadyComplete;
import controller.error.ChallengeNotActive;
import model.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;

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
    private boolean useLLMEvaluation;

    public ChallengeController(LLMService llm, ConsoleView v, GameState st) {
        llmService = llm;
        view = v;
        gameState = st;
        if(st.getDifficulty() != GameDifficulty.ULTRA) {
            allowHints = true;
        }
    }

    public void initiateChallenge(Room room, ChallengeType type) {
        if(activeChallenge.isChallengeCompleted()) {
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
        double deduct = switch (gameState.getDifficulty()) {
            case GameDifficulty.EASY -> 0.05;
            case GameDifficulty.MEDIUM -> 0.08;
            case GameDifficulty.HARD -> 0.12;
            case ULTRA -> 0.18;
        };
        double xp = activeChallenge.getbaseXP();
        activeChallenge.setbaseXP(xp - xp*deduct);

    }

    //Challenge Context Building for LLM
    private ChallengeContext buildContext(Room room, ChallengeType type) {
        ChallengeContext context = new ChallengeContext()
                .withPlayer(gameState.getPlayer())
                .withRoom(room)
                .withChallengeType(type)
                .withDifficulty(mapGameToChallengeeDifficulty(gameState.getDifficulty()));
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
            view.displayResult(result);
            if (!activeChallenge.hasAttemptsRemaining()) {
                result = new ChallengeResult(false, "No attempts remaining").withDamage(0, calculateDamage(activeChallenge, 0));
                applyConsequences(result);
                completeChallenge();
            }
        }
    }

    private void applyConsequences(ChallengeResult result) {
        Player player = gameState.getPlayer();
        if(result.getXPAwarded()) {
            player.addXP(result.getXPAwarded());
        }
        if (result.getDamageTaken() > 0) {
            player.takeDamage(result.getDamageTaken());
        }
        if(result.getItemsAwarded()) {
            for(Item item : result.getItemsAwarded()) {
                player.addInventory(item);
            }
        }
        view.displayResult(result);
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
        ChallengeType type = activeChallenge.getType();

        if (!type.requiresLLM() || !useLLMEvaluation) {
            return evaluateWithRules(response);
        }
        return evaluateWithLLM(response);
    }

    private ChallengeResult evaluateWithLLM(String response) {
        String expectedPattern = activeChallenge.getMetaData("expectedPattern");
        String challengePrompt = activeChallenge.getPrompt();
        String evalPrompt = String.format("""
        Challenge: %s
        Expected answer pattern: %s
        Player response: %s
        
        Evaluate if the response is correct. Consider creative interpretations.
        Return JSON: {"success": true/false, "effectiveness": 0-100, "feedback": "..."}
        """, challengePrompt, expectedPattern, response);

        String llmEval = llmService.evaluateResponse(response, expectedPattern, evalPrompt);
        ChallengeResult result = challengeEvaluator.parseEvaluation(llmEval);

        int effectiveness = result.getEffectivenessRating();
        return result.withXP(calculateXPReward(activeChallenge, effectiveness))
                .withDamage(calculateDamage(activeChallenge, effectiveness), 0)
                .withItems(determineItemRewards(activeChallenge));
    }


}
