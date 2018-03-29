package com.chaosbuffalo.mkultra;

import com.chaosbuffalo.mkultra.api.GameConstants;
import com.chaosbuffalo.mkultra.api.IMKUltraAPI;
import com.chaosbuffalo.mkultra.core.PluginLoader;
import com.chaosbuffalo.mkultra.command.MKCommand;
import com.chaosbuffalo.mkultra.api.BaseAbility;
import com.chaosbuffalo.mkultra.api.BaseClass;
import com.chaosbuffalo.mkultra.core.ClassData;
import com.chaosbuffalo.mkultra.network.PacketHandler;
import com.chaosbuffalo.mkultra.party.PartyCommand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;


@Mod(modid = MKUltra.MODID, name= MKUltra.MODNAME, version = MKUltra.VERSION,
        dependencies="required-after:basemetals;" +
                "required-after:poweradvantage;" +
                "after:versionchecker;")
public class MKUltra {
    public static final String MODID = GameConstants.MODID;
    public static final String VERSION = "@VERSION@";
    public static final String MODNAME = "MKUltra";

    public static final IMKUltraAPI API = new UltraAPI();

    @Mod.Instance
    public static MKUltra INSTANCE = new MKUltra();

    @SidedProxy(clientSide = "com.chaosbuffalo.mkultra.ClientProxy",
                serverSide = "com.chaosbuffalo.mkultra.ServerProxy")
    public static CommonProxy proxy;

    public static PacketHandler packetHandler;
    public static Logger LOG;

    @EventHandler
    public void preInit(FMLPreInitializationEvent e) {

        LOG = e.getModLog();

        PluginLoader.INSTANCE.load(e);

        updateCheck(MKUltra.MODID);
        updateCheck("versionchecker");

        // Cyano mods
        updateCheck("basemetals");
        updateCheck("poweradvantage");
        updateCheck("steamadvantage");
        updateCheck("lootablebodies"); // Cyano Lootable Bodies
        updateCheck("orespawn");

        updateCheck("lycanitesmobs");


        proxy.preInit(e);
    }

    public static void updateCheck(String modId) {
        // modId must be the value declared by the mod in the @Mod annotation!
        String url = String.format("https://bitbucket.org/thecubereich/releases-1.12/raw/master/%s/version.json", modId);
        FMLInterModComms.sendRuntimeMessage(modId, "versionchecker", "addVersionCheck", url);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        PluginLoader.INSTANCE.register();
        proxy.init(e);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        proxy.postInit(e);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new PartyCommand());
        event.registerServerCommand(new MKCommand());
    }


    private static class UltraAPI implements IMKUltraAPI {

        @Override
        public void registerAbility(BaseAbility ability) {
            ClassData.REGISTRY_ABILITIES.register(ability.setRegistryName(ability.getAbilityId()));
        }

        @Override
        public void registerClass(BaseClass baseClass) {
            ClassData.REGISTRY_CLASSES.register(baseClass.setRegistryName(baseClass.getClassId()));
        }
    }
}
