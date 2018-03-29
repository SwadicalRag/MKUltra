package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.api.IMKUltraPlugin;
import com.chaosbuffalo.mkultra.api.MKUltraPlugin;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.ArrayList;
import java.util.List;

public enum PluginLoader {

    INSTANCE;

    private class PluginData {
        final String modId;
        final String resourcePath;
        final IMKUltraPlugin plugin;

        PluginData(String modId, String resourcePath, IMKUltraPlugin plugin) {
            this.modId = modId;
            this.resourcePath = resourcePath;
            this.plugin = plugin;
        }
    }

    private static List<PluginData> dataStore = new ArrayList<>();

    private String getAnnotationItem(String item, final ASMDataTable.ASMData asmData) {
        if (asmData.getAnnotationInfo().get(item) != null) {
            return asmData.getAnnotationInfo().get(item).toString();
        } else {
            return "";
        }
    }

    public void load(FMLPreInitializationEvent event) {
        for (final ASMDataTable.ASMData asmDataItem : event.getAsmData().getAll(MKUltraPlugin.class.getCanonicalName())) {
            final String modId = getAnnotationItem("modid", asmDataItem);
            final String resourceBase = getAnnotationItem("resourcePath", asmDataItem);
            final String clazz = asmDataItem.getClassName();
            IMKUltraPlugin integration;

            try {
                integration = Class.forName(clazz).asSubclass(IMKUltraPlugin.class).newInstance();
                PluginData pd = new PluginData(modId, resourceBase, integration);
                dataStore.add(pd);
            } catch (final Exception ex) {
                MKUltra.LOG.error("Couldn't load integrations for " + modId, ex);
            }
        }
    }

    public void register() {
        dataStore.forEach(pd -> pd.plugin.register(MKUltra.API));
    }
}
