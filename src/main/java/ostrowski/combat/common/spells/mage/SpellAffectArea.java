/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellAffectArea extends MageSpell
{
   public static final String NAME = "Affect Area";
   public SpellAffectArea() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Evocation});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows other spells to be applied to an area. The spell has no effect when cast by itself.";
   }

}
