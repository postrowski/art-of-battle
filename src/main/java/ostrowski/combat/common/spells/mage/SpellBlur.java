/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;

/**
 * This spell causes the target to become hard to see, which effectively
 * increases the targets PD by the spells power level.
 */
public class SpellBlur extends ExpiringMageSpell
{
   public static final String NAME = "Blur";
   public SpellBlur() {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5 /*bonusTimeInTurnsPerPower*/,
            new Class[] {SpellControlLight.class}, new MageCollege[] {MageCollege.ILLUSION, MageCollege.PROTECTION});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " becomes blurry, and hard to see/hit (PD increased by " + getPower() + ").";
      }
      return "(PD increased by " + getPower() + ").";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell makes the subject harder to see, as their shape becomes less defined and hard to focus on."
           + " The result is that the subject becomes harder to hit as the subject’s PD increases by an amount equal to the power put into the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType) {
      if (!isExpired()) {
         return getPower();
      }
      return 0;
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
