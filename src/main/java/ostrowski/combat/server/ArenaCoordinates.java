package ostrowski.combat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.protocol.SerializableObject;

public class ArenaCoordinates extends SerializableObject implements Enums, Comparable<ArenaCoordinates>
{
   public    short              _x              = -1;
   public    short              _y              = -1;
   public    float              _altitude       = 0f;
   public ArenaCoordinates(short x, short y) {
      _x = x;
      _y = y;
   }
   public ArenaCoordinates() {
      // This constructor is only used by serialization.
   }

   public float getAltitude() {
      return _altitude;
   }
   public void adjustAltitude(float delta) {
      _altitude += delta;
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _x = readShort(in);
         _y = readShort(in);
         // _altitude = readFloat(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_x, out);
         writeToStream(_y, out);
         //writeToStream(_altitude, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public ArenaCoordinates clone() {
      ArenaCoordinates selfCopy = new ArenaCoordinates(_x, _y);
      selfCopy.copyData(this);
      return selfCopy;
   }
   public void copyData(ArenaCoordinates source) {
      _x = source._x;
      _y = source._y;
      _altitude = source._altitude;
   }

   @Override
   public int compareTo(ArenaCoordinates arg0) {
      if (_x < arg0._x) {
         return -1;
      }
      if (_x > arg0._x) {
         return 1;
      }
      if (_y < arg0._y) {
         return -1;
      }
      if (_y > arg0._y) {
         return 1;
      }
      return 0;
   }

   public boolean sameCoordinates(ArenaCoordinates otherCoord) {
      if (otherCoord == null) {
         return false;
      }
      if (otherCoord._x != _x) {
         return false;
      }
      if (otherCoord._y != _y) {
         return false;
      }
      return true;
   }

   @Override
   public boolean equals(Object obj) {
      if ((obj != null) && (obj instanceof ArenaCoordinates)) {
         return sameCoordinates((ArenaCoordinates) obj);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return ((_x<<11) + _y) << 11;// + (int)(_altitude * 100);
   }

   @Override
   public String toString() {
      return "ArenaCoord{"+_x+","+_y+" ^"+_altitude+"}";
   }

   public static short getDistance(ArenaCoordinates coord1, ArenaCoordinates coord2) {
      return getDistance((short)(Math.abs(coord1._x - coord2._x)),
                         (short)(Math.abs(coord1._y - coord2._y)));
   }
   public static Facing getFacingToLocation(ArenaCoordinates fromCoord, ArenaCoordinates toCoord) {
      if (fromCoord._x == toCoord._x) {
         if (fromCoord._y == (toCoord._y+2)) {
            return Facing.NOON;
         }
         if (fromCoord._y == (toCoord._y-2)) {
            return Facing._6_OCLOCK;
         }
      }
      else if (fromCoord._x == (toCoord._x-1)) {
         if (toCoord._y == (fromCoord._y+1)) {
            return Facing._4_OCLOCK;
         }
         if (toCoord._y == (fromCoord._y-1)) {
            return Facing._2_OCLOCK;
         }
      }
      else if (fromCoord._x == (toCoord._x+1)) {
         if (toCoord._y == (fromCoord._y+1)) {
            return Facing._8_OCLOCK;
         }
         if (toCoord._y == (fromCoord._y-1)) {
            return Facing._10_OCLOCK;
         }
      }
      return null;
   }
   public static ArenaCoordinates getForwardMovement(ArenaCoordinates curCoord, Facing direction) {
      return new ArenaCoordinates((short)(curCoord._x + direction.moveX),
                                  (short)(curCoord._y + direction.moveY));
   }

   public static byte getFacingToward(ArenaCoordinates fromCoord, ArenaCoordinates toCoord) {
      if ((toCoord != null) && (fromCoord != null)) {
         int xDiff = toCoord._x - fromCoord._x;
         int yDiff = toCoord._y - fromCoord._y;
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
