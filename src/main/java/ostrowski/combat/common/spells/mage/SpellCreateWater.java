/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateWater extends MageSpell
{
   public static final String NAME = "Create Water";
   public SpellCreateWater() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Water});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to magically create a volume of pure water, suitable for drinking or washing." +
            " The spell should be cast into a vessel, such as a bottle, or the newly created water will immediately spill onto the floor." +
            " The volume of the water created depends upon the power put into the spell." +
            " A 1 power point spell will create a liter of water." +
            " Each additional point doubles this volume.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
