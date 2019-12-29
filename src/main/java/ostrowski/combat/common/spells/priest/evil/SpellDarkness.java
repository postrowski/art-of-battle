/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellDarkness extends PriestSpell
{
   public static final String NAME = "Darkness";
   public SpellDarkness() {
   }
   public SpellDarkness(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the all light that enters an area to be absorbed and blocked out." +
             " The more power put into the spell, the brighter the light that can be absorbed, the longer the spell lasts, and the larger the area affected";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
