/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellDetectDisturbance extends MageSpell
{
   public static final String NAME = "Detect Disturbance";
   public SpellDetectDisturbance() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.DIVINATION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell detects anything that is out of its natural state." +
      " Recently disturbed elements are easier to detect than things disturbed long ago." +
      " Similarly, smaller disturbances are harder to detect than large scale disturbances.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
