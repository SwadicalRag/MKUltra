package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.api.BaseClass;
import com.chaosbuffalo.mkultra.api.IClassProvider;
import com.chaosbuffalo.mkultra.init.ModItems;
import net.minecraft.util.ResourceLocation;

public abstract class CoreClass extends BaseClass {
    protected CoreClass(String pathName, String className) {
        super(new ResourceLocation(MKUltra.MODID, pathName), className);
    }
}
