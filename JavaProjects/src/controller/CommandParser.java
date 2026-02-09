package controller;

import controller.error.InvalidCommandException;
import model.Directions;

import java.util.*;

public class CommandParser {
    private static Map<String , Action> CommandMap = new HashMap<>();
    private static Map<String , Directions> DirectionMap = new HashMap<>();
    private static Set<String> Noise = new HashSet<>();

    static {
        initializeCommandMap();
        initializeDirectionMap();
        initializeNoiseWords();
    }
    // INITIALIZERS
    private static void initializeCommandMap() {
        // Movement
        CommandMap.put("go", Action.MOVE);
        CommandMap.put("move", Action.MOVE);
        CommandMap.put("walk", Action.MOVE);
        CommandMap.put("run", Action.MOVE);
        CommandMap.put("travel", Action.MOVE);

        // Combat
        CommandMap.put("attack", Action.ATTACK);
        CommandMap.put("fight", Action.ATTACK);
        CommandMap.put("hit", Action.ATTACK);
        CommandMap.put("strike", Action.ATTACK);

        // Interaction
        CommandMap.put("examine", Action.EXAMINE);
        CommandMap.put("look", Action.EXAMINE);
        CommandMap.put("inspect", Action.EXAMINE);
        CommandMap.put("check", Action.EXAMINE);
        CommandMap.put("search", Action.EXAMINE);

        CommandMap.put("take", Action.TAKE);
        CommandMap.put("get", Action.TAKE);
        CommandMap.put("grab", Action.TAKE);
        CommandMap.put("pickup", Action.TAKE);

        CommandMap.put("use", Action.USE);
        CommandMap.put("activate", Action.USE);
        CommandMap.put("consume", Action.USE);

        CommandMap.put("talk", Action.TALK);
        CommandMap.put("speak", Action.TALK);
        CommandMap.put("chat", Action.TALK);
        CommandMap.put("converse", Action.TALK);

        // Inventory
        CommandMap.put("inventory", Action.INVENTORY);
        CommandMap.put("inv", Action.INVENTORY);
        CommandMap.put("items", Action.INVENTORY);
        CommandMap.put("bag", Action.INVENTORY);

        // System
        CommandMap.put("help", Action.HELP);
        CommandMap.put("commands", Action.HELP);
        CommandMap.put("?", Action.HELP);

        CommandMap.put("quit", Action.QUIT);
        CommandMap.put("exit", Action.QUIT);
        CommandMap.put("leave", Action.QUIT);

        CommandMap.put("save", Action.SAVE);
        CommandMap.put("load", Action.LOAD);

        CommandMap.put("stats", Action.STATS);
        CommandMap.put("status", Action.STATS);
        CommandMap.put("health", Action.STATS);
    }
    private static void initializeDirectionMap() {
        // Cardinal directions
        DirectionMap.put("north", Directions.NORTH);
        DirectionMap.put("n", Directions.NORTH);
        DirectionMap.put("up", Directions.NORTH);

        DirectionMap.put("south", Directions.SOUTH);
        DirectionMap.put("s", Directions.SOUTH);
        DirectionMap.put("down", Directions.SOUTH);

        DirectionMap.put("east", Directions.EAST);
        DirectionMap.put("e", Directions.EAST);
        DirectionMap.put("right", Directions.EAST);

        DirectionMap.put("west", Directions.WEST);
        DirectionMap.put("w", Directions.WEST);
        DirectionMap.put("left", Directions.WEST);
    }
    private static void initializeNoiseWords() {
        // Words to filter out during parsing
        Noise.addAll(Arrays.asList(
                "the", "a", "an", "at", "to", "in", "on", "with", "and", "or"
        ));
    }

    public Command parse(String input) {
        if(input == null || input.trim().isEmpty()) {
            throw new InvalidCommandException("Input String is empty");
        }

        String normalized = input.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9\\s-]", "");
        String[] tokens = normalized.split("\\s+");
        List<String> filteredtokens = filterNoiseWords(tokens);

        if(filteredtokens.isEmpty()) {
            return new Command(Action.UNKNOWN , input);
        }

