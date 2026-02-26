import controller.ConsoleView;
import controller.GameController;
import model.GameDifficulty;
import view.ConsoleViewImpl;

import java.util.Scanner;

public class App {
    private static final ConsoleView view = new ConsoleViewImpl();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        view.displayWelcome();
        GameDifficulty difficulty = selectDifficulty();
        String playerName = enterUserName();
        displayIntro();
        GameController game = new GameController();
        game.startGame(difficulty, playerName, 20);
    }

    private static GameDifficulty selectDifficulty() {
        System.out.println("Choose difficulty: ");
        System.out.println();

        GameDifficulty[] difficulties = GameDifficulty.values();
        for (int i = 0; i < difficulties.length; i++) {
            GameDifficulty diff = difficulties[i];
            System.out.printf("%d. %s - %s (%d lives)%n",
                    i + 1,
                    diff.name(),
                    diff.getDescription(),
                    diff.getMaxLives());
        }

        System.out.println();
        System.out.print("Enter your choice (1-" + difficulties.length + "): ");

        while (true) {
            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= difficulties.length) {
                    GameDifficulty selected = difficulties[choice - 1];
                    System.out.println();
                    System.out.println("You selected: " + selected.name());
                    System.out.println();
                    return selected;
                } else {
                    System.out.print("Invalid choice. Enter 1-" + difficulties.length + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Please enter a number: ");
            }
        }
    }

    private static String enterUserName() {
        System.out.print("Enter your name, adventurer: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "Hero";
        }
        System.out.println();
        System.out.println("Welcome, " + name + "!");
        System.out.println();
        return name;
    }

    private static void displayIntro() {
        System.out.println("You awaken in a dark forest with no memory of how you arrived.");
        System.out.println("The trees loom overhead, their branches creaking in the wind.");
        System.out.println("In the distance, you hear strange sounds...");
        System.out.println();
        System.out.println("Your adventure begins now.");
        System.out.println();
        System.out.println("Type 'help' at any time for a list of commands.");
        System.out.println();

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            // Ignore
        }

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
    }
}