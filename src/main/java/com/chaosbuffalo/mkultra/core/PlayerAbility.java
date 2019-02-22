package com.chaosbuffalo.mkultra.core;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.utils.RayTraceUtils;
import com.chaosbuffalo.targeting_api.Targeting;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;

public abstract class PlayerAbility extends IForgeRegistryEntry.Impl<PlayerAbility> implements IAbility {

    public static final int ACTIVE_ABILITY = 0;
    public static final int TOGGLE_ABILITY = 1;

    private ResourceLocation abilityId;

    public PlayerAbility(String domain, String id) {
        this(new ResourceLocation(domain, id));
    }

    public PlayerAbility(ResourceLocation abilityId) {
        this.abilityId = abilityId;
    }

    @Override
    public ResourceLocation getAbilityId() {
        return abilityId;
    }

    public String getAbilityName()
    {
        return I18n.format(String.format("%s.%s.name", abilityId.getNamespace(), abilityId.getPath()));
    }

    public String getAbilityDescription()
    {
        return I18n.format(String.format("%s.%s.description", abilityId.getNamespace(), abilityId.getPath()));
    }


    public ResourceLocation getAbilityIcon()
    {
        return new ResourceLocation(abilityId.getNamespace(), String.format("textures/class/abilities/%s.png", abilityId.getPath().split(Pattern.quote("."))[1]));
    }


    public float getDistance(int currentRank) {
        return 1.0f;
    }

    public abstract int getCooldown(int currentRank);

    public int getCooldownTicks(int currentRank) {
        return getCooldown(currentRank) * GameConstants.TICKS_PER_SECOND;
    }

    public int getType() {
        return ACTIVE_ABILITY;
    }

    @Override
    public abstract Targeting.TargetType getTargetType();

    @Override
    public boolean canSelfCast() {
        return false;
    }

    public abstract int getManaCost(int currentRank);

    public abstract int getRequiredLevel(int currentRank);

    public int getMaxRank() {
        return GameConstants.MAX_ABILITY_RANK;
    }

    public boolean meetsRequirements(IPlayerData player) {
        return player.getMana() >= getManaCost(player.getAbilityRank(abilityId)) &&
                player.getCurrentAbilityCooldown(abilityId) == 0;
    }

    public abstract void execute(EntityPlayer entity, IPlayerData data, World theWorld);
}
