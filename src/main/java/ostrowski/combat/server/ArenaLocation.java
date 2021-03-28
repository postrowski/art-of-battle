/*
 * Created on May 23, 2006
 *
 */
package ostrowski.combat.server;

import org.eclipse.swt.graphics.RGB;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.things.*;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.SerializableFactory;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ArenaLocation extends ArenaCoordinates implements IMonitorableObject, Enums
{
   public final        Semaphore                          lock_this                 = new Semaphore("AreanLocation_lock_this", CombatSemaphore.CLASS_ARENALOCATION_this);
   private             TerrainType                        terrain                   = TerrainType.FLOOR;
   private             long                               data                      = terrain.value;
   private             List<Object>                       things                    = new ArrayList<>();
   private             List<Door>                         doors                     = new ArrayList<>();
   private             List<IAreaSpell>                   activeSpells              = new ArrayList<>();
   private             HashSet<Integer>                   visibleTo                 = new HashSet<>();
   private             HashSet<Integer>                   viewedBy                  = new HashSet<>();
   public              HashMap<Integer, ArenaCoordinates> visibleToCharacterFromLoc = new HashMap<>();
   private             String                             label                     = null;
   transient           MonitoredObject                    monitoredProxy;
   private             boolean                            selectable                = true;
   public static final String                             PICKUP                    = "pickup ";

   public ArenaLocation(short x, short y) {
      super(x,y);
      if ((x%2) != (y%2)) {
         DebugBreak.debugBreak();
         throw new IllegalArgumentException(" x="+x+", y="+y+" is illegal!");
      }
      monitoredProxy = new MonitoredObject("ArenaLoc:" + x + "," + y, this);
   }
   public ArenaLocation() {
      // This constructor is only used by serialization.
      // Serialized objects should never need to be watched, since
      // they are only sent from the server to the client, and
      // the client doesn't need any watching/watcher functionality.
      monitoredProxy = null;
   }

   public void setDataInternal(long data)
   {
      this.data = data;
      terrain = TerrainType.getByValue(data);
   }
   public void setData(long data)
   {
      if (this.data == data) {
         return;
      }
      ArenaLocation origLoc = clone();
      setDataInternal(data);
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getData() { return data; }

   public void addSpell(IAreaSpell spell) {
      activeSpells.add(spell);
      // Affect the characters in the location
      for (Character chr : getCharacters()) {
         spell.affectCharacterOnActivation(chr);
      }
   }
   public List<IAreaSpell> getActiveSpells() {
      List<IAreaSpell> activeSpells = new ArrayList<>();
      // Check for any spells that have expired
      for (IAreaSpell spell : this.activeSpells) {
         if (((Spell)spell).getDuration() != 0) {
            activeSpells.add(spell);
         }
      }
      this.activeSpells = activeSpells;
      return activeSpells;
   }

   @Override
   public ArenaLocation clone() {
      ArenaLocation clone = (ArenaLocation) super.clone();
      clone.copyDataUnsafe(this);
      return clone;
   }

   // This method is not thread-safe. For thread-safe copies, use copyData(ArenaLocation)
   private void copyDataUnsafe(ArenaLocation source)
   {
      super.copyData(source);
      things = new ArrayList<>();
      doors = new ArrayList<>();
      activeSpells = new ArrayList<>();
      visibleTo = new HashSet<>();
      viewedBy = new HashSet<>();
      visibleToCharacterFromLoc = new HashMap<>();

      data = source.data;
      terrain = source.terrain;
      label = source.label;
      for (int i = 0; i<source.things.size() ; i++) {
         // Clone the object, if possible
         Object thing = source.things.get(i);
         if (thing instanceof Thing) {
            Thing dupThing = ((Thing)thing).clone();
            if (dupThing == null) {
               DebugBreak.debugBreak();
            }
            else {
               things.add(dupThing);
            }
         }
         else {
            things.add(thing);
         }
      }
      for (Door door : source.doors) {
         doors.add(door.clone());
      }
      for (IAreaSpell spell : source.activeSpells) {
         activeSpells.add((IAreaSpell) spell.clone());
      }
      visibleTo.addAll(source.visibleTo);
      viewedBy.addAll(source.viewedBy);
      for (Integer charId : source.visibleToCharacterFromLoc.keySet()) {
         ArenaCoordinates visFrom = source.visibleToCharacterFromLoc.get(charId);
         visibleToCharacterFromLoc.put(charId, new ArenaCoordinates(visFrom.x, visFrom.y));
      }
   }

   public void copyData(ArenaLocation source)
   {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            copyDataUnsafe(source);
         }
      }
   }

   public List<Door> getDoors() {
      return doors;
   }
   public void addDoor(Door door) {
      ArenaLocation origLoc = clone();
      synchronized (this) {
         lock_this.check();
         // check if we already have this door in place.
         for (Door existingDoor : doors) {
            if (existingDoor.orientation == door.orientation) {
               return;
            }
         }
         doors.add(door);
         // make sure we don't have a wall in the same orientation as this door:
         data &= ~door.orientation.bitMask;
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getBlockingDoorOrientations() {
      long orientationMask = 0;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            for (Door door : doors) {
               if (!door.isOpen() && !door.isHalfHeightWall()) {
                  orientationMask |= door.orientation.bitMask;
               }
            }
         }
      }
      return orientationMask;
   }

   public long getHalfHeightWallOrientations() {
      long orientationMask = 0;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            for (Door door : doors) {
               if (door.isHalfHeightWall()) {
                  orientationMask |= door.orientation.bitMask;
               }
            }
         }
      }
      return orientationMask;
   }

   public void addThing(Object thing) {
      if (thing != null) {
         if (thing instanceof Door) {
            addDoor((Door) thing);
         }
         else {
            synchronized (this) {
               lock_this.check();
               if (things.contains(thing)) {
                  return;
               }
            }
            ArenaLocation origLoc;
            synchronized (this) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
                  origLoc = clone();
                  if (thing instanceof Character) {
                     if (((Character)thing).stillFighting()) {
                        label = "";
                        for (Object obj : things) {
                           if (obj instanceof Character) {
                              if (((Character)obj).stillFighting()) {
                                 DebugBreak.debugBreak("multiple active characters on one hex");
                              }
                           }
                        }
                     }
                  }
                  things.add(thing);
               }
            }
            ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
            notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
         }
      }
   }
   public boolean hasThings(Object otherThan) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            for (Object thing : things) {
               if (otherThan != thing) {
                  return true;
               }
            }
         }
      }
      return false;
   }
   public List<Object> getThings() {
      return things;
   }
   public void clearItems() {
      ArenaLocation origLoc;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (things.size() == 0) {
               return;
            }
            origLoc = clone();
            things.clear();
         }
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public String nameThing(Object otherThan) {
      // First report any character in this location.
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            for (int i=1 ; i<=4 ; i++) {
               for (Object thing : things) {
                  if (thing != otherThan) {
                     switch (i) {
                        case 1: if (thing instanceof Character) {
                           return ((Character) thing).getName();
                        } break;
                        case 2: if (thing instanceof Weapon) {
                           return ((Weapon) thing).getName();
                        } break;
                        case 3: if (thing instanceof String) {
                           return ((String) thing);
                        } break;
                        case 4: if (thing instanceof Thing) {
                           return ((Thing) thing).getName();
                        } break;
                     }
                  }
               }
            }
         }
      }
      return null;
   }
   public boolean remove(Object thing) {
      ArenaLocation origLoc;
      boolean removed;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            origLoc = clone();
            removed = things.remove(thing);
         }
      }
      if (removed) {
         ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
         notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
         return true;
      }
      return false;
   }
   public boolean isEmpty() {
      synchronized (this) {
         lock_this.check();
         return things.size() == 0;
      }
   }
   @SuppressWarnings("unused")
   public String getLabel() {
      if (false) {
         return x + "," + y + "\n" + (label == null ? "" : label);
      }
      return label;
   }
   public void setLabel(String label) {
      if (((label == null) && (this.label != null)) ||
          ((label != null) && (!label.equals(this.label)))) {
         ArenaLocation origLoc = clone();
         this.label = label;
         ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
         notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      serializeContentsToStream(out);
   }
   public void serializeContentsToStream(DataOutputStream out) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            writeToStream(data, out);
            writeToStream(label, out);
            writeToStream(things.size(), out);
            for (Object thing : things) {
               if (thing instanceof Character) {
                  Character combatant = (Character) thing;
                  writeObject("ObjChr", out);
                  writeObject(combatant, out);
               } else if (thing instanceof Weapon) {
                  Weapon weap = (Weapon) thing;
                  writeObject("ObjStr", out);
                  writeObject(weap.getName(), out);
               } else if (thing instanceof String) {
                  writeObject("ObjStr", out);
                  writeObject(thing, out);
               } else if (thing instanceof Thing) {
                  writeObject("ObjStr", out);
                  writeObject(((Thing) thing).getName(), out);
               }
            }
            writeToStream(doors, out);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      serializeContentsFromStream(in);
   }
