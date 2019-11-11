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
   public DoorState _doorState = DoorState.Closed;
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
      if (_keyCode      != other._keyCode) {
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
         case Closed:   return "Door: closed";
         case Open:     return "Door: open";
         case Locked:   return "Door: locked";
         case Blocked:  return "Door: blocked";
         case Broken:   return "Door: broken";
      }
      return "Door";
   }
   @Override
   public Door clone() {
      return new Door(_name, _weight, _doorState, _keyCode, _orientation);
   }

   public boolean isOpen() {
      return (_doorState == DoorState.Open) || (_doorState == DoorState.Broken);
   }
   public boolean isLocked() {
      return (_doorState == DoorState.Locked);
   }

   public boolean close() {
      if (_doorState == DoorState.Open) {
         _doorState = DoorState.Closed;
         return true;
      }
      return false;
   }
   public boolean open() {
      if (_doorState == DoorState.Closed) {
         _doorState = DoorState.Open;
         return true;
      }
      return false;
   }
   public boolean lock() {
      if (_doorState == DoorState.Closed) {
         _doorState = DoorState.Locked;
         return true;
      }
      return false;
   }
   public boolean unlock() {
      if (_doorState == DoorState.Locked) {
         _doorState = DoorState.Closed;
         return true;
      }
      return false;
   }

}
