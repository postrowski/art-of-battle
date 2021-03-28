package ostrowski.combat.common.orientations;

import org.eclipse.swt.graphics.RGB;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;

import java.io.DataOutputStream;
import java.io.IOException;

public class OrientationSingleHumaniod extends OrientationSerpentine
{
   public OrientationSingleHumaniod() {
      super(1);
   }

   public static String getSerializeName() {
      return "O-SH";
   }
   @Override
   public void serializeNameToStream(DataOutputStream out) throws IOException {
      writeToStream(getSerializeName(), out);
   }

   @Override
   public void setPosition(Position newPosition, CombatMap map, Character actor) {
      Position currentPos = getPosition();
      byte newSize = getSizeForPosition(newPosition);
      byte oldSize = getSizeForPosition(currentPos);
      if ((newSize == 2) && (oldSize == 1)) {
         ArenaLocation newHeadLoc;
         Position altPosition;
         if (newPosition == Position.PRONE_BACK) {
            Facing newFacing = getFacing().turn(3);
            newHeadLoc = ArenaLocation.getForwardMovement(getHeadCoordinates(), newFacing, map);
            facings.add(newFacing);
            altPosition = Position.SITTING;
         }
         else {
            newHeadLoc = ArenaLocation.getForwardMovement(getHeadCoordinates(), getFacing(), map);
            facings.add(getFacing());
            altPosition = Position.KNEELING;
         }
         ArenaLocation curHeadLoc = map.getLocation(coordinates.get(0));
         if ((newHeadLoc != null) && (ArenaLocation.canMoveBetween(curHeadLoc, newHeadLoc, true/*blockByCharacters*/))) {
//         if ((newHeadLoc != null) && (newHeadLoc.canEnter(curHeadLoc, true/*blockByCharacters*/))) {
            coordinates.add(0, newHeadLoc);
            // set our position BEFORE we add this character to any hex, or the character may appear in the wrong position
            super.setPosition(newPosition, map, actor);
            newHeadLoc.addThing(actor);
         }
         else {
            // if we can't move into the next location (it may be occupied),
            // use the alternate position that only takes one hex.
            newSize = 1;
            newPosition = altPosition;
            facings.remove(facings.size() - 1);
         }
      }
      else if ((newSize == 1) && (oldSize == 2)) {
         ArenaLocation oldLoc = map.getLocation(coordinates.remove(0));
         oldLoc.remove(actor);
         facings.remove(facings.size() - 1);
      }
      baseSize = newSize;
      // when we roll, change our facing
      if (((newPosition == Position.PRONE_BACK)  && (currentPos == Position.PRONE_FRONT)) ||
          ((newPosition == Position.PRONE_FRONT) && (currentPos == Position.PRONE_BACK )))
      {
         facings.set(0, facings.get(0).turn(3));
      }

      if (facings.size() != coordinates.size()) {
         DebugBreak.debugBreak();
      }

      super.setPosition(newPosition, map, actor);
   }

   public byte getSizeForPosition(Position position) {
      if ((position == Position.PRONE_BACK) ||
          (position == Position.PRONE_FRONT)) {
         return 2;
      }
      return 1;
   }

   @Override
   public int getLimbLocationIndex(LimbType limbType) {
      if (getSizeForPosition(getPosition()) == 1)
      {
         return 0; // only location
      }

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
      if ((limb instanceof Hand) || (limb instanceof Wing)) {
         return true;
      }
      if (limb instanceof Leg) {
         return (getPosition() == Position.SITTING) || (getPosition() == Position.PRONE_BACK) || (getPosition() == Position.PRONE_FRONT);
      }
      return true;
   }
   @Override
   protected DrawnObject drawLimb(Limb limb, int narrowDiameter, int size, ArenaLocation loc, RGB foreground, RGB background) {
      DrawnObject limbOutline = super.drawLimb(limb, narrowDiameter, size, loc, foreground, background);
      if (limbOutline != null) {
//         // If we are on our stomach, switch the left & right orientations:
//         if (getPosition() == Position.PRONE_BACK) {
//            limbOutline.flipPoints(false/*alongHorizontalAxis*/);
//            //First set of arms should be drawn above our head:
//            if ((limb._id == Limb.APPENDAGE_HAND_LEFT) || (limb._id == Limb.APPENDAGE_HAND_RIGHT)) {
//               limbOutline.flipPoints(true/*alongHorizontalAxis*/);
//            }
//         }
         if (limb instanceof Leg) {
            if (getPosition()==Position.PRONE_FRONT) {
               limbOutline.flipPoints(true/*alongHorizontalAxis*/);
            }
         }
      }
      return limbOutline;
   }

