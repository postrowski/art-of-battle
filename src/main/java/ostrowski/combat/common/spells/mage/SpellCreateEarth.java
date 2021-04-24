/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateEarth extends MageSpell
{
   public static final String NAME = "Create Earth";
   public SpellCreateEarth() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Earth});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to magically create a block of dirt." +
             " The volume and mass of the dirt created depend upon the power put into the spell." +
             " A 1 power point spell will create a 60 lbs. pile of dirt, which takes up about a 1’x1’x1’ cube." +
             " Each additional point of power doubles this amount." +
             " For an addition point of power, an equal weight in miscellaneous small stones may be created, or for two additional points of power, a single large stone may be created.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
