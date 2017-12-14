/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellCreateLight extends MageSpell
{
   public static final String NAME = "Create Light";
   public SpellCreateLight() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.ILLUSION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell creates a magical light source, either floating in the air, or emanating from an object."
           + " The light radiates equally in all directions."
           + " When created in the air, the light remains fixed in its floating position until the spell expires."
           + " To move the light around, it must be cast on an object."
           + " The intensity and duration of the light depend upon the power put into the spell."
           + " A 1 power point light spell will last for 12 minutes, with brightness equal to 1 candle."
           + " Each additional point of power can either increase the duration by a factor of 5, or it can increase the brightness by a factor 10.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
