package model;

public class Potion extends Item{
    private int healAmount;

    Potion(String n, String d, int healCap) {
        super(n , d , ItemType.POTION);
        healAmount = healCap;
    }

    @Override
    void use(Player player) {
        System.out.println("Gulp! " + player.getName() + " drank " + getName());
        player.heal(this.healAmount);
    }

    public int getHealAmount() {
        return healAmount;
    }
}
