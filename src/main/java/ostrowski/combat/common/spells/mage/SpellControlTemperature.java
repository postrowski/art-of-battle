/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellControlTemperature extends MageSpell
{
   public static final String NAME = "Control Temperature";
   public SpellControlTemperature() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.FIRE});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to raise or lower the temperature of an object. " +
              "The temperature change depends on the power put into the spell, and thermal mass (size) of the object.";
   }

}
