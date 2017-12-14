/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.healing;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellResurrection extends PriestSpell
{
   public static final String NAME = "Resurrection";
   public SpellResurrection() {};
   public SpellResurrection(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell raises a single character from the dead." +
             " The more power put into the spell, the older, and more severely damages the corpse may be.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_DEAD;
   }

}
