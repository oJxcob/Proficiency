package proficiency.modid;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proficiency.modid.commands.ModArgumentTypes;
import proficiency.modid.commands.ProficiencyCommands;
import proficiency.modid.config.ProficiencyConfig;
import proficiency.modid.event.ProficiencyEvents;

public class Proficiency implements DedicatedServerModInitializer {
    public static final String MOD_ID = "proficiency";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        // Register commands and argument types when the server starts
        ModArgumentTypes.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ProficiencyCommands.register(dispatcher);
        });

        // Register all custom components
        ProficiencyConfig.load();
        ProficiencyEvents.register();

        LOGGER.info("Proficiency mod initialized on server!");
    }
}