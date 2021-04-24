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
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IMissileSpell;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;


public abstract class MissileMageSpell extends MageSpell implements IMissileSpell, ICastInBattle
{
   final byte       damageBase;
   final DieType    damageDieType;
   final DamageType damageType;
   final byte       damagePerPower;
   final short      missileRangeBase;

   @SuppressWarnings("rawtypes")
   public MissileMageSpell(String name, Class[] prerequisiteSpells, SkillType[] skillTypes,
                           byte damageBase, byte damagePerPower, DieType damageDieType, DamageType damageType, short missileRangeBase) {
      super(name, prerequisiteSpells, skillTypes);
      this.damageBase = damageBase;
      this.damagePerPower = damagePerPower;
      this.damageDieType = damageDieType;
      this.damageType = damageType;
      this.missileRangeBase = missileRangeBase;
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
      return "The '" + getName() + "' spell creates a " + getName() + " from the caster's hand, which immediately travels towards the target." +
             " The missile may be dodged or blocked as any missile weapon. The attack roll is the same as the casting roll." +
             "<br/>The damage done by the missile is " + damageBase + " + " + damagePerPower + "*(spell power) + " +
             DiceSet.getSingleDie(damageDieType) + " in " + damageType.shortname + " damage." +
             " If the caster is larger or smaller than human, the caster's racial build adjustment is added to the damage done.";
   }

   @Override
   public String getMissileWeaponName()            {return name;}
   @Override
   public int    getMissileWeaponSize()            {return 0;}
   @Override
   public int    getMissileWeaponSkillPenalty()    {return 0;}
   @Override
   public int    getHandsRequired()                {return 1;}

   @Override
   public String explainDamage() {
      StringBuilder sb = new StringBuilder();
      sb.append(damageBase).append(" + ").append(damagePerPower).append(" * ").append(getPower()).append(" (power points) + ").append(getDamageDice());
      if (caster != null) {
         byte size = caster.getRace().getBuildModifier();
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
      if (caster != null) {
         sizeAdjust = caster.getRace().getBuildModifier();
      }
      return (byte) (damageBase + (getPower() * damagePerPower) + sizeAdjust);
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
                               damageDieType,
                               damageType,
                               missileRangeBase,
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
      //return DiceSet.getSingleDie(damageDieType).addBonus(getCaster().getRace().getBuildModifier());
      return DiceSet.getSingleDie(damageDieType);
   }
   @Override
   public DamageType getDamageType() {
      return damageType;
   }
   @Override
   public short getRangeBase() {
      return missileRangeBase;
   }
}
