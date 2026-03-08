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
    protected LLMService llmService;
    private ChallengeController challengeController;
    private ChallengeEvaluatorImpl evaluator;
    private Challenge activeChallenge;
    private java.util.Set<Integer> challengedMonsterRooms = new java.util.HashSet<>();

    public GameController(GameDifficulty difficulty) {
        this(difficulty, new LLMServiceImpl(
                "http://localhost:8080/v1/chat/completions",
                "qwen2.5-3b-instruct",
                60,
                3
        ));
    }

    public GameController(GameDifficulty difficulty, LLMService llmService) {
        this.view = new ConsoleViewImpl();
        this.llmService = llmService;
        this.evaluator = new ChallengeEvaluatorImpl();
        this.gameState = new GameState(difficulty);
        this.challengeController = new ChallengeController(llmService , view , gameState , evaluator);
        this.activeChallenge = null;
    }

    public void startGame(GameDifficulty difficulty, String player_name, int totalrooms) throws Exception {
        startGame(difficulty, player_name, totalrooms, new Scanner(System.in));
    }

    public void startGame(GameDifficulty difficulty, String player_name, int totalrooms, Scanner scanner) throws Exception {
        this.gameState = new GameState(difficulty);
        this.player = new Player(player_name);
        this.mapbuilder = new MapBuilder(this.llmService);
        this.commandparser = new CommandParser();

        Room spawnRoom = this.mapbuilder.generateMap(totalrooms);
        Map<Integer, Room> worldMap = this.mapbuilder.getWorldMap();
        this.gameState.initialize(this.player, spawnRoom, worldMap);

        // Recreate challenge controller with the new game state
        this.challengeController = new ChallengeController(llmService, view, gameState, evaluator);

        gameLoop(this.gameState, scanner);
    }

    public void gameLoop(GameState gamestate, Scanner scanner) throws Exception {
        view.displayBanner("THE ADVENTURE BEGINS");
        view.displayRoom(gamestate.getCurrentRoom());

        while(!gamestate.isGameOver()) {
            view.displayMessage("\nWhat will you do? > ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            try {
                processCommand(input);
            } catch (Exception e) {
                // RIPException, PlayerAlreadyDeadException, etc.
            }
            if(!gamestate.getPlayer().isAlive() && !gamestate.hasLivesRemaining()) {
                view.displayGameOver(gamestate, false);
                gamestate.setGameOver(true);
            }
        }
        view.displayMessage("Thanks for playing!");
    }

    public void processCommand(String input) throws Exception {
        // Sync: timer thread may have completed the challenge
        if (activeChallenge != null && !challengeController.hasActiveChallenge()) {
            handleChallengeComplete();
        }

        if (activeChallenge != null) {
            processChallengeResponse(input);
            return;
        }

        Command command = this.commandparser.parse(input);
        Action action = command.getAction();

        if (action == Action.UNKNOWN) {
            String suggestion = command.getSuggestion();
            if (suggestion != null) {
                view.displayWarning("Unknown command. Did you mean '" + suggestion + "'?");
            } else {
                view.displayWarning("Unknown command. Type 'help' for a list of commands.");
            }
            return;
        }

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
            case DROP -> handleDrop(input);
            case USE -> handleUse(input);
            case EXAMINE -> handleExamine(input);
            case TALK -> handleTalk(input);
            case INVENTORY -> handleInventory();
            case STATS -> handleStats();
            default -> view.displayWarning("Unknown command. Type 'help' for a list of commands.");
        }
    }

    private void processChallengeResponse(String input) {
        String lowerInput = input.toLowerCase().trim();

        if (lowerInput.equals("hint") || lowerInput.equals("help")) {
            try {
                int nextHintLevel = activeChallenge.getHintsUsed() + 1;
                challengeController.requiresHint(nextHintLevel);
            } catch (Exception e) {
                view.displayMessage("No hints available.");
            }
            return;
        }

        if (lowerInput.equals("skip") || lowerInput.equals("abort")) {
            try {
                challengeController.abortChallenge();
            } catch (Exception e) {
                view.displayMessage("Cannot abort challenge.");
            }
            activeChallenge = null;
            if (!player.isAlive()) {
                handlePlayerDeath();
            }
            return;
        }

        // Submit answer to ChallengeController
        try {
            challengeController.submitResponse(input);
        } catch (Exception e) {
            // Player may have died from challenge damage (RIPException)
            if (!player.isAlive()) {
                activeChallenge = null;
                handlePlayerDeath();
                return;
            }
            view.displayMessage("Error processing response: " + e.getMessage());
            return;
        }

        // Check if challenge completed (success or attempts exhausted)
        if (!challengeController.hasActiveChallenge()) {
            handleChallengeComplete();
        }
    }

    private void handleChallengeComplete() {
        if (activeChallenge != null) {
            // Handle door unlock if this was a door challenge
            String dirStr = activeChallenge.getMetaData("unlockDirection");
            if (dirStr != null && activeChallenge.getWasSuccesful()) {
                Directions direction = Directions.valueOf(dirStr);
                gameState.getCurrentRoom().unlockExit(direction);
                view.displayMessage("The " + direction + " door swings open!");
            }

            // Guarantee key drop if room has locked exits and challenge was successful
            // (skip for door-unlock challenges, which already unlock directly)
            if (dirStr == null && activeChallenge.getWasSuccesful()) {
                Room currentRoom = gameState.getCurrentRoom();
                if (currentRoom.getLockedExits() != null && !currentRoom.getLockedExits().isEmpty()) {
                    // Only drop if room doesn't already have a key
                    boolean roomHasKey = currentRoom.getItems().stream().anyMatch(i -> i instanceof Key);
                    boolean playerHasKey = hasKey(player);
                    if (!roomHasKey && !playerHasKey) {
                        Key key = new Key("Gleaming Key", "A key revealed by solving the challenge", currentRoom.getId());
                        currentRoom.addItem(key);
                        view.displayMessage("Something clatters to the ground... a key!");
                        view.displayItemDrop(key);
                    }
                }
            }
        }
        activeChallenge = null;
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

        // First encounter with any monster triggers a challenge
        if (activeChallenge == null && !monster.isDefeated() && !challengedMonsterRooms.contains(currentRoom.getId())) {
            challengedMonsterRooms.add(currentRoom.getId());
            triggerBossCombatChallenge(monster, currentRoom);
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
        try {
            player.takeDamage(monsterDamage);
        } catch (Exception e) {
            // RIPException — player died from monster attack
        }

        view.displayCombatAction(monster.getName(), "strikes back at", monsterDamage, "you");
        view.displayPlayerHealth(player);

        if (!player.isAlive()) {
            handlePlayerDeath();
        }
    }

    private void triggerBossCombatChallenge(Monster monster, Room currentRoom) {
        try {
            ChallengeType challengeType = pickMonsterChallengeType();
            challengeController.initiateChallenge(currentRoom , challengeType);
            activeChallenge = challengeController.getActiveChallenge();
            if (activeChallenge != null) {
                view.displayMessage("\nBefore you can engage, you must solve the guardian's challenge...");
            }
        } catch (Exception e) {
            view.displayMessage("The boss roars, ready for battle!");
            activeChallenge = null;
        }
    }

    private int calculatePlayerDamage() {
        int baseDamage = 5;
        // Only equipped weapon adds damage
        if (player.getEquippedItem() instanceof Weapon weapon) {
            baseDamage += weapon.getDamage();
        }
        int variance = (int)(baseDamage * 0.3);
        return Math.max(1, baseDamage + (int)(Math.random() * variance * 2 - variance));
    }

    private void handleMonsterDefeated(Monster monster) {
        Monster.MonsterDrop reward = monster.getDefeatReward();
        view.displayVictory(monster, reward);

        // Award XP from monster kill
        if (reward.xp() > 0) {
            player.addXP(reward.xp());
        }

        // Drop item from monster loot (if any)
        if (reward.item() != null) {
            gameState.getCurrentRoom().addItem(reward.item());
            view.displayItemDrop(reward.item());
        }

        // Generate random drop based on monster difficulty
        Item drop = generateMonsterDrop(monster.getDifficulty());
        if (drop != null) {
            gameState.getCurrentRoom().addItem(drop);
            view.displayItemDrop(drop);
        }

        gameState.incrementMonstersDefeated();
        gameState.getCurrentRoom().removeMonster();

        if(player.levelUp()) {
            handleLevelUp();
        }

        if(gameState.getCurrentRoom().getRoomtype() == RoomType.BOSS) {
            view.displayGameOver(gameState , true);
            gameState.setGameOver(true);
            view.displayStats(player , gameState);
        }
    }

    private Item generateMonsterDrop(MonsterDifficulty difficulty) {
        double dropChance = switch (difficulty) {
            case EASY -> 0.3;
            case MEDIUM -> 0.5;
            case HARD -> 1.0;
        };
        if (Math.random() > dropChance) return null;

        return switch (difficulty) {
            case EASY -> new Potion("Small Vial", "A potion found on the creature", 15);
            case MEDIUM -> {
                if (Math.random() < 0.5) {
                    yield new Potion("Health Draught", "A restorative found on the fallen foe", 30);
                } else {
                    yield new Weapon("Crude Blade", "A weapon pried from the monster's grip", 12, 2);
                }
            }
            case HARD -> {
                if (Math.random() < 0.4) {
                    yield new Weapon("Champion's Edge", "A powerful weapon from a worthy foe", 25, 1);
                } else {
                    yield new Treasure("Monster Hoard", "Valuables hoarded by the beast", 50);
                }
            }
        };
    }

    private void handleLevelUp() {
        player.setHealth(100); // Full heal on level up
        view.displayLevelUp(player, player.getLevel());
    }

    private void handlePlayerDeath() {
        gameState.loseLife();

        if(gameState.checkGameOver()) {
            view.displayGameOver(gameState, false);
            gameState.setGameOver(true);
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
        //strip verb before passing direction
        String dir = extractDirection(input);
        Directions direction = commandparser.parseDirection(dir);
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
            if (hasKey(player)) {
                view.displayMessage("The " + direction + " exit is locked. You can use a key or attempt the magical puzzle.");
                view.displayMessage("Type 'use key' to unlock, or 'solve' to attempt the puzzle.");
            } else {
                triggerDoorUnlockChallenge(direction);
            }
            return;
        }

        if(currentRoom.hasMonster() && !currentRoom.getMonster().isDefeated()) {
            view.displayWarning("The " + currentRoom.getMonster().getName() + " blocks your path! You must defeat it first.");
            return;
        }

        gameState.moveToRoom(nextRoom);
        enterRoom(nextRoom);
    }

    private String extractDirection(String input) {
        for(String word : input.split(" ")) {
            for(Directions direction : Directions.values()) {
                if(word.equalsIgnoreCase(direction.getShortName())) {
                    return direction.getShortName();
                }
            }
        }
        return null;
    }

    private void triggerDoorUnlockChallenge(Directions direction) {
        try {
            challengeController.initiateChallenge(gameState.getCurrentRoom(), ChallengeType.PUZZLE);
            activeChallenge = challengeController.getActiveChallenge();
            if (activeChallenge != null) {
                activeChallenge.addMetaData("unlockDirection", direction.toString());
            }
        } catch (Exception e) {
            view.displayMessage("The door is locked and there's no way to open it.");
            activeChallenge = null;
        }
    }

    private boolean hasKey(Player player) {
        return player.getInventory().stream().anyMatch(item -> item instanceof Key);
    }

    private void enterRoom(Room room) {
        boolean firstVisit = !room.isVisited();

        if (firstVisit) {
            gameState.incrementRoomsExplored();
            gameState.setTotalScore(gameState.getTotalScore() + 10);
            room.setVisited(true);
            player.addXP(10);
        }

        view.displayRoom(room);

        if (firstVisit) {
            view.displaySuccess("+10 XP for exploring new area");
        }
        if (firstVisit && Math.random() < 0.6) {
            triggerExplorationChallenge(room);
        }
    }

    private void triggerExplorationChallenge(Room room) {
        try {
            ChallengeType challengeType = pickRandomChallengeType();
            challengeController.initiateChallenge(room , challengeType);
            activeChallenge = challengeController.getActiveChallenge();
            if (activeChallenge == null) {
                view.displayMessage("An ancient puzzle appears, but it seems broken...");
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] Challenge generation failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            view.displayMessage("An ancient puzzle appears, but it seems broken...");
            activeChallenge = null;
        }
    }

    private ChallengeType pickRandomChallengeType() {
        ChallengeType[] types = {
                ChallengeType.RIDDLE,
                ChallengeType.PUZZLE,
                ChallengeType.MORAL_DILEMMA,
                ChallengeType.NEGOTIATION,
                ChallengeType.CREATIVE,
                ChallengeType.COMBAT_CREATIVE
        };
        return types[(int)(Math.random() * types.length)];
    }

    private ChallengeType pickMonsterChallengeType() {
        ChallengeType[] types = {
                ChallengeType.COMBAT_CREATIVE,
                ChallengeType.COMBAT_CREATIVE,
                ChallengeType.RIDDLE,
                ChallengeType.NEGOTIATION
        };
        return types[(int)(Math.random() * types.length)];
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
        gameState.setGameOver(true);
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
                .replaceAll("\\b(take|get|pick up|grab|pick)\\b", "")
                .replaceAll("\\b(the|an|a)\\b", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void handleDrop(String input) {
        String targetName = extractItemName(input.replace("drop", "").replace("discard", "").replace("throw", ""));
        if (targetName == null) {
            view.displayWarning("Drop what? Check your inventory with 'inventory'");
            return;
        }
        ItemIndex itemindex = findItemInInventory(targetName);
        if (itemindex == null) {
            view.displayWarning("You don't have '" + targetName + "'.");
            return;
        }

        Item item = player.removeInventory(itemindex.index());
        if (item != null) {
            gameState.getCurrentRoom().addItem(item);
            view.displayItemDrop(item);
        }
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

        Item item = itemindex.item();

        // Key: check for locked exits before consuming
        if (item instanceof Key) {
            Room currentRoom = gameState.getCurrentRoom();
            if (currentRoom.getLockedExits() == null || currentRoom.getLockedExits().isEmpty()) {
                view.displayWarning("There are no locked exits here to use " + item.getName() + " on.");
                return;
            }
        }

        try {
            player.useItem(itemindex.index());
            view.displayItemUse(item, "Used successfully");
        } catch (Exception e) {
            view.displayError("Cannot use " + item.getName() + ": " + e.getMessage());
        }
    }

    private ItemIndex findItemInInventory(String target) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            Item item = player.getInventory(i);
            if (item == null) continue;
            if (item.getName().toLowerCase().contains(target) || target.contains(item.getName().toLowerCase())) {
                return new ItemIndex(item, i);
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
                .replaceAll("\\b(take a look|look|examine|view)\\b", "")
                .replaceAll("\\b(the|an|a|at)\\b", "")
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

        // TODO: add NPC system for friendly characters
    }

    private void handleInventory() {
        view.displayInventory(player);
    }

    private void handleStats() {
        view.displayStats(player, gameState);
    }
}