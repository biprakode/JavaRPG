package model;

import java.util.List;

public class ChallengeResult {
    private boolean isSucess;
    private int effectivenessRating;
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

    }


}
