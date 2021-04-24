/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.server.Arena;

/**
 * This spell increases the target Health attribute by an amount equal to twice the power put into the spell
 */
public class SpellArmor extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME = "Armor";
   public SpellArmor() {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/,
            new Class[] {SpellHarden.class}, new SkillType[] {SkillType.Spellcasting_Protection});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + "'s skin becomes like armor (BLD increased by " + (getPower()*2) + ").";
      }
      return "(HT incresed by " + (getPower()*2) + ").";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's Build characteristic by an amount equal to the power put into the spell."
         + " This spell may be cast on yourself or others. Multiple castings on the same subject do not accumulate."
         + " The highest power casting (of multiple castings) is the only one that has an effect.";
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) + (getPower()*2)), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) - (getPower()*2)), false/*containInLimits*/);
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }
}
