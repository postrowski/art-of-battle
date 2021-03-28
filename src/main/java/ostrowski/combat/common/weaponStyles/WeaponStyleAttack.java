/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.AttackType;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.Weapon;

public abstract class WeaponStyleAttack extends WeaponStyle
{
   public enum Charge {
      Never,
      With4Legs,
      WhenMounted,
      Anytime
   }

   protected byte       damageMod;
   protected DiceSet    varianceDice;
   protected DamageType damageType;
   protected AttackType attackType;
   protected byte       parryPenalty;
   protected Charge     chargeType;
   protected short      minRange;
   protected short      maxRange;

   public WeaponStyleAttack(SkillType skillType, int minSkill, int skillPenalty, String name,
                            int speedBase, int slowStr, int fastStr, int damageMod,
                            DiceSet varianceDice, DamageType damageType, AttackType attackType,
                            Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, handsRequired);
      this.damageMod = (byte) damageMod;
      this.varianceDice = varianceDice;
      this.damageType = damageType;
      this.attackType = attackType;
      this.parryPenalty = (byte) parryPenalty;
      this.chargeType = chargeType;
      this.minRange = (short) minRange;
      this.maxRange = (short) maxRange;
   }
   public DiceSet getVarianceDie() {
      return varianceDice;
   }
   public String getDamageString(byte damBase) {
      return "" + (damageMod + damBase) + " + " + getVarianceDie() + " " + damageType.shortname;
   }
   public byte getDamage(byte damBase) {
      return (byte) (damageMod + damBase);
   }
   public AttackType getAttackType() {
      return attackType;
   }
   public byte getParryPenalty() {
      return parryPenalty;
   }
   public DamageType getDamageType() {
      return damageType;
   }
   public String getDamageTypeString() {
      return damageType.fullname;
   }
   public byte getDamageMod() {
      return damageMod;
   }

   @Override
   public String toString()
   {
      return super.toString() +
             ", damageMod: " + damageMod +
             ", varianceDie: " + varianceDice +
             ", damageType: " + damageType +
             ", attackType: " + attackType +
             ", parryPenalty: " + parryPenalty +
             ", chargeType: " + chargeType;
   }
   public short getMaxRange()
   {
      if (!isRanged()) {
         if (weapon != null) {
            Race racialBase = weapon.getRacialBase();
            if (racialBase != null) {
               byte buildModifier = racialBase.getBuildModifier();
               short adjMaxRange = Rules.adjustMeleeRangeForSize(maxRange, buildModifier);
               // don't let the maximum range drop below 1 due to size adjustment
               if ((adjMaxRange == 0) && (maxRange > 0)) {
                  return 1;
               }
               return adjMaxRange;
            }
         }
      }
      return maxRange;
   }
   public short getMinRange()
   {
      if (!isRanged()) {
         if (weapon != null) {
            Race racialBase = weapon.getRacialBase();
            if (racialBase != null) {
               byte buildModifier = racialBase.getBuildModifier();
               return Rules.adjustMeleeRangeForSize(minRange, buildModifier);
            }
         }
      }
      return minRange;
   }
   public boolean isThrown() {
      return attackType == AttackType.THROW;
   }
   public boolean isMissile() {
      return attackType == AttackType.MISSILE;
   }
   public boolean isRanged() {
      return isThrown() || isMissile();
   }

   public byte getMaxAttackActions() {
      return 3;
   }
   public byte getMaxAimBonus() {
      return 0;
   }
   public boolean canAttack(Character character, Weapon weap, Limb useFromLimb) {
      if (getMinSkill() > character.getSkillLevel(getSkillType(), null/*useLimb*/,
                                                  false/*sizeAdjust*/, false/*adjustForEncumbrance*/, true/*adjustForHolds*/)) {
         return false;
      }
      if (weap.isReal()) {
         return true;
      }
      if (useFromLimb.isCrippled()) {
         return false;
      }
      // unarmed combat skills that require two hands should only show for the right hand
      if (isTwoHanded()) {
         Limb otherLimb = character.getLimb(useFromLimb.limbType.getPairedType());
         if ((otherLimb == null) || (otherLimb.isCrippled())) {
            return false;
         }
         return useFromLimb.getLocationSide() == Side.RIGHT;
      }
      return true;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyleAttack)) {
         return false;
      }
      if (!super.equals(other)) {
         return false;
      }
      WeaponStyleAttack otherStyle = (WeaponStyleAttack) other;
      if (this.damageMod != otherStyle.damageMod) {
         return false;
      }
      if (!this.varianceDice.equals(otherStyle.varianceDice)) {
         return false;
      }
      if (this.damageType != otherStyle.damageType) {
         return false;
      }
      if (this.attackType != otherStyle.attackType) {
         return false;
      }
      if (this.parryPenalty != otherStyle.parryPenalty) {
         return false;
      }
      if (this.chargeType != otherStyle.chargeType) {
         return false;
      }
      if (this.minRange != otherStyle.minRange) {
         return false;
      }
      return this.maxRange == otherStyle.maxRange;
   }
   public boolean canCharge(boolean isMounted, boolean hasFourLegs) {
      if (chargeType == Charge.Never) {
         return false;
      }
      if (chargeType == Charge.Anytime) {
         return true;
      }
      if ((chargeType == Charge.WhenMounted) && !isMounted) {
         return false;
      }
      return (chargeType != Charge.With4Legs) || hasFourLegs;
   }
   @Override
   public void copyDataFrom(WeaponStyle source) {
      super.copyDataFrom(source);
      if (source instanceof WeaponStyleAttack) {
         WeaponStyleAttack sourceAttack = (WeaponStyleAttack) source;
         damageMod = sourceAttack.damageMod;
         varianceDice = new DiceSet(sourceAttack.varianceDice.toString());
         damageType = sourceAttack.damageType;
         attackType = sourceAttack.attackType;
         parryPenalty = sourceAttack.parryPenalty;
         chargeType = sourceAttack.chargeType;
         minRange = sourceAttack.minRange;
         maxRange = sourceAttack.maxRange;
      }
   }
}