//   static public int readIntoListDoor(List<Door> data, DataInputStream in) throws IOException {
//      data.clear();
//      int size = in.readShort();
//      for (int i=0 ; i<size ; i++) {
//         Door door = new Door();
//         door.serializeFromStream(in);
//         data.add(door);
//      }
//      return size;
//   }
   public void serializeContentsFromStream(DataInputStream in)
   {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            setDataInternal(readLong(in));
            label = readString(in);
            int count = readInt(in);
            things.clear();
            for (int i=0 ; i<count ; i++) {
               String objID = SerializableObject.readString(in);
               if (objID.equals("ObjStr")) {
                  String object = readString(in);
                  Weapon weap = Weapons.getWeapon(object, null);
                  if (weap != null) {
                     if (weap.isUnarmedStyle()) {
                        things.add(object);
                     } else {
                        things.add(weap);
                     }
                  }
               }
               else if (objID.equals("ObjChr")) {
                  SerializableObject inObj = SerializableFactory.readObject(objID, in);
                  if (inObj instanceof Character) {
                     things.add(inObj);
                  }
               }
            }
//            readIntoListDoor(doors, in);
            for (SerializableObject obj : readIntoListSerializableObject(in)) {
               if (obj instanceof Door) {
                  // check if we already have this door in place.
                  boolean alreadyExists = false;
                  for (Door existingDoor : doors) {
                     if (existingDoor.orientation == ((Door) obj).orientation) {
                        alreadyExists = true;
                        break;
                     }
                  }
                  if (!alreadyExists) {
                     doors.add((Door) obj);
                  }
               }
            }

         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public List<Character> getCharacters() {
      // Report all characters in this location.
      List<Character> chars = new ArrayList<>();
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (things == null) {
               return chars;
            }
            for (Object thing : things) {
               if (thing instanceof Character) {
                  chars.add((Character) thing);
               }
            }
         }
      }
      return chars;
   }
   public int getCharacterCount(boolean onlyCountStandingCharacters, Character ignoreCharacter) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (things == null) {
               return 0;
            }
            int charCount = 0;
            for (Object thing : things) {
               if (thing instanceof Character) {
                  if (!onlyCountStandingCharacters || ((Character)thing).isStanding()) {
                     // we are not blocked by ourselves.
                     if ((ignoreCharacter == null) || (ignoreCharacter.uniqueID != ((Character)thing).uniqueID)) {
                        charCount++;
                     }
                  }
               }
            }
            return charCount;
         }
      }
   }

   public static boolean canMoveBetween(ArenaLocation fromLoc, ArenaLocation toLoc, boolean blockByCharacters) {
      return fromLoc.canExit(toLoc) && toLoc.canEnter(fromLoc, blockByCharacters);
   }
   public boolean canExit(ArenaCoordinates toCoord) {
      if (toCoord == null) {
         DebugBreak.debugBreak();
         return false;
      }

      if (toCoord.sameCoordinates(this))  {
         return true;
      }
      Facing charMoveDir = ArenaCoordinates.getFacingToLocation(this, toCoord);
      if (charMoveDir == null) {
         return false;
      }
      long fromWalls = getWallsAndClosedDoors();
      if (fromWalls !=0) {
         if (charMoveDir == Facing._6_OCLOCK) {
            return !TerrainWall.HORIZONTAL_BOTTOM.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_RIGHT_RIGHT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_LEFT_LEFT.contains(fromWalls);
         }
         else if (charMoveDir == Facing.NOON) {
            return !TerrainWall.HORIZONTAL_TOP.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_RIGHT_LEFT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_LEFT_RIGHT.contains(fromWalls);
         }
         else if (charMoveDir == Facing._8_OCLOCK) {
            return !TerrainWall.DIAG_LEFT_LEFT.contains(fromWalls) &&
                   !TerrainWall.VERT_LEFT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_LEFT_LEFT.contains(fromWalls);
         }
         else if (charMoveDir == Facing._2_OCLOCK) {
            return !TerrainWall.DIAG_LEFT_RIGHT.contains(fromWalls) &&
                   !TerrainWall.VERT_RIGHT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_LEFT_RIGHT.contains(fromWalls);
         }
         else if (charMoveDir == Facing._10_OCLOCK) {
            return !TerrainWall.DIAG_RIGHT_LEFT.contains(fromWalls) &&
                   !TerrainWall.VERT_LEFT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_RIGHT_LEFT.contains(fromWalls);
         }
         else if (charMoveDir == Facing._4_OCLOCK) {
            return !TerrainWall.DIAG_RIGHT_RIGHT.contains(fromWalls) &&
                   !TerrainWall.VERT_RIGHT.contains(fromWalls) &&
                   !TerrainWall.DIAG_FAR_RIGHT_RIGHT.contains(fromWalls);
         }
      }
      return true;

   }
   static private final HashMap<Facing, Long> BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION = new HashMap<>();
   static {
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing._6_OCLOCK , TerrainWall.HORIZONTAL_TOP.with(    TerrainWall.DIAG_FAR_RIGHT_LEFT.with( TerrainWall.DIAG_FAR_LEFT_RIGHT)));
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing.NOON      , TerrainWall.HORIZONTAL_BOTTOM.with( TerrainWall.DIAG_FAR_RIGHT_RIGHT.with(TerrainWall.DIAG_FAR_LEFT_LEFT)));
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing._8_OCLOCK , TerrainWall.DIAG_LEFT_RIGHT.with(   TerrainWall.VERT_RIGHT.with(          TerrainWall.DIAG_FAR_LEFT_RIGHT)));
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing._2_OCLOCK , TerrainWall.DIAG_LEFT_LEFT.with(    TerrainWall.VERT_LEFT.with(           TerrainWall.DIAG_FAR_LEFT_LEFT)));
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing._10_OCLOCK, TerrainWall.DIAG_RIGHT_RIGHT.with(  TerrainWall.VERT_RIGHT.with(          TerrainWall.DIAG_FAR_RIGHT_RIGHT)));
      BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.put(Facing._4_OCLOCK , TerrainWall.DIAG_RIGHT_LEFT.with(   TerrainWall.VERT_LEFT.with(           TerrainWall.DIAG_FAR_RIGHT_LEFT)));
   }
   public boolean canEnter(ArenaLocation fromLoc, boolean blockByCharacters) {
      if ((fromLoc != null) && (fromLoc.sameCoordinates(this))) {
         return true;
      }
      if (!getTerrain().canBeEnterd) {
         return false;
      }

      if (blockByCharacters) {
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
               for (Object thing : things) {
                  if (thing instanceof Character) {
                     if (fromLoc == null) {
                        return false;
                     }
                     Character character = (Character) thing;
                     // If we are a mult-hex character, don't be blocked by ourselves.
                     if (!fromLoc.things.contains(character)) {
                        if (character.stillFighting()) {
                           return false;
                        }
                     }
                  }
               }
            }
         }
      }

      long toWalls = getWallsAndClosedDoors();
      if (toWalls != 0) {
         if ((toWalls & TerrainWall.TERRAIN_ALL_CENTER_WALLS) != 0) {
            return false;
         }
         if (fromLoc == null) {
            return true;
         }

         Facing charMoveDir = ArenaCoordinates.getFacingToLocation(fromLoc, this);
         if (charMoveDir == null) {
            return false;
         }
         return (toWalls & BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.get(charMoveDir)) == 0;
      }
      return true;
   }

   private boolean doesEnteringCrossLowWalls(ArenaLocation fromLoc)
   {
      long toWalls = getHalfHeightWallOrientations();
      if ((fromLoc == null) || (toWalls == 0)) {
         return false;
      }
      if ((toWalls & TerrainWall.TERRAIN_ALL_CENTER_WALLS) != 0) {
         return true;
      }

      Facing charMoveDir = ArenaCoordinates.getFacingToLocation(fromLoc, this);
      if (charMoveDir == null) {
         return true;
      }
      return (toWalls & BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.get(charMoveDir)) != 0;
   }
   public byte costToEnter(ArenaLocation fromLoc, Character mover) {
      // even if we are flying, we can't traverse solid rock:
      if (terrain == TerrainType.SOLID_ROCK) {
         return 100;
      }

      byte entryCost;
      if (canEnter(fromLoc, true/*blockByCharacters*/)) {
         boolean isPenalizedInWater = mover.isPenalizedInWater();
         if (terrain.isWater && !isPenalizedInWater) {
            // water creatures in water always have a movement cost of 1
            entryCost = 1;
         }
         else {
            entryCost = (byte) terrain.costToEnter;
         }
         if (!terrain.isWater && mover.isPenalizedOutOfWater()) {
            // water creatures out of water have an additional penalty of 1:
            entryCost++;
         }
         if (doesEnteringCrossLowWalls(fromLoc)) {
            entryCost++;
         }
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
               for (Object thing : things) {
                  if (thing instanceof Character) {
                     // If we are a multi-hex character, don't be blocked by ourselves.
                     if (!fromLoc.things.contains(thing)) {
                        // We can't return 100 here, because if we do, then the AI.getAllRoutesFrom(...) method
                        // will not be able to find its way to this destination.
//                  Character character = (Character) thing;
//                  if (character.stillFighting()) {
//                     return 100;
//                  }
                        entryCost++;
                     }
                  }
               }
            }
         }
      }
      else {
         entryCost = 100;
      }

      // checking if we are flying is a rare exception, and is costly to determine, so
      // only check this in the less common case of a higher entry cost:
      if ((entryCost > 1) && (mover.isFlying())) {
         return 1;
      }
      return entryCost;
   }

   public static ArenaLocation getForwardMovement(ArenaCoordinates curCoord, Facing direction, CombatMap map) {
      return map.getLocation(getForwardMovement(curCoord, direction));
   }

   public RGB getRGBColor() {
      return terrain.color;
   }
   public int getRGBColorAsInt() {
      return terrain.colorAsInt;
   }
   public TerrainType getTerrain() {
      return terrain;
   }
   public String getTerrainName() {
      //return TERRAIN_NAMES[(int) (data & TERRAIN_MASK)];
      return terrain.name;
   }
   public byte getAttackPenaltyForTerrain(boolean attackerIsPenalizedInWater) {
      if (!attackerIsPenalizedInWater && terrain.isWater) {
         return 0;
      }
      return terrain.attackPenalty;
   }

   // TODO: call this:
   public byte getDefensePenaltyForTerrain(boolean attackerIsAquatic, byte defenseType) {
      if (attackerIsAquatic && terrain.isWater) {
         return 0;
      }
      return terrain.defensePenalty;
   }
   public void setTerrain(TerrainType terrain) {
      long walls = getWalls();
      ArenaLocation origLoc = clone();
      data = (data & ~TerrainType.MASK) | (terrain.value);
      this.terrain = terrain;
      addWall(walls);
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getWalls() {
      return (data & TerrainWall.MASK);
   }
   public long getWallsAndClosedDoors() {
      return getWalls() | getBlockingDoorOrientations();
   }
   public void removeAllWalls() {
      if (getWalls() != 0) {
         ArenaLocation origLoc = clone();
         data = terrain.value;
         ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
         notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
      }
   }
   public void removeAllDoors() {
      ArenaLocation origLoc;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (doors.isEmpty()) {
               return;
            }
            origLoc = clone();
            doors.clear();
         }
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public void addWall(long walls) {
      long pureWalls = (walls & TerrainWall.MASK);
      if ((data & pureWalls) == pureWalls) {
         return;
      }
      ArenaLocation origLoc = clone();
      data |= pureWalls;
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public boolean hasSameContents(ArenaLocation comp) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (data != comp.data) {
               return false;
            }
            if (terrain != comp.terrain) {
               return false;
            }
            if (!listsMatch(doors, comp.doors))  {
               return false;
            }
            if (!listsMatch(things, comp.things)) {
               return false;
            }
         }
      }
      return true;
   }
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ArenaLoc {").append(x).append(",").append(y).append("}");
      sb.append(", things:[");
      boolean first = true;
      for (Object thing : things) {
         if (!first) {
            sb.append("\n");
         }
         first = false;
         sb.append(thing);
      }
      sb.append("]");
      return sb.toString();
   }

   public void addLocationActions(RequestAction req, Character actor)
   {
      int actionOpt = 0;
      RequestActionType actOpt;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            for (Object thing : things) {
               actOpt = RequestActionType.getLocationActionByIndex(actionOpt++);
               if (actOpt == null) {
                  // This will happen if there are too many items to pick up, based on the number of location options defined
                  break;
               }
               String name;
               if (thing instanceof Thing) {
                  name = PICKUP + ((Thing) thing).getName();
               }
               else if (thing instanceof String) {
                  name = PICKUP + thing;
               }
               else {
                  if (!(thing instanceof Character)) {
                     DebugBreak.debugBreak("unknown object type.");
                  }
                  continue;
               }
               List<LimbType> preferredLimbTypes;
               List<LimbType> altLimbTypes;
               List<Limb> validLimbs = new ArrayList<>();
               if (thing instanceof Shield) {
                  preferredLimbTypes = Arrays.asList(LimbType.HAND_LEFT, LimbType.HAND_LEFT_2, LimbType.HAND_LEFT_3);
                  altLimbTypes = Arrays.asList(LimbType.HAND_RIGHT, LimbType.HAND_RIGHT_2, LimbType.HAND_RIGHT_3);
               }
               else {
                  preferredLimbTypes = Arrays.asList(LimbType.HAND_RIGHT, LimbType.HAND_RIGHT_2, LimbType.HAND_RIGHT_3);
                  altLimbTypes = Arrays.asList(LimbType.HAND_LEFT, LimbType.HAND_LEFT_2, LimbType.HAND_LEFT_3);
               }
               for (LimbType limbType : preferredLimbTypes) {
                  Limb limb = actor.getLimb(limbType);
                  if ((limb != null) && (limb.isEmpty())) {
                     validLimbs.add(limb);
                  }
               }
               if (validLimbs.isEmpty()) {
                  for (LimbType limbType : altLimbTypes) {
                     Limb limb = actor.getLimb(limbType);
                     if ((limb != null) && (limb.isEmpty())) {
                        validLimbs.add(limb);
                     }
                  }
               }
               for (Limb limb : validLimbs) {
                  if (validLimbs.size() > 1) {
                     name += " (" + limb.limbType.name + ")";
                  }
                  req.addOption(new RequestActionOption(name, actOpt, limb.limbType, true));
               }
            }
         }
      }
   }
   public String getActionDescription(Character actor, RequestAction actionReq)
   {
      // If the action is to pick up an item, then the item has already been picked up
      // when this is called, so we must use the request to describe the action
      String answer = actionReq.getAnswer();
      if (answer.startsWith(PICKUP)) {
          return actor.getName() + " picks up a " + answer.substring(PICKUP.length());
      }
      return null;
   }
   public boolean isPickupItem(Character actor, RequestAction actionReq)
   {
      String answer = actionReq.getAnswer();
      return answer.startsWith(PICKUP);
   }
   public boolean applyAction(Character actor, RequestAction actionReq) {
      return false;
   }

   public String getPickupItemName(Character actor, RequestAction actionReq, int itemIndex)
   {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (itemIndex < things.size()) {
               Object thing = things.get(itemIndex);
               if (thing instanceof Thing) {
                  return ((Thing) thing).getName();
               }
               if (thing instanceof String) {
                  return (String) thing;
               }
            }
         }
      }
      return null;
   }
   public Object pickupItem(Character actor, RequestAction actionReq, int itemIndex, Diagnostics diag)
   {
      ArenaLocation originalLoc;
      ObjectChanged changeNotification = null;
      Object thing = null;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            originalLoc = this.clone();
            if (itemIndex < things.size()) {
               thing = things.remove(itemIndex);
               if (thing != null) {
                  if (thing instanceof String) {
                     thing = Thing.getThing((String)thing, actor.getRace());
                  }
                  changeNotification = new ObjectChanged(originalLoc, this);
               }
            }
         }
      }
      if (changeNotification != null)
      {
         notifyWatchers(originalLoc, this, changeNotification , null/*skipList*/, diag);
         return thing;
      }
      return null;
   }

   // IMonitorableObject methods:
   @Override
   public String getObjectIDString()
   {
      return monitoredProxy.getObjectIDString();
   }
   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers()
   {
      return monitoredProxy.getSnapShotOfWatchers();
   }
   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      if (monitoredProxy == null) {
         DebugBreak.debugBreak();
      }
      else {
         monitoredProxy.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
      }
   }
   @Override
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return monitoredProxy.registerAsWatcher(watcherObject, diag);
   }
   @Override
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return monitoredProxy.unregisterAsWatcher(watcherObject, diag);
   }
   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return monitoredProxy.unregisterAsWatcherAllInstances(watcherObject, diag);
   }