   @Override
   public int getBodyWideDiameter(int size) {
      if ((getPosition()==Position.PRONE_BACK) || (getPosition()==Position.PRONE_FRONT)) {
         return (int) (size*.8);
      }
      return size;
   }
   @Override
   public int getBodyNarrowDiameter(int size) {
      if ((getPosition()==Position.PRONE_BACK) || (getPosition()==Position.PRONE_FRONT)) {
         return (int) (size * 1.5);
      }
      return (int) (size * .65);
   }
   @Override
   public double getLimbRotation(Limb limb) {
      double parentRotation = super.getLimbRotation(limb);
      if ((getPosition()==Position.PRONE_BACK) || (getPosition()==Position.PRONE_FRONT)) {
         if (limb.getLocationSide() == Wound.Side.RIGHT) {
            parentRotation += Math.PI/6;
         }
         else if (limb.getLocationSide() == Wound.Side.LEFT) {
            parentRotation -= Math.PI/6;
         }
      }
      else if (getPosition() == Position.SITTING) {
         if (limb instanceof Leg) {
            if (limb.getLocationSide() == Wound.Side.RIGHT) {
               parentRotation += Math.PI/6;
            }
            else if (limb.getLocationSide() == Wound.Side.LEFT) {
               parentRotation -= Math.PI/6;
            }
         }
      }
      if (limb instanceof Wing) {
         if (limb.getLocationSide() == Side.LEFT) {
            return Math.toRadians(-40);
         }
         return Math.toRadians(40);
      }
      return parentRotation;
   }

   @Override

   public int getLimbOffsetX(Limb limb, int size, ArenaLocation loc) {
      if (getPosition() == Position.SITTING) {
         if (limb instanceof Leg) {
            if (limb.getLocationSide() == Wound.Side.RIGHT) {
               return -(size / 6);
            }
            else if (limb.getLocationSide() == Wound.Side.LEFT) {
               return size/6;
            }
         }
      }
      if (limb instanceof Hand) {
         if ((getPosition()!=Position.PRONE_BACK) && (getPosition()!=Position.PRONE_FRONT)) {
            return 0;
         }
         if (limb.getLocationSide() == Wound.Side.LEFT) {
            return size/5;
         }
         return -(size / 5);
      }
      if (limb instanceof Wing) {
         if (limb.getLocationSide() == Side.LEFT) {
            return -(size / 3);
         }
         return size/3;
      }
      return 0;
   }
   @Override

   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (getPosition() == Position.SITTING) {
         if (limb instanceof Leg) {
            return -(size / 6);
         }
      }
      if (limb instanceof Wing) {
         return size/4;
      }
      if (limb instanceof Tail) {
         return super.getLimbOffsetY(limb, size, loc) * 2;
      }
      if ((getPosition()!=Position.PRONE_BACK) && (getPosition()!=Position.PRONE_FRONT)) {
         return super.getLimbOffsetY(limb, size, loc);
      }
      if (limb == null) {
         // drawing the body:
         int yOffset = 0;
         switch (coordinates.indexOf(loc)) {
            case 0: yOffset = (int) (size*.72); break;
            case 1: yOffset = (int) (0-(size*.72)); break;
         }
         if (getPosition()==Position.PRONE_BACK) {
            yOffset *= -1;
            yOffset += size*0.3;
         }
         return (int)(yOffset - (size*0.1));
      }
      if (limb instanceof Head) {
         if (getPosition()==Position.PRONE_BACK) {
            return (int) (size*.3);
         }
         return (int) (0-(size*.3));
      }
      if (limb instanceof Leg) {
         return (int) (size*.1);
      }
      if (getPosition()==Position.PRONE_FRONT) {
         return (int) (0-(size*.1));
      }
      if (getPosition()==Position.PRONE_BACK) {
         return (int) (0-(size*.2));
      }
      return (int) (0-(size*.3));
   }

   @Override
   public Facing getFacing(ArenaCoordinates loc) {
      Facing facing = super.getFacing(loc);
      if ((facing != null) &&
          (getPosition() == Position.PRONE_BACK) &&
          (coordinates.get(1).sameCoordinates(loc))) {
         return facing.turn(3);
      }
      return facing;
   }

}
