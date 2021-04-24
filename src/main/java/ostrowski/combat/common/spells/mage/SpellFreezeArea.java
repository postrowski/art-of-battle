/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellFreezeArea extends MageSpell
{
   public static final String NAME = "Freeze";
   public SpellFreezeArea() {
      super(NAME, new Class[] {SpellAffectArea.class, SpellControlTemperature.class},
            new SkillType[] {SkillType.Spellcasting_Evocation, SkillType.Spellcasting_Fire});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell affects an area, causing all non-moving items in the area to freeze." +
              " The size of the area is dependent upon the power put into the spell.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }


}
