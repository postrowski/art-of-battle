package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDig extends PriestSpell
{
   public static final String NAME = "Dig";

   public SpellDig() {
      this(null, 0);
   }

   public SpellDig(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell moves dirt, forming a tunnel. The diameter and depth of the tunnel depends upon the effective power of the spell. " +
      		"1 point of effective power will create a 3'x3'x6' hole (one hex, 6 feet deep). Each additional point of effective power doubles the size of any dimension." +
            " Digging through solid rock requires 3 points of power to create a hole 3’x3’x6’." +
      		" When digging horizontally through soft dirt, a tunnel will often collapse immediately after being dug.";
   }

}
