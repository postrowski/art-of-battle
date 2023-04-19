/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDetectGood extends PriestSpell
{
   public static final String NAME = "Detect Good";
   public SpellDetectGood() {
   }
   public SpellDetectGood(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to determine the source and type of any goodness within the area of affect." +
             " The more power put into the spell, the larger the area becomes, and the smaller the amount of good that can detected.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
