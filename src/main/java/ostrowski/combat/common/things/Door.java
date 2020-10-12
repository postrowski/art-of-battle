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
   public DoorState _doorState = DoorState.CLOSED;
   public String _keyCode = "";

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
      _doorState = doorState;
      _keyCode = keyCode;
   }

   public boolean equals(Door other) {
      if (other == null) {
         return false;
      }
      if (_doorState    != other._doorState) {
         return false;
      }
      if (!_keyCode.equals(other._keyCode)) {
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
         _doorState = doorSource._doorState;
         _keyCode = doorSource._keyCode;
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _doorState   = DoorState.getByValue(readInt(in));
         _keyCode     = readString(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_doorState.value, out);
         writeToStream(_keyCode, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public String toString()
   {
      switch (_doorState) {
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
      return new Door(_name, _weight, _doorState, _keyCode, _orientation);
   }

   public boolean isOpen() {
      return (_doorState == DoorState.OPEN) || (_doorState == DoorState.BROKEN);
   }
   public boolean isLocked() {
      return (_doorState == DoorState.LOCKED);
   }
   public boolean isHalfHeightWall() {
      return (_doorState == DoorState.HALF_HEIGHT_WALL);
   }

   public boolean close() {
      if (_doorState == DoorState.OPEN) {
         _doorState = DoorState.CLOSED;
         return true;
      }
      return false;
   }
   public boolean open() {
      if (_doorState == DoorState.CLOSED) {
         _doorState = DoorState.OPEN;
         return true;
      }
      return false;
   }
   public boolean lock() {
      if (_doorState == DoorState.CLOSED) {
         _doorState = DoorState.LOCKED;
         return true;
      }
      return false;
   }
   public boolean unlock() {
      if (_doorState == DoorState.LOCKED) {
         _doorState = DoorState.CLOSED;
         return true;
      }
      return false;
   }

}
