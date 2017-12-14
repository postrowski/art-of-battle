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


public class WeaponStyleCounterAttack extends WeaponStyleAttackMelee
{

   public WeaponStyleCounterAttack(SkillType skillType, int minSkill, int skillPenalty, String name,
                                   int speedBase, int slowStr, int fastStr, int damageMod,
                                   DieType varianceDie, DamageType damageType, AttackType attackType,
                                   int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      this(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
           DiceSet.getSingleDie(varianceDie), damageType, attackType, parryPenalty, minRange, maxRange, handsRequired);
   }
   public WeaponStyleCounterAttack(SkillType skillType, int minSkill, int skillPenalty, String name,
                                 int speedBase, int slowStr, int fastStr, int damageMod,
                                 DiceSet varianceDice, DamageType damageType, AttackType attackType,
                                 int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, damageMod,
            varianceDice, damageType, attackType, Charge.Never, parryPenalty, minRange, maxRange, handsRequired);
   }

   @Override
   public Object clone()
   {
      WeaponStyleCounterAttack style = new WeaponStyleCounterAttack(_skillType, _minSkill, _skillPenalty, _name,
                                                                    _speedBase, _slowStr, _fastStr,
                                                                    _damageMod, _varianceDice, _damageType, _attackType,
                                                                    _parryPenalty, _minRange, _maxRange, _handsRequired);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleCounterAttack)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      //WeaponStyleCounterAttack otherStyle = (WeaponStyleCounterAttack) other;
      return true;
   }

}
