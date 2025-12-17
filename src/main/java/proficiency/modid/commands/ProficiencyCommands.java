package proficiency.modid.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import proficiency.modid.component.ProficiencyComponents;
import proficiency.modid.proficiency.ProficiencyData;
import proficiency.modid.proficiency.ProficiencyLevel;
import static proficiency.modid.commands.ItemStackArgumentType.getItemStack;


import java.util.Collection;
import java.util.UUID;

public class ProficiencyCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("proficiency")
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.literal("category")
                                        .then(CommandManager.argument("category", IdentifierArgumentType.identifier())
                                                .executes(ProficiencyCommands::getCategoryLevel)
                                        )
                                )
                                .then(CommandManager.literal("item")
                                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                                                .executes(ProficiencyCommands::getItemLevel)
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("set")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.literal("category")
                                        .then(CommandManager.argument("category", IdentifierArgumentType.identifier())
                                                .then(CommandManager.argument("level", LongArgumentType.longArg(0))
                                                        .executes(ProficiencyCommands::setCategoryLevel)
                                                )
                                        )
                                )
                                .then(CommandManager.literal("item")
                                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())
                                                .then(CommandManager.argument("level", LongArgumentType.longArg(0))
                                                        .executes(ProficiencyCommands::setItemLevel)
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("points")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.literal("get")
                                        .then(CommandManager.literal("category")
                                                .then(CommandManager.argument("category", IdentifierArgumentType.identifier())
                                                        .executes(ProficiencyCommands::getCategoryPoints)
                                                )
                                        )
                                        .then(CommandManager.literal("item")
                                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())                                                        .executes(ProficiencyCommands::getItemPoints)
                                                )
                                        )
                                )
                                .then(CommandManager.literal("set")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .then(CommandManager.literal("category")
                                                .then(CommandManager.argument("category", IdentifierArgumentType.identifier())
                                                        .then(CommandManager.argument("points", LongArgumentType.longArg(0))
                                                                .executes(ProficiencyCommands::setCategoryPoints)
                                                        )
                                                )
                                        )
                                        .then(CommandManager.literal("item")
                                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack())                                                        .then(CommandManager.argument("points", LongArgumentType.longArg(0))
                                                                .executes(ProficiencyCommands::setItemPoints)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int getCategoryLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        String category = IdentifierArgumentType.getIdentifier(context, "category").toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyLevel level = data.getLevel(category);
            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.get.category",
                                    target.getDisplayName(),
                                    category,
                                    level.name().toLowerCase()
                            ),
                    false
            );
        }
        return targets.size();
    }

    private static int getItemLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        ItemStack stack = getItemStack(context, "item");
        String itemUuid = ProficiencyData.ensureItemUuid(stack).toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyLevel level = data.getLevel(itemUuid);
            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.get.item",
                                    target.getDisplayName(),
                                    stack.getName(),
                                    level.name().toLowerCase()
                            ),
                    false
            );
        }
        return targets.size();
    }

    private static int setCategoryLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        String category = IdentifierArgumentType.getIdentifier(context, "category").toString();
        long levelValue = LongArgumentType.getLong(context, "level");
        ProficiencyLevel level = ProficiencyLevel.values()[(int) Math.min(levelValue, ProficiencyLevel.values().length - 1)];

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getOrCreateType(category);
            progress.level = level;
            ProficiencyComponents.getProficiency().sync(target);

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.set.category",
                                    target.getDisplayName(),
                                    category,
                                    level.name().toLowerCase()
                            ),
                    true
            );
        }
        return targets.size();
    }

    private static int setItemLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        ItemStack stack = getItemStack(context, "item");
        long levelValue = LongArgumentType.getLong(context, "level");
        ProficiencyLevel level = ProficiencyLevel.values()[(int) Math.min(levelValue, ProficiencyLevel.values().length - 1)];
        String itemUuid = ProficiencyData.ensureItemUuid(stack).toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getOrCreateItem(UUID.fromString(itemUuid));
            progress.level = level;
            ProficiencyComponents.getProficiency().sync(target);

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.set.item",
                                    target.getDisplayName(),
                                    stack.getName(),
                                    level.name().toLowerCase()
                            ),
                    true
            );
        }
        return targets.size();
    }

    private static int getCategoryPoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        String category = IdentifierArgumentType.getIdentifier(context, "category").toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getType(category);
            long points = progress != null ? progress.points : 0;

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.points.get.category",
                                    target.getDisplayName(),
                                    category,
                                    points
                            ),
                    false
            );
        }
        return targets.size();
    }

    private static int getItemPoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        ItemStack stack = getItemStack(context, "item");
        String itemUuid = ProficiencyData.ensureItemUuid(stack).toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getItem(UUID.fromString(itemUuid));
            long points = progress != null ? progress.points : 0;

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.points.get.item",
                                    target.getDisplayName(),
                                    stack.getName(),
                                    points
                            ),
                    false
            );
        }
        return targets.size();
    }

    private static int setCategoryPoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        String category = IdentifierArgumentType.getIdentifier(context, "category").toString();
        long points = LongArgumentType.getLong(context, "points");

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getOrCreateType(category);
            progress.points = points;
            progress.updateLevel(data.getPublicThresholdsForCategory(category));
            ProficiencyComponents.getProficiency().sync(target);

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.points.set.category",
                                    target.getDisplayName(),
                                    category,
                                    points
                            ),
                    true
            );
        }
        return targets.size();
    }

    private static int setItemPoints(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
        ItemStack stack = getItemStack(context, "item");
        long points = LongArgumentType.getLong(context, "points");
        String itemUuid = ProficiencyData.ensureItemUuid(stack).toString();

        for (ServerPlayerEntity target : targets) {
            ProficiencyData data = ProficiencyComponents.getProficiency().get(target);
            ProficiencyData.Progress progress = data.getOrCreateItem(UUID.fromString(itemUuid));
            progress.points = points;
            progress.updateLevel(data.getPublicThresholdsForCategory("tools"));            ProficiencyComponents.getProficiency().sync(target);

            context.getSource().sendFeedback(() ->
                            Text.translatable("commands.proficiency.points.set.item",
                                    target.getDisplayName(),
                                    stack.getName(),
                                    points
                            ),
                    true
            );
        }
        return targets.size();
    }

    private static int showNextLevelPoints(CommandContext<ServerCommandSource> context) {
        // Implementation for showing points needed for next level
        // This would be similar to the other methods but would calculate points to next level
        return 1;
    }
}