/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateForce extends MageSpell
{
   public static final String NAME = "Create Force";
   public SpellCreateForce() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Energy});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to push, pull, lift or twist small objects from a distance."
           + " The magnitude of the pushing force and maximum distance to the object depends upon the power put into the spell."
           + " The force is applied for up to 1 second, so a 4 pound upwards push on a 2 pound object can lift the object upwards as if it ‘fell upwards’ for 1 second."
           + " A 1 power point spell can apply 1 pound of force to an object from up to 6 feet (2 hexes) away."
           + " Each additional point quadruples the force applied, or quadruples the distance to the object, or doubles the duration of force’s application.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
