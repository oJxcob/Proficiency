package proficiency.modid.proficiency;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import proficiency.modid.config.ProficiencyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores all proficiency data for a single player.
 * Tracks both per-type progress (e.g., all pickaxes) and per-item progress (specific pickaxe instance).
 * Attached to PlayerEntity via Cardinal Components API.
 */
public class ProficiencyData implements Component {

    public static final String ITEM_UUID_KEY = "ProficiencyUUID";

    /**
     * Represents the state of a single proficiency tracker.
     * Stores points (accumulated usage) and the derived level.
     */
    public static final class Progress {
        public long points;
        public ProficiencyLevel level;

        public Progress() {
            this.points = 0;
            this.level = ProficiencyLevel.UNTRAINED;
        }

        /**
         * Updates level based on points and config thresholds.
         * @param thresholds Array of point thresholds for each level
         */
        public void updateLevel(long[] thresholds) {
            this.level = ProficiencyLevel.fromPoints(this.points, thresholds);
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("points", this.points);
            nbt.putString("level", this.level.name());
            return nbt;
        }

        public static Progress fromNbt(NbtCompound nbt) {
            Progress p = new Progress();
            p.points = nbt.getLong("points");
            try {
                p.level = ProficiencyLevel.valueOf(nbt.getString("level"));
            } catch (IllegalArgumentException e) {
                p.level = ProficiencyLevel.UNTRAINED;
            }
            return p;
        }
    }

    // Per-type progress: tracks categories like "pickaxe", "sword", etc.
    private final Map<String, Progress> typeProgress = new HashMap<>();

    // Per-item progress: tracks individual item instances by UUID
    private final Map<UUID, Progress> itemProgress = new HashMap<>();

    // Tracks previous levels for level-up detection
    private final Map<String, ProficiencyLevel> previousLevels = new HashMap<>();

    /**
     * Gets or creates Progress for an item type category.
     * @param category Category identifier like "pickaxe", "sword", "tools"
     */
    public Progress getOrCreateType(String category) {
        return typeProgress.computeIfAbsent(category, k -> new Progress());
    }

    /**
     * Gets or creates Progress for a specific item instance.
     */
    public Progress getOrCreateItem(UUID uuid) {
        return itemProgress.computeIfAbsent(uuid, k -> new Progress());
    }

    public Progress getType(String category) {
        return typeProgress.get(category);
    }

    public Progress getItem(UUID uuid) {
        return itemProgress.get(uuid);
    }

    /**
     * Adds points to a category or item and updates its level.
     * @param id Category name (e.g., "pickaxe") or UUID string
     * @param amount Points to add
     */
    public void addPoints(String id, long amount) {
        if (amount <= 0) return;

        // Determine if this is a UUID or category
        Progress progress;
        long[] thresholds;

        try {
            // Try parsing as UUID first (for individual items)
            UUID uuid = UUID.fromString(id);
            progress = getOrCreateItem(uuid);
            thresholds = getThresholdsForCategory("tools"); // Default for now
        } catch (IllegalArgumentException e) {
            // Not a UUID, treat as category
            progress = getOrCreateType(id);
            thresholds = getThresholdsForCategory(id);
        }

        progress.points += amount;
        progress.updateLevel(thresholds);
    }

    /**
     * Gets the current proficiency level for a category or item.
     */
    public ProficiencyLevel getLevel(String id) {
        Progress progress;

        try {
            UUID uuid = UUID.fromString(id);
            progress = getItem(uuid);
        } catch (IllegalArgumentException e) {
            progress = getType(id);
        }

        return progress != null ? progress.level : ProficiencyLevel.UNTRAINED;
    }

    /**
     * Gets the previous level (before last update) for level-up detection.
     */
    public ProficiencyLevel getPreviousLevel(String id) {
        return previousLevels.getOrDefault(id, ProficiencyLevel.UNTRAINED);
    }

    /**
     * Updates the stored previous level after a level-up notification.
     */
    public void setPreviousLevel(String id, ProficiencyLevel level) {
        previousLevels.put(id, level);
    }

    /**
     * Gets config thresholds for a given category.
     * Maps category names to config threshold arrays.
     */
    private long[] getThresholdsForCategory(String category) {
        ProficiencyConfig config = ProficiencyConfig.get();

        return switch (category.toLowerCase()) {
            case "pickaxe", "axe", "shovel", "hoe", "shears", "fishing_rod" ->
                    config.tools.getScaledThresholds(config.useExponentialScaling);
            case "sword", "trident", "bow", "crossbow" ->
                    config.weapons.getScaledThresholds(config.useExponentialScaling);
            case "helmet", "chestplate", "leggings", "boots", "elytra" ->
                    config.armour.getScaledThresholds(config.useExponentialScaling);
            default ->
                    config.tools.getScaledThresholds(config.useExponentialScaling);
        };
    }

