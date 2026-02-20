package model;

public class Treasure extends Item{
    private int xpReward;

    Treasure(String n, String d, int xpReward) {
        super(n , d , ItemType.TREASURE);
        this.xpReward = xpReward;
    }

    @Override
    void use(Player player) {
        System.out.println("You cashed in the " + getName() + "!");
        player.setExperiencePoints(player.getExperiencePoints() + this.xpReward);
        System.out.println("Gained: " + xpReward + " XP");
    }

    public int getXpReward() {
        return xpReward;
    }
}
