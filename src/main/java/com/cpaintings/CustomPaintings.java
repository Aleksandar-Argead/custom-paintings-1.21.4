package com.cpaintings;

import com.cpaintings.commands.Painting;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPaintings implements ModInitializer {

    public static final String MOD_ID = "custom-paintings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Painting.register();
        LOGGER.info("Custom Paintings mod initialized");
    }
}
