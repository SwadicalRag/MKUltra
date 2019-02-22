package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.utils.RayTraceUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class MobAbility extends IForgeRegistryEntry.Impl<MobAbility> implements IAbility {


    public MobAbility(String domain, String id) {
        this(new ResourceLocation(domain, id));
    }

    @Override
    public ResourceLocation getAbilityId() {
        return getRegistryName();
    }

    public MobAbility(ResourceLocation abilityId) {
        setRegistryName(abilityId);
    }

    public float getDistance() {
        return 1.0f;
    }

    public abstract int getCooldown();

    public int getCastTime(){
        return GameConstants.TICKS_PER_SECOND;
    }

    @Nullable
    public Potion getEffectPotion(){
        return null;
    }

    public enum AbilityType{
        ATTACK,
        HEAL,
        BUFF
    }

    public abstract AbilityType getAbilityType();

    @Override
    public boolean canSelfCast() {
        return false;
    }

    @Override
    public abstract Targeting.TargetType getTargetType();

    public abstract void execute(EntityLivingBase entity, IMobData data, EntityLivingBase target, World theWorld);
}
