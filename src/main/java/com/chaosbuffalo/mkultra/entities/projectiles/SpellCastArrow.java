package com.chaosbuffalo.mkultra.entities.projectiles;

import com.chaosbuffalo.mkultra.core.BaseAbility;
import com.chaosbuffalo.mkultra.core.IAbilityLink;
import com.chaosbuffalo.mkultra.effects.SpellCast;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SpellCastArrow extends EntityTippedArrow implements IAbilityLink {

    private List<SpellCast> casts = new ArrayList<>();
    private BaseAbility ability;

    public SpellCastArrow(World worldIn, EntityLivingBase shooter) {
        super(worldIn, shooter);
    }

    public void addSpellCast(SpellCast cast, int amplifier) {
        addSpellCast(cast, 1, amplifier);
    }

    public void addSpellCast(SpellCast cast, int duration, int amplifier) {
        cast.setInt("amplifier", amplifier);
        cast.setInt("duration", duration);
        casts.add(cast);
    }

    @Override
    protected void arrowHit(EntityLivingBase target) {
        for (SpellCast cast : casts) {
            cast.link(this);

            target.addPotionEffect(cast.setTarget(target)
                    .toPotionEffect(cast.getInt("amplifier"), cast.getInt("duration")));
        }
        super.arrowHit(target);
    }

    @Override
    public BaseAbility getAbility() {
        return ability;
    }

    @Override
    public void setAbility(BaseAbility ability) {
        this.ability = ability;
    }
}
