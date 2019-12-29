package ostrowski.combat.common.enums;

import java.util.HashMap;

public enum DamageType {
   NONE    (-1, "",          ""),
   BLUNT   ( 0, "blunt",     "blunt"),
   CUT     ( 1, "cutting",   "cut"),
   IMP     ( 2, "impaling",  "imp"),
   FIRE    ( 3, "fire",      "fire"),
   ELECTRIC( 4, "eletrical", "electric"),
   GENERAL ( 5, "general",   "general");

   public final byte   value;
   public final String fullname;
   public final String shortname;

   DamageType(int val, String name, String shortname) {
      this.value = (byte) val;
      this.fullname = name;
      this.shortname = shortname;
   }

   private static HashMap<Byte, DamageType> MAP_TO_DAMAGETYPE = new HashMap<>();
   static {
      for (DamageType damageType : values()) {
         MAP_TO_DAMAGETYPE.put(damageType.value, damageType);
      }
   }
   public static DamageType getByValue(byte val) {
      return MAP_TO_DAMAGETYPE.get(val);
   }
}