        String commandword = filteredtokens.getFirst();
        Action type = identifyCommandType(commandword);

        if (type == Action.UNKNOWN && isDirection(commandword)) {
            type = Action.MOVE;
        }

        String suggestion = null;
        if (type == Action.UNKNOWN) {
            suggestion = suggestCommand(commandword);
        }

        String target = extractTarget(filteredtokens, type);
        Command command = new Command(type, target);

        command.setOriginalInput(input);
        command.setTokens(filteredtokens);
        command.setSuggestion(suggestion);
        return command;
    }

    private Action identifyCommandType(String word) {
        return CommandMap.getOrDefault(word , Action.UNKNOWN);
    }

    private boolean isDirection(String word) {
        return DirectionMap.containsKey(word);
    }

    private String extractTarget(List<String> tokens, Action type) {
        if (tokens.size() <= 1) {
            return "";
        }
        if(type == Action.MOVE) {
            String potentialDir = tokens.get(1);
            if(isDirection(potentialDir)) {
                return potentialDir;
            }
        }
        return String.join(" " , tokens.subList(1 , tokens.size())); // drop initial command word
    }

    public Directions parseDirection(String input) {
        String normalized = input.trim().toLowerCase();
        return DirectionMap.get(normalized);
    }

    public List<String> getAvailableDirections() {
        return new ArrayList<>(DirectionMap.keySet());
    }

    private List<String> filterNoiseWords(String[] tokens) {
        List<String> filtered = new ArrayList<>();
        for(String token : tokens) {
            if(!Noise.contains(token) && !token.isEmpty()) {
                filtered.add(token);
            }
        }
        return filtered;
    }

    public Map<String, List<String>> getAvailableCommands() {
        Map<String, List<String>> commandsByCategory = new LinkedHashMap<>();

        commandsByCategory.put("Movement", Arrays.asList("go", "move", "north", "south", "east", "west"));
        commandsByCategory.put("Combat", Arrays.asList("attack", "fight"));
        commandsByCategory.put("Interaction", Arrays.asList("examine", "take", "use", "talk"));
        commandsByCategory.put("Inventory", Arrays.asList("inventory", "stats"));
        commandsByCategory.put("System", Arrays.asList("help", "save", "load", "quit"));
        return commandsByCategory;
    }

    //Claude additions
    public String suggestCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String normalized = input.trim().toLowerCase();
        String closestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String validCommand : CommandMap.keySet()) {
            int distance = levenshteinDistance(normalized, validCommand);

            if (distance < minDistance && distance <= 2) {
                minDistance = distance;
                closestMatch = validCommand;
            }
        }
        return closestMatch;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public boolean requiresTarget(Action type) {
        return switch (type) {
            case MOVE, ATTACK, EXAMINE, TAKE, USE, TALK -> true;
            case INVENTORY, HELP, QUIT, SAVE, LOAD, STATS -> false;
            default -> false;
        };
    }

    public String getCommandHelp(Action type) {
        return switch (type) {
            case MOVE -> "Move in a direction. Usage: 'go north', 'move east', or just 'north'";
            case ATTACK -> "Attack an enemy. Usage: 'attack goblin'";
            case EXAMINE -> "Examine something closely. Usage: 'examine door', 'look around'";
            case TAKE -> "Pick up an item. Usage: 'take sword', 'get potion'";
            case USE -> "Use an item from inventory. Usage: 'use potion', 'use key'";
            case TALK -> "Talk to someone. Usage: 'talk guard', 'speak merchant'";
            case INVENTORY -> "View your inventory. Usage: 'inventory' or 'inv'";
            case STATS -> "View your stats. Usage: 'stats' or 'status'";
            case HELP -> "Show available commands. Usage: 'help'";
            case SAVE -> "Save your game. Usage: 'save'";
            case LOAD -> "Load a saved game. Usage: 'load'";
            case QUIT -> "Exit the game. Usage: 'quit' or 'exit'";
            default -> "Unknown command";
        };
    }

    public String getHelpText() {

    }
}
