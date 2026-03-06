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
        placeItems(rooms);
    }

    private void placeItems(List<Room> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);

            if (room.getRoomtype() == RoomType.SAFE) {
                // Starting gear: weak potion + weak weapon
                float itemStrength = 0.1f + random.nextFloat() * 0.15f; // 0.10–0.25
                String[] potionContent = generateItemContent(ItemType.POTION, itemStrength);
                room.addItem(new Potion(potionContent[0], potionContent[1], healAmountFromStrength(itemStrength)));

                String[] weaponContent = generateItemContent(ItemType.WEAPON, itemStrength);
                WeaponLvl lvl = WeaponLvl.fromStrength(itemStrength);
                room.addItem(new Weapon(weaponContent[0], weaponContent[1], lvl.damage, (int) lvl.cooldown));

            } else if (room.getRoomtype() == RoomType.BOSS) {
                // Reward: strong treasure + strong weapon
                float itemStrength = 0.75f + random.nextFloat() * 0.25f; // 0.75–1.0
                String[] treasureContent = generateItemContent(ItemType.TREASURE, itemStrength);
                room.addItem(new Treasure(treasureContent[0], treasureContent[1], xpRewardFromStrength(itemStrength)));

                String[] potionContent = generateItemContent(ItemType.POTION, itemStrength);
                room.addItem(new Potion(potionContent[0], potionContent[1], healAmountFromStrength(itemStrength)));

                String[] weaponContent = generateItemContent(ItemType.WEAPON, itemStrength);
                WeaponLvl lvl = WeaponLvl.fromStrength(itemStrength);
                room.addItem(new Weapon(weaponContent[0], weaponContent[1], lvl.damage, (int) lvl.cooldown));

            } else if (room.getRoomtype() == RoomType.NORMAL) {
                int numItems = random.nextInt(3); // 0, 1, or 2 items
                float progress = (float) i / rooms.size(); // 0.0 at start, ~1.0 at end
                for (int j = 0; j < numItems; j++) {
                    // Pick type: potions common, weapons uncommon, treasure rare
                    float itemStrength = Math.max(0.05f, progress * 0.6f + random.nextFloat() * 0.4f);
                    itemStrength = Math.min(itemStrength, 0.999f);
                    ItemType type = pickRandomItemType();
                    String[] content = generateItemContent(type, itemStrength);
                    switch (type) {
                        case WEAPON -> {
                            WeaponLvl lvl = WeaponLvl.fromStrength(itemStrength);
                            room.addItem(new Weapon(content[0], content[1], lvl.damage, (int) lvl.cooldown));
                        }
                        case POTION -> room.addItem(new Potion(content[0], content[1], healAmountFromStrength(itemStrength)));
                        case TREASURE -> room.addItem(new Treasure(content[0], content[1], xpRewardFromStrength(itemStrength)));
                        case KEY -> {} // Keys placed in placeLocks() (Section 3)
                    }
                }
            }
        }
    }

    private ItemType pickRandomItemType() {
        int roll = random.nextInt(100);
        if (roll < 45) return ItemType.POTION; // 45% potion
        if (roll < 75) return ItemType.WEAPON; // 30% weapon
        return ItemType.TREASURE; // 25% treasure
    }

    private int healAmountFromStrength(float strength) {
        // 15 HP at strength 0 → 65 HP at strength 1
        return 15 + (int) (strength * 50);
    }

    private int xpRewardFromStrength(float strength) {
        // 10 XP at strength 0 → 100 XP at strength 1
        return 10 + (int) (strength * 90);
    }

    private String[] generateItemContent(ItemType type , float itemStrength) {
        String prompt = buildItemPrompt(type , itemStrength , dungeonTheme);
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
        return fallbackItemContent(type, itemStrength);
    }

    private String[] fallbackItemContent(ItemType type, float itemStrength) {
        // Tier: 0 = weak, 1 = mid, 2 = strong, 3 = legendary
        int tier = (int) (itemStrength * 4);
        tier = Math.max(0, Math.min(tier, 3));

        String[][] weapons = {
                {"Rusty Dagger", "A pitted blade that's seen better days."},
                {"Iron Shortsword", "A sturdy blade with a leather-wrapped grip."},
                {"Enchanted Sabre", "A gleaming sword that hums with faint magic."},
                {"Abyssal Cleaver", "A legendary blade forged in darkness itself."}
        };
        String[][] potions = {
                {"Weak Tonic", "A cloudy vial of bitter restorative liquid."},
                {"Healing Draught", "A warm potion that mends minor wounds."},
                {"Elixir of Vigor", "A bright potion radiating restorative energy."},
                {"Phoenix Tears", "A legendary elixir that can restore even the gravest wounds."}
        };
        String[][] treasures = {
                {"Tarnished Coin", "A worn coin bearing an ancient king's face."},
                {"Silver Goblet", "An ornate cup studded with small gems."},
                {"Golden Idol", "A heavy idol depicting a forgotten deity."},
                {"Dragon's Hoard Gem", "A massive jewel pulsing with inner fire."}
        };

        return switch (type) {
            case WEAPON -> weapons[tier];
            case POTION -> potions[tier];
            case TREASURE -> treasures[tier];
            case KEY -> new String[]{"Old Key", "A heavy iron key with strange markings."};
        };
    }

    private String buildItemPrompt(ItemType type, float itemStrength, String dungeonTheme) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("Generate a %s for a text-based RPG dungeon.\n\n" , type.toString()));
        prompt.append("Dungeon Theme: ").append(dungeonTheme).append("\n");
        prompt.append("Item Strength (0-1): ").append(itemStrength).append("\n");

        String itemType = type.toString().toLowerCase(); // e.g., "potion" or "weapon"

        if (itemStrength < 0.25f) {
            prompt.append(String.format("A common, basic %s. Functional, but shows signs of wear or simple craftsmanship.\n", itemType));
        } else if (itemStrength < 0.5f) {
            prompt.append(String.format("A sturdy, reliable %s. Clearly better than standard fare; a dependable tool for an adventurer.\n", itemType));
        } else if (itemStrength < 0.75f) {
            prompt.append(String.format("A rare and finely-honed %s. It hums with quality (or magic) and stands out as a prize.\n", itemType));
        } else {
            prompt.append(String.format("A legendary, artifact-grade %s. It is an epic masterpiece of world-shaking importance.\n", itemType));
        }

        prompt.append("""
        \nReturn ONLY a JSON object:
        {
          "name": "Short name (1-3 words)",
          "description": "Brief description of %s (1 sentence, under 100 characters)"
        }
        """.formatted(itemType));

        return prompt.toString();
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