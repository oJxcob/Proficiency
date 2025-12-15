package proficiency.modid.proficiency;

import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import proficiency.modid.config.ProficiencyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// This is a class that stores all proficiency data for a single player

/** It tracks:
 * Per-type progress: e.g. All pickaxe type tools have a proficiency level
 * Per-item progressL e.g. This specific pickaxe has its own proficiency level
 * */
// This class is attached to PlayerEntity via Cardinal Components API

public class ProficiencyData implements Component {

    // (Constant) An NBT key for storing the UUID on an ItemStack's NBT data
    // When an item is saved, a unique ID is embedded so it can be tracked across sessions (persistent data storage)
    public static final String ITEM_UUID_KEY = "ProficiencyUUID";

    // This is an inner class that represents the state of a single proficiency log/tracking-thing (idk how to word it)
    /** It stores:
     * - points: The accumulated item usage (hits, blocks broken, similar)
     * - level: The derived ProficiencyLevel based on points and thresholds defined in the config
     * */
    public static final class Progress {
        public long points;            // Total accumulated points
        public ProficiencyLevel level; // Derived level

        // Initial values for  progress
        public Progress() {
            this.points = 0;
            this.level = ProficiencyLevel.UNTRAINED;
        }

        // A method to update the level based on `points` and a threshold array
        // Called whenever `points` change
        public void updateLevel() {
            ProficiencyConfig config = ProficiencyConfig.get();
            // For now, use tools thresholds as default (plan to add category detection later)
            long[] thresholds = config.tools.getScaledThresholds(config.useExponentialScaling);
            this.level = ProficiencyLevel.fromPoints(this.points, thresholds);
        }

        // A method to serialise progress to NBT (convert in-memory data to storable, NBT data)
        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("points", this.points);
            nbt.putString("level", this.level.name());
            return nbt;
        }

        // A method to de-serialise progress from NBT
        public static Progress fromNbt(NbtCompound nbt) {
            Progress p = new Progress();
            p.points = nbt.getLong("points");
            p.level = ProficiencyLevel.valueOf(nbt.getString("level"));
            return p;
        }
    }

    // A final variable, typeProgress, that has a key and value that tracks progression for each item type
    // Key: Identifier like "c:pickaxes" (from tags)
    // Value: Progress object with points and level
    private final Map<Identifier, Progress> typeProgress = new HashMap<>();

    // A final variable, itemProgress, that has a key and value that tracks progression for each individual item
    // Key: UUID embedded in the item's NBT
    // Value: Progress object with points and level
    private final Map<UUID, Progress> itemProgress = new HashMap<>();

    /** Learning note: computeIfAbsent is a Map method that:
     * - If the key exists, returns the existing value
     * - If the key doesn't exist, uses the lambda expression (id -> new Progress()) to create a new instance of Progress
     * This ensures that there is always a Progress object for a type (like at first access)
    */
    // Gets or creates an entry of the Progress object for an item type
    public Progress getOrCreateType(Identifier typeId) {
        return typeProgress.computeIfAbsent(typeId, id -> new Progress());
    }

    // Gets for creates an entry of the Progress object for an individual item
    public Progress getOrCreateItem(UUID uuid) {
        return itemProgress.computeIfAbsent(uuid, id -> new Progress());
    }

    // A method that retrieves the Progres of an item type, or null if it doesn't exist
    public Progress getType(Identifier typeId) {
        return typeProgress.get(typeId);
    }

    // A method that retrieves the Progres of an individual item, or null if it doesn't exist
    public Progress getItem(UUID uuid) {
        return itemProgress.get(uuid);
    }


    /** ItemStack:
     * - A data container that represents a "stack" of items in Minecraft (any item, not literally 16 or 64)
     * - It contains:
     *      - Item type
     *      - Count (no. of items in stack, up to max stack size)
     *      - Optional NBT data (like enchantments, durability, etc.)
     * */

    // Ensures an ItemStack has a unique UUID in its NBT
    // Uses getOrCreateNbt() method to return (or create if non-existent) the item's NBT
    // Uses containsUuid() method to check if the UUID key exists, if not a random UUID is generated and stored
    //     - This is used as the key in itemProgress map to track this specific item
    public static UUID ensureItemUuid(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();

        // If the UUID doesn't exist, create and store
        if (!nbt.containsUuid(ITEM_UUID_KEY)) {
            UUID id = UUID.randomUUID();
            nbt.putUuid(ITEM_UUID_KEY, id);
            return id;
        }
        // If the UUID does exist, retrieve and return it
        return nbt.getUuid(ITEM_UUID_KEY);
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        // Clear existing data
        typeProgress.clear();
        itemProgress.clear();

        // Load type progress
        NbtCompound typeNbt = tag.getCompound("types");
        for (String key : typeNbt.getKeys()) {
            Identifier typeId = new Identifier(key);
            Progress progress = Progress.fromNbt(typeNbt.getCompound(key));
            typeProgress.put(typeId, progress);
        }

        // Load item progress
        NbtCompound itemNbt = tag.getCompound("items");
        for (String key : itemNbt.getKeys()) {
            UUID itemId = UUID.fromString(key);
            Progress progress = Progress.fromNbt(itemNbt.getCompound(key));
            itemProgress.put(itemId, progress);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        // Save type progress
        NbtCompound typeNbt = new NbtCompound();
        for (Map.Entry<Identifier, Progress> entry : typeProgress.entrySet()) {
            typeNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        tag.put("types", typeNbt);

        // Save item progress
        NbtCompound itemNbt = new NbtCompound();
        for (Map.Entry<UUID, Progress> entry : itemProgress.entrySet()) {
            itemNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        tag.put("items", itemNbt);
    }

    // For backward compatibility
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        writeToNbt(tag);
        return tag;
    }

    // For backward compatibility
    public static ProficiencyData fromNbt(NbtCompound nbt) {
        ProficiencyData data = new ProficiencyData();
        data.readFromNbt(nbt);
        return data;
    }

}