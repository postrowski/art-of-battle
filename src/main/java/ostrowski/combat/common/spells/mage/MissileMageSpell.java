/*
 * Created on May 18, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IMissileSpell;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;


public abstract class MissileMageSpell extends MageSpell implements IMissileSpell, ICastInBattle
{
   byte _damageBase;
   DieType _damageDieType;
   DamageType _damageType;
   byte _damagePerPower;
   short _missileRangeBase;

   @SuppressWarnings("rawtypes")
   public MissileMageSpell(String name, Class[] prerequisiteSpells, MageCollege[] colleges,
                           byte damageBase, byte damagePerPower, DieType damageDieType, DamageType damageType, short missileRangeBase) {
      super(name, prerequisiteSpells, colleges);
      _damageBase       = damageBase;
      _damagePerPower   = damagePerPower;
      _damageDieType    = damageDieType;
      _damageType       = damageType;
      _missileRangeBase = missileRangeBase;
   }

   @Override
   public boolean isDefendable() {
      return true;
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '"+getName()+"' spell creates a "+getName()+" from the caster's hand, which immediately travels towards the target." +
              " The missile may be dodged or blocked as any missile weapon. The attack roll is the same as the casting roll." +
              "<br/>The damage done by the missile is " + _damageBase + " + " + _damagePerPower +"*(spell power) + " +
              DiceSet.getSingleDie(_damageDieType).toString() + " in " + _damageType.shortname + " damage." +
              " If the caster is larger or smaller than human, the caster's racial build adjustment is added to the damage done.";
   }

   @Override
   public String getMissileWeaponName()            {return _name;}
   @Override
   public int    getMissileWeaponSize()            {return 0;}
   @Override
   public int    getMissileWeaponSkillPenalty()    {return 0;}
   @Override
   public int    getHandsRequired()                {return 1;}

   @Override
   public String explainDamage() {
      StringBuilder sb = new StringBuilder();
      sb.append(_damageBase).append(" + ").append(_damagePerPower).append(" * ").append(getPower()).append(" (power points) + ").append(getDamageDice().toString());
      if (_caster != null) {
         byte size = _caster.getRace().getBuildModifier();
         sb.append(" ");
         if (size > 0) {
            sb.append("+");
         }
         if (size != 0) {
            sb.append(size).append(" (for racial size)");
         }
      }
      return sb.toString();
   }

   @Override
   public byte   getSpellDamageBase() {
      byte sizeAdjust = 0;
      if (_caster != null) {
         sizeAdjust = _caster.getRace().getBuildModifier();
      }
      return (byte) (_damageBase + (getPower() * _damagePerPower) + sizeAdjust);
   }
   @Override
   public MissileWeapon getMissileWeapon() {
      // damType, rngBase, hands,      preparation steps (last to first)
      return new MissileWeapon(getMissileWeaponSize(), getCaster().getRace(),
                               0/*lbs*/,  0/*cost*/,
                               getMissileWeaponName(),
                               null/*skill type*/, // TODO: how to convey the spell's skill level for use in the attack?
                               0 /*minimum skill level*/,
                               getMissileWeaponSkillPenalty(),
                               getSpellDamageBase(),
                               _damageDieType,
                               _damageType,
                               _missileRangeBase,
                               getHandsRequired(),
                               new String[] {});
   }
   @Override
   public WeaponStyleAttackRanged getWeaponStyleAttackRanged() {
      return (WeaponStyleAttackRanged) getMissileWeapon().getAttackStyle(0);
   }

   @Override
   public short getMaxRange(Character caster) {
      byte rangeDeterminingAttribute = caster.getAttributeLevel(getCastingAttribute());
      setCaster(caster);
      WeaponStyleAttackRanged rangedAttack = getWeaponStyleAttackRanged();
      return rangedAttack.getMaxDistance(rangeDeterminingAttribute);
   }
   @Override
   public short getMinRange(Character caster) {
      return 0;
   }

   @Override
   public RANGE getRange(short distanceInHexes) {
      Character caster = getCaster();
      byte rangeDeterminingAttribute = caster.getAttributeLevel(getCastingAttribute());
      rangeDeterminingAttribute += caster.getRace().getBuildModifier();
      WeaponStyleAttackRanged rangedAttack = getWeaponStyleAttackRanged();
      return rangedAttack.getRangeForDistance(distanceInHexes, rangeDeterminingAttribute);
   }
   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return Rules.getRangeDefenseAdjustmentToPD(getRange(distanceInHexes));
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return Rules.getRangeDefenseAdjustmentPerAction(getRange(distanceInHexes));
   }
   @Override
   public byte getRangeTNAdjustment(short distanceInHexes) {
      // The Range TN is already accounted for in the defender's TN.
      // so this should return 0
      //return (byte) (0-Rules.getRangeToHitAdjustment(getRange(distanceInHexes)));
      return 0;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   @Override
   public DiceSet getDamageDice() {
      //return DiceSet.getSingleDie(_damageDieType).addBonus(getCaster().getRace().getBuildModifier());
      return DiceSet.getSingleDie(_damageDieType);
   }
   @Override
   public DamageType getDamageType() {
      return _damageType;
   }
   @Override
   public short getRangeBase() {
      return _missileRangeBase;
   }
}
