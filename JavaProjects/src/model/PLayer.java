package model;
import model.error.PlayerAlreadyDeadException;
import model.error.RIPException;

public class Player {
    private String name;
    private static int maxHealth;
    private int health;
    private static int maxIventory;
    private int inventory;
    private int currentRoom;
    private int experiencePoints;
    private int level;

    Player(String n) {
        this.name = n;
        Player.maxHealth = 100;
        Player.maxIventory = 5;
        this.inventory = -1;
        this.currentRoom = -1;
        this.experiencePoints = -1;
        this.level = -1;
    }

    @Override
    public String toString() {
        return "PlayerStats :- ";
    }

    public static int getMaxIventory() {
        return maxIventory;
    }

    private void verifyAlive() {
        if (this.health <= 0) {
            throw new PlayerAlreadyDeadException("RIP || Player " + getName() + "is already dead");
        }
    }

    public void takeDamage(int dam) {
        if (dam < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        verifyAlive();
        int sub = this.getHealth() - dam;
        if (sub < 0) {
            throw new RIPException("RIP || Player " + getName() + "has dead");
        }
        this.setHealth(this.getHealth() - dam);
        System.out.println("Ouhh || -" + sub + "HP");
    }

    void heal(int h) {
        verifyAlive();
        if (h < 0) {
            throw new IllegalArgumentException("Health cannot be negative");
        }
        int add = this.getHealth() + h;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        Player.inventory = inventory;
    }

    public int getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(int currentRoom) {
        this.currentRoom = currentRoom;
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void setExperiencePoints(int experiencePoints) {
        this.experiencePoints = experiencePoints;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getHealth() {
        return this.health;
    }
}
