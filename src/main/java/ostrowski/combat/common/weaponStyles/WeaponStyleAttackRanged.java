/*
 * Created on July 6, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;


public abstract class WeaponStyleAttackRanged extends WeaponStyleAttack
{
   public short    rangeBase;
   public String[] preparationSteps;
   public WeaponStyleAttackRanged(SkillType skillType, int minSkill, int skillPenalty, String name,
                                  int damageMod, DiceSet varianceDice, DamageType damageType, AttackType attackType,
                                  int parryPenalty, int rangeBase, int handsRequired, String[] preparationSteps)
   {
      super(skillType, minSkill, skillPenalty, name, 0/*speedBase*/, -99/*slowStr*/, 99/*fastStr*/, damageMod,
            varianceDice, damageType, attackType, Charge.Never/*chargeType*/, parryPenalty, 2/*minRange*/, rangeBase*4/*maxRange*/, handsRequired);
      this.rangeBase = (short) rangeBase;
      this.preparationSteps = preparationSteps;
   }
   public String getPreparationStepName(String weaponName, int stepIndex) {
      if ((weaponName == null) || (weaponName.length() == 0)) {
         return preparationSteps[stepIndex].replaceAll("<weaponName> ", "");
      }
      return preparationSteps[stepIndex].replaceAll("<weaponName>", weaponName);
   }
   public byte getNumberOfPreparationSteps() {
      return (byte) preparationSteps.length;
   }

   public short getRangeBase()   { return rangeBase;}
   public short getDistanceForRange(RANGE range, byte adjustedStrength) {
      double adjustedRangeBase = rangeBase * Rules.getRangeAdjusterForAdjustedStr(adjustedStrength);
      return Rules.getDistanceForRange(range, adjustedRangeBase, isThrown());
   }

   public RANGE getRangeForDistance(short distanceInHexes, byte adjustedStrength) {
      if (distanceInHexes < getMinRange()) {
         return RANGE.OUT_OF_RANGE;
      }
      short rangeBase = (short) Math.round(this.rangeBase * Rules.getRangeAdjusterForAdjustedStr(adjustedStrength));
      return Rules.getRangeForWeapon(distanceInHexes, rangeBase, isThrown());
   }
   public short getMaxDistance(byte rangeDeterminingAttribute) {
      double rangeBase = this.rangeBase * Rules.getRangeAdjusterForAdjustedStr(rangeDeterminingAttribute);
      return (short) (rangeBase*4);
   }

   @Override
   public byte getMaxAttackActions() {
      return 1;
   }
   @Override
   public byte getMaxAimBonus() {
      return 2;
   }
   public String getPreparationStepsAsHTML(String weaponName)
   {
      StringBuilder sb = new StringBuilder();
      for (int i = preparationSteps.length - 1; i >= 0 ; i--) {
         String step = getPreparationStepName(weaponName, i).replaceAll(" ", "&nbsp;");
         sb.append(preparationSteps.length - i).append(".&nbsp;").append(step).append("<br>");
      }
      return sb.toString();
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttackRanged)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      WeaponStyleAttackRanged otherStyle = (WeaponStyleAttackRanged) other;
      if (rangeBase != otherStyle.rangeBase) {
         return false;
      }
      if (preparationSteps.length != otherStyle.preparationSteps.length) {
         return false;
      }
      for (int i = 0; i < preparationSteps.length ; i++) {
         if (preparationSteps[i].equals(otherStyle.preparationSteps[i])) {
            return false;
         }
      }
      return true;
   }

   @Override
   public void copyDataFrom(WeaponStyle source) {
      super.copyDataFrom(source);
      if (source instanceof WeaponStyleAttackRanged) {
         rangeBase = ((WeaponStyleAttackRanged)source).rangeBase;
         preparationSteps = ((WeaponStyleAttackRanged)source).preparationSteps;
      }
   }
}
