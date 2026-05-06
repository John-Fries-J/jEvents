package org.johnfries.jevents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.johnfries.jevents.core.JEventsService;

public final class JEventsMod {
    public static final String MOD_ID = "jevents";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static boolean initialized;

    private JEventsMod() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        JEventsService.register();
        LOGGER.info("Initialized {}", MOD_ID);
    }
}
