/*
 * Created on Jul 3, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;




public class WeaponStyleAttackGrapple extends WeaponStyleAttackMelee
{

   public WeaponStyleAttackGrapple(SkillType skillType, int minSkill, int skillPenalty, String name,
                                   int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, 0/*speedBase*/, -99/*slowStr*/, 99/*fastStr*/, 0/*damageMod*/,
            DieType.D1/*varianceDie*/, DamageType.BLUNT, AttackType.GRAPPLE, Charge.Never, parryPenalty, minRange, maxRange, handsRequired);
   }

   @Override
   public WeaponStyleAttackGrapple clone()
   {
      WeaponStyleAttackGrapple style = new WeaponStyleAttackGrapple(_skillType, _minSkill, _skillPenalty, _name,
                                                                   _parryPenalty, _minRange, _maxRange, _handsRequired);
      style.copyDataFrom(this);
      return style;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackGrapple)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      //WeaponStyleAttackGrapple otherStyle = (WeaponStyleAttackGrapple) other;
      return true;
   }

}
