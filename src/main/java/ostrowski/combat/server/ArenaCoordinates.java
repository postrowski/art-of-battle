package ostrowski.combat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.protocol.SerializableObject;

public class ArenaCoordinates extends SerializableObject implements Enums, Comparable<ArenaCoordinates>
{
   public    short x        = -1;
   public    short y        = -1;
   public    float altitude = 0f;
   public ArenaCoordinates(short x, short y) {
      this.x = x;
      this.y = y;
   }
   public ArenaCoordinates() {
      // This constructor is only used by serialization.
   }

   public float getAltitude() {
      return altitude;
   }
   public void adjustAltitude(float delta) {
      altitude += delta;
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         x = readShort(in);
         y = readShort(in);
         // altitude = readFloat(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(x, out);
         writeToStream(y, out);
         //writeToStream(altitude, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public ArenaCoordinates clone() {
      try {
         ArenaCoordinates dup = (ArenaCoordinates) super.clone();
         dup.copyData(this);
         return dup;
      } catch (CloneNotSupportedException e) {
         return null;
      }
   }
   public void copyData(ArenaCoordinates source) {
      x = source.x;
      y = source.y;
      altitude = source.altitude;
   }

   @Override
   public int compareTo(ArenaCoordinates arg0) {
      if (x < arg0.x) {
         return -1;
      }
      if (x > arg0.x) {
         return 1;
      }
      return Short.compare(y, arg0.y);
   }

   public boolean sameCoordinates(ArenaCoordinates otherCoord) {
      if (otherCoord == null) {
         return false;
      }
      if (otherCoord.x != x) {
         return false;
      }
      return otherCoord.y == y;
   }

   @Override
   public boolean equals(Object obj) {
      if ((obj instanceof ArenaCoordinates)) {
         return sameCoordinates((ArenaCoordinates) obj);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return ((x << 11) + y) << 11;// + (int)(altitude * 100);
   }

   @Override
   public String toString() {
      return "ArenaCoord{" + x + "," + y + " ^" + altitude + "}";
   }

   public static short getDistance(ArenaCoordinates coord1, ArenaCoordinates coord2) {
      return getDistance((short)(Math.abs(coord1.x - coord2.x)),
                         (short)(Math.abs(coord1.y - coord2.y)));
   }
   public static Facing getFacingToLocation(ArenaCoordinates fromCoord, ArenaCoordinates toCoord) {
      if (fromCoord.x == toCoord.x) {
         if (fromCoord.y == (toCoord.y + 2)) {
            return Facing.NOON;
         }
         if (fromCoord.y == (toCoord.y - 2)) {
            return Facing._6_OCLOCK;
         }
      }
      else if (fromCoord.x == (toCoord.x - 1)) {
         if (toCoord.y == (fromCoord.y + 1)) {
            return Facing._4_OCLOCK;
         }
         if (toCoord.y == (fromCoord.y - 1)) {
            return Facing._2_OCLOCK;
         }
      }
      else if (fromCoord.x == (toCoord.x + 1)) {
         if (toCoord.y == (fromCoord.y + 1)) {
            return Facing._8_OCLOCK;
         }
         if (toCoord.y == (fromCoord.y - 1)) {
            return Facing._10_OCLOCK;
         }
      }
      return null;
   }
   public static ArenaCoordinates getForwardMovement(ArenaCoordinates curCoord, Facing direction) {
      return new ArenaCoordinates((short)(curCoord.x + direction.moveX),
                                  (short)(curCoord.y + direction.moveY));
   }

   public static byte getFacingToward(ArenaCoordinates fromCoord, ArenaCoordinates toCoord) {
      if ((toCoord != null) && (fromCoord != null)) {
         int xDiff = toCoord.x - fromCoord.x;
         int yDiff = toCoord.y - fromCoord.y;
         boolean above3OclockLine = (yDiff <=0 );
         boolean above1OclockLine = (yDiff <= (-xDiff*3));
         boolean above5OclockLine = (yDiff <= (xDiff*3));
         if (above3OclockLine) {
            if (!above1OclockLine) {
               return 1;
            }
            if (!above5OclockLine) {
               return 5;
            }
            return 0;
         }
         if (above1OclockLine) {
            return 4;
         }
         if (above5OclockLine) {
            return 2;
         }
         return 3;
      }
      return -1;
   }

   /**
    * Given the difference in x position and y position, this method returns the number of hexes
    * between the two positions.
    * @param xDist
    * @param yDist
    * @return
    */
   private static short getDistance(short xDist, short yDist) {
      if (xDist == 0) {
         return (short) (yDist/2);
      }
      if (yDist == 0) {
         return xDist;
      }
      if (yDist > xDist) {
         short dist = getDistance((short)0, (short)(yDist-xDist));
         return (short) (dist + xDist);
      }
      return xDist;
   }

}
