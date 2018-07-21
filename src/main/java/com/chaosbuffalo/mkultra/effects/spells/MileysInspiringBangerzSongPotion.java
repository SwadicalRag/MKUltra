package com.chaosbuffalo.mkultra.effects.spells;

import com.chaosbuffalo.mkultra.GameConstants;
import com.chaosbuffalo.mkultra.MKUltra;
import com.chaosbuffalo.mkultra.core.BaseAbility;
import com.chaosbuffalo.mkultra.effects.SpellCast;
import com.chaosbuffalo.mkultra.effects.songs.SongApplicator;
import net.minecraft.entity.Entity;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

/**
 * Created by Jacob on 4/22/2018.
 */
@Mod.EventBusSubscriber(modid = MKUltra.MODID)
public class MileysInspiringBangerzSongPotion extends SongApplicator {

    public static final MileysInspiringBangerzSongPotion INSTANCE = new MileysInspiringBangerzSongPotion();

    public static final int PERIOD = 18 * GameConstants.TICKS_PER_SECOND;

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(INSTANCE.finish());
    }

    public static SpellCast Create(BaseAbility ability, Entity source) {
        return INSTANCE.newSpellCast(source, ability);
    }

    private MileysInspiringBangerzSongPotion() {
        super(PERIOD, false, 16762880);
        setPotionName("effect.mileys_bangerz_song");
    }

    @Override
    public Set<SpellCast> getSpellCasts(Entity source, SpellCast applicatorCast) {
        Set<SpellCast> ret = super.getSpellCasts(source, applicatorCast);
        ret.add(MileysInspiringBangerzPotion.Create(applicatorCast.getAbility(), source));
        return ret;
    }


    @Override
    public ResourceLocation getIconTexture() {
        return new ResourceLocation(MKUltra.MODID, "textures/class/abilities/mileys_bangerz.png");
    }
}
