/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;

public class SpellMissileShield extends ExpiringPriestSpell
{
   public static final String NAME = "Missile Shield";
   public SpellMissileShield() {
      this(null, 0);
   }
   public SpellMissileShield(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5 /*bonusTimeInTurnsPerPower*/,
            group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " is surrounded by a missile shield, increasing "+getTarget().getHisHer()+" PD by " + (2*getPower()) + " against ranged weapons.";
      }
      return " (PD increased by " + (2*getPower()) + " against ranged weapons)";
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "'' spell causes missiles fired at the subject to veer away. This makes the subject harder to hit with ranged attacks, increasing the subject's PD by an amount equal to twice the effective power of the spell.";
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType) {
      if (vsRangedWeapons) {
         if (!isExpired()) {
            return (byte) (getPower() * 2);
         }
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
