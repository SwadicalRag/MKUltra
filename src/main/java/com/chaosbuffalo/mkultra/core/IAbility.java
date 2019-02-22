package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.utils.RayTraceUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import java.util.List;

public interface IAbility {
    ResourceLocation getAbilityId();

    Targeting.TargetType getTargetType();

    boolean canSelfCast();

    default boolean isValidTarget(EntityLivingBase caster, EntityLivingBase target) {
        return Targeting.isValidTarget(getTargetType(), caster, target, !canSelfCast());
    }

    default EntityLivingBase getSingleLivingTarget(EntityLivingBase caster, float distance) {
        return getSingleLivingTarget(caster, distance, true);
    }

    default List<EntityLivingBase> getTargetsInLine(EntityLivingBase caster, Vec3d from, Vec3d to, boolean checkValid, float growth) {
        return RayTraceUtils.getEntitiesInLine(EntityLivingBase.class, caster, from, to, Vec3d.ZERO, growth,
                e -> !checkValid || (e != null && isValidTarget(caster, e)));
    }

    default EntityLivingBase getSingleLivingTarget(EntityLivingBase caster, float distance, boolean checkValid) {
        return getSingleLivingTarget(EntityLivingBase.class, caster, distance, checkValid);
    }

    default  <E extends EntityLivingBase> E getSingleLivingTarget(Class<E> clazz, EntityLivingBase caster,
                                                                   float distance, boolean checkValid) {
        RayTraceResult lookingAt = RayTraceUtils.getLookingAt(clazz, caster, distance,
                e -> !checkValid || (e != null && isValidTarget(caster, e)));

        if (lookingAt != null && lookingAt.entityHit instanceof EntityLivingBase) {

            if (checkValid && !isValidTarget(caster, (EntityLivingBase) lookingAt.entityHit)) {
                return null;
            }

            return (E) lookingAt.entityHit;
        }

        return null;
    }

    @Nonnull
    default EntityLivingBase getSingleLivingTargetOrSelf(EntityLivingBase caster, float distance, boolean checkValid) {
        EntityLivingBase target = getSingleLivingTarget(caster, distance, checkValid);
        return target != null ? target : caster;
    }
}
