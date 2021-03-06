/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;


public class WeaponStyleParry extends WeaponStyle
{
   double _effectiveness;
   public WeaponStyleParry(SkillType skillType, int minSkill, int skillPenalty, String name, int speedBase, int slowStr, int fastStr, double effectiveness, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, handsRequired);
      _effectiveness = effectiveness;
   }
   public double getEffectiveness() { return _effectiveness; }
   public boolean canDefendAgainstDamageType(DamageType damType, boolean isGrapple, short distance) {
      if (isGrapple) {
         return false;
      }
      if (damType == DamageType.BLUNT) {
         return true;
      }
      if (damType == DamageType.IMP) {
         // Aikido or Karate skill can defend against impaling attacks, but only within 2 hexes
         return ((getSkillType() == SkillType.Aikido) ||
                 (getSkillType() == SkillType.Karate)) && (distance < 3);
      }
      if (damType == DamageType.CUT) {
         // Aikido skill can defend against impaling attacks, but only within 2 hexes
         return (getSkillType() == SkillType.Aikido) && (distance < 3);
      }
      if (damType == DamageType.ELECTRIC) {
         return false;
      }
      return damType != DamageType.FIRE;
   }

   @Override
   public WeaponStyleParry clone() {
      WeaponStyleParry style =  new WeaponStyleParry(_skillType, _minSkill, _skillPenalty, _name, _speedBase, _slowStr,
                                                     _fastStr, _effectiveness, _handsRequired);
      style.copyDataFrom(this);
      return style;
   }
   @Override
   public void copyDataFrom(WeaponStyle source) {
      super.copyDataFrom(source);
      if (source instanceof WeaponStyleParry) {
         _effectiveness = ((WeaponStyleParry)source)._effectiveness;
      }
   }
   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleParry)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      WeaponStyleParry otherStyle = (WeaponStyleParry) other;
      return _effectiveness == otherStyle._effectiveness;
   }
}
