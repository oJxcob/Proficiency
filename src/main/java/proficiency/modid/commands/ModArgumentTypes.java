package proficiency.modid.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;

public class ModArgumentTypes {
    public static void register() {
        // This will only be called on the server side
        ArgumentTypeRegistry.registerArgumentType(
                new Identifier("proficiency", "item_stack"),
                ItemStackArgumentType.class,
                ConstantArgumentSerializer.of(ItemStackArgumentType::itemStack)
        );
    }
}