package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.protocol.SerializableObject;

/*
 * Created on May 6, 2006
 *
 */

/**
 * @author Paul
 *
 */
public class Skill extends SerializableObject implements Cloneable, Enums {

   public enum ArmsUsed {
      None,
      One,
      Both,
      OneOrTwo
   }

   private SkillType       _type;
   private byte            _level;

   public Skill()
   {
   }
   public Skill(SkillType type, byte level)
   {
      _type = type;
      _level = level;
   }
   public void setLevel(byte level)          {  _level = level;  }
   public byte getLevel()                    { return _level;}
   public SkillType getType()                { return _type;}
   public String getName()                   { return _type._name;}
   public ArmsUsed getArmUseCount()          { return _type._armUseCount; }
   public Attribute getAttributeBase()       { return _type._attributeBase; }
   public boolean isAdjustedForSize()        { return _type._isAdjustedForSize;  }
   public boolean isAdjustedForEncumbrance() { return _type._isAdjustedForEncumbrance; }

   public byte getPenaltyForEncumbranceLevel(byte encumbranceLevel) {
      if (_type._isAdjustedForEncumbrance) {
         return encumbranceLevel;
      }
      return 0;
   }

   @Override
   public String toString()
   {
      return "Skill: {" + _type._name + " = " + _level + '}';
   }

   @Override
   public Skill clone()
   {
      Skill newSkill = new Skill();
      newSkill._type = _type;
      newSkill.setLevel(_level);
      return newSkill;
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_type._name, out);
         writeToStream(_level, out);
         // The _attributeBase & _armUseCount don't need to be serialized,
         // because they are constant for a given skill (defined by its name).
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _type = SkillType.getSkillTypeByName(readString(in));
         _level = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
