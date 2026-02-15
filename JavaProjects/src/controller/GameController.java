package controller;

import controller.error.InvalidCommandException;
import model.*;
import model.error.InventoryFullException;
import model.error.PlayerAlreadyDeadException;
import view.ConsoleViewImpl;

import java.util.ArrayList;
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

    public void startGame(GameDifficulty difficulty , String player_name , int totalrooms) throws Exception {
        GameState gamestate = new GameState(difficulty);
        Player player = new Player(player_name);

        MapBuilder mapbuilder = new MapBuilder(); // TODO

        Map<Integer , Room> worldMap = mapbuilder.generateMap(totalrooms);
        gamestate.initialize(player , mapbuilder.getSpawnRoom() , worldMap);

        gameLoop(gamestate);
    }

    public void gameLoop(GameState gamestate) throws Exception {
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


    private void handleAttack(String input) throws Exception{
        Room currentRoom = gameState.getCurrentRoom();

        if(!currentRoom.hasMonster()) {
            view.displayMessage("There's nothing to attack here.");
            return;
        }

        Monster monster = currentRoom.getMonster();
        if (!monster.isDefeated()) {
            view.displayMessage("The " + monster.getName() + " is already defeated.");
            return;
        }

        int playerDamage = calculatePlayerDamage();
        monster.takeDamage(playerDamage);
        view.displayMessage("You attack the " + monster.getName() + " for " + playerDamage + " damage!");
        view.displayMessage(Integer.toString(monster.getHealth()));

        // Check if monster defeated
        if (!monster.isDefeated()) {
            handleMonsterDefeated(monster);
            return;
        }

        int monsterDamage = monster.attack();
        player.takeDamage(monsterDamage);

        view.displayMessage("\nThe " + monster.getName() + " strikes back for " + monsterDamage + " damage!");
        view.displayMessage("Your health: " + player.getHealth() + "/100");

        // Check if player died
        if (!player.isAlive()) {
            handlePlayerDeath();
        }
    }

    private int calculatePlayerDamage() {
        int baseDamage = 10;
        for (Item item : player.getInventory()) {
            if(item instanceof Weapon weapon) {
                baseDamage += weapon.getDamage();
                break;
            }
        }
        int variance = (int)(baseDamage * 0.3);
        return baseDamage + (int)(Math.random() * variance * 2 - variance); // 30% randomness
    }

    private void handleMonsterDefeated(Monster monster) {
        view.displayMessage("\n🎉 You defeated the " + monster.getName() + "!");

        Monster.MonsterDrop reward = monster.getDefeatReward();
        Item itemDropped = reward.item();
        int xpAwarded = reward.xp();

        view.displayMessage("[LOOT] You searched the remains of " + monster.getName() + " and found " + (itemDropped != null ? itemDropped.getName() : "nothing") + " and gained " + xpAwarded + " XP.");

        gameState.incrementMonstersDefeated();
        gameState.getCurrentRoom().removeMonster();

        if(player.levelUp()) {
            handleLevelUp();
        }
    }

    private void handleLevelUp() {
        view.displayMessage("\n✨ LEVEL UP! ✨");
        view.displayMessage("You are now level " + player.getLevel());
        view.displayMessage("Health restored to maximum!");
        player.setHealth(100); // Full heal on level up
    }

    private void handlePlayerDeath() {
        gameState.loseLife();

        if(gameState.checkGameOver()) {
            view.displayMessage("\n💀 GAME OVER 💀");
            view.displayMessage("You have run out of lives.");
            view.displayMessage("\nFinal Score: " + gameState.getGameScore());
            System.exit(0);
        } else {
            view.displayMessage("\n💀 You died!");
            view.displayMessage("Lives remaining: " + gameState.getLivesRemaining());
            Room checkpoint = gameState.getCheckpoint();
            gameState.setCurrentRoom(checkpoint);
            player.reset(checkpoint);
            view.displayMessage("\nYou respawn at the checkpoint...");
            enterRoom(checkpoint);
        }
    }

    private void handleMove(String input) {
        Directions direction = commandparser.parseDirection(input);
        if(direction == null) {
            view.displayMessage("Move where? Available exits: " + gameState.getCurrentRoom().getExits().keySet());
            return;
        }

        Room currentRoom = gameState.getCurrentRoom();

        if(!currentRoom.hasExit(direction)) {
            view.displayMessage("You can't go " + direction + " from here.");
            view.displayMessage("Available exits: " + currentRoom.getExits().keySet());
            return;
        }

        Room nextRoom = currentRoom.getExit(direction);
        if(currentRoom.isExitLocked(direction)) {
            view.displayMessage("The " + direction + " exit is locked. You need a key.");
            return;
        }

        if(currentRoom.hasMonster() && currentRoom.getMonster().isDefeated()) { // defeated same as unalive ig
            view.displayMessage("The " + currentRoom.getMonster().getName() + " blocks your path!");
            view.displayMessage("You must defeat it first.");
            return;
        }

        gameState.moveToRoom(nextRoom); // why send direction here??
        enterRoom(nextRoom);
    }

    private void enterRoom(Room room) {
        view.displayMessage("\n" + room.getName());
        view.displayMessage(room.getDesc());

        if (!room.isVisited()) {
            gameState.incrementRoomsExplored();
            room.setVisited(true);

            player.addXP(10);
            view.displayMessage("\n[+10 XP for exploring new area]");
        }

        // show room contents
        if (room.hasMonster()) {
            Monster monster = room.getMonster();
            if (monster.isDefeated()) {
                view.displayMessage("\n⚔️  " + monster.getName() + " appears! " + monster.getHealth());
            }
        }

        if (room.hasItem()) {
            view.displayMessage("\nItems here are: ");
            for (Item item : room.getItems()) {
                view.displayMessage(item.getName() + " :: " + item.getDesc());
            }
        }

        if (!room.getExits().isEmpty()) {
            view.displayMessage("\nExits: " + room.getExits().keySet());
        }

    }

    private void handleSystemCommand(Action action, String input) {
        switch (action) {
            case HELP -> handleHelp();
            case SAVE -> handleSave(input);
            case LOAD -> handleLoad(input);
            case QUIT -> handleQuit();
            default -> throw new InvalidCommandException("Unknown system command");
        }
    }

    private void handleQuit() {
        view.displayMessage("Thanks for playing JavaRPG!");
        view.displayMessage("\nFinal Stats:");
        view.displayMessage("Score: " + gameState.getGameScore());
        view.displayMessage("Rooms Explored: " + gameState.getRoomsExplored());
        view.displayMessage("Monsters Defeated: " + gameState.getMonstersDefeated());

        System.exit(0);
    }

    private void handleLoad(String input) {
        String filename = extractTarget(input);
        if (filename == null || filename.isEmpty()) {
            filename = "savegame.json";
        }

        // TODO: Implement in Phase 4 (Guide 13)
        view.displayMessage("Load functionality not yet implemented.");
    }

    private void handleHelp() {
        String helptext = commandparser.getHelpText();
        view.displayMessage(helptext);
        Room current = gameState.getCurrentRoom();
        if (current.hasMonster()) {
            view.displayMessage("\n💡 Tip: There's a monster here! Try 'attack monster' or 'examine monster'");
        }if (current.hasItem()) {
            view.displayMessage("\n💡 Tip: There are items here! Try 'take <item>' to collect them");
        }if (!current.getExits().isEmpty()) {
            view.displayMessage("\n💡 Tip: Available exits: " + current.getExits().keySet());
        }
    }

    private void handleSave(String input) {
        String filename = extractTarget(input);
        if (filename == null || filename.isEmpty()) {
            filename = "savegame.json"; // Default
        }
        // TODO: Implement in Phase 4 (Guide 12)
        view.displayMessage("Save functionality not yet implemented.");
        view.displayMessage("Target file: " + filename);
    }

    private String extractTarget(String input) {
        String[] words = input.toLowerCase().split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("save") && i + 1 < words.length) {
                return words[i + 1];
            }
        }
        return null;
    }

    private void handleTake(String input) throws Exception {
        Room currentRoom = gameState.getCurrentRoom();

        if(!currentRoom.hasItem()) {
            view.displayMessage("There's nothing to take here.");
            return;
        }

        String targetName = extractItemName(input);
        if (targetName == null) {
            view.displayMessage("Take what? Items here: " + currentRoom.getItems());
            return;
        }

        Item item = findItemInRoom(currentRoom, targetName);
        if (item == null) {
            view.displayMessage("There's no '" + targetName + "' here.");
            view.displayMessage("Items available: " + currentRoom.getItems());
            return;
        }

        try {
            player.addInventory(item);
            currentRoom.removeItem(item);
            view.displayMessage("You take the " + item.getName() + ".");
        } catch (InventoryFullException e) {
            view.displayMessage("Your inventory is full! (Maximum 5 items)");
            view.displayMessage("Drop something first with 'drop <item>'");

        }
    }

    private String extractItemName(String input) {
        String cleaned = input.toLowerCase()
                .replaceAll("take|get|pick up|grab|pick", "")
                .replaceAll("the|a|an", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Item findItemInRoom(Room room, String targetName) {
        for (Item item : room.getItems()) {
            if (item.getName().toLowerCase().contains(targetName) ||
                    targetName.contains(item.getName().toLowerCase())) {
                return item;
            }
        }
        return null;
    }

    private void handleUse(String input) throws Exception {
        String targetName = extractItemName(input.replace("use" , ""));
        if (targetName == null) {
            view.displayMessage("Use what? Your inventory: " + player.getInventory());
            return;
        }
        ItemIndex itemindex = findItemInInventory(targetName);
        if (itemindex == null) {
            view.displayMessage("You don't have '" + targetName + "'.");
            view.displayMessage("Your inventory: " + player.getInventory());
            return;
        }

        try {
            player.useItem(itemindex.index());
        } catch (Exception e) {
            view.displayMessage("Cannot use " + itemindex.item().getName() + ": " + e.getMessage());
        }

    }

    private ItemIndex findItemInInventory(String target) {
        for (int i=0 ; i<Player.getMaxInventory() ; i++) {
            if(player.getInventory(i).getName().toLowerCase().contains(target) || target.contains(player.getInventory(i).getName().toLowerCase())) {
                return new ItemIndex(player.getInventory(i) , i);
            }
        }
        return null;
    }

    private void handleExamine(String input) {
        String target = extractItemNameExamine(input);
        if(target == null || target.isEmpty()) {

        }
    }

    private String extractItemNameExamine(String input) {
        String cleaned = input.toLowerCase()
                .replaceAll("look|take a look|examine|view", "")
                .replaceAll("the|a|an|at", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
