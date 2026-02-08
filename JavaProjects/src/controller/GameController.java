package controller;

import controller.error.InvalidCommandException;
import model.GameDifficulty;
import model.GameState;
import model.Player;
import model.Room;
import model.Challenge;
import model.error.PlayerAlreadyDeadException;
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

    public void processCommand(String input) throws Exception {
        Action action = this.commandparser.parse(input).getAction();

        if(action.isSystemCommand()) {
            handleSystemCommand(action , input);
        }
        if(!player.isAlive()) {
            throw new PlayerAlreadyDeadException("Bruh, you're dead");
        }

        switch (action) {
            case MOVE -> handleMove(input);
            case ATTACK -> handleAttack(input);
            case TAKE -> handleTake(input);
            case USE -> handleUse(input);
            case EXAMINE -> handleExamine(input);
            case TALK -> handleTalk(input);
            case INVENTORY -> handleInventory();
            case STATS -> handleStats();
            default -> throw new InvalidCommandException("Unknown action: " + action);
        }
    }

    private void handleStats() {
    }

    private void handleInventory() {
    }

    private void handleTalk(String input) {
    }

    private void handleExamine(String input) {
    }

    private void handleUse(String input) {
    }

    private void handleTake(String input) {
    }

    private void handleAttack(String input) {
    }

    private void handleMove(String input) {
    }

    private void handleSystemCommand(Action action, String input) {
    }
}