    /**
     * Gets the main category for an item type (tools, weapons, or armour).
     */
    private String getMainCategory(String itemType) {
        return switch (itemType.toLowerCase()) {
            case "pickaxe", "axe", "shovel", "hoe", "shears", "fishing_rod" -> "tools";
            case "sword", "trident", "bow", "crossbow" -> "weapons";
            case "helmet", "chestplate", "leggings", "boots", "elytra" -> "armour";
            default -> "tools";
        };
    }

    /**
     * Checks if special unlock levels (VIRTUOSO, LEGENDARY, UNRIVALED) should be awarded.
     * Called after adding points to update special progression.
     */
    public ProficiencyLevel calculateSpecialUnlock(String itemType) {
        String mainCategory = getMainCategory(itemType);
        ProficiencyConfig config = ProficiencyConfig.get();
        ProficiencyConfig.CategoryThresholds thresholds = switch (mainCategory) {
            case "weapons" -> config.weapons;
            case "armour" -> config.armour;
            default -> config.tools;
        };

        Progress categoryProgress = getType(itemType);
        if (categoryProgress == null) return ProficiencyLevel.MASTERFUL;

        // Count items at PROFICIENT or higher in this category
        int proficientCount = 0;
        for (Progress itemProg : itemProgress.values()) {
            if (itemProg.level.atLeast(ProficiencyLevel.PROFICIENT)) {
                proficientCount++;
            }
        }

        long totalPoints = categoryProgress.points;

        // Check unlock conditions in descending order
        if (proficientCount >= thresholds.unrivaledItemsRequired &&
                totalPoints >= thresholds.unrivaledThreshold) {
            return ProficiencyLevel.UNRIVALED;
        }

        if (proficientCount >= thresholds.legendaryItemsRequired &&
                totalPoints >= thresholds.legendaryThreshold) {
            return ProficiencyLevel.LEGENDARY;
        }

        if (proficientCount >= thresholds.virtuosoItemsRequired &&
                totalPoints >= thresholds.virtuosoThreshold) {
            return ProficiencyLevel.VIRTUOSO;
        }

        return ProficiencyLevel.MASTERFUL;
    }

    /**
     * Ensures an ItemStack has a unique UUID for tracking.
     * Creates and stores a UUID if one doesn't exist.
     */
    public static UUID ensureItemUuid(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();

        if (!nbt.containsUuid(ITEM_UUID_KEY)) {
            UUID id = UUID.randomUUID();
            nbt.putUuid(ITEM_UUID_KEY, id);
            return id;
        }
        return nbt.getUuid(ITEM_UUID_KEY);
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        typeProgress.clear();
        itemProgress.clear();
        previousLevels.clear();

        // Load type progress
        NbtCompound typeNbt = tag.getCompound("types");
        for (String key : typeNbt.getKeys()) {
            Progress progress = Progress.fromNbt(typeNbt.getCompound(key));
            typeProgress.put(key, progress);
        }

        // Load item progress
        NbtCompound itemNbt = tag.getCompound("items");
        for (String key : itemNbt.getKeys()) {
            try {
                UUID itemId = UUID.fromString(key);
                Progress progress = Progress.fromNbt(itemNbt.getCompound(key));
                itemProgress.put(itemId, progress);
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }

        // Load previous levels
        NbtCompound prevNbt = tag.getCompound("previousLevels");
        for (String key : prevNbt.getKeys()) {
            try {
                ProficiencyLevel level = ProficiencyLevel.valueOf(prevNbt.getString(key));
                previousLevels.put(key, level);
            } catch (IllegalArgumentException e) {
                // Skip invalid levels
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        // Save type progress
        NbtCompound typeNbt = new NbtCompound();
        for (Map.Entry<String, Progress> entry : typeProgress.entrySet()) {
            typeNbt.put(entry.getKey(), entry.getValue().toNbt());
        }
        tag.put("types", typeNbt);

        // Save item progress
        NbtCompound itemNbt = new NbtCompound();
        for (Map.Entry<UUID, Progress> entry : itemProgress.entrySet()) {
            itemNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        tag.put("items", itemNbt);

        // Save previous levels
        NbtCompound prevNbt = new NbtCompound();
        for (Map.Entry<String, ProficiencyLevel> entry : previousLevels.entrySet()) {
            prevNbt.putString(entry.getKey(), entry.getValue().name());
        }
        tag.put("previousLevels", prevNbt);
    }
}