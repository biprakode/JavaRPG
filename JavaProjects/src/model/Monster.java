package model;

import model.error.MonsterAlreadyDeadException;

public class Monster {
    private String name;
    private String desc;
    private String monsterType;
    private String attack;
    private int maxHealth;
    private int health;
    private MonsterDifficulty difficulty;
    private boolean isDefeated;
    private Item itemToDrop;

    Monster(String name, String desc, String attack, int maxHealth, MonsterDifficulty difficulty , monsterType type) {
        this.name = name;
        this.desc = desc;
        this.attack = attack;
        this.maxHealth = switch(difficulty) {
            case EASY -> 20;
            case MEDIUM -> 60;
            case HARD -> 150;
        };
        this.health = this.maxHealth;
        this.difficulty = difficulty;
        this.isDefeated = false;
        this.monsterType = type.getType();
    }

    public void takeDamage(int d) {
        if (isDefeated) {
            throw new MonsterAlreadyDeadException("Cannot attack " + name + ", it is already a corpse.");
        }
        this.health -= d;
        System.out.println("[BATTLE] The " + name + " takes " + d + " damage!");
        if (this.health <= 0) {
            this.health = 0;
            this.isDefeated = true;
            System.out.println("[DEFEAT] " + name + " let out a final cry and collapsed!");
        } else {
            System.out.println("[STATUS] " + name + " " + getHealthStatus());
        }
    }

    private String getHealthStatus() {
        double ratio = (double) health / maxHealth;
        if (ratio > 0.75) return "looks mostly unharmed.";
        if (ratio > 0.40) return "is bleeding and looking tired.";
        if (ratio > 0.10) return "is barely standing, stumbling from its wounds.";
        return "is on the verge of death!";
    }

    public void attack() {
        if (isDefeated) return;
        System.out.println("[ACTION] The " + name + " lunges forward with " + attack + "!");
    }

    public record MonsterDrop(Item item , int xp) {}

    public MonsterDrop getDefeatReward() {
        if (!isDefeated) {
            throw new IllegalStateException("The " + name + " is still alive! No loot yet.");
        }
        int xpValue = switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 25;
            case HARD -> 75;
            default -> 0;
        };

        System.out.println("[LOOT] You searched the remains of " + name + " and found " + (itemToDrop != null ? itemToDrop.getName() : "nothing") + " and gained " + xpValue + " XP.");
        return new MonsterDrop(itemToDrop , xpValue);
    }

    public String getFullContext() {
        return String.format("Monster: %s (%s). Difficulty: %s. Health: %d/%d. Status: %s",
                name, desc, difficulty, health, maxHealth, isDefeated ? "Dead" : "Alive");
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getAttack() {
        return attack;
    }

    public void setAttack(String attack) {
        this.attack = attack;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public MonsterDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(MonsterDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isDefeated() {
        return isDefeated;
    }

    public void setDefeated(boolean defeated) {
        isDefeated = defeated;
    }

    public String getMonsterType() {
        return monsterType;
    }

    public void setMonsterType(String monsterType) {
        this.monsterType = monsterType;
    }
}
