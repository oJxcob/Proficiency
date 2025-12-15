package proficiency.modid.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)  // Add this annotation
public class ProficiencyConfigScreen {
    public static Screen create(Screen parent) {
        ProficiencyConfig config = ProficiencyConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.proficiency.config"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // General category
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.proficiency.general"));
        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("option.proficiency.exponential_scaling"), config.useExponentialScaling)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("tooltip.proficiency.exponential_scaling"))
                .setSaveConsumer(value -> config.useExponentialScaling = value)
                .build()
        );

        // Tools category
        addCategoryConfig(builder, entryBuilder, "tools", config.tools);

        // Weapons category
        addCategoryConfig(builder, entryBuilder, "weapons", config.weapons);

        // Armor category
        addCategoryConfig(builder, entryBuilder, "armor", config.armor);

        return builder.build();
    }

    private static void addCategoryConfig(ConfigBuilder builder, ConfigEntryBuilder entryBuilder,
                                          String categoryKey, ProficiencyConfig.CategoryThresholds thresholds) {
        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("category.proficiency." + categoryKey));

        // Base thresholds
        String[] levelNames = {
                "Rudimentary", "Novice", "Learning", "Basic", "Capable",
                "Familiar", "Accustomed", "Proficient", "Experienced", "Skilled",
                "Adept", "Expert", "Veteran", "Elite", "Masterful"
        };

        // Add threshold fields
        for (int i = 0; i < thresholds.baseThresholds.length; i++) {
            final int index = i;
            category.addEntry(entryBuilder
                    .startLongField(Text.translatable("option.proficiency." + categoryKey + ".threshold." + i, levelNames[i]),
                            thresholds.baseThresholds[i])
                    .setDefaultValue(thresholds.baseThresholds[i])
                    .setTooltip(Text.translatable("tooltip.proficiency.threshold", levelNames[i]))
                    .setSaveConsumer(value -> thresholds.baseThresholds[index] = value)
                    .build()
            );
        }

        // Virtuoso settings
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency." + categoryKey + ".virtuoso_items"),
                        thresholds.virtuosoItemsRequired)
                .setDefaultValue(2)
                .setTooltip(Text.translatable("tooltip.proficiency.virtuoso_items"))
                .setSaveConsumer(value -> thresholds.virtuosoItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency." + categoryKey + ".virtuoso_threshold"),
                        thresholds.virtuosoThreshold)
                .setDefaultValue(20000)
                .setTooltip(Text.translatable("tooltip.proficiency.virtuoso_threshold"))
                .setSaveConsumer(value -> thresholds.virtuosoThreshold = value)
                .build()
        );

        // Legendary settings
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency." + categoryKey + ".legendary_items"),
                        thresholds.legendaryItemsRequired)
                .setDefaultValue(3)
                .setTooltip(Text.translatable("tooltip.proficiency.legendary_items"))
                .setSaveConsumer(value -> thresholds.legendaryItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency." + categoryKey + ".legendary_threshold"),
                        thresholds.legendaryThreshold)
                .setDefaultValue(25000)
                .setTooltip(Text.translatable("tooltip.proficiency.legendary_threshold"))
                .setSaveConsumer(value -> thresholds.legendaryThreshold = value)
                .build()
        );

        // Unrivaled settings
        category.addEntry(entryBuilder
                .startIntField(Text.translatable("option.proficiency." + categoryKey + ".unrivaled_items"),
                        thresholds.unrivaledItemsRequired)
                .setDefaultValue(5)
                .setTooltip(Text.translatable("tooltip.proficiency.unrivaled_items"))
                .setSaveConsumer(value -> thresholds.unrivaledItemsRequired = value)
                .build()
        );

        category.addEntry(entryBuilder
                .startLongField(Text.translatable("option.proficiency." + categoryKey + ".unrivaled_threshold"),
                        thresholds.unrivaledThreshold)
                .setDefaultValue(30000)
                .setTooltip(Text.translatable("tooltip.proficiency.unrivaled_threshold"))
                .setSaveConsumer(value -> thresholds.unrivaledThreshold = value)
                .build()
        );
    }
}