package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.api.*;
import com.chaosbuffalo.mkultra.core.classes.*;
import com.chaosbuffalo.mkultra.init.ModItems;

@MKUltraPlugin(modid = MKUltra.MODID)
public class MKUltraCorePlugin implements IMKUltraPlugin {

    @Override
    public void register(IMKUltraAPI api) {
        registerClasses(api);
        registerArmorClasses(api);
    }

    private static void registerArmorClasses(IMKUltraAPI api) {
        ArmorClass.ROBES
                .register(ModItems.ROBEMAT)
                .register(ModItems.COPPER_THREADED_MAT)
                .register(ModItems.IRON_THREADED_MAT);

        ArmorClass.MEDIUM.register(ModItems.CHAINMAT);
    }

    private static void registerClasses(IMKUltraAPI api) {
        registerClass(api, new Archer());
        registerClass(api, new Brawler());
        registerClass(api, new Cleric());
        registerClass(api, new Digger());
        registerClass(api, new Druid());
        registerClass(api, new MoonKnight());
        registerClass(api, new NetherMage());
        registerClass(api, new Skald());
        registerClass(api, new WetKnight());
        registerClass(api, new WetWizard());
    }

    private static void registerClass(IMKUltraAPI api, BaseClass bc) {
        api.registerClass(bc);
        bc.getAbilities().forEach(api::registerAbility);
    }
}
