package org.johnfries.jevents.forge;

import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.johnfries.jevents.JEventsMod;

@Mod(JEventsMod.MOD_ID)
public class JEventsForge {
    public JEventsForge() {
        EventBuses.registerModEventBus(JEventsMod.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        JEventsMod.init();
    }
}
