package proficiency.modid.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;
import proficiency.modid.Proficiency;
import proficiency.modid.proficiency.ProficiencyData;

public final class ProficiencyComponents implements EntityComponentInitializer {
    public static final ComponentKey<ProficiencyData> PROFICIENCY =
            ComponentRegistry.getOrCreate(
                    new Identifier(Proficiency.MOD_ID, "proficiency"),
                    ProficiencyData.class
            );

    public static ComponentKey<ProficiencyData> getProficiency() {
        return PROFICIENCY;
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                PROFICIENCY,
                player -> new ProficiencyData(),
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}