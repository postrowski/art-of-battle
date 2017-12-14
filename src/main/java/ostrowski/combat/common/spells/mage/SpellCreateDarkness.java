/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellCreateDarkness extends MageSpell
{
   public static final String NAME = "Create Darkness";
   public SpellCreateDarkness() {
      super(NAME, new Class[] {SpellAffectArea.class}, new MageCollege[] {MageCollege.ILLUSION, MageCollege.EVOCATION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell prevents any visible light from passing through the area of effect." +
            " The size of the area depends upon the power put into the spell." +
            " A 1 power point spell blocks light in 1 hex, up to 10 feet high. Each additional point increases the radius for the area by 1, or doubles the height." +
            " So a 4 power point spell will cover all hexes within 3 hexes of the center hex." +
            " For 1 additional power point, the spell may be extended to include light outside of the visible range, such as infrared-red (heat) and ultraviolet (radiation).";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
