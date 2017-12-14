/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;

public class SpellCreateElectricity extends MageSpell
{
   public static final String NAME = "Create Electricity";
   public SpellCreateElectricity() {
      super(NAME, new Class[] {}, new MageCollege[] {MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to create an electrical charge." +
              " The magnitude of the charge created depends upon the power put into the spell.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
