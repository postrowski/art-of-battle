/*
 * Created on Jul 3, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;


public class WeaponStyleAttackMissile extends WeaponStyleAttackRanged
{

   public WeaponStyleAttackMissile(SkillType skillType, int minSkill, int skillPenalty,
                                   int damageMod, DiceSet varianceDice, DamageType damageType,
                                   int rangeBase, int handsRequired, String[] preparationSteps)
   {
      super(skillType, minSkill, skillPenalty, "Shoot", damageMod,
            varianceDice, damageType, AttackType.MISSILE, 0/*parryPenalty*/, rangeBase, handsRequired, preparationSteps);
   }

   @Override
   public WeaponStyleAttackMissile clone()
   {
      WeaponStyleAttackMissile style = new WeaponStyleAttackMissile(skillType, minSkill, skillPenalty,
                                                                    damageMod, varianceDice, damageType,
                                                                    rangeBase, handsRequired, preparationSteps);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackMissile)) {
         return false;
      }
      return super.equals(other);
      //WeaponStyleAttackMissile otherStyle = (WeaponStyleAttackMissile) other;
   }
}
