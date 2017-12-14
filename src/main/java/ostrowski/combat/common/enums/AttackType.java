package ostrowski.combat.common.enums;

import java.util.HashMap;

public enum AttackType {
   SWING  (0),
   THRUST (1),
   THROW  (2),
   MISSILE(3),
   GRAPPLE(4),
   COUNTER_ATTACK(5);
   private AttackType(int val) {
      this.value = (byte)val;
   }
   public final byte value;

   private static HashMap<Byte, AttackType> MAP_TO_ATTACK_TYPES = new HashMap<>();
   static {
      for (AttackType type : AttackType.values()) {
         MAP_TO_ATTACK_TYPES.put(type.value, type);
      }
   }
   public static AttackType getByValue(byte val) {
      return MAP_TO_ATTACK_TYPES.get(val);
   }
}