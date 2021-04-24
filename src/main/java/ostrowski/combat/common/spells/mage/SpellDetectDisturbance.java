/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;

public class SpellDetectDisturbance extends MageSpell
{
   public static final String NAME = "Detect Disturbance";
   public SpellDetectDisturbance() {
      super(NAME, new Class[] {}, new SkillType[] {SkillType.Spellcasting_Divination});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell detects anything that is out of its natural state." +
      " Recently disturbed elements are easier to detect than things disturbed long ago." +
      " Similarly, smaller disturbances are harder to detect than large scale disturbances.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_NONE;
   }

}
