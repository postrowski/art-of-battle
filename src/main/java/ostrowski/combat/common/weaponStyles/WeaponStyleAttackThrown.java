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


public class WeaponStyleAttackThrown extends WeaponStyleAttackRanged
{
   private static final String[] PREPARATION_STEPS = new String[] {"Raise <weaponName> to throw"};
   public WeaponStyleAttackThrown(int minSkill, int skillPenalty,
                                  int damageMod, DieType varianceDie, DamageType damageType,
                                  int rangeBase, int handsRequired)
   {
      this(minSkill, skillPenalty, damageMod, DiceSet.getSingleDie(varianceDie), damageType, rangeBase, handsRequired);
   }
   public WeaponStyleAttackThrown(int minSkill, int skillPenalty,
                                  int damageMod, DiceSet varianceDice, DamageType damageType,
                                  int rangeBase, int handsRequired)
   {
      super(SkillType.Throwing, minSkill, skillPenalty, "Thrown"/*name*/,
            damageMod, varianceDice, damageType, AttackType.THROW, 0/*parryPenalty*/,
            rangeBase, handsRequired, PREPARATION_STEPS);
   }

   @Override
   public WeaponStyleAttackThrown clone()
   {
      WeaponStyleAttackThrown style =  new WeaponStyleAttackThrown(minSkill, skillPenalty,
                                                                   damageMod, varianceDice, damageType,
                                                                   rangeBase, handsRequired);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public byte getMaxAttackActions() {
      return 2;
   }
   @Override
   public byte getMaxAimBonus() {
      return 1;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackThrown)) {
         return false;
      }
      return super.equals(other);
      //WeaponStyleAttackThrown otherStyle = (WeaponStyleAttackThrown) other;
   }

}
