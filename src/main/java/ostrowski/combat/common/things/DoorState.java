package ostrowski.combat.common.things;

import java.util.HashMap;

public enum DoorState {
   BROKEN(0),
   OPEN(1),
   CLOSED(2),
   LOCKED(3),
   BLOCKED(4),
   HALF_HEIGHT_WALL(5);
   DoorState(int val) {
      value = val;
   }

   public final         int                         value;
   private static final HashMap<Integer, DoorState> MAP_TO_DOORSTATE = new HashMap<>();
   static {
      for (DoorState state : values()) {
         MAP_TO_DOORSTATE.put(state.value, state);
      }
   }

   public static DoorState getByValue(int val) {
      return MAP_TO_DOORSTATE.get(val);
   }
}