/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;


public class WeaponStyleGrapplingParry extends WeaponStyleParry
{
   public WeaponStyleGrapplingParry(SkillType skillType, int minSkill, int skillPenalty, String name, int speedBase, int slowStr, int fastStr, double effectiveness, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, effectiveness, handsRequired);
   }
   @Override
   public boolean canDefendAgainstDamageType(DamageType damType, boolean isGrapple, short distance) {
      return isGrapple;
   }

   @Override
   public WeaponStyleGrapplingParry clone() {
      WeaponStyleGrapplingParry style =  new WeaponStyleGrapplingParry(skillType, minSkill, skillPenalty, name, speedBase, slowStr,
                                                                       fastStr, getEffectiveness(), handsRequired);
      style.copyDataFrom(this);
      return style;
   }
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleGrapplingParry)) {
         return false;
      }
      return super.equals(other);
      //WeaponStyleGrapplingParry otherStyle = (WeaponStyleGrapplingParry) other;
   }
}
