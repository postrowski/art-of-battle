package ostrowski.combat.common.spells.priest;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.spells.IMissileSpell;
import ostrowski.combat.common.spells.priest.elemental.SpellBreatheFire;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.server.Battle;

public abstract class PriestMissileSpell extends PriestSpell implements IMissileSpell
{
   DieType _damageDiceType;
   short _rangeBase;
   DamageType _damageType;
   String _missileWeaponName;

   public PriestMissileSpell(String name, Class< ? extends IPriestGroup> group, int affinity,
                             DieType damageDiceType, short rangeBase, DamageType damageType, String missileWeaponName) {
      super(name, group, affinity);
      _damageDiceType = damageDiceType;
      _rangeBase = rangeBase;
      _damageType = damageType;
      _missileWeaponName = missileWeaponName;
   }

   @Override
   public DiceSet getDamageDice() {
      return getDamageDice(_effectivePower);
   }
   private DiceSet getDamageDice(int power) {
      DiceSet damage = DiceSet.getGroupDice(_damageDiceType, power);
      Character caster = getCaster();
      if (caster != null) {
         return damage.addBonus(getCaster().getRace().getBuildModifier());
      }
      return damage;
   }
   @Override
   public short getRangeBase() {
      return _rangeBase;
   }
   @Override
   public DamageType getDamageType() { return _damageType; }



   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append(describeSpellPreamble());
      sb.append(" Anyone standing in the line of fire may dodge or block the attack.");
      sb.append(" The attack has a base range of ").append(getRangeBase()).append(", so the defender's TN will be modified by range adjustment.");
      sb.append(" The caster rolls a d10± to attack each and every target in the line of fire.");
      sb.append(" To each d10± (one for each target in the line), the caster adds their Divine Affinity level and their DEX attribute, then subtracts their combined pain and wound penalties to yield the to-hit roll.");
      sb.append(" Each to-hit roll is then compared against the defender's TN.");
      sb.append(" If the roll is greater than or equal to the defender's TN, they will suffer ").append(getDamageDice(1));
      sb.append(" ").append(getDamageType().shortname);
      sb.append(" damage for each point of effective power in the spell, plus 1 point bonus damage for every two full points the attack rolled over the defender's TN.");
//      if ((getSpecialDamageModifier().getBits() & SpecialDamage.MOD_NO_BUILD) != 0) {
//         sb.append(" Racial Build Adjuster of the target does not factor into the defender's build.");
//      }
      sb.append(" As with all ranged priest spells, each range increment reduces the effective power of the spell by one point.");
      sb.append(" So a target in Point-Blank range will suffer the full power of the spell, a target at short range (");
      sb.append(getRangeBase()/2).append(" to ").append(getRangeBase()).append(" hexes away) will take 1 less die damage.");
      sb.append(" So this spell cast with 3 power points does no damage beyond medium range (2 times range base, ").append(getRangeBase()*2).append(" hexes.)");
      sb.append(" As with all ranged attack, the maximum range of the spell is still 4 times the adjusted base range, for a maximum of ");
      sb.append(getRangeBase() * 4).append(" hexes, regardless of the effective power of the spell.");
      sb.append(" If using the optional spell range rule, spell range may be affected by the SOC attribute and racial size adjuster of the caster.");
      sb.append(" If the optional range adjustment rule is not used, the GM should still adjust the base range for extreme race sizes.");
      sb.append(" So, with the optional rule, a size -29 fairy with a SOC of 4 would lookup -25 on the range adjustment chart, resulting in an adjusted base range of ");
      sb.append(Math.round(Rules.getRangeAdjusterForAdjustedStr((byte) -25) * getRangeBase())).append(".");
      sb.append(" A size 8 ");
      if (this instanceof SpellBreatheFire) {
         sb.append("Fire");
      }
      else {
         sb.append("Water");
      }
      sb.append(" Elemental with a SOC of 2 would lookup +10 on the range adjustment chart, resulting in an adjusted base range of ");
      sb.append(Math.round(Rules.getRangeAdjusterForAdjustedStr((byte) 10) * getRangeBase())).append(".");
      return sb.toString();
   }

   abstract public String describeSpellPreamble();

   @Override
   public boolean isDefendable() {
      return true;
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public String explainDamage() {
      return getDamageDice().toString() + " (" + getEffectivePower() + " effective power points) ";
   }

   @Override
   public int getHandsRequired() {
      return 0;
   }

   @Override
   public MissileWeapon getMissileWeapon() {
      // damType, rngBase, hands,      preparation steps (last to first)
      return new MissileWeapon(getMissileWeaponSize(), getCaster().getRace(),
                               0/*lbs*/,  0/*cost*/,
                               _missileWeaponName,
                               null/*skill type*/, // TODO: how to convey the spell's skill level for use in the attack?
                               0 /*minimum skill level*/,
                               getMissileWeaponSkillPenalty(),
                               getSpellDamageBase() + getCaster().getRace().getBuildModifier(),
                               getDamageDice()/*variance*/,
                               getDamageType(),
                               getRangeBase(),
                               getHandsRequired(),
                               new String[] {});
   }

   @Override
   protected DiceSet getCastDice(RequestAction attack, byte distanceModifiedTN,
                               byte distanceModifiedPower, short distanceInHexes, RANGE range,
                               Battle battle, StringBuilder sbDescription) {
      return DiceSet.getSingleDie(DieType.D10);
   }

   @Override
   public DiceSet getCastDice(byte actionsUsed, RANGE range) {
      return DiceSet.getSingleDie(DieType.D10);
   }
   @Override
   public Attribute getCastingAttribute() {
      return Attribute.Dexterity;
   }

   @Override
   public int getMissileWeaponSize() { return 0;}
   @Override
   public int getMissileWeaponSkillPenalty() { return 0;}

   @Override
   public byte getSpellDamageBase() {return 0; }
   @Override
   public WeaponStyleAttackRanged getWeaponStyleAttackRanged() {
      return (WeaponStyleAttackRanged) getMissileWeapon().getAttackStyle(0);
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
   public short getMinRange(Character caster) {
      return 1;
   }
   @Override
   public short getMaxRange(Character caster) {
      setCaster(caster);
      WeaponStyleAttackRanged rangedAttack = getWeaponStyleAttackRanged();
      byte rangeDeterminingAttribute = caster.getAttributeLevel(getCastingAttribute());
      rangeDeterminingAttribute += caster.getRace().getBuildModifier();
      return rangedAttack.getMaxDistance(rangeDeterminingAttribute);
   }
   @Override
   public RANGE getRange(short distanceInHexes) {
      WeaponStyleAttackRanged rangedAttack = getWeaponStyleAttackRanged();
      byte rangeDeterminingAttribute = getCaster().getAttributeLevel(getCastingAttribute());
      rangeDeterminingAttribute += getCaster().getRace().getBuildModifier();
      return rangedAttack.getRangeForDistance(distanceInHexes, rangeDeterminingAttribute);
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
   public String getMissileWeaponName() {
      return _missileWeaponName;
   }

}
