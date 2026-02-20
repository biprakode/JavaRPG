package controller;

import controller.error.InvalidCommandException;
import model.*;
import model.error.InventoryFullException;
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

    public void startGame(GameDifficulty difficulty , String player_name , int totalrooms) throws Exception {
        this.gameState = new GameState(difficulty);
        this.player = new Player(player_name);
        this.mapbuilder = new MapBuilder(); // TODO
        this.commandparser = new CommandParser();

        Map<Integer , Room> worldMap = this.mapbuilder.generateMap(totalrooms);
        this.gameState.initialize(this.player , this.mapbuilder.getSpawnRoom() , worldMap);

        gameLoop(this.gameState);
    }

    public void gameLoop(GameState gamestate) throws Exception {
        Scanner scanner = new Scanner(System.in);
        view.displayBanner("THE ADVENTURE BEGINS");
        view.displayRoom(gamestate.getCurrentRoom());

        while(!gamestate.isGameOver()) {
            view.displayMessage("\nWhat will you do? > ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            processCommand(input);
            if(!gamestate.getPlayer().isAlive() && !gamestate.hasLivesRemaining()) {
                view.displayGameOver(gamestate, false);
                gamestate.setGameOver(true);
            }
        }
        view.displayMessage("Thanks for playing!");
    }

    public void processCommand(String input) throws Exception {
        Action action = this.commandparser.parse(input).getAction();

        if(action.isSystemCommand()) {
            handleSystemCommand(action , input);
            return;
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
            view.displayWarning("There's nothing to attack here.");
            return;
        }

        Monster monster = currentRoom.getMonster();
        if (monster.isDefeated()) {
            view.displayInfo("The " + monster.getName() + " is already defeated.");
            return;
        }

        int playerDamage = calculatePlayerDamage();
        monster.takeDamage(playerDamage);
        view.displayCombatAction("You", "attack", playerDamage, monster.getName());
        view.displayMonsterHealth(monster);

        // Check if monster defeated
        if (monster.isDefeated()) {
            handleMonsterDefeated(monster);
            return;
        }

        int monsterDamage = monster.attack();
        player.takeDamage(monsterDamage);

        view.displayCombatAction(monster.getName(), "strikes back at", monsterDamage, "you");
        view.displayPlayerHealth(player);

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
        Monster.MonsterDrop reward = monster.getDefeatReward();
        view.displayVictory(monster, reward);

        gameState.incrementMonstersDefeated();
        gameState.getCurrentRoom().removeMonster();

        if(player.levelUp()) {
            handleLevelUp();
        }
    }

    private void handleLevelUp() {
        player.setHealth(100); // Full heal on level up
        view.displayLevelUp(player, player.getLevel());
    }

    private void handlePlayerDeath() {
        gameState.loseLife();

        if(gameState.checkGameOver()) {
            view.displayGameOver(gameState, false);
            System.exit(0);
        } else {
            view.displayDefeat("You died! Lives remaining: " + gameState.getLivesRemaining());
            Room checkpoint = gameState.getCheckpoint();
            gameState.setCurrentRoom(checkpoint);
            player.reset(checkpoint);
            view.displayInfo("You respawn at the checkpoint...");
            enterRoom(checkpoint);
        }
    }

    private void handleMove(String input) {
        Directions direction = commandparser.parseDirection(input);
        if(direction == null) {
            view.displayWarning("Move where?");
            view.displayExits(gameState.getCurrentRoom().getExits(), gameState.getCurrentRoom().getLockedExits());
            return;
        }

        Room currentRoom = gameState.getCurrentRoom();

        if(!currentRoom.hasExit(direction)) {
            view.displayWarning("You can't go " + direction + " from here.");
            view.displayExits(currentRoom.getExits(), currentRoom.getLockedExits());
            return;
        }

        Room nextRoom = currentRoom.getExit(direction);
        if(currentRoom.isExitLocked(direction)) {
            view.displayWarning("The " + direction + " exit is locked. You need a key.");
            return;
        }

        if(currentRoom.hasMonster() && !currentRoom.getMonster().isDefeated()) {
            view.displayWarning("The " + currentRoom.getMonster().getName() + " blocks your path! You must defeat it first.");
            return;
        }

        gameState.moveToRoom(nextRoom);
        enterRoom(nextRoom);
    }

    private void enterRoom(Room room) {
        boolean firstVisit = !room.isVisited();

        if (firstVisit) {
            gameState.incrementRoomsExplored();
            room.setVisited(true);
            player.addXP(10);
        }

        view.displayRoom(room);

        if (firstVisit) {
            view.displaySuccess("+10 XP for exploring new area");
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
        view.displayGameOver(gameState, false);
        System.exit(0);
    }

    private void handleLoad(String input) {
        String filename = extractTarget(input);
        if (filename == null || filename.isEmpty()) {
            filename = "savegame.json";
        }

        // TODO: Implement in Phase 4 (Guide 13)
        view.displayWarning("Load functionality not yet implemented.");
    }

    private void handleHelp() {
        String helptext = commandparser.getHelpText();
        view.displayMessage(helptext);
        Room current = gameState.getCurrentRoom();
        if (current.hasMonster()) {
            view.displayInfo("Tip: There's a monster here! Try 'attack monster' or 'examine monster'");
        }
        if (current.hasItem()) {
            view.displayInfo("Tip: There are items here! Try 'take <item>' to collect them");
        }
        if (!current.getExits().isEmpty()) {
            view.displayExits(current.getExits(), current.getLockedExits());
        }
    }

    private void handleSave(String input) {
        String filename = extractTarget(input);
        if (filename == null || filename.isEmpty()) {
            filename = "savegame.json"; // Default
        }
        // TODO: Implement in Phase 4 (Guide 12)
        view.displayWarning("Save functionality not yet implemented.");
        view.displayInfo("Target file: " + filename);
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
            view.displayWarning("There's nothing to take here.");
            return;
        }

        String targetName = extractItemName(input);
        if (targetName == null) {
            view.displayWarning("Take what? Items here: " + currentRoom.getItems());
            return;
        }

        Item item = findItemInRoom(currentRoom, targetName);
        if (item == null) {
            view.displayWarning("There's no '" + targetName + "' here.");
            return;
        }

        try {
            player.addInventory(item);
            currentRoom.removeItem(item);
            view.displayItemPickup(item);
        } catch (InventoryFullException e) {
            view.displayError("Your inventory is full! (Maximum " + Player.getMaxInventory() + " items). Drop something first with 'drop <item>'");
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
            view.displayWarning("Use what? Check your inventory with 'inventory'");
            return;
        }
        ItemIndex itemindex = findItemInInventory(targetName);
        if (itemindex == null) {
            view.displayWarning("You don't have '" + targetName + "'.");
            return;
        }

        try {
            player.useItem(itemindex.index());
            view.displayItemUse(itemindex.item(), "Used successfully");
        } catch (Exception e) {
            view.displayError("Cannot use " + itemindex.item().getName() + ": " + e.getMessage());
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
            Room room = gameState.getCurrentRoom();
            view.displayRoom(room);
            return;
        }

        Room room = gameState.getCurrentRoom();

        // Check if it's a monster
        if (room.hasMonster() && room.getMonster().getName().toLowerCase().contains(target)) {
            view.displayMonster(room.getMonster());
            return;
        }

        Item roomItem = findItemInRoom(room, target);
        if (roomItem != null) {
            view.displayMessage(roomItem.getDesc());
            return;
        }

        ItemIndex invItemIndex = findItemInInventory(target);
        if (invItemIndex != null) {
            Item invItem = invItemIndex.item();
            view.displayMessage(invItem.getDesc());
            if (invItem instanceof Weapon weapon) {
                view.displayMessage("Damage: " + weapon.getDamage());
                view.displayMessage("Level: " + weapon.getLvl());
            }
            return;
        }
        view.displayWarning("You don't see any '" + target + "' here.");
    }

    private String extractItemNameExamine(String input) {
        String cleaned = input.toLowerCase()
                .replaceAll("look|take a look|examine|view", "")
                .replaceAll("the|a|an|at", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void handleTalk(String input) {
        Room room = gameState.getCurrentRoom();

        // Check if there's anyone to talk to
        if (!room.hasMonster()) {
            view.displayInfo("There's no one here to talk to.");
            return;
        }

        Monster monster = room.getMonster();

        // Peaceful monsters might talk
        // For now, all monsters are hostile
        if (!monster.isDefeated()) {
            view.displayWarning("The " + monster.getName() + " snarls at you menacingly! It doesn't seem interested in talking...");
        } else {
            view.displayInfo("The defeated " + monster.getName() + " doesn't respond.");
        }

        // TODO: In Phase 7, add NPC system for friendly characters
    }

    private void handleInventory() {
        view.displayInventory(player);
    }

    private void handleStats() {
        view.displayStats(player, gameState);
    }
}