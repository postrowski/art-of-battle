package ostrowski.combat.common.enums;

import java.util.HashMap;


// attributes
public enum Attribute {
   Strength    (0, "STR"),
   Health      (1, "HT"),
   Toughness   (2, "TOU"),
   Intelligence(3, "IQ"),
   Nimbleness  (4, "NIM"),
   Dexterity   (5, "DEX"),
   Social      (6, "SOC");

   public final         byte                     value;
   public final         String                   shortName;
   private static final HashMap<Byte, Attribute> MAP_TO_ATTRIBUTE = new HashMap<>();
   public static final  int                      COUNT;
   static {
      for (Attribute attr : Attribute.values()) {
         MAP_TO_ATTRIBUTE.put(attr.value, attr);
      }
      COUNT = Attribute.values().length;
   }

   Attribute(int val, String abbreviation) {
      this.value = (byte) val;
      this.shortName = abbreviation;
   }
   public static Attribute getByValue(byte val) {
      return MAP_TO_ATTRIBUTE.get(val);
   }
}