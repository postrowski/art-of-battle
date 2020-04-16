/*
 * Created on Aug 15, 2006
 *
 */
package ostrowski.combat.common.things;

import org.eclipse.swt.graphics.RGB;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackThrown;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.BattleTerminatedException;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Comparator;

public abstract class Thing extends SerializableObject implements Cloneable, Enums
{
   public String _name           = "";
   public double _weight         = 0;
   public int    _cost           = 0;
   private Race  _racialBase     = null;
   public byte   _passiveDefense = 0;

   @Override
   public String toString() {
       return _name + ", $" + _cost + ", " + _weight + " lbs, pd=" +_passiveDefense;
   }

   public Thing() {
   }

   public Thing (String name, Race racialBase, int cost, double weight, byte passiveDefense) {
      _name           = name;
      _racialBase     = racialBase;
      _cost           = cost;
      _weight         = weight;
      _passiveDefense = passiveDefense;
   }

   public Race getRacialBase() { return _racialBase; }
   public void setRacialBase(Race racialBase) { _racialBase = racialBase; }
   public String getName() { return _name; }
   public int    getCost() { return _cost; }
   public double getWeight() { return _weight; }
   public double getAdjustedWeight() {
      if (_weight == 0) {
         return 0;
      }
      double weightForHuman = _weight;
      if (!isSizeAdjusted()) {
         return weightForHuman;
      }
      double adjWeight = weightForHuman;
      if (_racialBase == null) {
         DebugBreak.debugBreak(getName() + " has no racial base.");
      }
      else {
         if (_racialBase.getName().equals(Race.NAME_Skeleton)) {
            return weightForHuman;
         }
         if (_racialBase.getName().equals(Race.NAME_Centaur)) {
            if ((this instanceof Weapon) || (this instanceof Shield)) {
               return weightForHuman;
            }
         }
         adjWeight = (weightForHuman  * _racialBase._aveWeight) / Race.HUMAN_AVE_WEIGHT;
      }
      return Rules.roundWeight(adjWeight);
   }
   public byte getPassiveDefense()   { return _passiveDefense; }
   public abstract String getActiveDefenseName();
   public abstract List<SkillType> getDefenseSkillTypes();
   public abstract byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands,
                                             DamageType damType, boolean isGrappleAttack, short distance);
   public boolean canDefendAgainstRangedWeapons() { return false;}
   public boolean isReal() { return _cost > 0;}
   public static Thing getThing(String thingName, Race racialBase) {
      return getThing(thingName, true/*allowTool*/, racialBase);
   }
   public static Thing getThing(String thingName, boolean allowTool, Race racialBase) {
      if ((thingName == null) || (thingName.length() == 0)) {
         return null;
      }

      Weapon weap = Weapons.getWeapon(thingName, racialBase);
      if ((weap != null) &&
          (weap.isReal() || thingName.equalsIgnoreCase(Weapon.NAME_Punch) ||
                            thingName.equalsIgnoreCase(Weapon.NAME_KarateKick) ||
                            thingName.equalsIgnoreCase(Weapon.NAME_HeadButt))) {
         return weap;
      }

      Shield shield = Shield.getShield(thingName, racialBase);
      if ((shield != null) && (shield.isReal())) {
         return shield;
      }

      Armor armor = Armor.getArmor(thingName, racialBase);
      if (armor.isReal()) {
         return armor;
      }

      Potion potion = Potion.getPotion(thingName, racialBase);
      if (potion != null) {
         return potion;
      }

      if (thingName.endsWith(" Fireball spell")) {
         int power = 0;
         if (thingName.startsWith("Small")) {
            power = 1;
         }
         else if (thingName.startsWith("Average")) {
            power = 2;
         }
         else if (thingName.startsWith("Large")) {
            power = 3;
         }
         else if (thingName.startsWith("Very large")) {
            power = 4;
         }
         else if (thingName.startsWith("Huge")) {
            power = 5;
         }
         else if (thingName.startsWith("Level-")) {
            power = Integer.parseInt(thingName.substring("Level-".length(), "Level-".length()+1));
         }
         int damage = 5 + (power * 5);
         WeaponStyleAttack style = new WeaponStyleAttackThrown(0/*minSkill*/, 0/*penalty*/, damage, DieType.D6, DamageType.FIRE, 12/*rangeBase*/,  1/*hands*/);
         // Give it a non-zero cost, so it will not quality as not being real
         return new Weapon(-power/*size*/, racialBase, 0/*lbs*/, 1/*$*/, thingName, new WeaponStyle[] {style});
      }

      if (allowTool) {
         return new Tool(thingName, racialBase);
      }
      return null;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _name  = readString(in);
         String race = readString(in);
         Gender gender = Gender.getByName(readString(in));
         _racialBase = Race.getRace(race, gender);
         Thing source = Thing.getThing(_name, _racialBase);
         copyData(source);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_name, out);
         writeToStream(_racialBase.getName(), out);
         writeToStream(_racialBase.getGender()._name, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   public void copyData(Thing source)
   {
      if (source != null) {
         _name           = source._name;
         _cost           = source._cost;
         _weight         = source._weight;
         _passiveDefense = source._passiveDefense;
      }
   }
   public boolean equals(Thing other)
   {
      if (other == null) {
         return false;
      }
      if (!_name.equals(other._name)) {
         return false;
      }
      if (_cost           != other._cost) {
         return false;
      }
      if (_weight         != other._weight) {
         return false;
      }
      return _passiveDefense == other._passiveDefense;
   }

   @Override
   public abstract Thing clone();
   public abstract String getDefenseName(boolean tensePast, Character defender);

   public boolean canBeApplied() {
      return false;
   }
   public String getApplicationName() {
      return null;
   }

   public DrawnObject drawThing(int size, RGB foreground, RGB background) {
      return null;
   }

   /**
    * Returns true is the item is consumed when applied (thus disappearing)
    * @param character
    * @param arena
    */
   @SuppressWarnings("unused")
   public boolean apply(Character character, Arena arena) throws BattleTerminatedException {
      if (!canBeApplied()) {
         DebugBreak.debugBreak();
      }
      return false;
   }

   public byte getHandUseagePenalties(LimbType limbType, Character wielder, SkillType skillType) {
      return Rules.getHandUsePenalty(limbType, wielder.getRace().getArmCount());
   }

   public boolean isSizeAdjusted() {
      return true;
   }

   public static final Comparator<Thing> comparatorByWeightHighToLow = new Comparator<>() {
      @Override
      public int compare(Thing o1, Thing o2) {
         // sort highest to lowest by multiplying by -1
         return Double.compare(o1.getWeight(), o2.getWeight()) * -1;
      }
   };

   public static final Comparator<Thing> comparatorByCostHighToLow = new Comparator<>() {
      @Override
      public int compare(Thing o1, Thing o2) {
         // sort highest to lowest by multiplying by -1
         return Double.compare(o1.getCost(), o2.getCost()) * -1;
      }
   };


}
