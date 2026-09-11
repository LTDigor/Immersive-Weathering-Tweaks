package org.admany.iwt.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.admany.iwt.core.IwtAreaTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("iwt")
public final class IwtForge {
    public static final String MOD_ID = "iwt";
    private static final Logger LOGGER = LoggerFactory.getLogger(IwtForge.class);

    public IwtForge() {
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
    }

    private void serverStarted(ServerStartedEvent event) {
        try {
            IwtAreaTemplates.precomputeIw205();
        } catch (RuntimeException error) {
            LOGGER.error("Failed to precompute IW area templates", error);
        }
    }
}
