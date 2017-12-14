/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellControlCharge extends MageSpell
{
   public static final String NAME = "Control Charge";
   public SpellControlCharge() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to move electric charges from one place to another." +
              " This spell effectively creates a current between the two locations. Electrical resistance between the two "+
              "locations must be overcome with the power put into the spell.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
