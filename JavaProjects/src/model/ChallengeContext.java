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

        // Type-specific task description
        String typeInstruction = getTypeInstruction();
        prompt.append(typeInstruction).append("\n\n");

        // Context
        prompt.append("Location: ").append(roomName).append(" - ").append(roomDescription).append("\n");
        if (monsterName != null) {
            prompt.append("Monster: ").append(monsterName).append(" (").append(monsterDifficulty).append(") - ").append(monsterDescription).append("\n");
        }
        prompt.append("Player Level: ").append(playerLevel).append(", HP: ").append(playerHealth).append("/").append(maxPlayerHealth).append("\n");
        prompt.append("Difficulty: ").append(challengeDifficulty).append("\n\n");

        // JSON format with type-specific example
        prompt.append("Return ONLY a JSON object like this example:\n");
        prompt.append(getTypeExample());

        return prompt.toString();
    }

    private String getTypeInstruction() {
        return switch (challengeType) {
            case RIDDLE -> "Create a riddle for the player to solve. The player must guess the answer in one or two words.";
            case PUZZLE -> "Create a logic puzzle or word puzzle. The player must figure out the answer.";
            case MORAL_DILEMMA -> "Create a moral dilemma scenario. The player must write a sentence explaining what they would do and why. There is no single correct answer — evaluate based on reasoning quality.";
            case NEGOTIATION -> "Create a negotiation scenario with an NPC. The player must write a persuasive argument. Evaluate based on how convincing their argument is, not an exact answer.";
            case CREATIVE -> "Create a creative writing prompt. The player must write a short creative response (1-3 sentences). Evaluate based on creativity and effort, not correctness.";
            case COMBAT_CREATIVE -> "Create a combat scenario where the player must describe a creative battle strategy in 1-3 sentences. Evaluate based on tactical creativity, not an exact answer.";
            case COMBAT_STANDARD -> "Create a quick combat challenge. The player must answer a tactical question about fighting the monster.";
        };
    }

    private String getTypeExample() {
        boolean isCreativeType = challengeType == ChallengeType.CREATIVE
                || challengeType == ChallengeType.COMBAT_CREATIVE
                || challengeType == ChallengeType.MORAL_DILEMMA
                || challengeType == ChallengeType.NEGOTIATION;

        if (isCreativeType) {
            return "{\"prompt\": \"YOUR CREATIVE SCENARIO HERE\", "
                    + "\"desc\": \"short summary\", "
                    + "\"hint1\": \"subtle hint\", "
                    + "\"hint2\": \"direct hint\", "
                    + "\"hint3\": \"obvious hint\", "
                    + "\"expectedAnswerPattern\": \"CREATIVE: evaluate reasoning quality, persuasiveness, and effort. Award FULL for thoughtful responses, PARTIAL for brief responses, NONE for nonsense.\"}\n"
                    + "IMPORTANT: Do NOT copy the example. Create an original scenario based on the location and context above.";
        }
        return "{\"prompt\": \"YOUR RIDDLE OR PUZZLE HERE\", "
                + "\"desc\": \"short summary\", "
                + "\"hint1\": \"subtle hint\", "
                + "\"hint2\": \"direct hint\", "
                + "\"hint3\": \"obvious hint\", "
                + "\"expectedAnswerPattern\": \"the answer\"}\n"
                + "IMPORTANT: Do NOT copy the example. Create an original challenge based on the location and context above.";
    }

    public ChallengeType getChallengeType() {
        return challengeType;
    }

}
