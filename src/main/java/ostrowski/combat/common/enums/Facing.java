package ostrowski.combat.common.enums;

import java.util.HashMap;

public enum Facing {
   NOON      (0,  0, -2),
   _2_OCLOCK (1,  1, -1),
   _4_OCLOCK (2,  1,  1),
   _6_OCLOCK (3,  0,  2),
   _8_OCLOCK (4, -1,  1),
   _10_OCLOCK(5, -1, -1);
   public final byte value;
   public final byte moveX;
   public final byte moveY;
   Facing(int val, int moveX, int moveY) {
      this.value = (byte) val;
      this.moveX = (byte) moveX;
      this.moveY = (byte) moveY;
   }
   public Facing turnRight() {
      return getByValue((byte)((this.value+1) % 6));
   }
   public Facing turnLeft() {
      return getByValue((byte)((this.value+5) % 6));
   }
   public Facing turn(int facingChange) {
      return getByValue((byte)(this.value + facingChange));
   }
   private static HashMap<Byte, Facing> MAP_TO_FACINGS = new HashMap<>();
   static {
      for (Facing facing : Facing.values()) {
         MAP_TO_FACINGS.put(facing.value, facing);
      }
   }
   public static Facing getByValue(byte val) {
      while (val <0) {
         val += 6;
      }
      return MAP_TO_FACINGS.get((byte)(val % 6));
   }
}