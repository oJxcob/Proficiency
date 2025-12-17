package proficiency.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ProficiencyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Client-side initialization code will go here
        Proficiency.LOGGER.info("Initializing Proficiency client");
    }
}