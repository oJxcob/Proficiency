package proficiency.modid.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import proficiency.modid.component.ProficiencyComponents;
import proficiency.modid.proficiency.ProficiencyData;

/**
 * Mixin to track armor proficiency when players take damage.
 * Awards points to armor pieces that successfully protect the player.
 */
@Mixin(PlayerEntity.class)
public class PlayerDamageMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Server-side only and must be a player
        if (player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        // Only award points for damage that armor can protect against
        // Check if damage bypasses armor via the damage type
        if (amount <= 0 || source.getType().effects().equals(net.minecraft.entity.damage.DamageEffects.DROWNING)) {
            return;
        }

        // Award points to each armor piece
        // More dangerous damage = more points
        int points = calculateArmorPoints(amount);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack armor = player.getEquippedStack(slot);
            if (armor.isEmpty()) continue;

            String armorType = getArmorType(slot);
            if (armorType == null) continue;

            ProficiencyData data = ProficiencyComponents.getProficiency().get(serverPlayer);
            String itemUuid = ProficiencyData.ensureItemUuid(armor).toString();

            // Award points
            data.addPoints(armorType, points);
            data.addPoints(itemUuid, points);
        }

        ProficiencyComponents.getProficiency().sync(serverPlayer);
    }

    /**
     * Calculates points based on damage amount.
     * Higher damage = more points (you're using armor effectively).
     */
    @Unique
    private int calculateArmorPoints(float damage) {
        if (damage < 2.0f) return 1;
        if (damage < 5.0f) return 2;
        if (damage < 10.0f) return 3;
        return 4; // Significant damage
    }

    /**
     * Maps equipment slot to armor category name.
     */
    @Unique
    private String getArmorType(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> null;
        };
    }
}