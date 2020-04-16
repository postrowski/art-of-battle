package ostrowski.combat.common.orientations;

import org.eclipse.swt.graphics.RGB;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.*;
import ostrowski.combat.server.ArenaLocation;

import java.io.DataOutputStream;
import java.io.IOException;

public class OrientationDoubleQuadraped extends OrientationSerpentine
{
   public OrientationDoubleQuadraped() {
      super(2);
   }

   public static String getSerializeName() {
      return "O-DQ";
   }

   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public DrawnObject getBodyOutlines(Character character, int wideDiameter, int narrowDiameter, ArenaLocation loc, int[] bounds, RGB foreground, RGB background)
   {
      DrawnObject charOutlines = DrawnObject.createElipse((int) (wideDiameter*.8), narrowDiameter*3, wideDiameter, foreground, background);
      int yOffset = narrowDiameter;
      int xOffset = 0;
      if (!loc.sameCoordinates(_coordinates.get(0))) {
         yOffset = -narrowDiameter;
      }
      charOutlines.offsetPoints(xOffset, yOffset);
      return charOutlines;
   }

   @Override
   public double getLimbRotation(Limb limb) {
      if (limb._limbType == LimbType.HAND_LEFT) {
         return 0-(Math.PI/6);
      }
      if (limb._limbType == LimbType.HAND_RIGHT) {
         return Math.PI/6;
      }
      return super.getLimbRotation(limb);
   }
   @Override
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb == null) {
         return 0;
      }
      if (limb instanceof Head) {
         return -(size / 4);
      }
      return size/24;
   }
   @Override
   public int getLimbOffsetX(Limb limb, int size, ArenaLocation loc) {
      if (limb == null) {
         return 0;
      }
      if (limb instanceof Hand) {
         if (limb.getLocationSide() == Side.LEFT) {
            return size/10;
         }
         return -(size / 10);
      }
      return super.getLimbOffsetX(limb, size, loc);
   }
   @Override
   public int getLimbLocationIndex(LimbType limbType) {
      if (limbType.isLeg() && (limbType.setId == 1)) {
         return 1;
      }
      return 0; // default is the head location
   }

   @Override
   public boolean shouldDraw(Limb limb) {
      if ((limb == null) || (limb.isSevered())) {
         return false;
      }
      if (limb instanceof Leg) {
         return (getPosition() == Position.PRONE_BACK) || (getPosition() == Position.PRONE_FRONT);
      }
      return true;
   }
}
