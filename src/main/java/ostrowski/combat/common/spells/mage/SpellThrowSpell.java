/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellThrowSpell extends MageSpell
{
   public static final String NAME = "Throw Spell";
   public SpellThrowSpell() {
      super(NAME, new Class[]{}, new SkillType[] {SkillType.Spellcasting_Conjuration});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' allows the caster to throw another spell at a distance.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
