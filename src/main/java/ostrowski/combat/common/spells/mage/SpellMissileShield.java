/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.ICastInBattle;

public class SpellMissileShield extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME = "Missile Shield";

   public SpellMissileShield() {
      super(NAME, (short) 10/*baseExpirationTimeInTurns*/, (short) 5 /*bonusTimeInTurnsPerPower*/,
            new Class[] { SpellCreateEarth.class, SpellCreateForce.class, SpellDetectObject.class, SpellMagicShield.class, SpellPush.class},
            new SkillType[] {SkillType.Spellcasting_Earth, SkillType.Spellcasting_Energy, SkillType.Spellcasting_Divination, SkillType.Spellcasting_Protection});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + " is surrounded by a missile shield, increasing " + getTarget().getHisHer() + " PD by " + (2 * getPower())
                + " against ranged weapons.";
      }
      return " (PD increased by " + (2 * getPower()) + " against ranged weapons)";
   }

   @Override
   public String describeSpell() {
      return "The '"
             + getName()
             + "'' spell causes missiles fired at the subject to veer away. This makes the subject harder to hit with ranged attacks, increasing the subject's PD by an amount equal to twice the power put into the spell.";
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
