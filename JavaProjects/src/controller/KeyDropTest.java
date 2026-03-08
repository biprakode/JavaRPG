package controller;

import model.*;
import view.ConsoleViewImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Targeted tests for the anti-softlock key drop mechanic.
 * When a challenge is won in a room with locked exits, a key must drop.
 *
 * Run: java -cp bin controller.KeyDropTest [llm]
 *   - No args: unit tests only (mock LLM)
 *   - "llm": also runs integration test against live llama-server
 */
public class KeyDropTest {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Key Drop Anti-Softlock Tests ===\n");

        // Unit tests (no LLM needed)
        testKeyDropsOnSuccessfulChallengeInLockedRoom();
        testNoKeyDropOnDoorUnlockChallenge();
        testNoKeyDropIfRoomAlreadyHasKey();
        testNoKeyDropIfPlayerAlreadyHasKey();
        testNoKeyDropOnFailedChallenge();
        testNoKeyDropIfRoomHasNoLockedExits();

        // Integration test with live LLM
        if (args.length > 0 && args[0].equals("llm")) {
            System.out.println("\n--- Integration Test (Live LLM) ---");
            testLiveChallengeThenKeyDrop();
        }

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    // === Test 1: Successful challenge in room with locked exits -> key drops ===
    static void testKeyDropsOnSuccessfulChallengeInLockedRoom() {
        // Setup: room with a locked NORTH exit, player inside
        Room room = new Room(10, "Locked Hall", "A hall with a locked door", RoomType.NORMAL);
        Room northRoom = new Room(11, "Beyond", "The room beyond", RoomType.NORMAL);
        room.addExit(Directions.NORTH, northRoom);
        room.lockExit(Directions.NORTH);

        Player player = new Player("Tester");
        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(10, room);
        worldMap.put(11, northRoom);
        gs.initialize(player, room, worldMap);

        // Simulate: a successful non-door challenge completed in this room
        TestableGameController gc = new TestableGameController(gs, player);

        // Create a successful challenge with NO unlockDirection metadata
        Challenge challenge = new Challenge(ChallengeType.RIDDLE, "What walks on 4 legs?", ChallengeDifficulty.EASY);
        challenge.complete(true, "Correct!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        // Assert: key should be in the room
        boolean hasKey = room.getItems().stream().anyMatch(i -> i instanceof Key);
        assertTest("Key drops on successful challenge in locked room", hasKey);
    }

    // === Test 2: Door-unlock challenge should NOT drop key (it unlocks directly) ===
    static void testNoKeyDropOnDoorUnlockChallenge() {
        Room room = new Room(20, "Door Room", "A room", RoomType.NORMAL);
        Room eastRoom = new Room(21, "East Room", "East", RoomType.NORMAL);
        room.addExit(Directions.EAST, eastRoom);
        room.lockExit(Directions.EAST);

        Player player = new Player("Tester");
        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(20, room);
        worldMap.put(21, eastRoom);
        gs.initialize(player, room, worldMap);

        TestableGameController gc = new TestableGameController(gs, player);

        // Simulate a door-unlock challenge (has unlockDirection metadata)
        Challenge challenge = new Challenge(ChallengeType.PUZZLE, "Solve to unlock", ChallengeDifficulty.EASY);
        challenge.addMetaData("unlockDirection", "EAST");
        challenge.complete(true, "Door unlocked!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        // Assert: NO key dropped (door was unlocked directly instead)
        boolean hasKey = room.getItems().stream().anyMatch(i -> i instanceof Key);
        assertTest("No key drop on door-unlock challenge", !hasKey);

        // Assert: the exit IS unlocked
        assertTest("Door-unlock challenge unlocks the exit", !room.isExitLocked(Directions.EAST));
    }

    // === Test 3: No duplicate key if room already has one ===
    static void testNoKeyDropIfRoomAlreadyHasKey() {
        Room room = new Room(30, "Key Room", "Already has key", RoomType.NORMAL);
        Room southRoom = new Room(31, "South", "South room", RoomType.NORMAL);
        room.addExit(Directions.SOUTH, southRoom);
        room.lockExit(Directions.SOUTH);
        room.addItem(new Key("Old Key", "An existing key", 30));

        Player player = new Player("Tester");
        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(30, room);
        worldMap.put(31, southRoom);
        gs.initialize(player, room, worldMap);

        TestableGameController gc = new TestableGameController(gs, player);

        Challenge challenge = new Challenge(ChallengeType.CREATIVE, "Write a poem", ChallengeDifficulty.EASY);
        challenge.complete(true, "Nice poem!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        // Assert: still only 1 key (no duplicate)
        long keyCount = room.getItems().stream().filter(i -> i instanceof Key).count();
        assertTest("No duplicate key if room already has one", keyCount == 1);
    }

    // === Test 4: No key if player already has one ===
    static void testNoKeyDropIfPlayerAlreadyHasKey() {
        Room room = new Room(40, "Player Key Room", "Player has key", RoomType.NORMAL);
        Room westRoom = new Room(41, "West", "West room", RoomType.NORMAL);
        room.addExit(Directions.WEST, westRoom);
        room.lockExit(Directions.WEST);

        Player player = new Player("Tester");
        player.addInventory(new Key("Player Key", "Already in inventory", 40));

        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(40, room);
        worldMap.put(41, westRoom);
        gs.initialize(player, room, worldMap);

        TestableGameController gc = new TestableGameController(gs, player);

        Challenge challenge = new Challenge(ChallengeType.RIDDLE, "Test riddle", ChallengeDifficulty.MEDIUM);
        challenge.complete(true, "Correct!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        // Assert: no key in room (player already has one)
        boolean roomHasKey = room.getItems().stream().anyMatch(i -> i instanceof Key);
        assertTest("No key drop if player already has key", !roomHasKey);
    }

    // === Test 5: Failed challenge should NOT drop key ===
    static void testNoKeyDropOnFailedChallenge() {
        Room room = new Room(50, "Fail Room", "Failed here", RoomType.NORMAL);
        Room northRoom = new Room(51, "North", "North room", RoomType.NORMAL);
        room.addExit(Directions.NORTH, northRoom);
        room.lockExit(Directions.NORTH);

        Player player = new Player("Tester");
        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(50, room);
        worldMap.put(51, northRoom);
        gs.initialize(player, room, worldMap);

        TestableGameController gc = new TestableGameController(gs, player);

        Challenge challenge = new Challenge(ChallengeType.PUZZLE, "Hard puzzle", ChallengeDifficulty.HARD);
        challenge.complete(false, "Wrong answer!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        boolean hasKey = room.getItems().stream().anyMatch(i -> i instanceof Key);
        assertTest("No key drop on failed challenge", !hasKey);
    }

    // === Test 6: No key drop if room has no locked exits ===
    static void testNoKeyDropIfRoomHasNoLockedExits() {
        Room room = new Room(60, "Open Room", "No locks", RoomType.NORMAL);
        Room northRoom = new Room(61, "North", "North room", RoomType.NORMAL);
        room.addExit(Directions.NORTH, northRoom); // unlocked

        Player player = new Player("Tester");
        GameState gs = new GameState(GameDifficulty.EASY);
        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(60, room);
        worldMap.put(61, northRoom);
        gs.initialize(player, room, worldMap);

        TestableGameController gc = new TestableGameController(gs, player);

        Challenge challenge = new Challenge(ChallengeType.RIDDLE, "Simple riddle", ChallengeDifficulty.EASY);
        challenge.complete(true, "Correct!");

        gc.setActiveChallenge(challenge);
        gc.callHandleChallengeComplete();

        boolean hasKey = room.getItems().stream().anyMatch(i -> i instanceof Key);
        assertTest("No key drop if room has no locked exits", !hasKey);
    }

    // === Integration Test: Full LLM challenge flow ===
    static void testLiveChallengeThenKeyDrop() throws Exception {
        LLMService llm = new LLMServiceImpl(
                "http://localhost:8080/v1/chat/completions",
                "qwen2.5-1.5b-instruct",
                60, 3
        );

        if (!llm.isAvailable()) {
            System.out.println("  SKIP: LLM server not reachable");
            return;
        }
        System.out.println("  LLM server is online.");

        // Build a small world: spawn -> locked room -> end
        Room spawn = new Room(1, "Spawn Chamber", "A dusty stone chamber", RoomType.NORMAL);
        Room locked = new Room(2, "Locked Vault", "A sealed vault", RoomType.NORMAL);
        Room end = new Room(3, "Treasure Room", "Gold everywhere", RoomType.NORMAL);

        spawn.addExit(Directions.NORTH, locked);
        locked.addExit(Directions.SOUTH, spawn);
        locked.addExit(Directions.NORTH, end);
        locked.lockExit(Directions.NORTH); // locked exit in room 2

        end.addExit(Directions.SOUTH, locked);

        Map<Integer, Room> worldMap = new HashMap<>();
        worldMap.put(1, spawn);
        worldMap.put(2, locked);
        worldMap.put(3, end);

        GameController gc = new GameController(GameDifficulty.EASY, llm);
        gc.gameState = new GameState(GameDifficulty.EASY);
        gc.player = new Player("IntegrationTester");
        gc.commandparser = new CommandParser();
        gc.view = new ConsoleViewImpl();
        gc.gameState.initialize(gc.player, spawn, worldMap);

        // Move to the locked room
        gc.gameState.moveToRoom(locked);
        locked.setVisited(true); // skip exploration trigger

        System.out.println("  Player is in room: " + gc.gameState.getCurrentRoom().getName());
        System.out.println("  Room has locked NORTH exit: " + locked.isExitLocked(Directions.NORTH));

        // Trigger a challenge in the locked room via attack simulation won't work.
        // Instead, use ChallengeController directly.
        ChallengeEvaluatorImpl evaluator = new ChallengeEvaluatorImpl();
        ChallengeController cc = new ChallengeController(llm, gc.view, gc.gameState, evaluator);

        System.out.println("  Initiating RIDDLE challenge...");
        cc.initiateChallenge(locked, ChallengeType.RIDDLE);

        Challenge active = cc.getActiveChallenge();
        if (active == null) {
            System.out.println("  SKIP: LLM failed to generate challenge");
            return;
        }

        System.out.println("  Challenge prompt: " + active.getPrompt());
        String expectedPattern = active.getMetaData("expectedPattern");
        System.out.println("  Expected pattern: " + expectedPattern);

        // Submit the expected answer (or a reasonable guess)
        String answer = expectedPattern != null ? expectedPattern : "I don't know";
        System.out.println("  Submitting answer: " + answer);
        cc.submitResponse(answer);

        // Check result
        boolean challengeCompleted = !cc.hasActiveChallenge();
        System.out.println("  Challenge completed: " + challengeCompleted);

        if (active.getWasSuccesful()) {
            System.out.println("  Challenge was SUCCESSFUL");

            // Now simulate handleChallengeComplete via our testable controller
            TestableGameController tgc = new TestableGameController(gc.gameState, gc.player);
            tgc.setActiveChallenge(active);
            tgc.callHandleChallengeComplete();

            boolean keyDropped = locked.getItems().stream().anyMatch(i -> i instanceof Key);
            assertTest("[LLM] Key drops after successful challenge in locked room", keyDropped);

            if (keyDropped) {
                Key key = (Key) locked.getItems().stream().filter(i -> i instanceof Key).findFirst().get();
                System.out.println("  Dropped key: " + key.getName() + " (targetId=" + key.getTargetId() + ")");
            }
        } else {
            System.out.println("  Challenge FAILED (LLM rejected answer) - testing failed path");
            boolean keyDropped = locked.getItems().stream().anyMatch(i -> i instanceof Key);
            assertTest("[LLM] No key drop on failed challenge", !keyDropped);
        }
    }

    // === Test Helper: Subclass to access private handleChallengeComplete ===
    static class TestableGameController extends GameController {
        private Challenge testChallenge;

        TestableGameController(GameState gs, Player p) {
            super(GameDifficulty.EASY, new StubLLMService());
            this.gameState = gs;
            this.player = p;
            this.view = new ConsoleViewImpl();
        }

        void setActiveChallenge(Challenge c) {
            this.testChallenge = c;
            // Use reflection-free approach: set via the accessible field
            try {
                var field = GameController.class.getDeclaredField("activeChallenge");
                field.setAccessible(true);
                field.set(this, c);
            } catch (Exception e) {
                throw new RuntimeException("Cannot set activeChallenge", e);
            }
        }

        void callHandleChallengeComplete() {
            try {
                var method = GameController.class.getDeclaredMethod("handleChallengeComplete");
                method.setAccessible(true);
                method.invoke(this);
            } catch (Exception e) {
                throw new RuntimeException("Cannot call handleChallengeComplete", e);
            }
        }
    }

    // === Stub LLM for unit tests (never actually called) ===
    static class StubLLMService implements LLMService {
        public String generateChallenge(ChallengeType t, ChallengeDifficulty d, String ctx) { return null; }
        public String generateChallenge(String p) { return null; }
        public String evaluateResponse(String r, String e, String c) { return null; }
        public String generateHint(String p, String a, int l) { return null; }
        public Map<String, String> parseJsonResponse(String j) { return new HashMap<>(); }
        public String generateText(String s, String u) { return null; }
        public boolean isAvailable() { return false; }
    }

    static void assertTest(String name, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + name);
            passed++;
        } else {
            System.out.println("  FAIL: " + name);
            failed++;
        }
    }
}
