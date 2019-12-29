package ostrowski.combat.common.orientations;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.*;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.Diagnostics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class Orientation extends SerializableObject implements Enums, Cloneable, Comparable<Orientation>
{
   protected final ArrayList<ArenaCoordinates> _coordinates = new ArrayList<>();
   protected final ArrayList<Facing>           _facings = new ArrayList<>();
   private Position                            _position = Position.STANDING;

   @Override
   public Orientation clone() {
      try {
         Orientation dup = (Orientation) super.clone();//this.getClass().getDeclaredConstructor().newInstance();
         dup.copyDataFrom(this);
         return dup;
      } catch (IllegalArgumentException | SecurityException | CloneNotSupportedException e) {
         e.printStackTrace();
         DebugBreak.debugBreak();
         return null;
      }
   }

   public void copyDataFrom(Orientation source) {
      _coordinates.clear();
      _coordinates.addAll(source._coordinates);
      _facings.clear();
      _facings.addAll(source._facings);
      _position    = source._position;
   }

   protected Orientation() {
   }

   public boolean isInLocation(ArenaCoordinates loc) {
      return _coordinates.contains(loc);
   }
   public ArrayList<ArenaCoordinates> getCoordinates() {
      return _coordinates;
   }
   public ArenaCoordinates getHeadCoordinates() {
      if (_coordinates.size() == 0) {
         DebugBreak.debugBreak();
         return null;
      }
      return _coordinates.get(0);
   }

   public abstract void serializeNameToStream(DataOutputStream out) throws IOException;

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         serializeNameToStream(out);

         short size = (short) _coordinates.size();
         writeToStream(size, out);
         for (int i=0 ; i<size ; i++) {
            // Serialize the ArenaLocation as an ArenaCoordinate, because
            // if you serialize it as an arenaLocation, it will serialize its contents,
            // which will serialize the character for this orientation, which will recurse
            // back into this method, causing an infinite loop.
            ArenaCoordinates coords = new ArenaCoordinates(_coordinates.get(i)._x, _coordinates.get(i)._y);
            coords.serializeToStream(out);
            writeToStream(_facings.get(i).value, out);
         }

         writeToStream(_position.value, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         short size=readShort(in);
         _coordinates.clear();
         _facings.clear();
         for (int i=0 ; i<size ; i++) {
            ArenaCoordinates arenaCoord = new ArenaCoordinates();
            arenaCoord.serializeFromStream(in);
            _coordinates.add(arenaCoord);
            _facings.add(Facing.getByValue(readByte(in)));
         }
         _position = Position.getByValue(readByte(in));
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   static public Orientation serializeOrientationFromStream(DataInputStream in)
   {
      try {
         String orientType = readString(in);
         Orientation orient = null;
         if (orientType.equals(OrientationSingleHumaniod.getSerializeName())) {
            orient = new OrientationSingleHumaniod();
         }
         else if (orientType.equals(OrientationSingleQuadraped.getSerializeName())) {
            orient = new OrientationSingleQuadraped();
         }
         else if (orientType.equals(OrientationDoubleCentaur.getSerializeName())) {
            orient = new OrientationDoubleCentaur();
         }
         else if (orientType.equals(OrientationDoubleQuadraped.getSerializeName())) {
            orient = new OrientationDoubleQuadraped();
         }
         else if (orientType.equals(OrientationReptilian.getSerializeName())) {
            orient = new OrientationReptilian();
         }
         if (orient != null) {
            orient.serializeFromStream(in);
         }
         return orient;
      } catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * Computes the locations required for placing the head at the new specified location, with the specified facing.
    * If this orientation is not allowed (because one of the destination locations are occupied, or otherwise can't
    * be entered, this method returns null. Either way, this method does not modify any members.
    * @param character
    * @param headLocation
    * @param facing
    * @param map
    * @param diag
    * @return
    */
   public List<ArenaLocation> getLocationsForNewHeadLocation(Character character, ArenaLocation headLocation,
                                                             Facing facing, CombatMap map, Diagnostics diag)
   {
      List<Limb> limbs = character.getLimbs();
      List<ArenaLocation> newLocs = new ArrayList<>();
      List<Byte> newFacingTwists = new ArrayList<>();
      List<Facing> newFacings = new ArrayList<>();
      if (facing == null) {
         DebugBreak.debugBreak();
      }
      newFacings.add(facing);
      newFacingTwists.add((byte) 0);

      for (Limb limb : limbs) {
         boolean locValid = false;
         for (byte twist : new byte[] {0, 5, 1}) {
            ArenaLocation limbLoc = getLimbLocation(limb._limbType, map, headLocation, newFacings);
            if (limbLoc != null) {
               if (!newLocs.contains(limbLoc)) {
                  if (!limbLoc.canEnter(null/*fromLoc*/, true/*blockByCharacter*/)) {
                     continue;
                  }
                  newLocs.add(limbLoc);
                  newFacings.add(facing.turn(twist));
                  newFacingTwists.add(twist);
               }
               // if this location is already in the list of locations, its a valid location.
               locValid = true;
               break;
            }
            newFacings.set(newFacings.size()-1, facing.turn(twist));
         }
         if (!locValid) {
            return null;
         }
      }
      return newLocs;
   }
   public boolean setHeadLocation(Character character, ArenaLocation headLocation, Facing facing, CombatMap map, Diagnostics diag, boolean allowTwisting)
   {
      List<ArenaLocation> newLocs = getLocationsForNewHeadLocation(character, headLocation, facing, map, diag);
      if (newLocs == null) {
         return false;
      }

      map.removeCharacter(character);
      _coordinates.clear();
      _coordinates.addAll(newLocs);
      _facings.clear();
      _facings.add(facing);
      // TODO: make sure that facings of 0 are OK on this map
      while (_facings.size() < _coordinates.size()) {
         _facings.add(Facing.NOON);
      }
      if (_facings.size() != _coordinates.size()) {
         DebugBreak.debugBreak();
      }
      return true;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("facings: ").append(_facings);
      sb.append(", position: ").append(getPositionName());
      sb.append(", headloc: (");
      if (_coordinates.size() > 0) {
         sb.append(_coordinates.get(0)._x).append(",").append(_coordinates.get(0)._y);
      }
      sb.append(")");
      return sb.toString();
   }
   public DrawnObject getBodyOutlines(Character character, int wideDiameter, int narrowDiameter, ArenaLocation loc, int[] bounds, RGB foreground, RGB background)
   {
      return DrawnObject.createElipse(wideDiameter, narrowDiameter, wideDiameter, foreground, background);
   }
   public DrawnObject getHeadOutlines(Character character, int size, ArenaLocation loc, RGB foreground, RGB background)
   {
      DrawnObject headOutlines = new DrawnObject(foreground, background);
      for (Limb limb : character.getLimbs()) {
         if ((limb._limbType.isHead()) && (limb instanceof Head)) {
            if (getLimbCoordinates(limb._limbType).sameCoordinates(loc)) {
               int wideDiameter = getLimbWidth(limb._limbType, size);
               int narrowDiameter = getLimbLength(limb._limbType, size);
               Head head = (Head) limb;
               DrawnObject headObj = DrawnObject.createElipse(wideDiameter, narrowDiameter, wideDiameter, foreground, background);
               int xOffset = getLimbOffsetX(head, size, loc);
               int yOffset = getLimbOffsetY(head, size, loc);
               if ((xOffset != 0) || (yOffset != 0)) {
                  headObj.offsetPoints(xOffset, yOffset);
               }
               headOutlines.addChild(headObj);
            }
         }
      }
      return headOutlines;
   }

   public int getLimbWidth(LimbType limbType, int size) {
      if (limbType.isHand()) {
         return size;
      }
      return size / 3;
   }
   public int getLimbLength(LimbType limbType, int size) {
      if (limbType.isHand()) {
         return (int) (size * .65);
      }
      return (int)(Math.round((size * 4.0)/10));
   }
   public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map) {
      ArenaCoordinates headCoordinates = getLimbCoordinates(LimbType.HEAD);
      return map.getLocation(getLimbLocation(limbType, map, map.getLocation(headCoordinates), _facings));
   }
   abstract public ArenaLocation getLimbLocation(LimbType limbType, CombatMap map, ArenaLocation headLocation, List<Facing> facings);
   abstract public ArenaCoordinates getLimbCoordinates(LimbType limbType);

   public Facing getFacing() {
      return _facings.get(0);
   }
   public Facing getFacing(ArenaCoordinates loc) {
      for (int i=0 ; i<getCoordinates().size() ; i++) {
         if (_coordinates.get(i).sameCoordinates(loc)) {
            return _facings.get(i);
         }
      }
      DebugBreak.debugBreak("Asking for a facing from a location not in our orientation");
      return null;
   }

   final byte[]    ADV_FACING_DELTA = new byte[] {0, 5, 5, 1, 1};
   final boolean[] ADV_ADVANCING    = new boolean[] {true, false, true, false, true};
   public ArrayList<Orientation> getPossibleAdvanceOrientations(CombatMap map, boolean blockByCharacters)
   {
      ArrayList<Orientation> possibleMoves = new ArrayList<>();
      for (int i=0 ; i<5 ; i++) {
         if (ADV_ADVANCING[i]) {
            Facing dir = getFacing().turn(ADV_FACING_DELTA[i]);
            Orientation orient = move(dir, map, blockByCharacters);
            if (orient != null) {
               possibleMoves.add(orient);
            }
         }
         else {
            // consider rotations about each possible point in our current orientation
            for (int pointIndex=0 ; (pointIndex<2) && (pointIndex<_coordinates.size()) ; pointIndex++) {
               Orientation orient = rotateAboutPoint(pointIndex, ADV_FACING_DELTA[i], map);
               if (orient != null) {
                  possibleMoves.add(orient);
               }
            }
         }
      }
      return possibleMoves;
   }
   public boolean getPossibleChargePathsToTarget(CombatMap map, Character mover, Character target, int movementRemaining, HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientations) {
      return getPossibleChargePathsToTarget(map, mover, target, movementRemaining, mapOrientationToNextOrientations, 0/*depth*/, 0/*turns*/);
   }
   private boolean getPossibleChargePathsToTarget(CombatMap map, Character mover, Character target, int movementRemaining,
                                                  HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientations, int depth, int turns) {
      if (turns >= 2) {
         return false;
      }
      if ((depth > 1) && (canAttack(mover, target, map, false/*allowRanged*/, true/*onlyChargeTypes*/))) {
         mapOrientationToNextOrientations.put(this, null);
         return true;
      }
      List<Orientation> orientationsToLeadToChargeAttacks = new ArrayList<>();
      ArrayList<Orientation> orientations = getPossibleAdvanceOrientations(map, true/*blockByCharacters*/);
      boolean result = false;
      for (Orientation orient : orientations) {
         // If this advance orientation is just a in-place turn, skip it
         if (orient.getHeadCoordinates().sameCoordinates(getHeadCoordinates())) {
            continue;
         }
         byte costToEnter = getCostToEnter(orient, mover, map);
         if (costToEnter <= movementRemaining) {
            int newTurns = turns;
            if (getFacing() != orient.getFacing()) {
               newTurns++;
            }
            if (orient.getPossibleChargePathsToTarget(map, mover, target, (byte)(movementRemaining - costToEnter), mapOrientationToNextOrientations, depth+1, newTurns)) {
               orientationsToLeadToChargeAttacks.add(orient);
               result = true;
            }
         }
      }
      if (orientationsToLeadToChargeAttacks.size() > 0) {
         mapOrientationToNextOrientations.put(this, orientationsToLeadToChargeAttacks);
         return result;
      }
      return false;
   }
   public ArrayList<Orientation> getPossibleFutureOrientations(CombatMap map)
   {
      ArrayList<Orientation> possibleMoves = new ArrayList<>();
      // First consider all the move locations
      Facing dir=_facings.get(0);
      for (Facing dirDelta : Facing.values()) {
         Orientation orient = move(dir.turn(dirDelta.value), map, true/*blockByCharacters*/);
         if (orient != null) {
            possibleMoves.add(orient);
         }
      }
      // Then consider the possible single turn locations
      // consider rotations about each possible point in our current orientation
      for (int pointIndex=0 ; (pointIndex<2) && (pointIndex<_coordinates.size()) ; pointIndex++) {
         for (byte dirDelta=-1 ; dirDelta<=1 ; dirDelta++) {
            if ((pointIndex == 0) && (dirDelta == 0)) {
               continue;
            }
            Orientation orient = rotateAboutPoint(pointIndex, dirDelta, map);
            if (orient != null) {
               possibleMoves.add(orient);
            }
         }
      }
      return possibleMoves;
   }

   abstract public Orientation move(Facing direction, CombatMap map, boolean blockByCharacters);
   abstract public Orientation rotateAboutPoint(int pointIndex, byte dirDelta, CombatMap map);

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      Orientation orient = (Orientation) obj;
      if (_coordinates.size() != orient._coordinates.size()) {
         return false;
      }
      if (_position != orient._position) {
         return false;
      }
      for (int i=0 ; i<_coordinates.size() ; i++) {
         if (_facings.get(i) != orient._facings.get(i)) {
            return false;
         }
         if (!_coordinates.get(i).sameCoordinates(orient._coordinates.get(i))) {
            return false;
         }
      }
      return true;
   }

   public byte getCostToEnter(Orientation toOrientation, Character mover, CombatMap map) {
      if (equals(toOrientation)) {
         return 0;
      }
      byte maxCostToEnter = 0;
      ArenaCoordinates fromCoord = null;
      ArenaCoordinates toCoord = null;
      ArrayList<ArenaCoordinates> fromCoords = getCoordinates();
      ArrayList<ArenaCoordinates> toCoords   = toOrientation.getCoordinates();
      if (fromCoords.size() != toCoords.size()) {
         // This is no a legal move (only a position change can do this),
         // so report this as 'can not enter'
         return 100;
      }
      for (int i=0 ; i<fromCoords.size() ; i++) {
         toCoord = toCoords.get(i);
         fromCoord = fromCoords.get(i);
         if ((fromCoord == null) || (toCoord == null)) {
            DebugBreak.debugBreak();
         }
         else {
            if (!fromCoord.sameCoordinates(toCoord)) {
               if (ArenaCoordinates.getDistance(fromCoord, toCoord) > 1) {
                  return 100;
               }
               ArenaLocation arenaToLoc = map.getLocation(toCoord);
               ArenaLocation arenaFromLoc = map.getLocation(fromCoord);
               byte costToEnter = arenaToLoc.costToEnter(arenaFromLoc, mover);
               if (costToEnter > maxCostToEnter) {
                  maxCostToEnter = costToEnter;
               }
            }
         }
      }
      if (maxCostToEnter == 0) {
         // no movement occurred. Check if the character turned.
         Facing curFacing = getFacing();
         Facing newFacing = toOrientation.getFacing();
         byte facingChange = (byte) Math.abs(curFacing.value - newFacing.value);
         if (facingChange > 3) {
            facingChange = (byte) (6-facingChange);
         }
//         if (facingChange >= 2) {
//            // turning more than one hex requires two separate movement operations.
//            return 100;
//         }
         return facingChange;
      }
      ArenaCoordinates curHeadCoord = getHeadCoordinates();
      ArenaCoordinates newHeadCoord = toOrientation.getHeadCoordinates();
      if (!newHeadCoord.sameCoordinates(curHeadCoord)) {
         Facing fromFacing = getFacing();
         // If we have to move backward (facing change >= 2), add an extra cost point
         byte facingChange = Arena.getFacingChangeNeededToFace(fromFacing, curHeadCoord, newHeadCoord);
         if (Math.abs(facingChange) >= 2) {
            // if we are on our back, our facing will remain the same, so don't assess this penalty
            if (getPosition() != Position.PRONE_BACK) {
               maxCostToEnter++;
            }
         }
         // If we are not standing, add extra costs
         maxCostToEnter += _position.extraMovementPenalty;
      }
      return maxCostToEnter;
   }


   public DrawnObject getBodyOutlines(Character character, int size, ArenaLocation loc, int[] bounds, RGB foreground, RGB background) {
      int wideDiameter = getBodyWideDiameter(size);
      int narrowDiameter = getBodyNarrowDiameter(size);
      DrawnObject charOutlines = getBodyOutlines(character, wideDiameter, narrowDiameter, loc, bounds, foreground, background);
      int xOffset = getLimbOffsetX(null, size, loc);
      int yOffset = getLimbOffsetY(null, size, loc);
      if ((xOffset != 0) || (yOffset != 0)) {
         charOutlines.offsetPoints(xOffset, yOffset);
      }
      return charOutlines;
   }

   public DrawnObject getLegsOutlines(Character character, int size, ArenaLocation loc, RGB foreground, RGB background) {
      return null;
   }
   public DrawnObject getArmsOutlines(Character character, int size, ArenaLocation loc, RGB foreground, RGB background) {
      return getOutlines(character, size, loc, false/*weapon*/, foreground, background);
   }
   public DrawnObject getWeaponOutlines(Character character, int size, ArenaLocation loc, RGB foreground, RGB background) {
      return getOutlines(character, size, loc, true/*weapon*/, foreground, background);
   }
   public ArrayList<Limb> getLimbsToDrawAtLocation(Character character, ArenaLocation loc) {
      ArrayList<Limb> limbs = new ArrayList<> ();
      for (Limb limb : character.getLimbs()) {
         if (shouldDraw(limb)) {
            if (getLimbCoordinates(limb._limbType).sameCoordinates(loc)) {
               limbs.add(limb);
            }
         }
      }
      return limbs;
   }
   public int adjustLimbSize(Limb limb, int baseSize) {
      return baseSize;
   }
   public boolean shouldDraw(Limb limb) {
      return (limb != null) && (!limb.isSevered());
   }
   protected DrawnObject getOutlines(Character character, int size, ArenaLocation loc, boolean weapon, RGB foreground, RGB background) {
      int wideDiameter = size;
      int narrowDiameter = (int) (wideDiameter * .65);

      DrawnObject limbOutlines = new DrawnObject(foreground, background);
      DrawnObject weaponOutlines = new DrawnObject(foreground, background);

      for (Limb limb : getLimbsToDrawAtLocation(character, loc)) {
         narrowDiameter = getLimbLength(limb._limbType, size);
         wideDiameter = getLimbWidth(limb._limbType, size);
         DrawnObject limbOutline =  drawLimb(limb, narrowDiameter, size, loc, foreground, background);
         if (limbOutline != null) {
            if (weapon) {
               Thing thing = limb.getHeldThing();
               if (thing != null) {
                  DrawnObject weaponOutline = thing.drawThing(narrowDiameter, foreground, background);
                  if (weaponOutline != null) {
                     double ang = Math.PI/6;
                     if (thing instanceof Weapon) {
                        Weapon weap = (Weapon) thing;
                        if (weap.isOnlyTwoHanded()) {
                           ang *= 2;
                        }
                     }
                     else if (thing instanceof Shield) {
                        ang = 0;
                     }
                     if (limb.getLocationSide() == Wound.Side.LEFT) {
                        ang *= -1;
                     }
                     weaponOutline.rotatePoints(ang);
                     int offsetX = limbOutline.getXPoint(3);
                     int offsetY = limbOutline.getYPoint(3);
                     weaponOutline.offsetPoints(offsetX, offsetY);
                     weaponOutlines.addChild(weaponOutline);
                  }
               }
            }
            else {
               limbOutlines.addChild(limbOutline);
            }
         }
      }
      if (weapon) {
         return weaponOutlines;
      }
      return limbOutlines;
   }

   protected DrawnObject drawLimb(Limb limb, int narrowDiameter, int size, ArenaLocation loc, RGB foreground, RGB background) {
      if (!shouldDraw(limb)) {
         return null;
      }
      size = adjustLimbSize(limb, size);
      narrowDiameter = adjustLimbSize(limb, narrowDiameter);
      DrawnObject limbOutline =  limb.drawThing(narrowDiameter, size, foreground, background);
      if (limbOutline == null) {
         return null;
      }
      if (shouldFlipPoints(limb)) {
         limbOutline.flipPoints(true/*alongHorizontalAxis*/);
      }
      limbOutline.offsetPoints(getLimbOffsetX(limb, size, loc), getLimbOffsetY(limb, size, loc));
      limbOutline.rotatePoints(getLimbRotation(limb));
      return limbOutline;
   }

   public boolean shouldFlipPoints(Limb limb) {
      return false;
   }
   public double getLimbRotation(Limb limb) {
      double angle = 0;
      if (limb.getLocationPair() == Pair.SECOND) {
         angle = Math.toRadians(20);
      }
      if (limb.getLocationSide() == Side.LEFT) {
         angle *= -1;
      }
      return angle ;
   }
   public int getLimbOffsetX(Limb limb, int size, ArenaLocation loc) {
      return 0;
   }
   public int getLimbOffsetY(Limb limb, int size, ArenaLocation loc) {
      if (limb != null) {
         if (limb instanceof Head) {
            return size/9;
         }
      }
      return size/6;
   }
   public int getBodyWideDiameter(int size) { return size;}
   public int getBodyNarrowDiameter(int size) { return (int) (size * .65);}
   public int[][] getWeaponsOutline(GC gc, Device display, int size) {
      return null;
   }

   /*
    *   10 9 8      y = lowest
    *  11     7     y = low
    *  0       6    y = middle
    *   1     5     y = high
    *    2 3 4      y = highest
    */
   private static final HashMap<Facing, Integer> MAP_FACING_TO_SIDE_CCW = new HashMap<>();
   static {
      MAP_FACING_TO_SIDE_CCW.put(Facing.NOON,       0);
      MAP_FACING_TO_SIDE_CCW.put(Facing._2_OCLOCK, 10);
      MAP_FACING_TO_SIDE_CCW.put(Facing._4_OCLOCK,  8);
      MAP_FACING_TO_SIDE_CCW.put(Facing._6_OCLOCK,  6);
      MAP_FACING_TO_SIDE_CCW.put(Facing._8_OCLOCK,  4);
      MAP_FACING_TO_SIDE_CCW.put(Facing._10_OCLOCK, 2);
   }
   public static int[] getFacingDimensions(int[] hexDim, Facing facing)
   {
      int sideCCW = MAP_FACING_TO_SIDE_CCW.get(facing);
      int frontCCW  = (10 + sideCCW) % 12;
      int frontCW   = ( 8 + sideCCW) % 12;
      int sideCW    = ( 6 + sideCCW) % 12;
      int sideCCW_X = (hexDim[sideCCW]     + (2*hexDim[frontCCW]))     / 3;
      int sideCCW_Y = (hexDim[sideCCW + 1] + (2*hexDim[frontCCW + 1])) / 3;
      int sideCW_X  = (hexDim[sideCW]      + (2*hexDim[frontCW]))      / 3;
      int sideCW_Y  = (hexDim[sideCW + 1]  + (2*hexDim[frontCW + 1]))  / 3;

      return new int[] { sideCCW_X, sideCCW_Y, hexDim[frontCCW], hexDim[frontCCW + 1], hexDim[frontCW], hexDim[frontCW + 1], sideCW_X, sideCW_Y};
   }

   final HashMap<Integer, RGB> _mapOfIntToRGB = new HashMap<>();
   private RGB getRGB(int color) {
      RGB rgb = _mapOfIntToRGB.get(color);
      if (rgb == null) {
         rgb = new RGB( (color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff);
         _mapOfIntToRGB.put(color, rgb);
      }
      return rgb;
   }
   public void drawCharacter(GC gc, Device display, int[] bounds, ArenaLocation loc, int background, int foreground, Character character)
   {
      Facing facing = getFacing();
      int[] frontBounds = null;
      if (loc.sameCoordinates(character.getHeadCoordinates())) {
         frontBounds = getFacingDimensions(bounds, facing);
      }

      int backgroundFront = MapWidget2D.darkenColor(background, 50);
      int foregroundFront = MapWidget2D.darkenColor(foreground, 50);

      int size = Math.abs(bounds[(2 * 2)] - bounds[(2)]); // the length of the bottom horizontal line
      if (frontBounds != null) {
         Color bgColor = new Color(display, MapWidget2D.getColor(backgroundFront));
         Color fgColor = new Color(display, MapWidget2D.getColor(foregroundFront));
         gc.setBackground(bgColor);
         gc.setForeground(fgColor);
         gc.fillPolygon(frontBounds);
         bgColor.dispose();
         fgColor.dispose();
      }
      DrawnObject charOutline = character.getDrawnObject(size, getRGB(background), getRGB(foreground), loc, bounds, this);
      if (charOutline != null) {
         charOutline.draw(gc, display);
      }
   }
   public void applyWound(Wound wound, CombatMap map, Character actor) {
   }
   public void setPosition(Position newPosition, CombatMap map, Character actor) {
      if (_position == newPosition) {
         return;
      }

      if (_position != Position.PRONE_BACK) {
         // change position from single hex to double hex
      }
      _position = newPosition;
   }
   public Position getPosition()                        { return _position; }
   public boolean isStanding()                          { return (_position == Position.STANDING); }

   public int getAvailablePositions() {
      int actionsAllowed = 0;
      {
         if ((_position != Position.STANDING) &&
             (_position != Position.PRONE_BACK) &&
             (_position != Position.PRONE_FRONT)) {
            actionsAllowed |= ACTION_STAND;
         }

         if ((_position != Position.KNEELING) &&
             (_position != Position.PRONE_BACK)) {
            actionsAllowed |= ACTION_KNEEL;
         }

         if ((_position != Position.CROUCHING) &&
             (_position != Position.PRONE_BACK) &&
             (_position != Position.PRONE_FRONT)) {
            actionsAllowed |= ACTION_CROUCH;
         }

         if ((_position != Position.SITTING) &&
             (_position != Position.PRONE_FRONT)) {
            actionsAllowed |= ACTION_SIT;
         }

         if ((_position != Position.PRONE_BACK) &&
             (_position != Position.STANDING)) {
            actionsAllowed |= ACTION_LAYDOWN_BACK;
         }

         if ((_position != Position.PRONE_FRONT) &&
             (_position != Position.SITTING)) {
            actionsAllowed |= ACTION_LAYDOWN_FRONT;
         }
      }
      if (actionsAllowed != 0) {
         actionsAllowed |= ACTION_POSITION;
      }
      return actionsAllowed;
   }

   @Override
   public int compareTo(Orientation arg0) {
      if (getFacing() == arg0.getFacing()) {
         return getHeadCoordinates().compareTo(arg0.getHeadCoordinates());
      }
      if (getFacing().value < arg0.getFacing().value) {
         return -1;
      }
      return 1;
   }

   @Override
   public int hashCode() {
      int hashCode = 0;
      for (int i=0 ; i<_coordinates.size(); i++) {
         hashCode += (((_coordinates.get(i)._x *100) +_coordinates.get(i)._y)*100) + _facings.get(i).value;
         hashCode *= 10;
      }
      return hashCode + _position.value;
   }

   public byte getAttackPenaltyForTerrain(Character attacker, CombatMap map, List<String> terrainNames) {
      if (attacker.isFlying()) {
         return 0;
      }
      boolean attackerIsPenalizedInWater = attacker.isPenalizedInWater();
      int penalty = 0;
      int legCount = 0;
      HashSet<ArenaLocation> locationSet = null;
      if (terrainNames != null) {
         locationSet = new HashSet<>();
      }
      for (Limb limb : attacker.getLimbs()) {
         if (limb instanceof Leg) {
            ArenaLocation limbLoc = getLimbLocation(limb._limbType, map);
            penalty += limbLoc.getAttackPenaltyForTerrain(attackerIsPenalizedInWater);
            legCount++;
            if (locationSet != null) {
               locationSet.add(limbLoc);
            }
         }
      }
      if (terrainNames != null) {
         for (ArenaLocation loc : locationSet) {
            terrainNames.add(loc.getTerrainName());
         }
      }
      // Average the penalties over the number of legs on the ground.
      return (byte) Math.round(penalty / legCount);
   }
   public boolean canLimbAttack(Character attacker, Character defender, Limb limb, CombatMap map, boolean allowRanged, boolean onlyChargeTypes) {
      ArenaLocation headLoc = map.getLocation(getHeadCoordinates());
      if (getAttackPenaltyForTerrain(attacker, map, null/*terrainNames*/) > 10) {
         return false;
      }
      Weapon weapon = limb.getWeapon(attacker);
      if (weapon == null) {
         return false;
      }
      short minRange = weapon.getWeaponMinRange(allowRanged, onlyChargeTypes, attacker);
      if (minRange == 10000) {
         return false;
      }
      short maxRange = weapon.getWeaponMaxRange(allowRanged, onlyChargeTypes, attacker);
      if (maxRange < 0) {
         return false;
      }
      ArenaLocation limbLoc = getLimbLocation(limb._limbType, map);
      for (ArenaLocation targetLoc : map.getLocations(defender.getCoordinates())) {
         short distance = ArenaCoordinates.getDistance(limbLoc, targetLoc);
         // if either character is holding the other, consider the distance to be zero
         if ((attacker.getHoldLevel(defender) != null) ||
                  (defender.getHoldLevel(attacker) != null)) {
            distance = 0;
         }
         if ((distance >= minRange) && (distance <= maxRange)) {
            if (map.hasLineOfSight(headLoc, targetLoc, true/*blockedByAnyStandingCharacter*/)) {
               boolean facingMatters = (minRange >=0) && ((limb instanceof Head) || (limb instanceof Hand));
               if (!facingMatters || map.isFacing(this, targetLoc)) {
                  if (headLoc.sameCoordinates(limbLoc)) {
                     return true;
                  }
                  // some attacks (head butt & elbow strike) can attack hexes behind them,
                  // so these don't need to be facing the enemy
                  if ((minRange > -1) ||
                           (map.hasLineOfSight(limbLoc, targetLoc, true/*blockedByAnyStandingCharacter*/))) {
                     return true;
                  }
               }
            }
         }
         // If we are far away, don't consider every location of the target:
         if ((distance - defender.getOrientation()._coordinates.size()) > maxRange) {
            break;
         }
      }
      return false;
   }
   public boolean canAttack(Character attacker, Character defender, CombatMap map, boolean allowRanged, boolean onlyChargeTypes) {
      ArrayList<Limb> limbs = attacker.getLimbs();
      ArenaLocation headLoc = map.getLocation(getHeadCoordinates());
      if (getAttackPenaltyForTerrain(attacker, map, null/*terrainNames*/) > 10) {
         return false;
      }
      for (Limb limb : limbs) {
         if (canLimbAttack(attacker, defender, limb, map, allowRanged, onlyChargeTypes)) {
            return true;
         }
      }
      if (!onlyChargeTypes) {
         Spell attackingSpell = attacker.getCurrentSpell(false/*eraseCurrentSpell*/);
         if ((attackingSpell != null) && attackingSpell.requiresTargetToCast() && (attackingSpell.canTarget(attacker, defender) == null)) {
            short maxRange = attackingSpell.getMaxRange(attacker);
            short minRange = attackingSpell.getMinRange(attacker);
            for (ArenaLocation targetLoc : map.getLocations(defender.getCoordinates())) {
               short distance = ArenaCoordinates.getDistance(attacker.getHeadCoordinates(), targetLoc);
               if ((distance >= minRange) && (distance <= maxRange)) {
                  if (map.hasLineOfSight(headLoc, targetLoc, true/*blockedByAnyStandingCharacter*/)) {
                     if (attacker.hasPeripheralVision() || map.isFacing(this, targetLoc)) {
                        return true;
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   public byte getActionsNeededToChangePosition() {
      return RequestActionType.OPT_CHANGE_POS.getActionsUsed((byte) 0);
   }

   public String getPositionName() {
      return _position.name;
   }
   public byte getPositionAdjustedDefenseOption(DefenseOption defOption, byte def) {
      return Rules.getPositionAdjustedDefenseOption(getPosition(), defOption, def);
   }
   public byte getPositionAdjustmentForAttack() {
      return _position.adjustmentToAttack;
   }

   public Element getXMLObject(Document parentDoc, String newLine) {
      Element mainElement = parentDoc.createElement("Orientation");

      mainElement.setAttribute("position",          String.valueOf(_position.value));
      for (int i=0 ; i<_coordinates.size() ; i++) {
         Element locationElement = parentDoc.createElement("Location");
         locationElement.setAttribute("facing", String.valueOf(_facings.get(i).value));
         locationElement.setAttribute("coordX", String.valueOf(_coordinates.get(i)._x));
         locationElement.setAttribute("coordY", String.valueOf(_coordinates.get(i)._y));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(locationElement);
      }
      mainElement.appendChild(parentDoc.createTextNode(newLine));
      return mainElement;
   }
   public boolean serializeFromXmlObject(Node element)
   {
      if (!element.getNodeName().equals("Orientation")) {
         return false;
      }

      NamedNodeMap attributes = element.getAttributes();
      if (attributes == null) {
         return false;
      }
      String position = attributes.getNamedItem("position").getNodeValue();
      _position    = Position.getByValue(Byte.parseByte(position));
      _facings.clear();
      _coordinates.clear();

      NodeList children = element.getChildNodes();
      for (int index=0 ; index<children.getLength() ; index++) {
         Node child = children.item(index);
         attributes = child.getAttributes();
         if (attributes != null) {
            if (child.getNodeName().equals("Location")) {
               String facing = attributes.getNamedItem("facing").getNodeValue();
               String coordX = attributes.getNamedItem("coordX").getNodeValue();
               String coordY = attributes.getNamedItem("coordY").getNodeValue();
               _facings.add(Facing.getByValue(Byte.parseByte(facing)));
               _coordinates.add(new ArenaCoordinates(Short.parseShort(coordX), Short.parseShort(coordY)));
            }
         }
      }
      if (_facings.size() != _coordinates.size()) {
         DebugBreak.debugBreak();
      }
      return true;
   }


}
