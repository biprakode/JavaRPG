package model;

import java.util.HashSet;

public class Key extends Item{
    private int targetId;

    public Key(String n , String d, int targetId) {
        super(n , d , ItemType.KEY);
        this.targetId = targetId;
    }

    @Override
    void use(Player player) {
        Room room = player.getCurrentRoom();
        if (room == null) {
            System.out.println("You can't use a key here.");
            return;
        }

        HashSet<Directions> locked = room.getLockedExits();
        if (locked.isEmpty()) {
            System.out.println("There are no locked exits here to use " + getName() + " on.");
            return;
        }

        // Prefer the exit matching targetId
        // Otherwise unlock the first available locked exit
        Directions toUnlock = null;
        if (room.getId() == targetId) {
            toUnlock = locked.iterator().next();
        } else {
            // Fallback: unlock any locked exit in this room
            toUnlock = locked.iterator().next();
        }

        room.unlockExit(toUnlock);
        System.out.println("You used " + getName() + " to unlock the " + toUnlock + " exit!");
    }

    public int getTargetId() { return targetId; }
}
