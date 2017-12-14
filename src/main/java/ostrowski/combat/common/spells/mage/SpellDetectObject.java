/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellDetectObject extends MageSpell
{
   public static final String NAME = "Detect Object";
   public SpellDetectObject() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.DIVINATION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to locate a nearby object." +
              " If the object is not nearby, the spell is ineffective. Putting more power into the spell " +
              "increases the range of the spell.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