//   private double getHashValue() {
//      return Math.pow(2.34567, x) + Math.pow(8.7654, y);
//   }

//   public void setVisible(CombatMap map, ArenaLocation viewerLoc, int viewerID) {
//      boolean isVisible = (viewerLoc==null) || (map.hasLineOfSight(viewerLoc, this, false/*blockedByAnyStandingCharacter*/));
//      setVisible(isVisible, map, viewerLoc, viewerID, true/*basedOnFacing*/);
//   }

   /*
    * returns true if the visibility changed, false if the visibility didn't change
    */
   public boolean setVisible(boolean isVisible, CombatMap map, ArenaLocation viewerLoc, int viewerID, boolean basedOnFacing) {
      boolean oldVisibility = visibleTo.contains(viewerID);
      boolean newVisibility = isVisible;
      if (sameCoordinates(viewerLoc)) {
         newVisibility = true;
      }
      else if (newVisibility && (map != null) && (viewerLoc != null) && basedOnFacing && (viewerID >= 0)) {
         for (Character viewer : viewerLoc.getCharacters()) {
            if (viewer.uniqueID == viewerID) {
               // This is the character looking at this hex.
               if (!map.isFacing(viewer, this) && !viewer.hasPeripheralVision()) {
                  newVisibility = false;
               }
               break;
            }
         }
      }
      if (newVisibility == oldVisibility) {
         return false;
      }

      if (viewerID == -1) {
         visibleTo.clear();
         return true;
      }
      if (viewerID >= 0) {
         Integer viewerInt = viewerID;
         if (newVisibility ) {
            if ((map != null) && (viewerLoc != null)) {
               ArenaCoordinates visibleFrom = map.getFirstLocationInPath(this, viewerLoc);
               visibleToCharacterFromLoc.put(viewerInt, visibleFrom);
               short dist = ArenaCoordinates.getDistance(this, visibleFrom);
               if (dist > 1) {
                  DebugBreak.debugBreak();
               }
            }
            visibleTo.add(viewerInt);
            viewedBy.add(viewerInt);
         }
         else {
            visibleTo.remove(viewerInt);
            visibleToCharacterFromLoc.remove(viewerInt);
         }
      }
      return true;
   }
   public boolean getVisible(int viewerID) {
      return visibleTo.contains(viewerID);
   }
   public boolean isKnownBy(int viewerID) {
      return (viewedBy.contains(viewerID));
   }
   public boolean setKnownBy(int viewerID, boolean isKnown) {
      Integer intVal = viewerID;
      if (isKnown) {
         if (viewedBy.contains(intVal)) {
            return false;
         }
         viewedBy.add(intVal);
         return true;
      }
      return viewedBy.remove(intVal);
   }
   public void clearCharacterViewedHistory() {
      viewedBy.clear();
   }

   public boolean sameContents(ArenaLocation otherLoc) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_this)) {
            if (data != otherLoc.data) {
               return false;
            }
            if (terrain != otherLoc.terrain) {
               return false;
            }
            if (!listsMatch(things, otherLoc.things)) {
               return false;
            }
            if (!listsMatch(doors, otherLoc.doors)) {
               return false;
            }
            if ((label != null) && (otherLoc.label != null)) {
               return label.equals(otherLoc.label);
            }
            else return (label == null) && (otherLoc.label == null);
         }
      }
   }

   private <T> boolean listsMatch(List<T> l1, List<T> l2) {
      if (l1.size() != l2.size()) {
         return false;
      }
      for (int i = 0; i < l1.size(); i++) {
         if (!l1.get(i).equals(l2.get(i))) {
            return false;
         }
      }
      return true;
   }

   public boolean getSelectable() {
      return selectable;
   }
   public void setSelectable(boolean selectable) {
      this.selectable = selectable;
   }
}
