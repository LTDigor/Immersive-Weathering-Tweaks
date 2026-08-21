package org.admany.iwt.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.admany.iwt.core.IwtAreaTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IwtFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IwtFabric.class);
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                IwtAreaTemplates.precomputeIw205();
            } catch (RuntimeException error) {
                LOGGER.error("Failed to precompute IW area templates", error);
            }
        });
    }
}
