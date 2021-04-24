/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellSuggestion extends MageSpell
{
   public static final String NAME = "Suggestion";
   public SpellSuggestion() {
      super(NAME, new Class[] {},
            new SkillType[] {SkillType.Spellcasting_Enchantment});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to plan a simple suggestion in the subjects mind." +
      " The power of the spell determines the complexity of the suggestion allowed.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
