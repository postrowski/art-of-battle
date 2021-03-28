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

   private SkillType type;
   private byte      level;

   public Skill()
   {
   }
   public Skill(SkillType type, byte level)
   {
      this.type = type;
      this.level = level;
   }
   public void setLevel(byte level)          {  this.level = level;  }
   public byte getLevel()                    { return level;}
   public SkillType getType()                { return type;}
   public String getName()                   { return type.name;}
   public ArmsUsed getArmUseCount()          { return type.armUseCount; }
   public Attribute getAttributeBase()       { return type.attributeBase; }
   public boolean isAdjustedForSize()        { return type.isAdjustedForSize;  }
   public boolean isAdjustedForEncumbrance() { return type.isAdjustedForEncumbrance; }

   public byte getPenaltyForEncumbranceLevel(byte encumbranceLevel) {
      if (type.isAdjustedForEncumbrance) {
         return encumbranceLevel;
      }
      return 0;
   }

   @Override
   public String toString()
   {
      return "Skill: {" + type.name + " = " + level + '}';
   }

   @Override
   public Skill clone()
   {
      Skill newSkill = new Skill();
      newSkill.type = type;
      newSkill.setLevel(level);
      return newSkill;
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(type.name, out);
         writeToStream(level, out);
         // The attributeBase & armUseCount don't need to be serialized,
         // because they are constant for a given skill (defined by its name).
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         type = SkillType.getSkillTypeByName(readString(in));
         level = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
