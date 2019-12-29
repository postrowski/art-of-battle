package ostrowski.combat.common.orientations;

import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.things.Head;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Wing;
import ostrowski.combat.server.ArenaLocation;

public class OrientationSingleWinged extends OrientationSerpentine
{
   public OrientationSingleWinged() {
      super(1);
   }

   public static String getSerializeName() {
      return "O-SW";
   }
   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public int getBodyWideDiameter(int size)   { return (int) (size * .32);}
   @Override
   public int getBodyNarrowDiameter(int size) { return size / 2;}

   @Override
   public int getLimbWidth(LimbType limbType, int size) {
      return size / 5;
   }
   @Override
   public int getLimbLength(LimbType limbType, int size) {
      return (int)(Math.round((size * 3.0)/10));
   }

   @Override
   public int adjustLimbSize(Limb limb, int baseSize) {
      int size = super.adjustLimbSize(limb, baseSize);
      if (limb instanceof Wing) {
         size *= 1.5;
      }
      return size;
   }
   @Override
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb != null) {

         if (limb instanceof Head) {
            return 0-(size/6);
         }
         if (limb instanceof Wing) {
            return size/15;
         }
      }
      return super.getLimbOffsetY(limb, size, loc);
   }

   @Override
   public boolean shouldDraw(Limb limb) {
      if ((limb == null) || (limb.isSevered())) {
         return false;
      }
      return !(limb instanceof Leg);
   }
}
