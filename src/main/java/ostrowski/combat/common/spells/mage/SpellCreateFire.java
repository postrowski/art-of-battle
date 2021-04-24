/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellCreateFire extends MageSpell
{
   public static final String NAME = "Create Fire";
   public SpellCreateFire() {
      super(NAME, new Class[] {SpellControlTemperature.class}, new SkillType[] {SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Fire});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell allows the caster to create fire." +
              " If the fire does not have any fuel to burn, or is not hot enough to catch, then the fire will burn out immediately." +
              " The temperature and size of the fire created depends upon the power put into the spell.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

}
