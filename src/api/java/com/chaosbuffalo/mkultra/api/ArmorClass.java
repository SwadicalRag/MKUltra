package com.chaosbuffalo.mkultra.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.item.ItemArmor;

import java.util.List;
import java.util.Set;

public class ArmorClass  {

    public static IArmorClass HEAVY = new ArmorImpl();
    public static IArmorClass MEDIUM = new ArmorImpl();
    public static IArmorClass LIGHT = new ArmorImpl();
    public static IArmorClass ROBES = new ArmorImpl();


    public static void registerDefaults() {
        LIGHT.inherit(ROBES)
                .register(ItemArmor.ArmorMaterial.LEATHER);

        MEDIUM.inherit(LIGHT)
                .register(ItemArmor.ArmorMaterial.GOLD)
                .register(ItemArmor.ArmorMaterial.CHAIN);

        HEAVY.inherit(MEDIUM)
                .register(ItemArmor.ArmorMaterial.IRON);
    }

    private static class ArmorImpl implements IArmorClass {

        private Set<ItemArmor.ArmorMaterial> materials = Sets.newHashSet();
        private List<IArmorClass> ancestors = Lists.newArrayList();

        ArmorImpl() {
        }

        @Override
        public boolean canWear(ItemArmor.ArmorMaterial material) {
            return materials.contains(material) ||
                    ancestors.stream().anyMatch(a -> a.canWear(material));
        }

        public IArmorClass inherit(IArmorClass armorClass) {
            ancestors.add(armorClass);
            return this;
        }

        public IArmorClass register(ItemArmor.ArmorMaterial material) {
            materials.add(material);
            return this;
        }
    }
}
