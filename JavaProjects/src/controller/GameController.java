package controller;

import model.GameDifficulty;
import model.GameState;
import model.Player;
import model.Room;
import model.Challenge;
import view.ConsoleViewImpl;

import java.util.Map;
import java.util.Scanner;

public class GameController {
    protected GameState gameState;
    protected Player player;
    protected MapBuilder mapbuilder;
    protected CommandParser commandparser;
    protected ConsoleView view;

    public GameController() {
        this.view = new ConsoleViewImpl();
    }

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
        view.displayMessage("--- THE ADVENTURE BEGINS ---");
        gamestate.getCurrentRoom().describe();

        while(!gamestate.isGameOver()) {
            view.displayMessage("\nWhat will you do? > ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            processCommand(input);
            if(!gamestate.getPlayer().isAlive() && !gamestate.hasLivesRemaining()) {
                view.displayMessage("[GAMEOVER] You have perished in the depths.");
                gamestate.setGameOver(true);
            }
        }
        view.displayMessage("Thanks for playing!");
    }

    public void processCommand(String input) {

    }
}
