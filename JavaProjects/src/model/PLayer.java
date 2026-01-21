package model;
import model.error.InventoryFullException;
import model.error.PlayerAlreadyDeadException;
import model.error.RIPException;

public class Player {
    private String name;
    private static int maxHealth;
    private int health;
    private static int maxInventory;
    private Item[] inventory;
    private Item equippedItem;
    private int currentRoom;
    private int experiencePoints;
    private int level;

    Player(String n) {
        this.name = n;
        this.equippedItem = null;
        Player.maxHealth = 100;
        Player.maxInventory = 5;
        this.inventory = new Item[maxInventory];
        this.currentRoom = -1;
        this.experiencePoints = -1;
        this.level = -1;
    }

    @Override
    public String toString() {
        return "PlayerStats :- ";
    }

    private void verifyAlive() {
        if (this.health <= 0) {
            throw new PlayerAlreadyDeadException("RIP || Player " + getName() + "is already dead");
        }
    }

    public void takeDamage(int dam) {
        if (dam < 0) throw new IllegalArgumentException("Damage cannot be negative");
        verifyAlive();

        this.health -= dam;
        System.out.println("Ouhh || -" + dam + " HP. Remaining: " + this.health);

        if (this.health <= 0) {
            this.health = 0;
            throw new RIPException("RIP || Player " + getName() + " has died");
        }
    }

    void heal(int h) {
        verifyAlive();
        if (h < 0) throw new IllegalArgumentException("Heal amount cannot be negative");

        this.health += h;
        if (this.health > maxHealth) {
            this.health = maxHealth;
        }

        System.out.println("Ahhh || +" + h + " HP. Current: " + this.health);
    }

    void addItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add a null item to inventory.");
        }
        verifyAlive();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                inventory[i] = item;
                System.out.println("Picked up: " + item.getName() + " (Slot " + i + ")");
                return; // Exit method once item is added
            }
        }
        throw new InventoryFullException("Pockets are full! Cannot carry " + item.getName());
    }

    public void useItem(int index) {
        verifyAlive();
        Item item = getInventory(index);
        if (item == null) {
            System.out.println("That inventory slot is empty!");
            return;
        }
        item.use(this);
        if(item.getItemtype() == ItemType.POTION) {
            setInventory(null, index);
            System.out.println(item.getName() + " was consumed.");
        }

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

    public Item getInventory(int i) {
        if(i < 0 || i >= Player.getMaxInventory()) {
            throw new IllegalArgumentException("Item index out of bounds");
        }
        return inventory[i];
    }

    public void setInventory(Item item , int i) {
        if(i < 0 || i >= Player.getMaxInventory()) {
            throw new IllegalArgumentException("Item index out of bounds");
        }
        inventory[i] = item;
    }

    public static int getMaxInventory() {
        return maxInventory;
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

    public Item getEquippedItem() {
        return equippedItem;
    }

    public void setEquippedItem(Item equippedItem) {
        this.equippedItem = equippedItem;
    }
}
