package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;

public class SpellBreatheFire extends PriestMissileSpell
{
   public static final String NAME = "Breathe Fire";

   public SpellBreatheFire() {
      this(null, 0);
   }

   public SpellBreatheFire(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, group, affinity, DieType.D8, (short)8/*rangeBase*/, DamageType.FIRE, "Flaming Breath");
   }

   @Override
   public String describeSpellPreamble() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' causes a cone of fire to erupt from the caster mouth.");
      sb.append(" The effective power of the spell determines the maximum effective distance the flame shoots, and the damage done.");
      sb.append(" The flame shoots out in a straight line determined by the caster.");
      return sb.toString();
   }

//   @Override
//   public SpecialDamage getSpecialDamageModifier() {
//      return new SpecialDamage(SpecialDamage.MOD_NO_BUILD);
//   }

}
