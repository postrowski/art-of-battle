package ostrowski.combat.common.orientations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.util.Diagnostics;

public abstract class OrientationSerpentine extends Orientation
{
   public byte _baseSize = 0;
   protected byte _maxBendAllowed = 1;
   public OrientationSerpentine(int baseSize) {
      super();
      _baseSize = (byte) baseSize;
   }


   @Override
   public void copyDataFrom(Orientation source) {
      super.copyDataFrom(source);
      if (source instanceof OrientationSerpentine) {
         OrientationSerpentine serpentineSource = (OrientationSerpentine)source;
         _baseSize = serpentineSource._baseSize;
         _maxBendAllowed = serpentineSource._maxBendAllowed;
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         super.serializeToStream(out);
         writeToStream(_baseSize, out);
         writeToStream(_maxBendAllowed, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         super.serializeFromStream(in);
         _baseSize       = readByte(in);
         _maxBendAllowed = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }


   @Override
   public boolean setHeadLocation(Character character, ArenaLocation headLocation, Facing facing, CombatMap map,
                                  Diagnostics diag, boolean allowTwisting) {
      List<ArenaLocation> newLocs = getLocationsForNewHeadLocation(character, headLocation, facing, map, diag);
      if (newLocs == null) {
         return false;
      }
      if (newLocs.size() != _baseSize) {
         DebugBreak.debugBreak();
         getLocationsForNewHeadLocation(character, headLocation, facing, map, diag);
         return false;
      }
      // If we can move into the new locations (they are not occupied, etc.),
      // then make sure this new orientation doesn't bend us up too much
      int maxBendAllowed = 1;
      if ((_baseSize == 1) && (getPosition() == Position.PRONE_BACK)) {
         maxBendAllowed = 3;
      }
      List<Facing> newFacings = OrientationSerpentine.computeFacingsRequiredForLocations(newLocs, facing, maxBendAllowed);
      if (newFacings == null) {
         DebugBreak.debugBreak();
         OrientationSerpentine.computeFacingsRequiredForLocations(newLocs, facing, maxBendAllowed);
         return false;
      }

      map.removeCharacter(character);
      _coordinates.clear();
      _coordinates.addAll(newLocs);
      _facings.clear();
      _facings.addAll(newFacings);
      if (_facings.size() != _coordinates.size()) {
         DebugBreak.debugBreak();
      }
      return true;
   }

   static public List<Facing> computeFacingsRequiredForLocations(List<? extends ArenaCoordinates> coordinates,
                                                                 Facing headFacing, int maxBendAllowed) {
      List<Facing> facingAtPoint = new ArrayList<>();
      Facing prevFacing = headFacing;
      facingAtPoint.add(prevFacing);
      for (int i=1 ; i<coordinates.size() ; i++) {
         Facing facing = ArenaCoordinates.getFacingToLocation(coordinates.get(i), coordinates.get(i-1));
         facingAtPoint.add(facing);

         byte bend = (byte) Math.abs(prevFacing.value - facing.value);
         if (bend > 3) {
            bend = (byte) (6-bend);
         }
         if (bend > maxBendAllowed) {
            return null;
         }
      }
      return facingAtPoint;
   }
   static public List<ArenaCoordinates> computeLocationsRequiredForFacings(List<Facing> newFacings, ArenaCoordinates headCoords) {
      List<ArenaCoordinates> newCoordinates = new ArrayList<>();
      ArenaCoordinates coord = headCoords;
      newCoordinates.add(coord);
      for (int i=1 ; i<newFacings.size() ; i++) {
         coord = ArenaCoordinates.getForwardMovement(coord, newFacings.get(i).turn(3));
         newCoordinates.add(coord);
      }
      return newCoordinates;
   }

   @Override
   public Orientation move(Facing direction, CombatMap map, boolean blockByCharacters) {
      OrientationSerpentine newOrientation = (OrientationSerpentine) clone();
      newOrientation._coordinates.clear();
      // a move forward puts the hind location at the previous head location
      // If this is a movement forward to the right or left, the facing will change by 1.
      boolean movingForward = ((direction.turnRight() == getFacing()) ||
                               (direction             == getFacing()) ||
                               (direction.turnLeft()  == getFacing())    );

      if (this instanceof OrientationSingleHumaniod) {
         if (getPosition() == Position.PRONE_BACK) {
            movingForward = false;
         }
      }
      if (movingForward) {
         ArenaLocation headLoc = map.getLocation(_coordinates.get(0));
         short x = (short) (headLoc._x + direction.moveX);
         short y = (short) (headLoc._y + direction.moveY);
         ArenaLocation newHeadLoc = map.getLocation(x, y);
         if (newHeadLoc == null) {
            // Must be off the map.
            return null;
         }

//         if (!newHeadLoc.canEnter(headLoc, blockByCharacters)) {
         if (!ArenaLocation.canMoveBetween(headLoc, newHeadLoc, blockByCharacters)) {
            return null;
         }
         // Start with our current locations
         newOrientation._coordinates.addAll(_coordinates);
         // Put the new head location at the front
         newOrientation._coordinates.add(0,newHeadLoc);
         // Then remove the old tail location
         newOrientation._coordinates.remove(newOrientation._coordinates.size()-1);

         // insert the new facing, which pushes all other facings down two entry
         newOrientation._facings.add(0, direction);
         newOrientation._facings.add(0, direction);
         // and remove the previous tail entries:
         newOrientation._facings.remove(newOrientation._facings.size()-1);
         newOrientation._facings.remove(newOrientation._facings.size()-1);
      }
      else {
         // We must be moving backwards.
         // Translate each location one at a time:
         for (ArenaCoordinates coord : _coordinates) {
            short x = (short) (coord._x + direction.moveX);
            short y = (short) (coord._y + direction.moveY);
            ArenaLocation newLoc = map.getLocation(x, y);
            if (newLoc == null) {
               // Must be off the map.
               return null;
            }
            ArenaLocation oldLoc = map.getLocation(coord);
            if (!ArenaLocation.canMoveBetween(oldLoc, newLoc, blockByCharacters)) {
//            if (!newLoc.canEnter(oldLoc, blockByCharacters)) {
               return null;
            }
            newOrientation._coordinates.add(map.getLocation(x, y));
         }
         // facings don't change
      }
      if (newOrientation._facings.size() != newOrientation._coordinates.size()) {
         DebugBreak.debugBreak();
      }

      // error-check:
      for (int i=0 ; i<newOrientation._coordinates.size() ; i++) {
         // make certain that each segment of the destination orientation is continuous.
         if (newOrientation._coordinates.get(i) == null) {
            DebugBreak.debugBreak();
            return null;
         }
         if (i > 0) {
            ArenaLocation loc1 = map.getLocation(newOrientation._coordinates.get(i));
            ArenaLocation loc2 = map.getLocation(newOrientation._coordinates.get(i-1));
            if (!ArenaLocation.canMoveBetween(loc1, loc2, blockByCharacters)) {
//            if (!loc2.canEnter(loc1, blockByCharacters)) {
               return null;
            }
         }
      }
      return newOrientation;
   }

   @Override
   public Orientation rotateAboutPoint(int pointIndex, byte dirDelta, CombatMap map) {
      if (pointIndex >= 2) {
         return null;
      }

      OrientationSerpentine newOrientation = (OrientationSerpentine) clone();
      // change the facing as specified
      newOrientation._facings.set(pointIndex, _facings.get(pointIndex).turn(dirDelta));
      if (newOrientation._facings.size() != newOrientation._coordinates.size()) {
         DebugBreak.debugBreak();
      }

      // exit in trivial conditions
      if (_baseSize == 1) {
         return newOrientation;
      }

      // work our way towards the head, moving each point along the way, until we arrive at the head
      for (int index = pointIndex ; index>0 ; index--) {
         ArenaCoordinates curCoord = newOrientation._coordinates.get(index);
         Facing curFacing = _facings.get(index);
         ArenaCoordinates nextLoc = ArenaLocation.getForwardMovement(curCoord, curFacing, map);
         newOrientation._coordinates.set(index-1, nextLoc);
         if (ArenaCoordinates.getDistance(newOrientation._coordinates.get(index), _coordinates.get(index)) > 1) {
            return null;
         }

         ArenaLocation curLoc = map.getLocation(_coordinates.get(index));
         ArenaLocation newLoc = map.getLocation(newOrientation._coordinates.get(index));
         if (newLoc == null) {
            // must be off the map.
            return null;
         }
         // make sure that there is no wall between the current and new locations.
         if (!ArenaLocation.canMoveBetween(curLoc, newLoc, false/*blockByCharacters*/)) {
//         if (!curLoc.canEnter(newLoc, false/*blockByCharacters*/)) {
            return null;
         }
      }
      // Now start from the head, and work our way to the tail, ensuring that we aren't bend too much:
      for (int index=1 ; index<newOrientation._facings.size() ; index++) {
         byte bend = (byte) Math.abs(newOrientation._facings.get(index-1).value - newOrientation._facings.get(index).value);
         if (bend > 3) {
            bend = (byte) (6-bend);
         }
         if (bend > _maxBendAllowed) {
            // bend the back of the body around to make this facing work
            Facing newFacing = newOrientation._facings.get(index).turn(1);
            bend = (byte) Math.abs(newOrientation._facings.get(index-1).value - newOrientation._facings.get(index).value);
            if (bend > 3) {
               bend = (byte) (6-bend);
            }
            if (bend > _maxBendAllowed) {
               newFacing = newOrientation._facings.get(index).turn(4);
               if (bend > 3) {
                  bend = (byte) (6-bend);
               }
               if (bend > _maxBendAllowed) {
                  return null;
               }
            }
            newOrientation._facings.set(index, newFacing);
            List<ArenaCoordinates> newCoords = computeLocationsRequiredForFacings(newOrientation._facings, newOrientation._coordinates.get(0));
            if (newCoords != null) {
               newOrientation._coordinates.clear();
               newOrientation._coordinates.addAll(newCoords);

               for (int i=0 ; i<_coordinates.size() ; i++) {
                  if (ArenaCoordinates.getDistance(newOrientation._coordinates.get(i), _coordinates.get(i)) > 1) {
                     return null;
                  }
               }
            }
         }
      }


      List<Facing> newFacings = OrientationSerpentine.computeFacingsRequiredForLocations(_coordinates, _facings.get(0), _maxBendAllowed);
      if (newFacings == null) {
         return null;
      }

      newOrientation._facings.clear();
      newOrientation._facings.addAll(newFacings);
      for (ArenaCoordinates coord : newOrientation._coordinates) {
         if (coord == null) {
            DebugBreak.debugBreak();
         }
      }
      if (newOrientation._facings.size() != newOrientation._coordinates.size()) {
         DebugBreak.debugBreak();
      }

      return newOrientation;
   }

//   @Override
//   public boolean setLocations(List<ArenaLocation> newLocations, byte newFacing, CombatMap map, Diagnostics diag)
//   {
//      boolean result = super.setLocations(newLocations, newFacing, map, diag);
//      for (ArenaLocation loc : _coordinates) {
//         if (loc == null) {
//            Rules.debugBreak();
//         }
//      }
//      return result;
//   }

   @Override
   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map, ArenaLocation headLocation,
                                        List<Facing> facings)
   {
      int index = 0;
      Facing dirAwayFromHead = facings.get(0).turn(3);
      int limbIndex = getLimbLocationIndex(limbType);
      if (_coordinates.size() > limbIndex) {
         if (_coordinates.get(0).sameCoordinates(headLocation)) {
            ArenaCoordinates coord = _coordinates.get(limbIndex);
            if (coord != null) {
               return map.getLocation(coord);
            }
         }
      }
      while ((index < limbIndex ) && (headLocation != null)) {
         if (_facings.size() > (index+1)) {
            dirAwayFromHead = _facings.get(index+1).turn(3);
         }
         headLocation = ArenaLocation.getForwardMovement(headLocation, dirAwayFromHead, map);
         index++;
      }
      return headLocation;
   }

   @Override
   public ArenaCoordinates getLimbCoordinates(LimbType limbType) {
      int index = getLimbLocationIndex(limbType);
      if (_coordinates.size() <= index) {
         DebugBreak.debugBreak();
      }
      return _coordinates.get(getLimbLocationIndex(limbType));
   }

   public int getLimbLocationIndex(LimbType limbType) {
      if (_baseSize == 1) {
         return 0; // only location
      }

      if (limbType.isLeg()) {
         if (limbType.setId == 1) {
            if (_baseSize > 3) {
               return 1;
            }
            return 0;
         }
         if (_baseSize > 3) {
            return 2; // get the hex behind the hex behind the head
         }
         return 1; // get the hex behind the head
      }
      if (limbType.isTail()) {
         // otherwise, this must be the tail, return the last location
         return _baseSize-1;
      }
      if (limbType.isWing()) {
         if (_baseSize > 1) {
            return 1;
         }
         return 0; // TODO: what location are the wings in for multi-hex creatures with wings?
      }
      return 0; // default is the head location
   }
}
