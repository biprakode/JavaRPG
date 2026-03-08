import controller.ConsoleView;
import controller.GameController;
import controller.LLMService;
import controller.LLMServiceImpl;
import model.GameDifficulty;
import view.ConsoleViewImpl;

import java.util.Scanner;

public class App {
    private static final ConsoleView view = new ConsoleViewImpl();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        view.displayWelcome();

        LLMService llmService = new LLMServiceImpl(
                "http://localhost:8080/v1/chat/completions",
                "qwen2.5-1.5b-instruct",
                60,
                3
        );

        displayIntro(llmService);

        GameDifficulty difficulty = selectDifficulty();
        String playerName = enterUserName();

        GameController game = new GameController(difficulty, llmService);
        game.startGame(difficulty, playerName, 20);
    }

    private static void displayIntro(LLMService llmService) {
        String intro = llmService.generateText(
                "You are a narrator for a text-based RPG called JavaRPG. Be atmospheric and brief.",
                "Generate a short (2-3 sentences) welcome message for a player about to start a fantasy RPG adventure. No titles, no formatting, just the narration."
        );

        System.out.println();
        if (intro != null) {
            System.out.println(intro);
            System.out.println();
        } else {
            System.out.println("The oracle is silent today... LLM server is offline.");
            System.out.println("Challenges will not be available. Go touch some grass, or start the server.");
            System.out.println();
        }
        System.out.println("Type 'help' at any time for a list of commands.");
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
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
}