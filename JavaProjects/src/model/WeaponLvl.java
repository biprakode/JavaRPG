package model;

public enum WeaponLvl {
    LVL0(10, 2.0f),
    LVL1(15, 1.8f),
    LVL2(25, 1.5f),
    LVL3(40, 1.2f),
    MASTER(60, 0.8f);

    public final int damage;
    public final float cooldown;

    WeaponLvl(int damage, float cooldown) {
        this.damage = damage;
        this.cooldown = cooldown;
    }

    public static WeaponLvl fromStrength(float itemStrength) {
        float clamped = Math.max(0, Math.min(itemStrength, 0.999f));

        WeaponLvl[] levels = WeaponLvl.values();
        int index = (int) (clamped * levels.length);

        return levels[index];
    }
}
