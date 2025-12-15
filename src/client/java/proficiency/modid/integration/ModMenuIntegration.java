package proficiency.modid.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import proficiency.modid.config.ProficiencyConfigScreen;

@Environment(EnvType.CLIENT)  // Add this annotation
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ProficiencyConfigScreen::create;
    }
}