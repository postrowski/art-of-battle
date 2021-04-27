/*
 * Created on Jul 3, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.SkillRank;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;


public class WeaponStyleAttackMelee extends WeaponStyleAttack
{

   public WeaponStyleAttackMelee(SkillType skillType, SkillRank minRank, int skillPenalty, String name,
                                 int speedBase, int slowStr, int fastStr, int damageMod,
                                 DieType varianceDie, DamageType damageType, AttackType attackType,
                                 Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      this(skillType, minRank, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
           DiceSet.getSingleDie(varianceDie), damageType, attackType, chargeType, parryPenalty, minRange, maxRange, handsRequired);
   }
   public WeaponStyleAttackMelee(SkillType skillType, SkillRank minRank, int skillPenalty, String name,
                                 int speedBase, int slowStr, int fastStr, int damageMod,
                                 DiceSet varianceDice, DamageType damageType, AttackType attackType,
                                 Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minRank, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
            varianceDice, damageType, attackType, chargeType, parryPenalty, minRange, maxRange, handsRequired);
   }

   @Override
   public WeaponStyleAttack clone()
   {
      WeaponStyleAttack style = new WeaponStyleAttackMelee(skillType, minRank, skillPenalty, name,
                                                           speedBase, slowStr, fastStr,
                                                           damageMod, varianceDice, damageType, attackType,
                                                           chargeType, parryPenalty, minRange, maxRange, handsRequired);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackMelee)) {
         return false;
      }
      return super.equals(other);
      //WeaponStyleAttackMelee otherStyle = (WeaponStyleAttackMelee) other;
   }

}
