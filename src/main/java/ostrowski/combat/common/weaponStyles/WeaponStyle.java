/*
 * Created on May 17, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.SkillRank;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.things.Weapon;

public abstract class WeaponStyle implements Enums//, Serializable
{
   public SkillType skillType;
   public String    name;
   public SkillRank minRank;
   public byte      skillPenalty;
   public byte      speedBase;
   public byte      slowStr;
   public byte      fastStr;
   public byte      handsRequired;
   public Weapon    weapon;
   public WeaponStyle(SkillType skillType, SkillRank minRank, int skillPenalty, String name, int speedBase, int slowStr, int fastStr, int handsRequired) {
      this.skillType = skillType;
      this.minRank = minRank;
      this.skillPenalty = (byte) skillPenalty;
      this.name = name;
      this.speedBase = (byte) speedBase;
      this.slowStr = (byte) slowStr;
      this.fastStr = (byte) fastStr;
      this.handsRequired = (byte) handsRequired;
   }

   public void setWeapon(Weapon weapon) {
      this.weapon = weapon;
   }
   public Weapon getWeapon() {
      return weapon;
   }

   public byte getSpeed(byte strengthAttr) {
      if (strengthAttr >= fastStr) {
         return (byte) (speedBase - 1);
      }
      if (strengthAttr <= slowStr) {
         return (byte) (speedBase + 1);
      }
      return speedBase;
   }

   public boolean isTwoHanded()
   {
      return (handsRequired == 2);
   }
   public SkillType getSkillType(){ return skillType;    }
   public String getName()        { return name;         }
   public SkillRank getMinRank()  { return minRank;      }
   public byte getSkillPenalty()  { return skillPenalty; }
   public byte getSpeedBase()     { return speedBase;    }
   public byte getSlowStr()       { return slowStr;      }
   public byte getFastStr()       { return fastStr;      }
   public byte getHandsRequired() { return handsRequired;}

   @Override
   abstract public WeaponStyle clone();

   public void copyDataFrom(WeaponStyle source) {
      skillType = source.skillType;
      name = source.name;
      minRank = source.minRank;
      skillPenalty = source.skillPenalty;
      speedBase = source.speedBase;
      slowStr = source.slowStr;
      fastStr = source.fastStr;
      handsRequired = source.handsRequired;
      weapon = source.weapon;
   }

   @Override
   public String toString()
   {
      return "name: " + name +
             ", skillName: " + skillType.getName() +
             ", speedBase: " + speedBase +
             ", slowStr: " + slowStr +
             ", fastStr: " + fastStr +
             ", hands: " + handsRequired;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyle)) {
         return false;
      }
      WeaponStyle otherStyle = (WeaponStyle) other;
      if (skillType != otherStyle.skillType) {
         return false;
      }
      if (!name.equals(otherStyle.name)) {
         return false;
      }
      if (minRank != otherStyle.minRank) {
         return false;
      }
      if (skillPenalty != otherStyle.skillPenalty) {
         return false;
      }
      if (speedBase != otherStyle.speedBase) {
         return false;
      }
      if (slowStr != otherStyle.slowStr) {
         return false;
      }
      if (fastStr != otherStyle.fastStr) {
         return false;
      }
      return handsRequired == otherStyle.handsRequired;
   }
}
