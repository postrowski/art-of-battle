/*
 * Created on Dec 10, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.CombatMap;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MapVisibility extends SerializableObject
{
   private byte[] _visibilityMask;
   private short  _bytesPerRow;

   public MapVisibility() {
   }

   public MapVisibility(CombatMap map) {
      _bytesPerRow = (short) ((map.getSizeX()+7)/8);
      _visibilityMask = new byte[_bytesPerRow * ((map.getSizeY()/2) +1)];
   }
   public MapVisibility(byte[] mask, short bytesPerRow)
   {
      _bytesPerRow = bytesPerRow;
      _visibilityMask = new byte[mask.length];
   }

   public boolean isVisible(short x, short y) {
      short rowOffset = (short) (x / 8);
      byte bitMask = (byte) (1 << (x % 8));
      return (_visibilityMask[((_bytesPerRow * y)/2) + rowOffset] & bitMask) != 0;
   }
   public boolean setVisible(short x, short y, boolean visibility) {
      short rowOffset = (short) (x / 8);
      byte bitMask = (byte) (1 << (x % 8));
      boolean wasVisible = (_visibilityMask[((_bytesPerRow * y)/2) + rowOffset] & bitMask) != 0;
      if (wasVisible == visibility) {
         return false;
      }
      if (visibility) {
         _visibilityMask[((_bytesPerRow * y)/2) + rowOffset] |= bitMask;
      }
      else {
         _visibilityMask[((_bytesPerRow * y)/2) + rowOffset] &= ~bitMask;
      }
      return true;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _bytesPerRow = readShort(in);
         _visibilityMask = readByteArray(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_bytesPerRow, out);
         writeToStream(_visibilityMask, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public MapVisibility clone() {
      try {
         MapVisibility copy = (MapVisibility) super.clone();
         copy._visibilityMask = new byte[_visibilityMask.length];
         System.arraycopy(_visibilityMask, 0, copy._visibilityMask, 0, _visibilityMask.length);
         return copy;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
}
