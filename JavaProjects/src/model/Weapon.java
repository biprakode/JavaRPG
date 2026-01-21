package model;

import model.error.PlayerAlreadyDeadException;

public class Weapon extends Item {
    private int damage;
    private int cooldown;
    private WeaponLvl lvl;

    Weapon(String n, String d, int damage, int cooldown) {
        super(n , d , ItemType.WEAPON);
        this.damage = damage;
        this.cooldown = cooldown;
        lvl = WeaponLvl.LVL0;
        System.out.println("Message");
    }

    @Override
    void use(Player player) {
        if (player.getHealth() <= 0) {
            throw new PlayerAlreadyDeadException("A corpse cannot wield a weapon.");
        }
        System.out.println(player.getName() + " equipped the " + this.getName() + "!");
        player.setEquippedItem(this);
        System.out.println("Damage: " + this.damage + " | Level: " + this.lvl);
    }

    public void upgrade() {
        switch (this.lvl) {
            case LVL0:
                this.lvl = WeaponLvl.LVL1;
                this.damage *= 10;
                this.cooldown -= 15;
                break;
            case LVL1:
                this.lvl = WeaponLvl.LVL2;
                this.damage += 25;
                break;
            case LVL2:
                this.lvl = WeaponLvl.LVL3;
                this.damage += 40;
                this.cooldown -= 40;
                break;
            case LVL3:
                this.lvl = WeaponLvl.MASTER;
                this.damage += 50;
                this.cooldown -= 50;
                break;
            case MASTER:
                System.out.println("This weapon is already at its peak!");
                return;
        }
        System.out.println(getName() + " upgraded to " + this.lvl + "!");
    }

    public int getDamage() { return this.damage; }
    public WeaponLvl getLvl() { return this.lvl; }
}
