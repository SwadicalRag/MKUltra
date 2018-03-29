package com.chaosbuffalo.mkultra.api;


import net.minecraft.util.ResourceLocation;

public class GameConstants {
    public static final String MODID = "@MODID@";
    public static final ResourceLocation INVALID_CLASS = new ResourceLocation(MODID, "class.invalid");
    public static final ResourceLocation INVALID_ABILITY = new ResourceLocation(MODID, "ability.invalid");

    public static final int ACTION_BAR_SIZE = 5;
    public static final int ACTION_BAR_INVALID_LEVEL = 0;
    public static final int ACTION_BAR_INVALID_COOLDOWN = -1;
    public static final int ACTION_BAR_INVALID_SLOT = -1;

    public static final int MAX_CLASS_LEVEL = 10;
    public static final int MAX_ABILITY_LEVEL = 2;

    public static final int TICKS_PER_SECOND = 20;
}
