package controller;

import model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapBuilder {
    private Random random = new Random();
    private Map<Integer, Room> worldMap;
    private String dungeonTheme;
    Room spawnRoom;
    LLMService llmService;

    public MapBuilder(LLMService llmService) {
        this.llmService = llmService;
        this.worldMap = new HashMap<>();
    }

    public Room generateMap(int numRooms) {
        List<Room> rooms = createRoomGraph(numRooms);

        assignRoomTypes(rooms);

        populateRoomContent(rooms);

        // Store world map and spawn room
        for (Room room : rooms) {
            worldMap.put(room.getId(), room);
        }
        spawnRoom = rooms.getFirst();

        return spawnRoom;
    }

    public Map<Integer, Room> getWorldMap() {
        return worldMap;
    }

    private void populateRoomContent(List<Room> rooms) {
        this.dungeonTheme = generateDungeonTheme();

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            RoomContent content = generateRoomContent(room, i, rooms.size(), dungeonTheme);
            room.setName(content.title());
            room.setDesc(content.description());
        }

        placeMonsters(rooms);
    }

    private void placeMonsters(List<Room> rooms) {
        monsterType[] types = monsterType.values();

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            if (room.getRoomtype() == RoomType.SAFE) {
                continue;
            }

            // Pick difficulty based on position in dungeon
            MonsterDifficulty difficulty;
            int attack;
            if (room.getRoomtype() == RoomType.BOSS) {
                difficulty = MonsterDifficulty.HARD;
                attack = 30;
            } else if (i < rooms.size() / 3) {
                difficulty = MonsterDifficulty.EASY;
                attack = 8;
            } else if (i < 2 * rooms.size() / 3) {
                difficulty = MonsterDifficulty.MEDIUM;
                attack = 15;
            } else {
                difficulty = MonsterDifficulty.HARD;
                attack = 22;
            }

            // NORMAL rooms: 65% chance; BOSS rooms: always
            if (room.getRoomtype() == RoomType.NORMAL && random.nextInt(100) >= 65) {
                continue;
            }

            monsterType type = types[random.nextInt(types.length)];
            String[] content = generateMonsterContent(type, difficulty);
            Monster monster = new Monster(content[0], content[1], attack, difficulty, type);
            room.setMonster(monster);
        }
    }

    private String[] generateMonsterContent(monsterType type, MonsterDifficulty difficulty) {
        String prompt = buildMonsterPrompt(type, difficulty, dungeonTheme);
        try {
            String response = llmService.generateChallenge(
                    ChallengeType.CREATIVE,
                    ChallengeDifficulty.EASY,
                    prompt
            );
            String name = extractJsonField(response, "name");
            String description = extractJsonField(response, "description");
            if (name != null && description != null) {
                return new String[]{name, description};
            }
        } catch (Exception e) {
            // fall through to fallback
        }
        return fallbackMonsterContent(type, difficulty);
    }

    private String[] fallbackMonsterContent(monsterType type, MonsterDifficulty difficulty) {
        String[][] easy = {
                {"Cave Rat", "A mangy rodent with yellowed fangs, hissing from the shadows."},
                {"Fungal Crawler", "A mold-encrusted insect that skitters across damp stone."},
                {"Decrepit Skeleton", "Bones held together by faint dark magic, rattling as it moves."}
        };
        String[][] medium = {
                {"Armored Ghoul", "A rotting corpse in rusted chainmail, its dead eyes burning with hunger."},
                {"Stone Golem", "A hulking figure of cracked granite that grinds forward relentlessly."},
                {"Shadow Stalker", "A wisp of living darkness that strikes from blind corners."}
        };
        String[][] hard = {
                {"Abyssal Wyrm", "A serpentine horror wreathed in black flame, radiating dread."},
                {"Iron Revenant", "An undying knight fused to cursed plate armor, sword raised eternally."},
                {"Dread Chimera", "A three-headed monstrosity that fills the chamber with its roar."}
        };

        String[][] pool = switch (difficulty) {
            case EASY -> easy;
            case MEDIUM -> medium;
            case HARD -> hard;
        };

        return pool[random.nextInt(pool.length)];
    }

    private String buildMonsterPrompt(monsterType type, MonsterDifficulty difficulty, String dungeonTheme) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a monster for a text-based RPG dungeon.\n\n");
        prompt.append("Dungeon Theme: ").append(dungeonTheme).append("\n");
        prompt.append("Monster Difficulty: ").append(difficulty).append("\n");
        prompt.append("Monster Type: ").append(type.getType()).append("\n");

        switch (difficulty) {
            case EASY -> prompt.append("A weak early-dungeon creature. Unsettling but not terrifying.\n");
            case MEDIUM -> prompt.append("A dangerous mid-dungeon threat. Noticeably stronger and meaner.\n");
            case HARD -> prompt.append("A fearsome late-dungeon or boss creature. Epic and foreboding.\n");
        }

        prompt.append("""
        \nReturn ONLY a JSON object:
        {
          "name": "Short name (1-3 words)",
          "description": "Brief menacing description (1 sentence, under 100 characters)"
        }
        """);

        return prompt.toString();
    }



    private String generateDungeonTheme() {
        String prompt = """
                Generate a theme for a fantasy dungeon in a text-based RPG.
                Return ONLY a JSON object with this format:
                {
                    "theme": "Ancient Crypt",
                    "atmosphere": "dark, foreboding, undead-infested",
                    "primaryDanger": "undead creatures"
                }
                """;
        try {
            String response = llmService.generateChallenge(ChallengeType.CREATIVE,
                    ChallengeDifficulty.EASY,
                    prompt);
            String theme = extractJsonField(response, "theme");
            return theme != null ? theme : chooseRandomTheme();

        } catch (Exception e) {
            return chooseRandomTheme();
        }
    }

    private RoomContent generateRoomContent(Room room, int roomIndex, int totalRooms, String roomTheme) {
        RoomType roomType = room.getRoomtype();
        String prompt = buildRoomPrompt(roomType, roomIndex, totalRooms, roomTheme);
        try {
            String response = llmService.generateChallenge(
                    ChallengeType.CREATIVE,
                    ChallengeDifficulty.EASY,
                    prompt
            );
            String name = extractJsonField(response, "name");
            String description = extractJsonField(response, "description");
            return new RoomContent(
                    name != null ? name : "Unknown Room",
                    description != null ? description : "An unremarkable chamber."
            );
        } catch (Exception e) {
            return generateFallbackContent(roomType, roomIndex);
        }
    }

    private String buildRoomPrompt(RoomType roomtype, int roomIndex, int totalRooms, String roomTheme) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a room for a text-based RPG dungeon.\n\n");
        prompt.append("Dungeon Theme: ").append(roomTheme).append("\n");
        prompt.append("Room Type: ").append(roomtype).append("\n");

        switch (roomtype) {
            case SAFE -> prompt.append("This is the starting/safe room. Make it feel secure but ominous.\n");
            case BOSS -> prompt.append("This is the final boss room. Make it epic and foreboding.\n");
            case NORMAL -> {
                if (roomIndex < totalRooms / 3) {
                    prompt.append("This is an early area. Less dangerous but still atmospheric.\n");
                } else if (roomIndex < 2 * totalRooms / 3) {
                    prompt.append("This is a mid-game area. Tension building.\n");
                } else {
                    prompt.append("This is a late-game area. Very dangerous.\n");
                }
            }
        }

        prompt.append("""
        \nReturn ONLY a JSON object:
        {
          "name": "Short name (2-4 words)",
          "description": "Atmospheric description (2-3 sentences, vivid and immersive)"
        }

        Requirements:
        - Name should be evocative and match the theme
        - Description should create atmosphere through sensory details
        - Keep descriptions under 200 characters
        - No markdown, just plain text
        """);

        return prompt.toString();
    }

    private RoomContent generateFallbackContent(RoomType type, int roomIndex) {
        return switch (type) {
            case SAFE -> new RoomContent(
                    "Safe Haven",
                    "A quiet chamber with stone walls. You feel momentarily safe here."
            );
            case BOSS -> new RoomContent(
                    "Throne Room",
                    "A vast chamber with towering pillars. Dark energy pulses from deeper within."
            );
            case NORMAL -> new RoomContent(
                    "Chamber " + (roomIndex + 1),
                    "A dark stone room. The air is cold and musty. Shadows dance on the walls."
            );
        };
    }

    private String chooseRandomTheme() {
        String[] themes = {
                "Ancient Ruins",
                "Dark Forest",
                "Abandoned Castle",
                "Underground Caverns",
                "Haunted Catacombs"
        };
        return themes[(int) (Math.random() * themes.length)];
    }

    private String extractJsonField(String json, String fieldName) {
        if (json == null) return null;

        String searchPattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(searchPattern);
        java.util.regex.Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private void assignRoomTypes(List<Room> rooms) {
        if (rooms.isEmpty()) return;

        rooms.getFirst().setRoomtype(RoomType.SAFE);
        if (rooms.size() > 1) {
            rooms.getLast().setRoomtype(RoomType.BOSS);
        }

        for (int i = 1; i < rooms.size() - 1; i++) {
            rooms.get(i).setRoomtype(RoomType.NORMAL);
        }
    }

    private List<Room> createRoomGraph(int numRooms) {
        if (numRooms <= 0) numRooms = 1;

        List<Room> rooms = new ArrayList<>();
        Directions[] dirs = Directions.values();

        // Create all rooms with temporary names (will be overwritten by populateRoomContent)
        for (int i = 0; i < numRooms; i++) {
            rooms.add(new Room(i, "Room " + i, "A dark chamber.", null));
        }

        // Connect rooms in a linear chain first to ensure all rooms are reachable
        for (int i = 0; i < rooms.size() - 1; i++) {
            Directions dir = dirs[random.nextInt(dirs.length)];
            Room current = rooms.get(i);
            Room next = rooms.get(i + 1);

            // Find a direction that isn't already used
            while (current.getExits().containsKey(dir)) {
                dir = dirs[random.nextInt(dirs.length)];
            }

            current.addExit(dir, next);
            next.addExit(dir.getOpposite(), current);
        }

        // Add some extra connections for variety (about 30% extra edges)
        int extraConnections = Math.max(1, numRooms / 3);
        for (int i = 0; i < extraConnections; i++) {
            int a = random.nextInt(numRooms);
            int b = random.nextInt(numRooms);
            if (a == b) continue;

            Room roomA = rooms.get(a);
            Room roomB = rooms.get(b);
            Directions dir = dirs[random.nextInt(dirs.length)];

            // Only connect if both directions are free
            if (!roomA.getExits().containsKey(dir) && !roomB.getExits().containsKey(dir.getOpposite())) {
                roomA.addExit(dir, roomB);
                roomB.addExit(dir.getOpposite(), roomA);
            }
        }

        return rooms;
    }

    private Directions getOpposite(Directions dir) {
        return dir.getOpposite();
    }

    public Room getSpawnRoom() {
        return spawnRoom;
    }
}