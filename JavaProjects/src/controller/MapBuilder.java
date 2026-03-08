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
            System.out.println(content.title());
            System.out.println(content.description());
            room.setName(content.title());
            room.setDesc(content.description());
        }

        placeMonsters(rooms);
        placeItems(rooms);
        placeLocks(rooms);
    }

    private void placeLocks(List<Room> rooms) {
        if (rooms.size() < 3) return; // need at least spawn + middle + boss

        // 1-3 locks total, scaled to dungeon size
        int maxLocks = Math.min(3, (rooms.size() - 2) / 2);
        int numLocks = random.nextInt(maxLocks) + 1;

        // Eligible rooms: not spawn (0), not boss (last)
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < rooms.size() - 1; i++) {
            candidates.add(i);
        }

        for (int n = 0; n < numLocks && !candidates.isEmpty(); n++) {
            // Pick random eligible room
            int pick = random.nextInt(candidates.size());
            int roomIndex = candidates.remove(pick);
            Room room = rooms.get(roomIndex);

            // direction that actually has an exit
            List<Directions> exits = new ArrayList<>(room.getExits().keySet());
            if (exits.isEmpty()) continue;
            Directions dir = exits.get(random.nextInt(exits.size()));

            // Don't double-lock
            if (room.isExitLocked(dir)) continue;

            room.lockExit(dir);

            // Place key in a random room before the locked room
            int keyRoomIndex = random.nextInt(roomIndex); // 0 to roomIndex-1
            String[] keyContent = generateItemContent(ItemType.KEY, 0.5f);
            rooms.get(keyRoomIndex).addItem(new Key(keyContent[0], keyContent[1], room.getId()));
        }
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
            String response = llmService.generateText(CONTENT_SYSTEM_PROMPT, prompt);
            if (response != null) {
                String name = extractJsonField(response, "name");
                String description = extractJsonField(response, "description");
                if (name != null && description != null) {
                    return new String[]{name, description};
                }
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
        String itemType = type.toString().toLowerCase();
        if (itemStrength < 0.25f) {
            return String.format(
                    "A rusty, worn %s found in a %s dungeon. Barely functional. Name and describe it.",
                    itemType, dungeonTheme);
        } else if (itemStrength < 0.5f) {
            return String.format(
                    "A sturdy %s crafted for adventurers in a %s dungeon. Reliable and solid. Name and describe it.",
                    itemType, dungeonTheme);
        } else if (itemStrength < 0.75f) {
            return String.format(
                    "A rare, enchanted %s hidden deep in a %s dungeon. Glows with faint magic. Name and describe it.",
                    itemType, dungeonTheme);
        } else {
            return String.format(
                    "A legendary %s of immense power from a %s dungeon. Artifact-grade, world-shaking. Name and describe it.",
                    itemType, dungeonTheme);
        }
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
            String response = llmService.generateText(CONTENT_SYSTEM_PROMPT, prompt);
            if (response != null) {
                String name = extractJsonField(response, "name");
                String description = extractJsonField(response, "description");
                if (name != null && description != null) {
                    return new String[]{name, description};
                }
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
        return switch (difficulty) {
            case EASY -> String.format(
                    "A weak %s creature lurking in a %s dungeon. Small and unsettling. Name and describe it.",
                    type.getType(), dungeonTheme);
            case MEDIUM -> String.format(
                    "A dangerous %s warrior guarding a %s dungeon. Strong and menacing. Name and describe it.",
                    type.getType(), dungeonTheme);
            case HARD -> String.format(
                    "A terrifying %s boss of a %s dungeon. Massive and dreadful. Name and describe it.",
                    type.getType(), dungeonTheme);
        };
    }



    private String generateDungeonTheme() {
        String prompt = "Generate a fantasy dungeon theme. Return ONLY: {\"theme\":\"2-3 words\",\"atmosphere\":\"3 adjectives\",\"primaryDanger\":\"2 words\"}";
        try {
            String response = llmService.generateText(CONTENT_SYSTEM_PROMPT, prompt);
            if (response == null) return chooseRandomTheme();
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
            String response = llmService.generateText(CONTENT_SYSTEM_PROMPT, prompt);
            if (response != null) {
                String name = extractJsonField(response, "name");
                String description = extractJsonField(response, "description");
                if (name != null && description != null) {
                    return new RoomContent(name, description);
                }
            }
        } catch (Exception e) {
            // fall through to fallback
        }
        return generateFallbackContent(roomType, roomIndex);
    }

    private static final String CONTENT_SYSTEM_PROMPT =
            "You name locations, creatures, and items for a fantasy RPG. " +
            "Always respond with ONLY a JSON object like: {\"name\":\"Frozen Antechamber\",\"description\":\"Ice coats the walls and your breath hangs in the still air.\"} " +
            "No extra text. Keep description under 100 characters. Keep name 2-4 words.";

    private String buildRoomPrompt(RoomType roomtype, int roomIndex, int totalRooms, String roomTheme) {
        // Give the model a concrete location type to anchor on
        String[] earlyLocations = {"narrow corridor", "dusty alcove", "crumbling passage", "dim antechamber", "moss-covered tunnel"};
        String[] midLocations = {"vaulted hall", "ritual chamber", "sunken gallery", "collapsed bridge", "echoing vault"};
        String[] lateLocations = {"scorched sanctum", "bone-littered lair", "cursed throne hall", "abyssal pit", "sealed tomb"};

        return switch (roomtype) {
            case SAFE -> String.format(
                    "A %s dungeon entrance room. Sheltered but with a sense of dread creeping in. Name and describe it.",
                    roomTheme);
            case BOSS -> String.format(
                    "The final boss chamber of a %s dungeon. Massive, terrifying, powerful. Name and describe it.",
                    roomTheme);
            case NORMAL -> {
                String location;
                if (roomIndex < totalRooms / 3) {
                    location = earlyLocations[random.nextInt(earlyLocations.length)];
                } else if (roomIndex < 2 * totalRooms / 3) {
                    location = midLocations[random.nextInt(midLocations.length)];
                } else {
                    location = lateLocations[random.nextInt(lateLocations.length)];
                }
                yield String.format(
                        "A %s in a %s dungeon. Name and describe this room.",
                        location, roomTheme);
            }
        };
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