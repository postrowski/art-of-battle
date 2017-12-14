/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellHarden extends MageSpell
{
   public static final String NAME = "Harden";
   public SpellHarden() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.PROTECTION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell makes a non-living subject of the spell harder and stronger.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
