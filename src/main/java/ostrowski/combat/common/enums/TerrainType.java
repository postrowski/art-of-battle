package ostrowski.combat.common.enums;

import org.eclipse.swt.graphics.RGB;

import java.util.HashMap;

public enum TerrainType {
   FLOOR         (0x00,   1,   0,   0, new RGB(0xff, 0xff, 0xff), "Floor"),
   GRASS         (0x01,   1,   0,   0, new RGB(0x80, 0xff, 0x80), "Grass"),
   DIRT          (0x02,   1,   0,   0, new RGB(0xB0, 0x80, 0x40), "Dirt"),
   MUD           (0x03,   1,   1,   1, new RGB(0x80, 0x80, 0x00), "Mud"),
   WATER_SHALLOW (0x04,   2,   0,   0, new RGB(0x00, 0x00, 0xff), "Shallow Water"),
   WATER         (0x05,   3,   2,   2, new RGB(0x00, 0x00, 0xC0), "Water"),
   WATER_DEEP    (0x06,   3, 100, 100, new RGB(0x00, 0x00, 0x80), "Deep Water"),
   GRAVEL        (0x07,   1,   1,   0, new RGB(0xB0, 0xB0, 0xB0), "Gravel"),
   BUSHES        (0x08,   2,   0,   0, new RGB(0x40, 0x80, 0x40), "Bushes"),
   DENSE_BUSH    (0x09, 100,   0,   0, new RGB(0x20, 0x60, 0x20), "Dense Bushes"),
   TREE_TRUNK    (0x0A, 100,   0,   0, new RGB(0x80, 0x80, 0x40), "Tree Trunk"),
   SOLID_ROCK    (0x0B, 100,   0,   0, new RGB(0x40, 0x40, 0x40), "Solid Rock"),
   ICE           (0x0C,   2,   4,   4, new RGB(0x40, 0xB0, 0xB0), "Ice"),
   PAVERS        (0x0D,   1,   0,   0, new RGB(0xA0, 0xA0, 0xA0), "Pavers");
   //FUTURE_1      (0x0E,  true,   0,   0, new RGB(0x00, 0x00, 0x00), ""),
   //FUTURE_2      (0x0F,  true,   0,   0, new RGB(0x00, 0x00, 0x00), ""),

   public final int value;
   public final int costToEnter;
   public final boolean canBeEnterd;
   public final byte defensePenalty;
   public final byte attackPenalty;
   public final RGB color;
   public final int colorAsInt;
   public final String name;
   public final boolean isWater;
   TerrainType(int val, int costToEnter, int defensePenalty, int attackPenalty, RGB color, String nam) {
      this.value = val;
      this.costToEnter = costToEnter;
      this.canBeEnterd = (costToEnter != 100);
      this.defensePenalty = (byte)defensePenalty;
      this.attackPenalty = (byte)attackPenalty;
      this.color = color;
      this.colorAsInt = ((color.red << 16) & 0xFF0000) | ((color.green << 8) & 0x00FF00) | (color.blue & 0x0000FF);
      this.name = nam;
      this.isWater = nam.contains("Water");
   }

   public static int                           MASK            = 0;
   private static final HashMap<Integer, TerrainType> MAP_TO_TERRAINS = new HashMap<>();
   static {
      for (TerrainType ter : TerrainType.values()) {
         MAP_TO_TERRAINS.put(ter.value, ter);
         MASK |= ter.value;
      }
   }
   public static TerrainType getByValue(long val) {
      return MAP_TO_TERRAINS.get((int)(val & MASK));
   }
}