/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateSound extends MageSpell
{
   public static final String NAME = "Create Sound";
   public SpellCreateSound() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Illusion});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to magically create a sound"
               + " they are familiar with. They can create voices, simple music or environments"
               + " sounds like creaking, water running, etc. A 1 power point spell will create a"
               + " single sound from a single location. Each additional point creates doubles the"
               + " number source of the sounds. So, to create a barber shop quartet, 3 points are"
               + " required (1 for the first singer, 1 for the second, and 1 for the next two)."
               + " Any aesthetic beauty of the sound must come from the caster’s musical skills.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
