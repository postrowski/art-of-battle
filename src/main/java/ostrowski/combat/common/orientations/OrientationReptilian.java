package ostrowski.combat.common.orientations;

import org.eclipse.swt.graphics.RGB;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.things.*;
import ostrowski.combat.server.ArenaLocation;

import java.io.DataOutputStream;
import java.io.IOException;

public class OrientationReptilian extends OrientationSerpentine
{
   // must have this 'ctor for the Clone() to work
   public OrientationReptilian() {
      super(0);
   }
   public OrientationReptilian(int size) {
      super(size);
   }

   public static String getSerializeName() {
      return "O-R";
   }

   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public DrawnObject getBodyOutlines(Character character, int size, int narrowDiameter, ArenaLocation loc,
                                      int[] bounds, RGB foreground, RGB background)
   {
      int pointCount = size;
      DrawnObject charOutlines = new DrawnObject(foreground, background);
      if (character.getRace().hasProperty(Race.PROPERTIES_TAIL)) {
         if ( _baseSize == 2) {
            size *= .5;
            narrowDiameter *= .7;
         }
         else if ( _baseSize == 3) {
            size *= .7;
            narrowDiameter *= 1.0;
         }
         Limb tail = character.getLimb(LimbType.TAIL);
         if (tail != null) {
            DrawnObject tailOutline = tail.drawThing(narrowDiameter, size, foreground, background);
            if (tailOutline != null) {
               tailOutline.offsetPoints(getLimbOffsetX(tail, size, loc), getLimbOffsetY(tail, size, loc));
               tailOutline.rotatePoints(getLimbRotation(tail));
               charOutlines.addChild(tailOutline);
            }
         }
      }
      charOutlines.addChild(DrawnObject.createElipse((int) (size*.7), (int) (narrowDiameter*2.5), pointCount, foreground, background));
      return charOutlines;
   }
   @Override
   public boolean shouldDraw(Limb limb) {
      if (super.shouldDraw(limb)) {
         return !(limb instanceof Tail);
      }
      return false;
   }
   @Override
   public boolean shouldFlipPoints(Limb limb) {
      return ((limb._limbType == LimbType.LEG_LEFT_2) || (limb._limbType == LimbType.LEG_RIGHT_2));
   }

   @Override
   public double getLimbRotation(Limb limb) {
      if (limb._limbType == LimbType.LEG_LEFT) {
         return 0-(Math.PI/3);
      }
      if (limb._limbType == LimbType.LEG_LEFT_2) {
         return Math.PI/3;
      }
      if (limb._limbType == LimbType.LEG_RIGHT) {
         return Math.PI/3;
      }
      if (limb._limbType == LimbType.LEG_RIGHT_2) {
         return 0-(Math.PI/3);
      }
      return super.getLimbRotation(limb);
   }
   @Override
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb == null) {
         int yOffset = 0;
         int narrowDiameter = getBodyNarrowDiameter(size);
         switch (_coordinates.indexOf(loc)) {
            case 0: yOffset = narrowDiameter; break;
            case 1: yOffset = -narrowDiameter; break;
            case 2: yOffset = -(3 * narrowDiameter); break;
         }
         return (int)(yOffset + (narrowDiameter*0.2));
      }
      if (limb instanceof Head) {
         return -(size / 10);
      }
      if (limb instanceof Wing) {
         if (_baseSize == 2) {
            return -(size / 2);
         }
      }
      if ((limb instanceof Leg) && (limb.getLocationPair() == Pair.SECOND)) {
         return (size * _baseSize)/40; // 20 for size = 2, 10 for size = 3
      }
      return size/24;
   }
   @Override
   public int getLimbOffsetX(Limb limb, int size, ArenaLocation loc) {
      if (limb == null) {
         return 0;
      }
      if (limb instanceof Leg) {
         int base = size/10;
         if (_baseSize == 2) {
             base = size/6;
         }
         if (limb.getLocationPair() == Pair.SECOND) {
            base = base * _baseSize;
         }

         if (limb.getLocationSide() == Side.LEFT) {
            return base;
         }
         return -base;
      }
      return super.getLimbOffsetX(limb, size, loc);
   }

   @Override
   public int getLimbWidth(LimbType limbType, int size) {
      if (limbType.isHead()) {
         return size /4;
      }
      if (limbType.isLeg()) {
         return size/2;
      }
      if (limbType.isTail()) {
         if ( _baseSize == 2) {
             size /= 2;
         }
         if ( _baseSize == 3) {
            return (int) (size * .7);
         }
      }
      return size / 4;
   }
   @Override
   public int getLimbLength(LimbType limbType, int size) {
      if (limbType.isHead()) {
         return (int)(Math.round((size * 6.0)/10));
      }
      if (limbType.isLeg()) {
         return (int) ((size * .65) / 2.0);
      }
      if (limbType.isTail()) {
         if ( _baseSize == 2) {
            size *= .7;
         }
         else if ( _baseSize == 3) {
            return size;
         }
      }
      return (int)(Math.round((size * 6.0)/10));
   }


}
