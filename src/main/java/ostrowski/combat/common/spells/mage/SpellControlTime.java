/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellControlTime extends AreaMageSpell
{
   public static final String NAME = "Control Time";
   public SpellControlTime() {
      super(NAME, new Class[] {SpellAffectArea.class}, new MageCollege[] {MageCollege.EVOCATION, MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to increase or decrease the rate at which time flows. " +
              "The rate change depends on the power put into the spell, and size of the area being affected.";
   }

}
