package model;

import java.util.ArrayList;
import java.util.List;

public class ChallengeContext {
    private int playerLevel;
    private int playerHealth;
    private int maxPlayerHealth;
    private List<Item> playerInventory;

    private String roomName;
    private String roomDescription;

    private String monsterName;
    private String monsterType;
    private String monsterDescription;
    private String monsterDifficulty;

    private ChallengeType challengeType;

    private String challengeDifficulty;
    private GameDifficulty gameDifficulty;

    private List<String> recentChallengeTypes;

    public ChallengeContext() {
        playerInventory = new ArrayList<>();
        recentChallengeTypes = new ArrayList<>();
    }

    public ChallengeContext withPlayer(Player player) {
        playerLevel = player.getLevel();
        playerHealth = player.getHealth();
        maxPlayerHealth = player.getMaxHealth();
        playerInventory = player.getInventory();
        return this; // for chaining
    }

    public ChallengeContext withRoom(Room room) {
        this.roomName = room.getName();
        this.roomDescription = room.getDesc();
        return this;
    }

    public ChallengeContext withMonster(Monster monster) {
        this.monsterName = monster.getName();
        this.monsterType =monster.getMonsterType();
        this.monsterDescription = monster.getDesc();
        this.monsterDifficulty = monster.getDifficulty().getDifficulty();
        return this;
    }

    public ChallengeContext withChallengeType(ChallengeType type) {
        challengeType = type;
        return this;
    }

    public ChallengeContext withDifficulty(ChallengeDifficulty diff) {
        challengeDifficulty = diff.getDifficulty();
        return this;
    }

    public ChallengeContext withHistory(List<ChallengeType> history) {
        for(ChallengeType hist : history) {
            recentChallengeTypes.add(hist.getChallengeType());
        }
        return this;
    }

    public String buildLLMPrompt() {
        StringBuilder prompt = new StringBuilder();
        // 1. Task Setting
        prompt.append("Generate a ").append(challengeType).append(" challenge for the following RPG context:\n\n");

        // 2. Environment Context
        prompt.append("--- ENVIRONMENT ---\n");
        prompt.append("Location: ").append(roomName).append("\n");
        prompt.append("Description: ").append(roomDescription).append("\n\n");

        // 3. Player Context
        prompt.append("--- PLAYER ---\n");
        prompt.append("Level: ").append(playerLevel).append("\n");
        prompt.append("Condition: ").append(playerHealth).append("/").append(maxPlayerHealth).append(" HP\n");
        prompt.append("Inventory: ").append(playerInventory.isEmpty() ? "Empty" : playerInventory).append("\n\n");

        // 4. Enemy Context (Optional)
        if (monsterName != null) {
            prompt.append("--- THREAT ---\n");
            prompt.append("Monster: ").append(monsterName).append(" (").append(monsterDifficulty).append(")\n");
            prompt.append("Details: ").append(monsterDescription).append("\n\n");
        }

        // 5. Challenge Constraints
        prompt.append("--- CHALLENGE REQUIREMENTS ---\n");
        prompt.append("Difficulty: ").append(challengeDifficulty).append("\n");
        if (recentChallengeTypes != null && !recentChallengeTypes.isEmpty()) {
            prompt.append("Avoid these recent themes: ").append(recentChallengeTypes).append("\n");
        }
        prompt.append("Style: Immersive, dark fantasy, and high-stakes.\n\n");

        // 6. JSON Schema (The "Contract")
        prompt.append("--- OUTPUT FORMAT ---\n");
        prompt.append("Return ONLY a valid JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"prompt\": \"The flavor text and the actual riddle/puzzle description\",\n");
        prompt.append("  \"desc\": \"A brief summary of what the player sees\",\n");
        prompt.append("  \"hint1\": \"A subtle hint\",\n");
        prompt.append("  \"hint2\": \"A direct hint\",\n");
        prompt.append("  \"hint3\": \"An obvious hint (last resort)\",\n");
        prompt.append("  \"expectedAnswerPattern\": \"A regex or list of keywords to solve this\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

}
