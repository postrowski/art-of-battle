/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateAir extends MageSpell
{
   public static final String NAME = "Create Air";
   public SpellCreateAir() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Air});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to magically create a volume of air, suitable for breathing or burning." +
              " The newly created air will immediately dissipate into its surroundings, creating a loud clapping sound if not cast into a vessel, such as a large bottle. " +
              " The volume of the air created depends upon the power put into the spell." +
              " A 1 power point spell will create enough air for someone the caster's size to live on for 10 minutes." +
              " Each additional point doubles this volume.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
