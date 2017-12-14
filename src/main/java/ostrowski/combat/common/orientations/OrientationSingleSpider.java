package ostrowski.combat.common.orientations;

import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Head;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.server.ArenaLocation;

public class OrientationSingleSpider extends OrientationSingleQuadraped
{
   public OrientationSingleSpider() {
      super();
   }

   public static String getSerializeName() {
      return "O-SS";
   }
   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public int getBodyWideDiameter(int size)   { return size/2;}
   @Override
   public int getBodyNarrowDiameter(int size) { return size/2;}
   @Override
   public int getLimbWidth(LimbType limbType, int size) {
      if (limbType.isHead()) {
         return super.getLimbWidth(limbType, size)/2;
      }
      return super.getLimbWidth(limbType, size);
   }
   @Override
   public int getLimbLength(LimbType limbType, int size) {
      if (limbType.isHead()) {
         return super.getLimbLength(limbType, size)/2;
      }
      return super.getLimbLength(limbType, size);
   }
   @Override
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb != null) {
         if (limb instanceof Head) {
            return 0-(size/3);
         }
         if (limb instanceof Hand) {
            if (limb.getLocationPair() == Pair.FIRST) {
               return 0-((size*1)/4);
            }
            return 0-((size*1)/5);
         }
         else if (limb instanceof Leg) {
            if (limb.getLocationPair() == Pair.FIRST) {
               return (size*1)/6;
            }
            return (size*1)/5;
         }
      }
      return 0;//super.getLimbOffsetY(limb, size, loc);
   }
   @Override
   public int adjustLimbSize(Limb limb, int baseSize) {
      if (limb instanceof Hand) {
         return (baseSize*4)/5;
      }
      return (baseSize*5)/5;
   }
   @Override
   public int getLimbOffsetX(Limb limb, int size, ArenaLocation loc) {
      if (limb == null) {
         return super.getLimbOffsetX(limb, size, loc);
      }

      int dist = size/10;
      if (limb instanceof Hand) {
         if (limb.getLocationPair() == Pair.FIRST) {
            dist = size/2;
         }
         else {
            dist = size/2;
         }
      }
      else if (limb instanceof Leg) {
         if (limb.getLocationPair() == Pair.FIRST) {
            dist = (size*2)/3;
         }
         else {
            dist = (size*3)/5;
         }
      }
      if (limb instanceof Head) {
         return 0;
      }
      if (limb.getLocationSide() == Side.RIGHT) {
         return 0-dist;//super.getLimbOffsetX(limb, size, loc);
      }
      return dist;//super.getLimbOffsetX(limb, size, loc);
   }
   @Override
   public boolean shouldFlipPoints(Limb limb) {
      return limb instanceof Leg;
   }
   @Override
   public double getLimbRotation(Limb limb) {
      double angle = 0;
      if (limb instanceof Hand) {
         if (limb.getLocationPair() == Pair.FIRST) {
            angle = -40;
         }
         else {
            angle = -50;
         }
      }
      else if (limb instanceof Leg) {
         if (limb.getLocationPair() == Pair.FIRST) {
            angle = 70;
         }
         else {
            angle = 110;
         }
      }
      if (limb.getLocationSide() == Side.RIGHT) {
         angle *= -1;
      }
      return Math.toRadians(angle)  + super.getLimbRotation(limb);
   }
   @Override
   public boolean shouldDraw(Limb limb) {
      if ((limb == null) || (limb.isSevered())) {
         return false;
      }
      if (limb instanceof Leg) {
         return true;
      }
      return true;
   }
}
