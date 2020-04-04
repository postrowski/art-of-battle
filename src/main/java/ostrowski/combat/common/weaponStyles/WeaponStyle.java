/*
 * Created on May 17, 2006
 *
 */
package ostrowski.combat.common.weaponStyles;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.things.Weapon;

public abstract class WeaponStyle implements Enums//, Serializable
{
   public SkillType  _skillType;
   public String _name;
   public byte   _minSkill;
   public byte   _skillPenalty;
   public byte   _speedBase;
   public byte   _slowStr;
   public byte   _fastStr;
   public byte   _handsRequired;
   public Weapon _weapon;
   public WeaponStyle (SkillType skillType, int minSkill, int skillPenalty, String name, int speedBase, int slowStr, int fastStr, int handsRequired) {
      _skillType     = skillType;
      _minSkill      = (byte) minSkill;
      _skillPenalty  = (byte) skillPenalty;
      _name          = name;
      _speedBase     = (byte) speedBase;
      _slowStr       = (byte) slowStr;
      _fastStr       = (byte) fastStr;
      _handsRequired = (byte) handsRequired;
   }

   public void setWeapon(Weapon weapon) {
      _weapon = weapon;
   }
   public Weapon getWeapon() {
      return _weapon;
   }

   public byte getSpeed(byte strengthAttr) {
      if (strengthAttr >= _fastStr) {
         return (byte) (_speedBase -1);
      }
      if (strengthAttr <= _slowStr) {
         return (byte) (_speedBase +1);
      }
      return _speedBase;
   }

   public boolean isTwoHanded()
   {
      return (_handsRequired == 2);
   }
   public SkillType getSkillType(){ return _skillType;    }
   public String getName()        { return _name;         }
   public byte getMinSkill()      { return _minSkill;     }
   public byte getSkillPenalty()  { return _skillPenalty; }
   public byte getSpeedBase()     { return _speedBase;    }
   public byte getSlowStr()       { return _slowStr;      }
   public byte getFastStr()       { return _fastStr;      }
   public byte getHandsRequired() { return _handsRequired;}

   @Override
   abstract public WeaponStyle clone();

   public void copyDataFrom(WeaponStyle source) {
      _skillType     = source._skillType;
      _name          = source._name;
      _minSkill      = source._minSkill;
      _skillPenalty  = source._skillPenalty;
      _speedBase     = source._speedBase;
      _slowStr       = source._slowStr;
      _fastStr       = source._fastStr;
      _handsRequired = source._handsRequired;
      _weapon        = source._weapon;
   }

   @Override
   public String toString()
   {
      String sb = "name: " + _name +
                  ", skillName: " + _skillType.getName() +
                  ", speedBase: " + _speedBase +
                  ", slowStr: " + _slowStr +
                  ", fastStr: " + _fastStr +
                  ", hands: " + _handsRequired;
      return sb;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof WeaponStyle)) {
         return false;
      }
      WeaponStyle otherStyle = (WeaponStyle) other;
      if (_skillType != otherStyle._skillType) {
         return false;
      }
      if (!_name.equals(otherStyle._name)) {
         return false;
      }
      if (_minSkill != otherStyle._minSkill) {
         return false;
      }
      if (_skillPenalty != otherStyle._skillPenalty) {
         return false;
      }
      if (_speedBase != otherStyle._speedBase) {
         return false;
      }
      if (_slowStr != otherStyle._slowStr) {
         return false;
      }
      if (_fastStr != otherStyle._fastStr) {
         return false;
      }
      return _handsRequired == otherStyle._handsRequired;
   }
}
