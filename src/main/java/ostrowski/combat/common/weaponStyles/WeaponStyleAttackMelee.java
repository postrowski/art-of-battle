/*
 * Created on Jul 3, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;


public class WeaponStyleAttackMelee extends WeaponStyleAttack
{

   public WeaponStyleAttackMelee(SkillType skillType, int minSkill, int skillPenalty, String name,
                                 int speedBase, int slowStr, int fastStr, int damageMod,
                                 DieType varianceDie, DamageType damageType, AttackType attackType,
                                 Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      this(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
           DiceSet.getSingleDie(varianceDie), damageType, attackType, chargeType, parryPenalty, minRange, maxRange, handsRequired);
   }
   public WeaponStyleAttackMelee(SkillType skillType, int minSkill, int skillPenalty, String name,
                                 int speedBase, int slowStr, int fastStr, int damageMod,
                                 DiceSet varianceDice, DamageType damageType, AttackType attackType,
                                 Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
            varianceDice, damageType, attackType, chargeType, parryPenalty, minRange, maxRange, handsRequired);
   }

   @Override
   public WeaponStyleAttack clone()
   {
      WeaponStyleAttack style = new WeaponStyleAttackMelee(_skillType, _minSkill, _skillPenalty, _name,
                                        _speedBase, _slowStr, _fastStr,
                                        _damageMod, _varianceDice, _damageType, _attackType,
                                        _chargeType, _parryPenalty, _minRange, _maxRange, _handsRequired);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackMelee)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      //WeaponStyleAttackMelee otherStyle = (WeaponStyleAttackMelee) other;
      return true;
   }

}
