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
             " The size of the area depends upon the power put into the spell." +
             " A 1 power point spell blocks light in 1 hex, up to 10 feet high. Each additional point increases the radius for the area by 1, or doubles the height." +
             " So a 4 power point spell will cover all hexes within 3 hexes of the center hex." +
             " For 1 additional power point, the spell may be extended to include light outside of the visible range, such as infrared-red (heat) and ultraviolet (radiation).";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
