/*
 * Created on Nov 22, 2006
 *
 */
package ostrowski.combat.common.things;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.TerrainWall;

public class Door extends Wall implements Enums
{
   public DoorState doorState = DoorState.CLOSED;
   public String    keyCode   = "";

   public Door() {
      // default 'ctor used for serialization
   }
   public Door(DoorState doorState, String keyCode, TerrainWall orientation) {
      this("Door", 50/*weight*/, doorState, keyCode, orientation);
   }
   public Door(String name, DoorState doorState, String keyCode, TerrainWall orientation) {
      this(name, 50/*weight*/, doorState, keyCode, orientation);
   }
   public Door(String name, double weight, DoorState doorState, String keyCode, TerrainWall orientation) {
      super(name, weight, orientation);
      this.doorState = doorState;
      this.keyCode = keyCode;
   }

   public boolean equals(Door other) {
      if (other == null) {
         return false;
      }
      if (doorState != other.doorState) {
         return false;
      }
      if (!keyCode.equals(other.keyCode)) {
         return false;
      }
      return super.equals(other);
   }

   @Override
   public void copyData(Thing source)
   {
      super.copyData(source);
      if (source instanceof Door) {
         Door doorSource = (Door) source;
         doorState = doorSource.doorState;
         keyCode = doorSource.keyCode;
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         doorState = DoorState.getByValue(readInt(in));
         keyCode = readString(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(doorState.value, out);
         writeToStream(keyCode, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public String toString()
   {
      switch (doorState) {
         case CLOSED:           return "Door: closed";
         case OPEN:             return "Door: open";
         case LOCKED:           return "Door: locked";
         case BLOCKED:          return "Door: blocked";
         case BROKEN:           return "Door: broken";
         case HALF_HEIGHT_WALL: return "Half wall";
      }
      return "Door";
   }
   @Override
   public Door clone() {
      return new Door(name, weight, doorState, keyCode, orientation);
   }

   public boolean isOpen() {
      return (doorState == DoorState.OPEN) || (doorState == DoorState.BROKEN);
   }
   public boolean isLocked() {
      return (doorState == DoorState.LOCKED);
   }
   public boolean isHalfHeightWall() {
      return (doorState == DoorState.HALF_HEIGHT_WALL);
   }

   public boolean close() {
      if (doorState == DoorState.OPEN) {
         doorState = DoorState.CLOSED;
         return true;
      }
      return false;
   }
   public boolean open() {
      if (doorState == DoorState.CLOSED) {
         doorState = DoorState.OPEN;
         return true;
      }
      return false;
   }
   public boolean lock() {
      if (doorState == DoorState.CLOSED) {
         doorState = DoorState.LOCKED;
         return true;
      }
      return false;
   }
   public boolean unlock() {
      if (doorState == DoorState.LOCKED) {
         doorState = DoorState.CLOSED;
         return true;
      }
      return false;
   }

}
