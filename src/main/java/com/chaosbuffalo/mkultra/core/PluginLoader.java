package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.api.IMKUltraAPI;
import com.chaosbuffalo.mkultra.api.IMKUltraPlugin;
import com.chaosbuffalo.mkultra.api.MKUltraPlugin;
import com.chaosbuffalo.mkultra.log.Log;
import com.google.common.collect.Lists;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum PluginLoader {

    INSTANCE;

    public final List<Pair<IMKUltraPlugin, MKUltraPlugin>> PLUGINS = Lists.newArrayList();

    @SuppressWarnings("unchecked")
    @Nonnull
    public List<Pair<IMKUltraPlugin, MKUltraPlugin>> gatherPlugins(ASMDataTable dataTable) {
        List<Pair<IMKUltraPlugin, MKUltraPlugin>> discoveredAnnotations = Lists.newArrayList();
        Set<ASMDataTable.ASMData> discoveredPlugins = dataTable.getAll(MKUltraPlugin.class.getName());

        for (ASMDataTable.ASMData data : discoveredPlugins) {
            try {
                Class<?> asmClass = Class.forName(data.getClassName());
                Class<? extends IMKUltraPlugin> pluginClass = asmClass.asSubclass(IMKUltraPlugin.class);

                IMKUltraPlugin instance = pluginClass.newInstance();

                Log.info("Discovered plugin at %s", data.getClassName());
                discoveredAnnotations.add(Pair.of(instance, pluginClass.getAnnotation(MKUltraPlugin.class)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        discoveredAnnotations.sort((o1, o2) -> {
            if (o1.getLeft().getClass() == MKUltraCorePlugin.class)
                return -1;

            return o1.getClass().getCanonicalName().compareToIgnoreCase(o2.getClass().getCanonicalName());
        });
        return discoveredAnnotations;
    }

    @Nonnull
    public List<Field> gatherInjections(ASMDataTable dataTable) {
        List<Field> injectees = Lists.newArrayList();
        Set<ASMDataTable.ASMData> discoveredInjectees = dataTable.getAll(MKUltraPlugin.Inject.class.getName());

        for (ASMDataTable.ASMData data : discoveredInjectees) {
            try {
                Class<?> asmClass = Class.forName(data.getClassName());
                Field toInject = asmClass.getDeclaredField(data.getObjectName());
                if (toInject.getType() != IMKUltraAPI.class) {
                    continue;
                }

                injectees.add(toInject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return injectees;
    }

    public void load(FMLPreInitializationEvent event) {
        PLUGINS.addAll(gatherPlugins(event.getAsmData()));
        injectAPIInstances(gatherInjections(event.getAsmData()));
    }

    public void injectAPIInstances(List<Field> injectees) {
        int errors = 0;

        for (Field injectee : injectees) {
            if (!Modifier.isStatic(injectee.getModifiers()))
                continue;

            try {
                EnumHelper.setFailsafeFieldValue(injectee, null, MKUltra.API);
            } catch (Exception e) {
                errors++;
            }
        }
    }

    public void register() {
    }
}
