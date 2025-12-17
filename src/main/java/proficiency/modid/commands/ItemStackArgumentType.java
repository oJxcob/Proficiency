package proficiency.modid.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

//@Environment(EnvType.SERVER)
public class ItemStackArgumentType implements ArgumentType<ItemStack> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stick", "diamond_sword");

    private ItemStackArgumentType() {}  // Private constructor

    public static ItemStackArgumentType itemStack() {
        return new ItemStackArgumentType();
    }

    public static ItemStack getItemStack(CommandContext<?> context, String name) {
        return context.getArgument(name, ItemStack.class);
    }

    @Override
    public ItemStack parse(StringReader reader) throws CommandSyntaxException {
        ItemStringReader.ItemResult result = ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), reader);
        return new ItemStack(result.item().value(), 1);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());

        try {
            ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), stringReader);
            return Suggestions.empty();
        } catch (CommandSyntaxException e) {
            return ItemStringReader.getSuggestions(Registries.ITEM.getReadOnlyWrapper(), builder, false);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}