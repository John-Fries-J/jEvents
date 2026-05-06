package org.johnfries.jevents.fabric;

import net.fabricmc.api.ModInitializer;
import org.johnfries.jevents.JEventsMod;

public class JEventsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        JEventsMod.init();
    }
}
