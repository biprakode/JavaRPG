package model;

import java.util.*;

public class Challenge {
    private String id;
    private ChallengeType type;
    private ChallengeState state;

    private String prompt;
    private String desc;
    private String expectAnswer;

    private ChallengeDifficulty difficulty;
    private double baseRewardXP;
    private int timeLimit;

    private int maxAttempts;
    private int attemptsRemaining;
    private List<String> playerResponses;
    private int hintsUsed;

    private Map<String, String> metadata;

    private boolean isCompleted;
    private boolean wasSuccessful;
    private String finalFeedback;

    private long startTime;
    private long endTime;

    public Challenge(ChallengeType t, String p, ChallengeDifficulty diff) {
        type = t;
        prompt = p;
        difficulty = diff;
        state = ChallengeState.ACTIVE;
        maxAttempts = type.getMaxAttempts();
        attemptsRemaining = maxAttempts;
        UUID uuid = UUID.randomUUID();
        id = uuid.toString();
        timeLimit = type.getBaseTimeout();
        playerResponses = new ArrayList<>();
        metadata = new HashMap<>();
        baseRewardXP = (difficulty.getrewardXP()*50);
    }

    public void activate() {
        state = ChallengeState.ACTIVE;
        startTimer();
        System.out.println("Timer started");
    }

    public void startEval() {
        state = ChallengeState.EVALUATING;
        endTime = System.currentTimeMillis();
        System.out.println("LLM Started evaluating");
    }

    public void complete(boolean success , String feedback) {
        state = ChallengeState.COMPLETED;
        isCompleted = true;
        wasSuccessful = success;
        finalFeedback = feedback;
        endTime = System.currentTimeMillis();
        System.out.println("Challenge completed at\n");
    }

    public void resolve() { // after XP/Story updates
        state = ChallengeState.RESOLVED;
    }

    public void recordResponse(String response) {
        playerResponses.add(response);
        //updateTimestamp
    }

    public void decrementAttempts() {
        if(attemptsRemaining == 0) {
            complete(false , "Max Attempts reached");
        }
        attemptsRemaining--;
    }

    public boolean hasAttemptsRemaining() {
        return attemptsRemaining > 0;
    }

    public void useHint() {
        hintsUsed++;
        baseRewardXP -= 1.10 * baseRewardXP;
    }

    public void startTimer() {
        startTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public boolean hasTimedOut() {
        if(getElapsedTime() > timeLimit) {
            complete(false , "Max TimeLimit reached");
            return true;
        }
        return false;
    }

    public void addMetaData(String key, String value) {
        metadata.put(key , value);
    }

    public String getMetaData(String key) {
        return metadata.get(key);
    }

    public ChallengeSnapshot createSnapshot() {
        return new ChallengeSnapshot(
                this.id,
                this.type,
                this.state,
                this.prompt,
                this.desc,
                this.expectAnswer,
                this.difficulty,
                this.baseRewardXP,
                this.timeLimit,
                this.maxAttempts,
                this.attemptsRemaining,
                new ArrayList<>(this.playerResponses), // Copy list to prevent reference issues
                this.hintsUsed,
                this.metadata != null ? new HashMap<>(this.metadata) : null,
                this.isCompleted,
                this.wasSuccessful,
                this.finalFeedback,
                this.startTime,
                this.endTime
        );
    }

    public static Challenge fromSnapshot(ChallengeSnapshot snapshot) {
        // 1. Use the main constructor for initial setup
        Challenge challenge = new Challenge(snapshot.type(), snapshot.prompt(), snapshot.difficulty());

        // 2. Overwrite the randomized/calculated fields with saved data
        challenge.id = snapshot.id();
        challenge.state = snapshot.state();
        challenge.desc = snapshot.desc();
        challenge.expectAnswer = snapshot.expectAnswer();
        challenge.baseRewardXP = snapshot.baseRewardXP();
        challenge.timeLimit = snapshot.timeLimit();
        challenge.maxAttempts = snapshot.maxAttempts();
        challenge.attemptsRemaining = snapshot.attemptsRemaining();
        challenge.playerResponses = new ArrayList<>(snapshot.playerResponses());
        challenge.hintsUsed = snapshot.hintsUsed();
        challenge.metadata = snapshot.metadata() != null ? new HashMap<>(snapshot.metadata()) : new HashMap<>();
        challenge.isCompleted = snapshot.isCompleted();
        challenge.wasSuccessful = snapshot.wasSuccessful();
        challenge.finalFeedback = snapshot.finalFeedback();
        challenge.startTime = snapshot.startTime();
        challenge.endTime = snapshot.endTime();

        return challenge;
    }

    public boolean isChallengeCompleted() {
        return isCompleted;
    }

    public void setChallengeState(ChallengeState st) {
        this.state = st;
    }

    public ChallengeState getChallengeState() {
        return state;
    }

    public void incrementHints() {
        hintsUsed++;
    }

    public void setbaseXP(double xp) {
        baseRewardXP = xp;
    }

    public double getbaseXP() {
        return baseRewardXP;
    }

    public ChallengeType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public ChallengeDifficulty getDifficulty() {
        return difficulty;
    }

    public boolean getWasSuccesful() {
        return wasSuccessful;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}