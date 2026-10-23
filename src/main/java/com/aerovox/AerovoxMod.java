package com.aerovox;

import com.aerovox.glue.GlueEvents;
import com.aerovox.network.AerovoxNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AerovoxMod.MODID)
public class AerovoxMod {
    public static final String MODID = "aerovox";

    public AerovoxMod(IEventBus modBus, ModContainer container) {
        AerovoxRegistries.register(modBus);
        modBus.addListener(AerovoxNetwork::register);
        container.registerConfig(ModConfig.Type.COMMON, AerovoxConfig.SPEC);
        NeoForge.EVENT_BUS.register(new GlueEvents());
    }
}
