package com.chaosbuffalo.mkultra.core.mob_abilities;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.IMobData;
import com.chaosbuffalo.mkultra.core.MobAbility;
import com.chaosbuffalo.mkultra.entities.projectiles.EntityMobFireballProjectile;
import com.chaosbuffalo.mkultra.init.ModSounds;
import com.chaosbuffalo.mkultra.utils.EntityUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class Fireball extends MobAbility {
    private static float PROJECTILE_SPEED = 1.0f;
    private static float PROJECTILE_INACCURACY = 1.0f;

    public Fireball() {
        super(MKUltra.MODID, "mob_ability.fireball");
    }

    @Override
    public int getCooldown() {
        return 3 * GameConstants.TICKS_PER_SECOND;
    }

    @Override
    public AbilityType getAbilityType() {
        return AbilityType.ATTACK;
    }

    @Override
    public Targeting.TargetType getTargetType() {
        return Targeting.TargetType.ENEMY;
    }

    @Override
    public float getDistance() {
        return 20.0f;
    }

    @Override
    public SoundEvent getCastingSoundEvent() {
        return ModSounds.hostile_casting_fire;
    }


    @Nullable
    @Override
    public SoundEvent getCastingCompleteEvent() {
        return ModSounds.spell_fire_2;
    }

    @Override
    public void execute(EntityLivingBase entity, IMobData data, EntityLivingBase target, World theWorld) {
        World world = entity.getEntityWorld();
        EntityMobFireballProjectile flamep = new EntityMobFireballProjectile(world, entity,
                entity.getEyeHeight() / 2.0);
        flamep.setAmplifier(data.getMobLevel());
        EntityUtils.shootProjectileAtTarget(flamep, target, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        world.spawnEntity(flamep);
    }
}
