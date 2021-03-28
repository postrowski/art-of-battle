/*
 * Created on Jun 27, 2007
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MageCollege extends SerializableObject implements Enums
{
   public static List<String>                 nameList = new ArrayList<>();
   public static HashMap<String, MageCollege> colleges = new HashMap<>();

   private             String      name;
   private             byte        level;
   public static final MageCollege FIRE        = new MageCollege("Fire");
   public static final MageCollege WATER       = new MageCollege("Water");
   public static final MageCollege EARTH       = new MageCollege("Earth");
   public static final MageCollege AIR         = new MageCollege("Air");
   public static final MageCollege ENERGY      = new MageCollege("Energy");
   public static final MageCollege EVOCATION   = new MageCollege("Evocation");
   public static final MageCollege CONJURATION = new MageCollege("Conjuration");
   public static final MageCollege ILLUSION    = new MageCollege("Illusion");
   public static final MageCollege DIVINATION  = new MageCollege("Divination");
   public static final MageCollege NECROMANCY  = new MageCollege("Necromancy");
   public static final MageCollege PROTECTION  = new MageCollege("Protection");
   public static final MageCollege ENCHANTMENT = new MageCollege("Enchantment");

   public MageCollege() {
      // This ctor required for serialization
      name = "For Serialization only";
   }

   public MageCollege(MageCollege source) {
      name = source.name;
      level = source.level;
   }

   public MageCollege(String name) {
      this.name = name;
      if (!nameList.contains(name)) {
         nameList.add(name);
         colleges.put(name, this);
      }
   }

   public String getName() {
      return name;
   }

   public static List<String> getCollegeNames() {
      return nameList;
   }

   public static MageCollege getCollege(String collegeName) {
      MageCollege college = colleges.get(collegeName);
      if (college == null) {
         return null;
      }
      return college.clone();
   }

   public byte getLevel() {
      return level;
   }

   public void setLevel(byte newLevel) {
      level = newLevel;
   }

   @Override
   public MageCollege clone() {
      try {
         return (MageCollege) super.clone();
      } catch (CloneNotSupportedException e) {
         DebugBreak.debugBreak("clone error");
         return null;
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(name, out);
         writeToStream(level, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         name = readString(in);
         level = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      return "College: " + name + "~" + level;
   }
}
