/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellControlFire extends MageSpell
{
   public static final String NAME = "Control Fire";
   public SpellControlFire() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.FIRE});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to move and shape an existing fire." +
              " This spell may also be used to extinguish fires.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
