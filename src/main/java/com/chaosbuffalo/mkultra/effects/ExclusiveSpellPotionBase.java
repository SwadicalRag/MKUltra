package com.chaosbuffalo.mkultra.effects;

import com.chaosbuffalo.mkultra.core.BaseAbility;
import com.chaosbuffalo.mkultra.core.IAbilityLink;

public abstract class ExclusiveSpellPotionBase extends SpellPotionBase implements IAbilityLink {
    private BaseAbility linkedAbility;

    protected ExclusiveSpellPotionBase(boolean isBadEffectIn, int liquidColorIn) {
        super(isBadEffectIn, liquidColorIn);
    }


    public BaseAbility getAbility() {
        return linkedAbility;
    }

    public void setAbility(BaseAbility ability) {
        this.linkedAbility = ability;
    }
}
