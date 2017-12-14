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
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' causes a jet of water to shoot from the caster hand.");
      sb.append(" The effective power of the spell determines the maximum effective distance the water shoots, and the damage done.");
      sb.append(" The jet shoots out in a straight line determined by the caster.");
      return sb.toString();
   }

}
