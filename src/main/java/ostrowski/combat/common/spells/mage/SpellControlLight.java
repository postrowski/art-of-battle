/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellControlLight extends MageSpell
{
   public static final String NAME = "Control Light";
   public SpellControlLight() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.ILLUSION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to bend, distort, change the color of, "+
              "and adjust the intensity of existing light." +
              " This spell is a basic component of many higher-level spell such as illusion, blur, and invisibility.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
