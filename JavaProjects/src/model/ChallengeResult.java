package model;

import java.util.List;

public class ChallengeResult {
    private boolean isSucess;
    private int effectivenessRating; // LLM creativity rating 0-100
    private String feedback;

    private int xpAwarded;
    private int damageDealt;
    private int damageTaken;
    private List<Item> itemsAwarded;

    private boolean unlocksPath;
    private String storyProgression;

    private String evaluationMethod;
    private long evaluationTime;

    public ChallengeResult(boolean isSuccess, String feedback) {
        this.isSucess = isSuccess;
        this.feedback = feedback;
    }

    public ChallengeResult withXP(int xp) {
        this.xpAwarded = xp;
        return this;
    }

    public ChallengeResult withDamage(int dealt, int taken) {
        this.damageDealt = dealt;
        this.damageTaken = taken;
        return this;
    }

    public ChallengeResult withItems(List<Item> items) {
        this.itemsAwarded = items;
        return this;
    }

    public ChallengeResult withEffectiveness(int rating) {
        this.effectivenessRating = rating;
        return this;
    }

    public int getEffectivenessRating() {
        return effectivenessRating;
    }

    public ChallengeResult withStoryImpact(boolean unlocks, String progression) {
        this.unlocksPath = unlocks;
        this.storyProgression = progression;
        return this;
    }

    public boolean isSuccess() {
        return isSucess;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public int getDamageDealt() {
        return damageDealt;
    }

    public List<Item> getItemsAwarded() {
        return itemsAwarded;
    }

    public String getFeedback() {
        return feedback;
    }
}
