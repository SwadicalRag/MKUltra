package com.chaosbuffalo.mkultra.core.abilities;

import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.effects.spells.YankPotion;
import com.chaosbuffalo.mkultra.core.PlayerAbility;
import com.chaosbuffalo.mkultra.core.IPlayerData;
import com.chaosbuffalo.mkultra.fx.ParticleEffects;
import com.chaosbuffalo.mkultra.network.packets.ParticleEffectSpawnPacket;
import com.chaosbuffalo.mkultra.fx.ParticleStyle;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Yank extends PlayerAbility {

    public static float BASE_FORCE = 1.0f;
    public static float FORCE_SCALE = .75f;

    public Yank() {
        super(MKUltra.MODID, "ability.yank");
    }

    @Override
    public int getCooldown(int currentRank) {
        return 5;
    }

    @Override
    public int getType() {
        return ACTIVE_ABILITY;
    }

    @Override
    public Targeting.TargetType getTargetType() {
        return Targeting.TargetType.ALL;
    }

    @Override
    public int getManaCost(int currentRank) {
        return 4 - currentRank;
    }

    @Override
    public float getDistance(int currentRank) {
        return 5.0f + 2 * 5.0f;
    }

    @Override
    public int getRequiredLevel(int currentRank) {
        return currentRank * 2;
    }

    private ParticleStyle castStyle = new ParticleStyle(EnumParticleTypes.SPELL_INSTANT, ParticleEffects.DIRECTED_SPOUT, 50, 1, 5.0f, RADIUS_P25, OFFSET_Y_ONE);

    @Override
    public void execute(EntityPlayer entity, IPlayerData pData, World theWorld) {
        int level = pData.getAbilityRank(getAbilityId());

        EntityLivingBase targetEntity = getSingleLivingTarget(entity, getDistance(level));
        if (targetEntity != null) {
            pData.startAbility(this);

            targetEntity.addPotionEffect(YankPotion.Create(entity, targetEntity).toPotionEffect(level));

            Vec3d partHeading = targetEntity.getPositionVector()
                    .add(OFFSET_Y_ONE)
                    .subtract(entity.getPositionVector())
                    .normalize();
            performCastAnimation(castStyle, entity, partHeading);
        }
    }
}
