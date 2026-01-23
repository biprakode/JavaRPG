package model;

import java.util.Map;

public class GameState {
    //core shit
    private Player player;
    private Room currentRoom;
    private Room spawnRoom;
    private Map<Integer , Room> roomMap;

    //life shit
    private int livesRemaining;
    private int maxLives;
    private boolean isGameOver;

    //progress trackers
    private int totalScore;
    private int roomsExplored;
    private int monstersDefeated;
    private long gameTime;

    private GameDifficulty difficulty;

    private Room checkpoint;
    private int livesAtCheckpoint;

    GameState(GameDifficulty difficulty) {
        maxLives = switch (difficulty) {
            case ULTRA -> 1;
            case HARD -> 2;
            case MEDIUM -> 3;
            case EASY -> 4;
            default -> -1;
        };
        this.isGameOver = false;
        this.totalScore = 0;
        this.livesRemaining = maxLives;
        this.roomsExplored = 0;
        this.monstersDefeated = 0;
        this.gameTime = System.currentTimeMillis();

        //map service handle map generation
    }

    public void intialize(Player p, Room start, Map<Integer,Room> rooms) {
        player = p;
        roomMap = rooms;
        this.spawnRoom = start;
        this.currentRoom = start;
        this.checkpoint = start;
        this.livesAtCheckpoint = livesRemaining;
    }

    public void loseLife() {
        livesRemaining--;
        if(livesRemaining == 0) {
            isGameOver = true;
        }
    }

    public void gainLife() {
        if(livesRemaining < maxLives) {
            livesRemaining++;
        }
    }

    public boolean hasLivesRemaining() {
        return livesRemaining>0;
    }

    public void setCheckpoint(Room room) {
        checkpoint = room;
        livesAtCheckpoint = livesRemaining;
    }

    public void respawnCheckpoint() {
        if(checkpoint != null) {
            this.currentRoom = checkpoint;
            this.player.setCurrentRoom(checkpoint);
            this.player.heal(player.getMaxHealth() / 2);
        }
    }

    public void moveToRoom(Room room) {
        this.currentRoom = room;
        this.player.setCurrentRoom(currentRoom);
        if(!room.isVisited()) {
            room.setVisited(true);
            roomsExplored++;
            totalScore+=10;
        }
    }

    public Room getRoomById(int id) {
        return roomMap.get(id);
    }

    public void monsterDefeated(int monsterValue) {
        monstersDefeated++;
        totalScore += monsterValue;
    }

    public void checkGameOver() {
        if(player.isAlive() || livesRemaining<=0) {
            isGameOver = true;
        }
    }

    public void resetGame() {
        this.isGameOver = false;
        this.totalScore = 0;
        this.livesRemaining = maxLives;
        this.roomsExplored = 0;
        this.monstersDefeated = 0;
        this.gameTime = System.currentTimeMillis();
        this.currentRoom = spawnRoom;
        this.checkpoint = spawnRoom;
        this.livesAtCheckpoint = maxLives;

        if(player!=null) {player.reset(spawnRoom);}
        for(Room room : roomMap.values()) {
            room.reset();
        }
    }

    public long getPlayTime() {
        return (System.currentTimeMillis() - gameTime) / 1000; // seconds
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    public Room getSpawnRoom() {
        return spawnRoom;
    }

    public void setSpawnRoom(Room spawnRoom) {
        this.spawnRoom = spawnRoom;
    }

    public Map<Integer, Room> getRoomMap() {
        return roomMap;
    }

    public void setRoomMap(Map<Integer, Room> roomMap) {
        this.roomMap = roomMap;
    }

    public int getLivesRemaining() {
        return livesRemaining;
    }

    public void setLivesRemaining(int livesRemaining) {
        this.livesRemaining = livesRemaining;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getRoomsExplored() {
        return roomsExplored;
    }

    public void setRoomsExplored(int roomsExplored) {
        this.roomsExplored = roomsExplored;
    }

    public int getMonstersDefeated() {
        return monstersDefeated;
    }

    public void setMonstersDefeated(int monstersDefeated) {
        this.monstersDefeated = monstersDefeated;
    }

    public long getGameTime() {
        return gameTime;
    }

    public void setGameTime(long gameTime) {
        this.gameTime = gameTime;
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(GameDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Room getCheckpoint() {
        return checkpoint;
    }

    public void setRoomCheckpoint(Room checkpoint) {
        this.checkpoint = checkpoint;
    }

    public int getLivesAtCheckpoint() {
        return livesAtCheckpoint;
    }

    public void setLivesAtCheckpoint(int livesAtCheckpoint) {
        this.livesAtCheckpoint = livesAtCheckpoint;
    }

}
