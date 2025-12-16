package proficiency.modid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import proficiency.modid.Proficiency;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

// Config class for Proficiency, handles loading and saving via json, provides default values

public class ProficiencyConfig {
    private static ProficiencyConfig INSTANCE;
    private static final File CONFIG_FILE = new File("Config/proficiency.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public double expoBase = 100.0;
    public double expoMultiplier = 1.5;
    public double expoPower = 1.1;

    // General Settings
    // Increase Proficiency levels exponentially:
    public boolean useExponentialScaling = false;

    // Item Category Thresholds
    // Categories:
    // - Tools: pickaxes, shovels, hoes, shears, flint and steel, fishing rods, axes (when breaking blocks)
    public CategoryThresholds tools = new CategoryThresholds(
            new long[]{100, 200, 300, 500, 750, 1000, 1500, 2500, 3500, 4500, 5500, 7500, 10000, 12500, 15000},
            2, 20000, // Virtuoso: 2 items at or over PROFICIENT and 20000 total progression points
            3, 25000, // Legendary: 3 items at or over PROFICIENT and 25000 total progression points
            5, 30000 // Unrivaled: 5 items at or over PROFICIENT and 30000 total progression points
    );

    // Weapons: swords, axes, tridents, bows, crossbows, modded weapons (when used for combat)
    public CategoryThresholds weapons = new CategoryThresholds(
            new long[]{100, 200, 300, 500, 750, 1000, 1500, 2500, 3500, 4500, 5500, 7500, 10000, 12500, 15000},
            2, 20000,
            3, 25000,
            5, 30000
    );

    // Armour: helmets, chestplates, leggings, boots, elytra, turtle shell
    public CategoryThresholds armour = new CategoryThresholds(
            new long[]{100, 200, 300, 500, 750, 1000, 1500, 2500, 3500, 4500, 5500, 7500, 10000, 12500, 15000},
            2, 20000,
            3, 25000,
            5, 30000
    );

    // Class to hold category thresholds
    public static class CategoryThresholds {
        public long[] baseThresholds;

        public int virtuosoItemsRequired;
        public long virtuosoThreshold;

        public int legendaryItemsRequired;
        public long legendaryThreshold;

        public int unrivaledItemsRequired;
        public long unrivaledThreshold;

        public CategoryThresholds(long[] baseThresholds,
                                  int virtuosoItems, long virtuosoPoints,
                                  int legendaryItems, long legendaryPoints,
                                  int unrivaledItems, long unrivaledPoints) {
            this.baseThresholds = baseThresholds;
            this.virtuosoItemsRequired = virtuosoItems;
            this.virtuosoThreshold = virtuosoPoints;
            this.legendaryItemsRequired = legendaryItems;
            this.legendaryThreshold = legendaryPoints;
            this.unrivaledItemsRequired = unrivaledItems;
            this.unrivaledThreshold = unrivaledPoints;
        }
        // Exponential scaling to thresholds
        // Formula: threshold[i] = base * (multiplier ^ i)
        public long[] getScaledThresholds(boolean exponential) {
            if (!exponential) {
                return baseThresholds;
            }

            // Calculate exponential scaling with a multiplicator of 1.375 per level
            long[] scaled = new long[baseThresholds.length];
            double multiplier = 1.35;
            long base = baseThresholds[0];

            for (int i=0; i<scaled.length; i++) {
                scaled[i] = (long) (base * Math.pow(multiplier, i));
            }
            return scaled;
        }
    }

    // Method to get values for config threshold calculations
    public long getThresholdForLevel(String category, int level) {
        if (useExponentialScaling) {
            return (long) (expoBase * Math.pow(expoMultiplier, Math.pow(level, expoPower)));
        } else {
            // Get category thresholds;
            CategoryThresholds thresholds;
            switch (category.toLowerCase()) {
                case "tools" -> thresholds = this.tools;
                case "weapons" -> thresholds = this.weapons;
                case "armour" -> thresholds = this.armour;
                default -> throw new IllegalArgumentException("Unknown category " + category);
            }
            int safeLevel = Math.min(level, thresholds.baseThresholds.length - 1);
            return thresholds.baseThresholds[Math.min(0, safeLevel)];
        }
    }

    // Load config from file, or create default if it doesn't exist. Called during mod initialisation
    public static ProficiencyConfig load() {
        if (INSTANCE == null) {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    INSTANCE = GSON.fromJson(reader, ProficiencyConfig.class);
                    Proficiency.LOGGER.info("Loaded Proficiency config from file");
                } catch (IOException e) {
                    Proficiency.LOGGER.error("Failed to load config, using defaults", e);
                    INSTANCE = new ProficiencyConfig();
                    INSTANCE.save();
                }
            } else {
                Proficiency.LOGGER.error("No config file found, creating default");
                INSTANCE = new ProficiencyConfig();
                INSTANCE.save();
            }
        }
        return INSTANCE;
    }

    // Save current config to file
    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
                Proficiency.LOGGER.info("Saved Proficiency config");
            }
        } catch (IOException e) {
            Proficiency.LOGGER.error("Failed to save config", e);
        }
    }

    // Get singleton instance
    public static ProficiencyConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }

    public double getExpoBase() { return expoBase; }
    public double getExpoMultiplier() { return expoMultiplier; }
    public double getExpoPower() { return expoPower; }
}