package ostrowski.combat.common.enums;

import java.util.HashMap;

public enum TerrainWall {
   /*
    *   10 9 8      y = lowest
    *  11     7     y = low
    *  0       6    y = middle
    *   1     5     y = high
    *    2 3 4      y = highest
    */
   VERT_LEFT             ( 0,  10,    2,    2,    0), //     64
   VERT_CENTER           ( 1,   9,    3,    1,   -1), //    128
   VERT_RIGHT            ( 2,   8,    4,    2,    6), //    256
   HORIZONTAL_TOP        ( 3,  10,    8,    4,   -1), //    512
   HORIZONTAL_CENTER     ( 4,   0,    6,    2,   -1), //   1024
   HORIZONTAL_BOTTOM     ( 5,   2,    4,    4,   -1), //   2048
   DIAG_RIGHT_LEFT       ( 6,   0,   10,    4,   -1), //   4096
   DIAG_RIGHT_CENTER     ( 7,   2,    8,    2,   -1), //   8192
   DIAG_RIGHT_RIGHT      ( 8,   4,    6,    4,   -1), //  16384
   DIAG_LEFT_LEFT        ( 9,   2,    0,    4,   -1), //  32668
   DIAG_LEFT_CENTER      (10,   4,   10,    2,   -1), //  65336
   DIAG_LEFT_RIGHT       (11,   6,    8,    4,   -1), // 130672
   DIAG_FAR_RIGHT_LEFT   (12,   0,    8,    2,   10), // 261344
   DIAG_FAR_RIGHT_CENTER (13,   1,    7,    2,   -1), // 524288
   DIAG_FAR_RIGHT_RIGHT  (14,   2,    6,    2,    4), //1048576
   DIAG_FAR_LEFT_LEFT    (15,   0,    4,    2,    2), //2097152
   DIAG_FAR_LEFT_CENTER  (16,  11,    5,    2,   -1), //4194304
   DIAG_FAR_LEFT_RIGHT   (17,  10,    6,    2,    8); //8388608

   public static final int TERRAIN_WALL_START_BITPOS       = 6;
   public static final int TERRAIN_ATTRIBUTES_START_BITPOS = TERRAIN_WALL_START_BITPOS + 17;
   public static final int TERRAIN_ATTRIBUTE_FLAMES        = 1 << (TERRAIN_ATTRIBUTES_START_BITPOS + 0);

   private TerrainWall(int bitPos, int startPoint, int endPoint, int thickness, int fillpoint) {
      this.bitMask = 1 << (TERRAIN_WALL_START_BITPOS + bitPos);
      this.startPoint = startPoint;
      this.endPoint = endPoint;
      this.thickness = thickness;
      this.fillpoint = fillpoint;
   }
   public long with(TerrainWall otherWall) {
      return this.bitMask | otherWall.bitMask;
   }
   public long with(long otherWall) {
      return (this.bitMask | otherWall) & MASK;
   }
   public boolean contains(long walls) {
      return ((this.bitMask & walls) != 0);
   }
   public final long bitMask;
   public final int startPoint;
   public final int endPoint;
   public final int thickness;
   public final int fillpoint;

   private static HashMap<Long, TerrainWall> MAP_BY_BITMASK = new HashMap<>();
   public static long MASK;
   static {
      for (TerrainWall terrainWall : values()) {
         MAP_BY_BITMASK.put(terrainWall.bitMask, terrainWall);
         MASK |= terrainWall.bitMask;
      }
   }
   public static TerrainWall getByBitMask(long bitMask) {
      return MAP_BY_BITMASK.get(bitMask);
   }

   public static final long TERRAIN_ALL_CENTER_WALLS = TerrainWall.VERT_CENTER.with(
                                                       TerrainWall.HORIZONTAL_CENTER.with(
                                                       TerrainWall.DIAG_RIGHT_CENTER.with(
                                                       TerrainWall.DIAG_LEFT_CENTER.with(
                                                       TerrainWall.DIAG_FAR_RIGHT_CENTER.with(
                                                       TerrainWall.DIAG_FAR_LEFT_CENTER)))));

}