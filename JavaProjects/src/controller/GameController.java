package controller;

import model.GameDifficulty;
import model.GameState;
import model.Player;
import model.Room;
import model.Challenge;

import java.util.Map;
import java.util.Scanner;

public class GameController {
    protected GameState gameState;
    protected Player player;
    protected MapBuilder mapbuilder;
    protected CommandParser commandparser;

    public void startGame(GameDifficulty difficulty , String player_name , int totalrooms) {
        GameState gamestate = new GameState(difficulty);
        Player player = new Player(player_name);

        MapBuilder mapbuilder = new MapBuilder(); // TODO

        Map<Integer , Room> worldMap = mapbuilder.generateMap(totalrooms);
        gamestate.initialize(player , mapbuilder.getSpawnRoom() , worldMap);

        gameLoop(gamestate);
    }

    public void gameLoop(GameState gamestate) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("--- THE ADVENTURE BEGINS ---");
        gamestate.getCurrentRoom().describe();

        while(!gamestate.isGameOver()) {
            System.out.print("\nWhat will you do? > ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            processCommand(input);
            if(!gamestate.getPlayer().isAlive() && !gamestate.hasLivesRemaining()) {
                System.out.println("[GAMEOVER] You have perished in the depths.");
                gamestate.setGameOver(true);
            }
        }
        System.out.println("Thanks for playing!");
    }

    public void processCommand(String input) {

    }
}
