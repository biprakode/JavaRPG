package controller;

import model.Directions;
import model.Room;
import model.RoomType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MapBuilder {
    private Random random = new Random();
    private Map<Integer , Room> worldMap;
    Room spawnRoom;

    public Map<Integer , Room> generateMap(int totalRooms) { // RandomWalk MapBuilder
        worldMap = new HashMap<>();
        Room startRoom = new Room(1 , "The Entrance" , "A dimly lit cave mouth" , RoomType.NORMAL);
        spawnRoom = startRoom;
        worldMap.put(startRoom.getId() , startRoom);
        Room current = startRoom;
        int roomcount = 1;

        while(roomcount < totalRooms) {
            Directions dir = Directions.values()[random.nextInt(Directions.values().length)];
            if(current.getExit(dir) == null) {
                roomcount++;
                Room nextRoom = new Room(roomcount , null , null , null); // LLM will fill later
                current.addExit(dir , nextRoom);
                nextRoom.addExit(getOpposite(dir) , current);
                worldMap.put(nextRoom.getId() , nextRoom);
                current = nextRoom;
            } else {
                current = current.getExit(dir);
            }
        }
        return worldMap;
    }

    private Directions getOpposite(Directions dir) {
        return switch (dir) {
            case NORTH -> Directions.SOUTH;
            case SOUTH -> Directions.NORTH;
            case EAST -> Directions.WEST;
            case WEST -> Directions.EAST;
        };
    }

    public Room getSpawnRoom() {
        return spawnRoom;
    }
}
