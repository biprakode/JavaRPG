package controller;

import model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConsoleView {
    // === Basic Output ===
    void displayMessage(String message);
    void displaySeparator();
    void displayBanner(String text);

    // === Challenge System ===
    void displayChallenge(Challenge challenge);
    void displayResult(Challenge challenge, ChallengeResult result);
    void displayHint(String hint, int xpCost);
    void displayTimer(int secondsRemaining);

    // === Room Display ===
    void displayRoom(Room room);
    void displayRoomBrief(Room room);
    void displayExits(Map<Directions, Room> exits, Set<Directions> lockedExits);

    // === Player Display ===
    void displayPlayer(Player player);
    void displayPlayerHealth(Player player);
    void displayPlayerBrief(Player player);
    void displayInventory(Player player);
    void displayLevelUp(Player player, int newLevel);

    // === Monster Display ===
    void displayMonster(Monster monster);
    void displayMonsterBrief(Monster monster);
    void displayMonsterHealth(Monster monster);

    // === Combat System ===
    void displayCombatAction(String attacker, String action, int damage, String target);
    void displayVictory(Monster monster, Monster.MonsterDrop loot);
    void displayDefeat(String message);

    // === Item System ===
    void displayItemPickup(Item item);
    void displayItemUse(Item item, String effect);
    void displayItemDrop(Item item);

    // === Game State ===
    void displayStats(Player player, GameState state);
    void displayGameOver(GameState state, boolean victory);
    void displayWelcome(GameDifficulty difficulty);

    // === Notifications ===
    void displayError(String error);
    void displayWarning(String warning);
    void displaySuccess(String message);
    void displayInfo(String info);
}
