package model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Challenge {
    private ChallengeType challenge_type;
    private ChallengeDifficulty challenge_diff;
    private String prompt;
    private String expectedAnswer;
    private boolean isComplete;
    private int rewardXP;

    public Challenge(ChallengeType challengeType, String prompt, String expectedAnswer, int rewardXP) {
        challenge_type = challengeType;
        this.prompt = prompt;
        this.expectedAnswer = expectedAnswer;
        this.rewardXP = rewardXP;
        this.isComplete = false;
    }

    public boolean ValidateResponse(String playerInput) {
        if(isComplete) return  true;
        Pattern pattern = Pattern.compile(expectedAnswer , Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(playerInput);
        if(matcher.find()) {
            System.out.println("[SUCCESS] Input matches the solution pattern!");
            complete();
            return true;
        }
        System.out.println("[FAIL] That doesn't seem to solve the challenge.");
        return false;
    }

    public void complete() {
        isComplete = true;
        System.out.println("[CHALLENGE] Challenge Completed! Gained " + rewardXP + " XP.");
    }

    public String getPrompt() { return prompt; }
    public boolean isCompleted() { return isComplete; }
    public int getRewardXP() {return rewardXP;}
}
