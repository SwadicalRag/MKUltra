package com.chaosbuffalo.mkultra.core.classes;

import com.chaosbuffalo.mkultra.api.BaseAbility;
import com.chaosbuffalo.mkultra.api.BaseClass;
import com.chaosbuffalo.mkultra.api.IArmorClass;
import com.chaosbuffalo.mkultra.core.CoreClass;
import com.chaosbuffalo.mkultra.core.abilities.*;
import com.chaosbuffalo.mkultra.init.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;

import java.util.ArrayList;
import java.util.List;

public class Digger extends CoreClass {

    public static final List<BaseAbility> abilities = new ArrayList<>(5);
    public static IArmorClass ARMORCLASS = new DiggerArmorClass();

    static {
        abilities.add(new GoldenOpportunity());
        abilities.add(new HopeBread());
        abilities.add(new PierceTheHeavens());
        abilities.add(new TNTWhisperer());
        abilities.add(new LavaWanderer());
    }

    public Digger() {
        super("class.digger", "Digger");
    }

    @Override
    public List<BaseAbility> getAbilities() {
        return abilities;
    }

    @Override
    public int getIconU() {
        return 0;
    }

    @Override
    public int getIconV() {
        return 0;
    }

    @Override
    public int getHealthPerLevel() {
        return 4;
    }

    @Override
    public int getBaseHealth() {
        return 22;
    }

    @Override
    public float getBaseManaRegen() {
        return 1;
    }

    @Override
    public float getManaRegenPerLevel() {
        return 0.2f;
    }

    @Override
    public int getBaseMana() {
        return 10;
    }

    @Override
    public int getManaPerLevel() {
        return 1;
    }


    @Override
    public IArmorClass getArmorClass() {
        return ARMORCLASS;
    }

    @Override
    public Item getUnlockItem() {
        return ModItems.sunicon;
    }

    private static class DiggerArmorClass implements IArmorClass {
        @Override
        public boolean canWear(ItemArmor.ArmorMaterial material) {
            return true;
        }

        @Override
        public IArmorClass inherit(IArmorClass armorClass) {
            return this;
        }

        @Override
        public IArmorClass register(ItemArmor.ArmorMaterial material) {
            return this;
        }
    }
}
