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

        if (type == Action.UNKNOWN && isDirection(commandWord)) {
            type = Action.MOVE;
        }

        String target = extractTarget(filteredTokens, type);
        Command command = new Command(type, target);
        command.setOriginalInput(input);
        command.setTokens(filteredTokens);
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
}
