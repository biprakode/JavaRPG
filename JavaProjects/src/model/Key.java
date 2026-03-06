package model;

public class Key extends Item{
    private int targetId;

    public Key(String n , String d, int targetId) {
        super(n , d , ItemType.KEY);
        this.targetId = targetId;
    }

    @Override
    void use(Player player) { // TODO
        // Room currentRoom = GameWorld.getRoom(player.getCurrentRoom());
        // if(currentRoom.getLockId().equals(this.targetId)) { ... unlock ... }
        System.out.println("Checking " + getName() + " against nearby locks...");
    }

    public int getTargetId() { return targetId; }
}
