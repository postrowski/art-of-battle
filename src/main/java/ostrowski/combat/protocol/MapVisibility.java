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
   private byte[] visibilityMask;
   private short  bytesPerRow;

   public MapVisibility() {
   }

   public MapVisibility(CombatMap map) {
      bytesPerRow = (short) ((map.getSizeX() + 7) / 8);
      visibilityMask = new byte[bytesPerRow * ((map.getSizeY() / 2) + 1)];
   }
   public MapVisibility(byte[] mask, short bytesPerRow)
   {
      this.bytesPerRow = bytesPerRow;
      visibilityMask = new byte[mask.length];
   }

   public boolean isVisible(short x, short y) {
      short rowOffset = (short) (x / 8);
      byte bitMask = (byte) (1 << (x % 8));
      return (visibilityMask[((bytesPerRow * y) / 2) + rowOffset] & bitMask) != 0;
   }
   public boolean setVisible(short x, short y, boolean visibility) {
      short rowOffset = (short) (x / 8);
      byte bitMask = (byte) (1 << (x % 8));
      boolean wasVisible = (visibilityMask[((bytesPerRow * y) / 2) + rowOffset] & bitMask) != 0;
      if (wasVisible == visibility) {
         return false;
      }
      if (visibility) {
         visibilityMask[((bytesPerRow * y) / 2) + rowOffset] |= bitMask;
      }
      else {
         visibilityMask[((bytesPerRow * y) / 2) + rowOffset] &= ~bitMask;
      }
      return true;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         bytesPerRow = readShort(in);
         visibilityMask = readByteArray(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(bytesPerRow, out);
         writeToStream(visibilityMask, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public MapVisibility clone() {
      try {
         MapVisibility copy = (MapVisibility) super.clone();
         copy.visibilityMask = new byte[visibilityMask.length];
         System.arraycopy(visibilityMask, 0, copy.visibilityMask, 0, visibilityMask.length);
         return copy;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
}
