package proficiency.modid.event;

import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import proficiency.modid.component.ProficiencyComponents;
import proficiency.modid.proficiency.ProficiencyData;
import proficiency.modid.proficiency.ProficiencyLevel;

public class ProficiencyEvents {

    public static void register() {
        // Register event listeners
        PlayerBlockBreakEvents.AFTER.register(ProficiencyEvents::onBlockBreak);
        AttackEntityCallback.EVENT.register(ProficiencyEvents::onAttackEntity);
        UseItemCallback.EVENT.register(ProficiencyEvents::onUseItem);
    }

    /**
     * Called when a player breaks a block.
     * Awards points based on tool type used.
     */
    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable net.minecraft.block.entity.BlockEntity blockEntity) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) return;

        // Determine tool category
        String toolType = getToolType(stack, state);
        if (toolType != null) {
            awardPoints(serverPlayer, stack, toolType, 1);
        }
    }

    /**
     * Called when a player attacks an entity.
     * Awards points for weapon usage.
     */
    private static ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand,
                                               net.minecraft.entity.Entity entity, @Nullable EntityHitResult hitResult) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer) || !(entity instanceof LivingEntity)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) return ActionResult.PASS;

        String weaponType = getWeaponType(stack);
        if (weaponType != null) {
            awardPoints(serverPlayer, stack, weaponType, 2); // More points for combat
        }

        return ActionResult.PASS;
    }

    /**
     * Called when a player uses an item (right-click).
     * Awards points for special tool usage like fishing rods.
     */
    private static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(ItemStack.EMPTY);
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) return TypedActionResult.pass(ItemStack.EMPTY);

        String itemType = getItemUseType(stack);
        if (itemType != null) {
            awardPoints(serverPlayer, stack, itemType, 1);
        }

        return TypedActionResult.pass(stack);
    }

    /**
     * Awards points to both the item category and individual item instance.
     * Handles level-up detection and notification.
     */
    private static void awardPoints(ServerPlayerEntity player, ItemStack stack, String category, long points) {
        if (points <= 0) return;

        ProficiencyData data = ProficiencyComponents.PROFICIENCY.get(player);

        // Get/create UUID for this specific item
        String itemUuid = ProficiencyData.ensureItemUuid(stack).toString();

        // Store previous levels for level-up detection
        ProficiencyLevel oldCategoryLevel = data.getLevel(category);
        ProficiencyLevel oldItemLevel = data.getLevel(itemUuid);

        // Award points to both category and item
        data.addPoints(category, points);
        data.addPoints(itemUuid, points);

        // Check for special unlocks if at MASTERFUL or higher
        ProficiencyLevel newCategoryLevel = data.getLevel(category);
        if (newCategoryLevel == ProficiencyLevel.MASTERFUL) {
            ProficiencyLevel specialLevel = data.calculateSpecialUnlock(category);
            if (specialLevel.ordinal() > ProficiencyLevel.MASTERFUL.ordinal()) {
                // Manually set the special level
                data.getOrCreateType(category).level = specialLevel;
                newCategoryLevel = specialLevel;
            }
        }

        // Sync data to client
        ProficiencyComponents.PROFICIENCY.sync(player);

        // Check for level-ups
        checkLevelUp(player, data, category, oldCategoryLevel, newCategoryLevel, capitalize(category));
        checkLevelUp(player, data, itemUuid, oldItemLevel, data.getLevel(itemUuid), stack.getName().getString());
    }

    /**
     * Checks if a level-up occurred and sends a notification message.
     * Overloaded version that accepts both old and new levels directly.
     */
    private static void checkLevelUp(ServerPlayerEntity player, ProficiencyData data,
                                     String id, ProficiencyLevel oldLevel, ProficiencyLevel newLevel, String displayName) {

        if (newLevel != oldLevel && newLevel.ordinal() > oldLevel.ordinal()) {
            // Determine message color and style based on level tier
            String color = getColorForLevel(newLevel);
            String prefix = newLevel.ordinal() >= ProficiencyLevel.VIRTUOSO.ordinal() ? "§l" : "";

            // Level up message
            player.sendMessage(
                    Text.literal(String.format("%s%s%s proficiency increased to %s%s!",
                            color, prefix, displayName, formatLevelName(newLevel), color
                    )),
                    false
            );

            // Play sound for level-up
            playSoundForLevel(player, newLevel);

            data.setPreviousLevel(id, newLevel);
        }
    }

    /**
     * Returns color code based on proficiency tier.
     */
    private static String getColorForLevel(ProficiencyLevel level) {
        if (level.ordinal() >= ProficiencyLevel.UNRIVALED.ordinal()) return "§6"; // Gold
        if (level.ordinal() >= ProficiencyLevel.LEGENDARY.ordinal()) return "§5"; // Purple
        if (level.ordinal() >= ProficiencyLevel.VIRTUOSO.ordinal()) return "§b"; // Aqua
        if (level.ordinal() >= ProficiencyLevel.PROFICIENT.ordinal()) return "§e"; // Yellow
        return "§a"; // Green
    }

    /**
     * Plays a sound effect based on level tier.
     */
    private static void playSoundForLevel(ServerPlayerEntity player, ProficiencyLevel level) {
        net.minecraft.sound.SoundEvent sound;
        float pitch;

        if (level.ordinal() >= ProficiencyLevel.UNRIVALED.ordinal()) {
            sound = net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            pitch = 1.0f;
        } else if (level.ordinal() >= ProficiencyLevel.LEGENDARY.ordinal()) {
            sound = net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP;
            pitch = 2.0f;
        } else if (level.ordinal() >= ProficiencyLevel.VIRTUOSO.ordinal()) {
            sound = net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP;
            pitch = 1.5f;
        } else if (level.ordinal() >= ProficiencyLevel.PROFICIENT.ordinal()) {
            sound = net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            pitch = 1.5f;
        } else {
            sound = net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            pitch = 1.0f;
        }

        player.playSound(sound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
    }

    /**
     * Determines tool type based on item class and effectiveness.
     * Returns category name like "pickaxe", "axe", etc.
     */
    private static String getToolType(ItemStack stack, BlockState state) {
        // Check effectiveness first for proper tool usage
        if (!stack.isSuitableFor(state)) return null;

        // Identify tool type by class
        if (stack.getItem() instanceof net.minecraft.item.PickaxeItem) return "pickaxe";
        if (stack.getItem() instanceof net.minecraft.item.AxeItem) return "axe";
        if (stack.getItem() instanceof net.minecraft.item.ShovelItem) return "shovel";
        if (stack.getItem() instanceof net.minecraft.item.HoeItem) return "hoe";
        if (stack.getItem() instanceof net.minecraft.item.ShearsItem) return "shears";

        return null;
    }

    /**
     * Identifies weapon type for combat actions.
     */
    private static String getWeaponType(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.item.SwordItem) return "sword";
        if (stack.getItem() instanceof net.minecraft.item.TridentItem) return "trident";
        if (stack.getItem() instanceof net.minecraft.item.BowItem) return "bow";
        if (stack.getItem() instanceof net.minecraft.item.CrossbowItem) return "crossbow";
        // Axes can be weapons too in combat
        if (stack.getItem() instanceof net.minecraft.item.AxeItem) return "axe";

        return null;
    }

    /**
     * Identifies special item usage types (fishing, etc.).
     */
    private static String getItemUseType(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.item.FishingRodItem) return "fishing_rod";
        // Add more special use items here

        return null;
    }

    /**
     * Capitalizes first letter of a string for display.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Formats ProficiencyLevel enum to readable string.
     * Example: BASIC -> "Basic", EXPERIENCED -> "Experienced"
     */
    private static String formatLevelName(ProficiencyLevel level) {
        String name = level.name();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}