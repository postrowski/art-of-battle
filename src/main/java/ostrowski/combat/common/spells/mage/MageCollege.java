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
   public static List<String>                 _nameList = new ArrayList<>();
   public static HashMap<String, MageCollege> _colleges = new HashMap<>();

   private String                             _name;
   private byte                               _level;
   public static final MageCollege            FIRE        = new MageCollege("Fire");
   public static final MageCollege            WATER       = new MageCollege("Water");
   public static final MageCollege            EARTH       = new MageCollege("Earth");
   public static final MageCollege            AIR         = new MageCollege("Air");
   public static final MageCollege            ENERGY      = new MageCollege("Energy");
   public static final MageCollege            EVOCATION   = new MageCollege("Evocation");
   public static final MageCollege            CONJURATION = new MageCollege("Conjuration");
   public static final MageCollege            ILLUSION    = new MageCollege("Illusion");
   public static final MageCollege            DIVINATION  = new MageCollege("Divination");
   public static final MageCollege            NECROMANCY  = new MageCollege("Necromancy");
   public static final MageCollege            PROTECTION  = new MageCollege("Protection");
   public static final MageCollege            ENCHANTMENT = new MageCollege("Enchantment");

   public MageCollege() {
      // This ctor required for serialization
      _name = "For Serialization only";
   }

   public MageCollege(MageCollege source) {
      _name = source._name;
      _level = source._level;
   }

   public MageCollege(String name) {
      _name = name;
      if (!_nameList.contains(name)) {
         _nameList.add(name);
         _colleges.put(name, this);
      }
   }

   public String getName() {
      return _name;
   }

   public static List<String> getCollegeNames() {
      return _nameList;
   }

   public static MageCollege getCollege(String collegeName) {
      MageCollege college = _colleges.get(collegeName);
      if (college == null) {
         return null;
      }
      return college.clone();
   }

   public byte getLevel() {
      return _level;
   }

   public void setLevel(byte newLevel) {
      _level = newLevel;
   }

   @Override
   public MageCollege clone() {
      try {
         MageCollege dup = (MageCollege) super.clone();
         dup._nameList = new ArrayList<>(this._nameList);
         dup._colleges = new HashMap<>(this._colleges);
         return dup;
      } catch (CloneNotSupportedException e) {
         DebugBreak.debugBreak("clone error");
         return null;
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_name, out);
         writeToStream(_level, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _name = readString(in);
         _level = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static MageCollege serializeCollegeFromStream(DataInputStream in) {
      MageCollege college = null;
      try {
         String name = readString(in);
         college = getCollege(name);
         college.serializeFromStream(in);
         // _resistedAtt, _prerequisiteSpellNames & _attributeMod don't need to be serialized,
         // because they are constant for a given spell (defined by its name).
      } catch (IOException e) {
         e.printStackTrace();
      }
      return college;
   }

   @Override
   public String toString() {
      return "College: " + _name + "~" + _level;
   }
}
