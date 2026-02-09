package model;

import model.error.DuplicateRoomException;

import java.util.HashMap;
import java.util.HashSet;

public class Room {
    private int id;
    private String name;
    private String desc;
    private Monster monster;
    private HashMap<Directions , Room> roomMap;
    private HashSet<Directions> lockedExits;
    private RoomType Roomtype;
    private Item item;
    private boolean isVisited;

    public Room(int id, String n, String d, RoomType rt) {
        this.id = id;
        this.name = n;
        this.desc = d;
        this.Roomtype = rt;
        this.isVisited = false;
        this.roomMap = new HashMap<>();
        this.lockedExits = new HashSet<>();
    }

    public void reset() {
        this.isVisited = false;
        this.roomMap = new HashMap<>();
        this.lockedExits = new HashSet<>();
    }

    void addExit(Directions d, Room r, boolean isLocked) {
        if(roomMap.get(d) != null) {
            throw new DuplicateRoomException("Direction " + d + " is already connected to " + roomMap.get(d).getName());
        }
        roomMap.put(d , r);
        if (isLocked) {
            lockedExits.add(d);
        }
        System.out.println("Path created: " + this.name + " ---[" + d + " (Locked: " + isLocked + ")]---> " + r.getName());
    }
    public void addExit(Directions d, Room r) {
        addExit(d, r, false);
    }

    public Room getExit(Directions d) {
        if(lockedExits.contains(d)) {
            System.out.println("The door to the " + d + " is locked tight!");
            return null;
        }
        return roomMap.get(d);
    }

    public boolean isExitLocked(Directions d) {return lockedExits.contains(d); }

    public void unlockExit(Directions d) {
        if (lockedExits.remove(d)) {
            System.out.println("CLACK! The " + d + " door is now open.");
        }
    }

    public boolean hasMonster() {
        return (monster != null); // Fixed logic
    }

    public void killMonster() { // TODO
        if (hasMonster()) {
            System.out.println("VICTORY! The " + monster.getName() + " collapses to the floor.");
            this.monster = null;
        }
    }

    public void describe() {
        System.out.println("\n[" + name + "]");
        System.out.println(desc);
        if (hasMonster()) {
            System.out.println("WARNING: A " + monster.getName() + " is lurking here!");
        }
        if (item != null) {
            System.out.println("You spot a " + item.getName() + " on the ground.");
        }
    }

    void addItem(Item item) {
        this.item = item;
    }

    public int getId() {
        return id;
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

    public Monster getMonster() {
        return monster;
    }

    public void setMonster(Monster monster) {
        this.monster = monster;
        if (monster != null) {
            System.out.println("A " + monster.getName() + " has spawned in " + this.name);
        }
    }

    public HashMap<Directions, Room> getRoomMap() {
        return roomMap;
    }

    public void setRoomMap(HashMap<Directions, Room> roomMap) {
        this.roomMap = roomMap;
    }

    public RoomType getRoomtype() {
        return Roomtype;
    }

    public void setRoomtype(RoomType roomtype) {
        this.Roomtype = roomtype;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }

    public boolean hasItem() { return item != null; }

    public HashMap<Directions , Room> getExits() { return roomMap; }

    public boolean hasExit(Directions d) {
        return getExit(d) != null;
    }
}
