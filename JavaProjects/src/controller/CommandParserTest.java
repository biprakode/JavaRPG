package controller;

import model.Directions;

public class CommandParserTest {
    public static void main(String[] args) {
        CommandParser parser = new CommandParser();

        // Test cases
        testParse(parser, "go north");
        testParse(parser, "north");
        testParse(parser, "attack the evil goblin");
        testParse(parser, "use health potion");
        testParse(parser, "inventory");
        testParse(parser, "attak"); // Typo

        // Test direction parsing
        Directions dir = parser.parseDirection("n");
        System.out.println("Parsed direction: " + dir);
    }

    private static void testParse(CommandParser parser, String input) {
        Command cmd = parser.parse(input);
        System.out.printf("Input: '%s' → %s%n", input, cmd);
    }
}
