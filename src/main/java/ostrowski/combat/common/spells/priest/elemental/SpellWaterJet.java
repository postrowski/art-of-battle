package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;

public class SpellWaterJet extends PriestMissileSpell
{
   public static final String NAME = "Water Jet";

   public SpellWaterJet() {
      this(null, 0);
   }

   public SpellWaterJet(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, group, affinity, DieType.D6, (short)12/*rangebase*/, DamageType.BLUNT, "Water Jet");
   }

   @Override
   public String describeSpellPreamble() {
      String sb = "The '" + getName() + "' causes a jet of water to shoot from the caster hand." +
                  " The effective power of the spell determines the maximum effective distance the water shoots, and the damage done." +
                  " The jet shoots out in a straight line determined by the caster.";
      return sb;
   }

}
