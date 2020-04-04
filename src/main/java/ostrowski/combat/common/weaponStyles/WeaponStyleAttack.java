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

   protected byte    _damageMod;
   protected DiceSet _varianceDice;
   protected DamageType    _damageType;
   protected AttackType    _attackType;
   protected byte    _parryPenalty;
   protected Charge  _chargeType;
   protected short   _minRange;
   protected short   _maxRange;

   public WeaponStyleAttack(SkillType skillType, int minSkill, int skillPenalty, String name,
                            int speedBase, int slowStr, int fastStr, int damageMod,
                            DiceSet varianceDice, DamageType damageType, AttackType attackType,
                            Charge chargeType, int parryPenalty, int minRange, int maxRange, int handsRequired)
   {
      super(skillType, minSkill, skillPenalty, name, speedBase, slowStr, fastStr, handsRequired);
      _damageMod     = (byte) damageMod;
      _varianceDice  = varianceDice;
      _damageType    = damageType;
      _attackType    = attackType;
      _parryPenalty  = (byte) parryPenalty;
      _chargeType    = chargeType;
      _minRange      = (short) minRange;
      _maxRange      = (short) maxRange;
   }
   public DiceSet getVarianceDie() {
      return _varianceDice;
   }
   public String getDamageString(byte damBase) {
      return "" + (_damageMod + damBase) + " + " + getVarianceDie() + " " + _damageType.shortname;
   }
   public byte getDamage(byte damBase) {
      return (byte) (_damageMod + damBase);
   }
   public AttackType getAttackType() {
      return _attackType;
   }
   public byte getParryPenalty() {
      return _parryPenalty;
   }
   public DamageType getDamageType() {
      return _damageType;
   }
   public String getDamageTypeString() {
      return _damageType.fullname;
   }
   public byte getDamageMod() {
      return _damageMod;
   }

   @Override
   public String toString()
   {
      String sb = super.toString() +
                  ", damageMod: " + _damageMod +
                  ", varianceDie: " + _varianceDice +
                  ", damageType: " + _damageType +
                  ", attackType: " + _attackType +
                  ", parryPenalty: " + _parryPenalty +
                  ", chargeType: " + _chargeType;
      return sb;
   }
   public short getMaxRange()
   {
      if (!isRanged()) {
         if (_weapon != null) {
            Race racialBase = _weapon.getRacialBase();
            if (racialBase != null) {
               byte buildModifier = racialBase.getBuildModifier();
               short adjMaxRange = Rules.adjustMeleeRangeForSize(_maxRange, buildModifier);
               // don't let the maximum range drop below 1 due to size adjustment
               if ((adjMaxRange == 0) && (_maxRange > 0)) {
                  return 1;
               }
               return adjMaxRange;
            }
         }
      }
      return _maxRange;
   }
   public short getMinRange()
   {
      if (!isRanged()) {
         if (_weapon != null) {
            Race racialBase = _weapon.getRacialBase();
            if (racialBase != null) {
               byte buildModifier = racialBase.getBuildModifier();
               return Rules.adjustMeleeRangeForSize(_minRange, buildModifier);
            }
         }
      }
      return _minRange;
   }
   public boolean isThrown() {
      return _attackType == AttackType.THROW;
   }
   public boolean isMissile() {
      return _attackType == AttackType.MISSILE;
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
         Limb otherLimb = character.getLimb(useFromLimb._limbType.getPairedType());
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
      if (this._damageMod != otherStyle._damageMod) {
         return false;
      }
      if (!this._varianceDice.equals(otherStyle._varianceDice)) {
         return false;
      }
      if (this._damageType != otherStyle._damageType) {
         return false;
      }
      if (this._attackType != otherStyle._attackType) {
         return false;
      }
      if (this._parryPenalty != otherStyle._parryPenalty) {
         return false;
      }
      if (this._chargeType != otherStyle._chargeType) {
         return false;
      }
      if (this._minRange != otherStyle._minRange) {
         return false;
      }
      return this._maxRange == otherStyle._maxRange;
   }
   public boolean canCharge(boolean isMounted, boolean hasFourLegs) {
      if (_chargeType == Charge.Never) {
         return false;
      }
      if (_chargeType == Charge.Anytime) {
         return true;
      }
      if ((_chargeType == Charge.WhenMounted) && !isMounted) {
         return false;
      }
      return (_chargeType != Charge.With4Legs) || hasFourLegs;
   }
   @Override
   public void copyDataFrom(WeaponStyle source) {
      super.copyDataFrom(source);
      if (source instanceof WeaponStyleAttack) {
         WeaponStyleAttack sourceAttack = (WeaponStyleAttack) source;
         _damageMod    = sourceAttack._damageMod;
         _varianceDice = new DiceSet(sourceAttack._varianceDice.toString());
         _damageType   = sourceAttack._damageType;
         _attackType   = sourceAttack._attackType;
         _parryPenalty = sourceAttack._parryPenalty;
         _chargeType   = sourceAttack._chargeType;
         _minRange     = sourceAttack._minRange;
         _maxRange     = sourceAttack._maxRange;
      }
   }
}
