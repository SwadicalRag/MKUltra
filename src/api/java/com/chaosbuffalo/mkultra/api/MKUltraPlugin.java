package com.chaosbuffalo.mkultra.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MKUltraPlugin {
    // the Mod this is for - will be used for
    // generating the name of the json the config
    // will get saved to and should also be the
    // actual mod-id we can use for creating a
    // resource location
    String modid();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Inject {

    }
}
