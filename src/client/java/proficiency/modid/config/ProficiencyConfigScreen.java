package proficiency.modid.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class ProficiencyConfigScreen {

    // Helper methods for text formatting
    private static Text styledHeading(String key) {
        return Text.translatable(key)
                .setStyle(Style.EMPTY
                .withBold(true)
                );
    }

    private static Text styledItalics(String key) {
        return Text.translatable(key)
                .setStyle(Style.EMPTY
                .withItalic(true)
                );
    }

    // Config screen builder class
    public static Screen create(Screen parent) {
        ProficiencyConfig config = ProficiencyConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.proficiency.config"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General category
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.proficiency.general"));

        // Exponential scaling toggle
        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("option.proficiency.exponential_scaling"), config.useExponentialScaling)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("tooltip.proficiency.exponential_scaling"),
                        Text.translatable("tooltip.proficiency.requires_restart"))
                .setSaveConsumer(value -> config.useExponentialScaling = value)
                .build()
        );

        // Exponential equation description
        general.addEntry(entryBuilder
                .startTextDescription(Text.translatable("text.proficiency.expo_equation"))
                .build()
        );

        // General exponential description
        general.addEntry(entryBuilder
                .startTextDescription(Text.translatable("text.proficiency.points_description"))
                .build()
        );

        // Base value
        general.addEntry(entryBuilder
                .startDoubleField(Text.translatable("option.proficiency.expo_base"), config.expoBase)
                .setDefaultValue(100.0)
                .setMin(1.0)
                .setTooltip(Text.translatable("tooltip.proficiency.expo_base"),
                        Text.translatable("tooltip.proficiency.requires_restart"))
                .setSaveConsumer(value -> config.expoBase = value)
                .build()
        );

        // Multiplier value
        general.addEntry(entryBuilder
                .startDoubleField(Text.translatable("option.proficiency.expo_multiplier"), config.expoMultiplier)
                .setDefaultValue(1.5)
                .setMin(1.0)
                .setTooltip(Text.translatable("tooltip.proficiency.expo_multiplier"),
                        Text.translatable("tooltip.proficiency.requires_restart"))
                .setSaveConsumer(value -> config.expoMultiplier = value)
                .build()
        );

        // Power value
        general.addEntry(entryBuilder
                .startDoubleField(Text.translatable("option.proficiency.expo_power"), config.expoPower)
                .setDefaultValue(1.1)
                .setMin(0.1)
                .setTooltip(Text.translatable("tooltip.proficiency.expo_power"),
                        Text.translatable("tooltip.proficiency.requires_restart"))
                .setSaveConsumer(value -> config.expoPower = value)
                .build()
        );

        // Tools category
        addCategoryConfig(builder, entryBuilder, "tools", config.tools);

        // Weapons category
        addCategoryConfig(builder, entryBuilder, "weapons", config.weapons);

        // Armour category
        addCategoryConfig(builder, entryBuilder, "armour", config.armour);

        return builder.build();
    }

    /**
     * Adds a category configuration page with threshold fields and special unlock settings
     * @param builder The ConfigBuilder instance
     * @param entryBuilder The ConfigEntryBuilder for creating entries
     * @param categoryKey The translation key for the category (e.g., "tools", "weapons", "armour")
     * @param thresholds The CategoryThresholds object containing all threshold values
     */
    private static void addCategoryConfig(ConfigBuilder builder, ConfigEntryBuilder entryBuilder,
                                          String categoryKey, ProficiencyConfig.CategoryThresholds thresholds) {
        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("category.proficiency." + categoryKey));

        // Level names for display
        String[] levelNames = {
                "Rudimentary", "Novice", "Learning", "Basic", "Capable",
                "Familiar", "Accustomed", "Proficient", "Experienced", "Skilled",
                "Adept", "Expert", "Veteran", "Elite", "Masterful"
        };

        // Tools/Weapons/Armour Proficiency threshold heading
        category.addEntry(entryBuilder
                .startTextDescription(styledHeading("text.proficiency.item_category_heading"))
                .build()
        );

        // Tools/Weapons/Armour Proficiency threshold description
        category.addEntry(entryBuilder
                .startTextDescription(Text.translatable("text.proficiency.item_category_description"))
                .build()
        );

        // Base thresholds / points per level
        for (int i = 0; i < thresholds.baseThresholds.length; i++) {
            final int index = i;
            category.addEntry(entryBuilder
                    .startLongField(Text.translatable("option.proficiency.threshold_label", levelNames[i]),
                            thresholds.baseThresholds[i])
                    .setDefaultValue(thresholds.baseThresholds[i])
                    .setMin(0L)
                    .setTooltip(Text.translatable("tooltip.proficiency.threshold", levelNames[i]))
                    .setSaveConsumer(value -> thresholds.baseThresholds[index] = value)
                    .build()
            );
        }

        // Special unlocks section

        // Section heading
        category.addEntry(entryBuilder
                .startTextDescription(styledHeading("text.proficiency.general_heading"))
                .build()
        );
        // Special unlocks description
        category.addEntry(entryBuilder
                .startTextDescription(Text.translatable("text.proficiency.special_unlocks_description"))
                .build()
        );
        // Special unlocks example
        category.addEntry(entryBuilder
                .startTextDescription(Text.translatable("text.proficiency.special_unlocks_example"))
                .build()
        );


        // Virtuoso unlock requirements
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency.virtuoso_items_label", "Virtuoso Items"),
                        thresholds.virtuosoItemsRequired)
                .setDefaultValue(2)
                .setMin(0)
                .setTooltip(Text.translatable("tooltip.proficiency.virtuoso_items"))
                .setSaveConsumer(value -> thresholds.virtuosoItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency.virtuoso_threshold_label", "Virtuoso Points"),
                        thresholds.virtuosoThreshold)
                .setDefaultValue(20000L)
                .setMin(0L)
                .setTooltip(Text.translatable("tooltip.proficiency.virtuoso_threshold"))
                .setSaveConsumer(value -> thresholds.virtuosoThreshold = value)
                .build()
        );

        // Legendary unlock requirements
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency.legendary_items_label", "Legendary Items"),
                        thresholds.legendaryItemsRequired)
                .setDefaultValue(3)
                .setMin(0)
                .setTooltip(Text.translatable("tooltip.proficiency.legendary_items"))
                .setSaveConsumer(value -> thresholds.legendaryItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency.legendary_threshold_label", "Legendary Points"),
                        thresholds.legendaryThreshold)
                .setDefaultValue(25000L)
                .setMin(0L)
                .setTooltip(Text.translatable("tooltip.proficiency.legendary_threshold"))
                .setSaveConsumer(value -> thresholds.legendaryThreshold = value)
                .build()
        );

        // Unrivaled unlock requirements
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency.unrivaled_items_label", "Unrivaled Items"),
                        thresholds.unrivaledItemsRequired)
                .setDefaultValue(5)
                .setMin(0)
                .setTooltip(Text.translatable("tooltip.proficiency.unrivaled_items"))
                .setSaveConsumer(value -> thresholds.unrivaledItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency.unrivaled_threshold_label", "Unrivaled Points"),
                        thresholds.unrivaledThreshold)
                .setDefaultValue(30000L)
                .setMin(0L)
                .setTooltip(Text.translatable("tooltip.proficiency.unrivaled_threshold"))
                .setSaveConsumer(value -> thresholds.unrivaledThreshold = value)
                .build()
        );
    }
}