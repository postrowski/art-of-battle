/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellWind extends MageSpell
{
   public static final String NAME = "Wind";
   public SpellWind() {
      super(NAME, new Class[] {SpellCreateAir.class, SpellThrowSpell.class},
            new SkillType[] {SkillType.Spellcasting_Air, SkillType.Spellcasting_Conjuration});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes a gust of wind to come from the caster fingertips." +
      " The power of the spell determines the power of the wind created, from a light breeze to a strong gale.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }
}
