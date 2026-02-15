package view;

import controller.ConsoleView;
import model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConsoleViewImpl implements ConsoleView {

    // ANSI Color codes
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String BOLD = "\u001B[1m";

    @Override
    public void displayMessage(String message) {
        System.out.println(message + '\n');
    }

    @Override
    public void displayChallenge(Challenge challenge) {
        String type = challenge.getType().toString().toLowerCase();
        String difficulty = challenge.getDifficulty().toString();
        String prompt = challenge.getPrompt();
        String timeLimit = String.valueOf(challenge.getTimeLimit());
        int attemptsRemaining = challenge.getAttemptsRemaining();

        String challengeString = visualSeperator() +
                "CHALLENGE: " + type + " (" + difficulty + ")\n" +
                visualSeperator() +
                prompt + "\n" +
                "Time Limit: " + timeLimit + " | Attempts: " + attemptsRemaining + " remaining\n" +
                visualSeperator();

        displayMessage(challengeString);
    }

    @Override
    public void displayResult(Challenge challenge, ChallengeResult result) {
        boolean success = result.isSuccess();
        StringBuilder sb = new StringBuilder();
        String message = result.getFeedback();

        if (success) {
            int xpAwarded = result.getXpAwarded();
            List<Item> items = result.getItemsAwarded();

            sb.append("CORRECT!\n");
            sb.append("You earned ").append(xpAwarded).append(" XP!\n");
            sb.append(message).append("\n");

            sb.append("Item Rewarded: ");
            if (items == null || items.isEmpty()) {
                sb.append("No items rewarded");
            } else {
                for (int i = 0; i < items.size(); i++) {
                    sb.append(items.get(i).getName());
                    if (i < items.size() - 1) sb.append(", ");
                }
            }
        } else {
            String type = challenge.getType().toString().toLowerCase();
            int damageTaken = result.getDamageTaken();
            int attemptsRemaining = challenge.getAttemptsRemaining();

            sb.append("INCORRECT\n");
            sb.append("You take ").append(damageTaken).append(" damage from the ").append(type).append("!\n");
            sb.append(message).append("\n");
            sb.append(attemptsRemaining).append(" attempts remaining.");
        }

        String resultString = sb.toString();
        displayMessage(resultString);
    }

    @Override
    public void displayHint(String hint, int xpCost) {
        StringBuilder sb = new StringBuilder();

        sb.append("HINT (-").append(xpCost).append(" XP)\n");
        sb.append(visualSeperator());
        sb.append(hint).append("\n");
        sb.append(visualSeperator());

        String hintString = sb.toString();
        displayMessage(hintString);
    }

    @Override
    public void displayTimer(int secondsRemaining) {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String YELLOW = "\u001B[33m";
        String CYAN = "\u001B[36m";

        StringBuilder sb = new StringBuilder();
        sb.append("\r");

        if (secondsRemaining <= 10) {
            // Urgent State: Red text with alert icon
            sb.append(RED).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining! ").append(RESET);
        } else if (secondsRemaining <= 20) {
            // Warning State: Yellow text
            sb.append(YELLOW).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining  ").append(RESET);
        } else {
            // Normal State: Cyan text
            sb.append(CYAN).append("Time: ").append(secondsRemaining)
                    .append(" seconds remaining  ").append(RESET);
        }

        System.out.print(sb.toString());
        System.out.flush();
    }

    private String visualSeperator() {
        return "══════════════════════════════════════════════════════════\n";
    }

    // === Basic Output Methods ===

    @Override
    public void displaySeparator() {
        System.out.println(visualSeperator());
    }

    @Override
    public void displayBanner(String text) {
        System.out.println("\n" + visualSeperator());
        System.out.println(centerText(text, 60));
        System.out.println(visualSeperator());
    }

    // === Room Display Methods ===

    @Override
    public void displayRoom(Room room) {
        displaySeparator();
        System.out.println(BOLD + "[" + room.getName() + "]" + RESET);
        displaySeparator();
        System.out.println(room.getDesc());
        System.out.println();

        // Show monster if present
        if (room.hasMonster()) {
            Monster monster = room.getMonster();
            if (!monster.isDefeated()) {
                System.out.println(RED + "⚔ WARNING: " + monster.getName() + " is here!" + RESET);
                System.out.println("   " + monster.getDesc());
            }
        }

        // Show item if present
        if (room.getItems() != null) {
            for (Item item : room.getItems()) {
                System.out.println(YELLOW + "✦ You spot: " + item.getName() + RESET);
            }
        }

        // Show exits
        if (!room.getRoomMap().isEmpty()) {
            displayExits(room.getRoomMap(), null);
        }

        System.out.println();
    }

    @Override
    public void displayRoomBrief(Room room) {
        System.out.println(CYAN + room.getName() + RESET);
        if (room.hasMonster() && !room.getMonster().isDefeated()) {
            System.out.println(RED + "  ⚔ " + room.getMonster().getName() + RESET);
        }
    }

    @Override
    public void displayExits(Map<Directions, Room> exits, Set<Directions> lockedExits) {
        if (exits == null || exits.isEmpty()) {
            System.out.println("No obvious exits.");
            return;
        }

        System.out.print("Exits: ");
        int count = 0;
        for (Directions dir : exits.keySet()) {
            if (count > 0) System.out.print(", ");

            boolean isLocked = (lockedExits != null && lockedExits.contains(dir));
            if (isLocked) {
                System.out.print(RED + dir + " (🔒 locked)" + RESET);
            } else {
                System.out.print(GREEN + dir + RESET);
            }
            count++;
        }
        System.out.println();
    }

    // === Player Display Methods ===

    @Override
    public void displayPlayer(Player player) {
        displaySeparator();
        System.out.println(BOLD + "PLAYER: " + player.getName() + RESET);
        displaySeparator();

        // Health bar
        displayPlayerHealth(player);

        // Level and XP
        System.out.println("Level: " + player.getLevel() + " | XP: " + player.getExperiencePoints());

        // Inventory
        System.out.println("Inventory: " + player.getInventory().size() + "/" + Player.getMaxInventory());

        // Equipped item
        if (player.getEquippedItem() != null) {
            System.out.println(YELLOW + "Equipped: " + player.getEquippedItem().getName() + RESET);
        }

        System.out.println();
    }

    @Override
    public void displayPlayerHealth(Player player) {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();
        double ratio = (double) health / maxHealth;

        String color = ratio > 0.6 ? GREEN : ratio > 0.3 ? YELLOW : RED;
        String healthBar = generateHealthBar(health, maxHealth, 20);

        System.out.println("Health: " + color + healthBar + " " + health + "/" + maxHealth + RESET);
    }

    @Override
    public void displayPlayerBrief(Player player) {
        int health = player.getHealth();
        int maxHealth = player.getMaxHealth();
        double ratio = (double) health / maxHealth;
        String color = ratio > 0.6 ? GREEN : ratio > 0.3 ? YELLOW : RED;

        System.out.println(player.getName() + " | " + color + "❤ " + health + "/" + maxHealth + RESET +
                          " | Lv." + player.getLevel());
    }

    @Override
    public void displayInventory(Player player) {
        List<Item> items = player.getInventory();

        displaySeparator();
        System.out.println(BOLD + "INVENTORY (" + items.size() + "/" + Player.getMaxInventory() + ")" + RESET);
        displaySeparator();

        if (items.isEmpty()) {
            System.out.println("Your pockets are empty.");
        } else {
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                if (item != null) {
                    String equipped = (item.equals(player.getEquippedItem())) ? " " + YELLOW + "[EQUIPPED]" + RESET : "";
                    System.out.println((i + 1) + ". " + item.getName() + " (" + item.getItemtype() + ")" + equipped);
                    System.out.println("   " + item.getDesc());
                }
            }
        }
        System.out.println();
    }

    @Override
    public void displayLevelUp(Player player, int newLevel) {
        displayBanner("✨ LEVEL UP! ✨");
        System.out.println(GREEN + player.getName() + " reached level " + newLevel + "!" + RESET);
        System.out.println("Health fully restored!");
        System.out.println();
    }

    // === Monster Display Methods ===

    @Override
    public void displayMonster(Monster monster) {
        displaySeparator();
        System.out.println(RED + BOLD + "MONSTER: " + monster.getName() + RESET);
        displaySeparator();
        System.out.println(monster.getDesc());
        System.out.println();
        System.out.println("Type: " + monster.getMonsterType());
        System.out.println("Difficulty: " + monster.getDifficulty());
        displayMonsterHealth(monster);
        System.out.println();
    }

    @Override
    public void displayMonsterBrief(Monster monster) {
        String status = monster.isDefeated() ? CYAN + "[DEFEATED]" + RESET : RED + "[ALIVE]" + RESET;
        System.out.println(monster.getName() + " " + status);
    }

    @Override
    public void displayMonsterHealth(Monster monster) {
        int health = monster.getHealth();
        int maxHealth = monster.getHealth() + 50; // Approximate max health
        String healthBar = generateHealthBar(health, maxHealth, 15);

        if (monster.isDefeated()) {
            System.out.println("Health: " + CYAN + "[DEFEATED]" + RESET);
        } else {
            System.out.println("Health: " + RED + healthBar + " " + health + RESET);
        }
    }

    // === Combat System Methods ===

    @Override
    public void displayCombatAction(String attacker, String action, int damage, String target) {
        String color = attacker.equals("You") ? GREEN : RED;
        System.out.println(color + "⚔ " + attacker + " " + action + " " + target +
                          " for " + damage + " damage!" + RESET);
    }

    @Override
    public void displayVictory(Monster monster, Monster.MonsterDrop loot) {
        displayBanner("⚔ VICTORY ⚔");
        System.out.println(GREEN + "You defeated the " + monster.getName() + "!" + RESET);

        if (loot != null) {
            System.out.println("\nRewards:");
            System.out.println("  ✦ " + loot.xp() + " XP");
            if (loot.item() != null) {
                System.out.println("  ✦ " + loot.item().getName());
            }
        }
        System.out.println();
    }

    @Override
    public void displayDefeat(String message) {
        displayBanner("💀 DEFEAT 💀");
        System.out.println(RED + message + RESET);
        System.out.println();
    }

    // === Item System Methods ===

    @Override
    public void displayItemPickup(Item item) {
        System.out.println(GREEN + "✓ Picked up: " + item.getName() + RESET);
    }

    @Override
    public void displayItemUse(Item item, String effect) {
        System.out.println(CYAN + "Used " + item.getName() + ": " + effect + RESET);
    }

    @Override
    public void displayItemDrop(Item item) {
        System.out.println(YELLOW + "Dropped: " + item.getName() + RESET);
    }

    // === Game State Methods ===

    @Override
    public void displayStats(Player player, GameState state) {
        displaySeparator();
        System.out.println(BOLD + "GAME STATISTICS" + RESET);
        displaySeparator();

        System.out.println("Player: " + player.getName());
        System.out.println("Level: " + player.getLevel() + " | XP: " + player.getExperiencePoints());
        System.out.println("Health: " + player.getHealth() + "/" + player.getMaxHealth());
        System.out.println();

        System.out.println("Difficulty: " + state.getDifficulty());
        System.out.println("Lives: " + state.getLivesRemaining() + "/" + state.getMaxLives());
        System.out.println();

        System.out.println("Score: " + state.getTotalScore());
        System.out.println("Rooms Explored: " + state.getRoomsExplored());
        System.out.println("Monsters Defeated: " + state.getMonstersDefeated());
        System.out.println("Play Time: " + formatPlayTime(state.getPlayTime()));
        System.out.println();
    }

    @Override
    public void displayGameOver(GameState state, boolean victory) {
        displaySeparator();
        if (victory) {
            System.out.println(GREEN + BOLD);
            System.out.println("  ██╗   ██╗██╗ ██████╗████████╗ ██████╗ ██████╗ ██╗   ██╗");
            System.out.println("  ██║   ██║██║██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗╚██╗ ██╔╝");
            System.out.println("  ██║   ██║██║██║        ██║   ██║   ██║██████╔╝ ╚████╔╝ ");
            System.out.println("  ╚██╗ ██╔╝██║██║        ██║   ██║   ██║██╔══██╗  ╚██╔╝  ");
            System.out.println("   ╚████╔╝ ██║╚██████╗   ██║   ╚██████╔╝██║  ██║   ██║   ");
            System.out.println("    ╚═══╝  ╚═╝ ╚═════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ");
            System.out.println(RESET);
        } else {
            System.out.println(RED + BOLD);
            System.out.println("   ██████╗  █████╗ ███╗   ███╗███████╗     ██████╗ ██╗   ██╗███████╗██████╗ ");
            System.out.println("  ██╔════╝ ██╔══██╗████╗ ████║██╔════╝    ██╔═══██╗██║   ██║██╔════╝██╔══██╗");
            System.out.println("  ██║  ███╗███████║██╔████╔██║█████╗      ██║   ██║██║   ██║█████╗  ██████╔╝");
            System.out.println("  ██║   ██║██╔══██║██║╚██╔╝██║██╔══╝      ██║   ██║╚██╗ ██╔╝██╔══╝  ██╔══██╗");
            System.out.println("  ╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗    ╚██████╔╝ ╚████╔╝ ███████╗██║  ██║");
            System.out.println("   ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝     ╚═════╝   ╚═══╝  ╚══════╝╚═╝  ╚═╝");
            System.out.println(RESET);
        }
        displaySeparator();

        System.out.println("Final Score: " + state.getTotalScore());
        System.out.println("Rooms Explored: " + state.getRoomsExplored());
        System.out.println("Monsters Defeated: " + state.getMonstersDefeated());
        System.out.println("Play Time: " + formatPlayTime(state.getPlayTime()));

        displaySeparator();
    }

    @Override
    public void displayWelcome(GameDifficulty difficulty) {
        displaySeparator();
        System.out.println(CYAN + BOLD);
        System.out.println("     ██╗ █████╗ ██╗   ██╗ █████╗     ██████╗ ██████╗  ██████╗ ");
        System.out.println("     ██║██╔══██╗██║   ██║██╔══██╗    ██╔══██╗██╔══██╗██╔════╝ ");
        System.out.println("     ██║███████║██║   ██║███████║    ██████╔╝██████╔╝██║  ███╗");
        System.out.println("██   ██║██╔══██║╚██╗ ██╔╝██╔══██║    ██╔══██╗██╔═══╝ ██║   ██║");
        System.out.println("╚█████╔╝██║  ██║ ╚████╔╝ ██║  ██║    ██║  ██║██║     ╚██████╔╝");
        System.out.println(" ╚════╝ ╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝    ╚═╝  ╚═╝╚═╝      ╚═════╝ ");
        System.out.println(RESET);
        displaySeparator();
        System.out.println("Difficulty: " + difficulty + difficulty.getDescription());
        displaySeparator();
        System.out.println();
    }

    // === Notification Methods ===

    @Override
    public void displayError(String error) {
        System.out.println(RED + "✗ ERROR: " + error + RESET);
    }

    @Override
    public void displayWarning(String warning) {
        System.out.println(YELLOW + "⚠ WARNING: " + warning + RESET);
    }

    @Override
    public void displaySuccess(String message) {
        System.out.println(GREEN + "✓ " + message + RESET);
    }

    @Override
    public void displayInfo(String info) {
        System.out.println(CYAN + "ℹ " + info + RESET);
    }

    // === Helper Methods ===

    private String generateHealthBar(int current, int max, int width) {
        int filled = (int) ((double) current / max * width);
        filled = Math.max(0, Math.min(width, filled));

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String formatPlayTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
}
