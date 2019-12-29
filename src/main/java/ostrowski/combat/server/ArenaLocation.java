/*
 * Created on May 23, 2006
 *
 */
package ostrowski.combat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

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
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.SerializableFactory;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.CombatSemaphore;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.MonitoredObject;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoTracker;

public class ArenaLocation extends ArenaCoordinates implements IMonitorableObject, Enums
{
   public    Semaphore             _lock_this               = new Semaphore("AreanLocation_lock_this", CombatSemaphore.CLASS_ARENALOCATION_this);
   private   TerrainType           _terrain                 = TerrainType.FLOOR;
   private   long                  _data                    = _terrain.value;
   private   ArrayList<Object>     _things                  = new ArrayList<>();
   private   ArrayList<Door>       _doors                   = new ArrayList<>();
   private   ArrayList<IAreaSpell> _activeSpells            = new ArrayList<>();
   private   HashSet<Integer>      _visibleTo               = new HashSet<>();
   private   HashSet<Integer>      _viewedBy                = new HashSet<>();
   public    HashMap<Integer, ArenaCoordinates> _visibleToCharacterFromLoc  = new HashMap<>();
   private   String                _label                   = null;
   transient MonitoredObject       _monitoredProxy          = null;
   private   boolean               _selectable              = true;
   public static final String      PICKUP                   = "pickup ";

   public ArenaLocation(short x, short y) {
      super(x,y);
      if ((x%2) != (y%2)) {
         DebugBreak.debugBreak();
         throw new IllegalArgumentException(" x="+x+", y="+y+" is illegal!");
      }
      _monitoredProxy = new MonitoredObject("ArenaLoc:"+x+","+y, this);
   }
   public ArenaLocation() {
      // This constructor is only used by serialization.
      // Serialized objects should never need to be watched, since
      // they are only sent from the server to the client, and
      // the client doesn't need any watching/watcher functionality.
      _monitoredProxy = null;
   }

   public void setDataInternal(long data)
   {
      _data = data;
      _terrain = TerrainType.getByValue(data);
   }
   public void setData(long data)
   {
      if (_data == data) {
         return;
      }
      ArenaLocation origLoc = clone();
      setDataInternal(data);
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getData() { return _data; }

   public void addSpell(IAreaSpell spell) {
      _activeSpells.add(spell);
      // Affect the characters in the location
      for (Character chr : getCharacters()) {
         spell.affectCharacterOnActivation(chr);
      }
   }
   public List<IAreaSpell> getActiveSpells() {
      ArrayList<IAreaSpell> activeSpells = new ArrayList<>();
      // Check for any spells that have expired
      for (IAreaSpell spell : _activeSpells) {
         if (((Spell)spell).getDuration() != 0) {
            activeSpells.add(spell);
         }
      }
      _activeSpells = activeSpells;
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
      _data  = source._data;
      _terrain = source._terrain;
      _doors = new ArrayList<>();
      for (Door door : source._doors) {
         _doors.add(door.clone());
      }
      _label = source._label;
      _things = new ArrayList<>();
      for (int i=0 ; i<source._things.size() ; i++) {
         // Clone the object, if possible
         Object thing = source._things.get(i);
         if (thing instanceof Thing) {
            Thing dupThing = ((Thing)thing).clone();
            if (dupThing == null) {
               DebugBreak.debugBreak();
            }
            else {
               _things.add(dupThing);
            }
         }
         else {
            _things.add(thing);
         }
      }
      _visibleTo = new HashSet<>();
      _visibleToCharacterFromLoc = new HashMap<>();
      _viewedBy = new HashSet<>();
      if (source._visibleTo != null) {
         _visibleTo.addAll(source._visibleTo);
      }
      if (source._visibleToCharacterFromLoc != null) {
         for (Integer charId : source._visibleToCharacterFromLoc.keySet()) {
            ArenaCoordinates visFrom = source._visibleToCharacterFromLoc.get(charId);
            _visibleToCharacterFromLoc.put(charId, new ArenaCoordinates(visFrom._x, visFrom._y));
         }
      }
      if (source._viewedBy != null) {
         _viewedBy.addAll(source._viewedBy);
      }
   }

   public void copyData(ArenaLocation source)
   {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            copyDataUnsafe(source);
         }
      }
   }

   public ArrayList<Door> getDoors() {
      return _doors;
   }
   public void addDoor(Door door) {
      ArenaLocation origLoc = clone();
      synchronized (this) {
         _lock_this.check();
         // check if we already have this door in place.
         for (Door existingDoor : _doors) {
            if (existingDoor._orientation == door._orientation) {
               return;
            }
         }
         _doors.add(door);
         // make sure we don't have a wall in the same orientation as this door:
         _data &= ~door._orientation.bitMask;
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getBlockingDoorOrientations() {
      long orientationMask = 0;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            for (Door door : _doors) {
               if (!door.isOpen()) {
                  orientationMask |= door._orientation.bitMask;
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
               _lock_this.check();
               if (_things.contains(thing)) {
                  return;
               }
            }
            ArenaLocation origLoc;
            synchronized (this) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
                  origLoc = clone();
                  if (thing instanceof Character) {
                     if (((Character)thing).stillFighting()) {
                        _label = "";
                        for (Object obj : _things) {
                           if (obj instanceof Character) {
                              if (((Character)obj).stillFighting()) {
                                 DebugBreak.debugBreak("multiple active characters on one hex");
                              }
                           }
                        }
                     }
                  }
                  _things.add(thing);
               }
            }
            ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
            notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
         }
      }
   }
   public boolean hasThings(Object otherThan) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            for (Object thing : _things) {
               if (otherThan != thing) {
                  return true;
               }
            }
         }
      }
      return false;
   }
   public ArrayList<Object> getThings() {
      return _things;
   }
   public void clearItems() {
      ArenaLocation origLoc;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_things.size() == 0) {
               return;
            }
            origLoc = clone();
            _things.clear();
         }
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public String nameThing(Object otherThan) {
      // First report any character in this location.
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            for (int i=0 ; i<4 ; i++) {
               for (Object thing : _things) {
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            origLoc = clone();
            removed = _things.remove(thing);
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
         _lock_this.check();
         return _things.size() == 0;
      }
   }
   @SuppressWarnings("unused")
   public String getLabel() {
      if (false) {
         return _x + "," + _y + "\n" + (_label == null ? "" : _label);
      }
      return _label;
   }
   public void setLabel(String label) {
      if (((label == null) && (_label != null)) ||
          ((label != null) && (!label.equals(_label)))) {
         ArenaLocation origLoc = clone();
         _label = label;
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            writeToStream(_data, out);
            writeToStream(_label, out);
            writeToStream(_things.size(), out);
            for (int i=0 ; i<_things.size() ; i++) {
               if (_things.get(i) instanceof Character) {
                  Character combatant = (Character) _things.get(i);
                  writeObject("ObjChr", out);
                  writeObject(combatant, out);
               }
               else if (_things.get(i) instanceof Weapon) {
                  Weapon weap = (Weapon) _things.get(i);
                  writeObject("ObjStr", out);
                  writeObject(weap.getName(), out);
               }
               else if (_things.get(i) instanceof String) {
                  writeObject("ObjStr", out);
                  writeObject(_things.get(i), out);
               }
               else if (_things.get(i) instanceof Thing) {
                  writeObject("ObjStr", out);
                  writeObject(((Thing)_things.get(i)).getName(), out);
               }
            }
            writeToStream(_doors, out);
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            setDataInternal(readLong(in));
            _label = readString(in);
            int count = readInt(in);
            _things.clear();
            for (int i=0 ; i<count ; i++) {
               String objID = SerializableObject.readString(in);
               if (objID != null) {
                  if (objID.equals("ObjStr")) {
                     String object = readString(in);
                     Weapon weap = Weapon.getWeapon(object, null);
                     if (weap.isUnarmedStyle()) {
                        if (object != null) {
                           _things.add(object);
                        }
                     }
                     else {
                        _things.add(weap);
                     }
                  }
                  else if (objID.equals("ObjChr")) {
                     SerializableObject inObj = SerializableFactory.readObject(objID, in);
                     if ((inObj != null) && (inObj instanceof Character)) {
                        _things.add(inObj);
                     }
                  }
               }
            }
//            readIntoListDoor(_doors, in);
            for (SerializableObject obj : readIntoListSerializableObject(in)) {
               if (obj instanceof Door) {
                  // check if we already have this door in place.
                  boolean alreadyExists = false;
                  for (Door existingDoor : _doors) {
                     if (existingDoor._orientation == ((Door) obj)._orientation) {
                        alreadyExists = true;
                        continue;
                     }
                  }
                  if (!alreadyExists) {
                     _doors.add((Door) obj);
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_things == null) {
               return chars;
            }
            for (Object thing : _things) {
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_things == null) {
               return 0;
            }
            int charCount = 0;
            for (Object thing : _things) {
               if (thing instanceof Character) {
                  if (!onlyCountStandingCharacters || ((Character)thing).isStanding()) {
                     // we are not blocked by ourselves.
                     if ((ignoreCharacter == null) || (ignoreCharacter._uniqueID != ((Character)thing)._uniqueID)) {
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
      boolean canMove1 = fromLoc.canExit(toLoc) && toLoc.canEnter(fromLoc, blockByCharacters);
      return canMove1;
   }
   public boolean canExit(ArenaCoordinates toCoord) {
      if (toCoord == null) {
         DebugBreak.debugBreak();
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
            if (TerrainWall.HORIZONTAL_BOTTOM.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_RIGHT_RIGHT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_LEFT_LEFT.contains(fromWalls)) {
               return false;
            }
         }
         else if (charMoveDir == Facing.NOON) {
            if (TerrainWall.HORIZONTAL_TOP.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_RIGHT_LEFT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_LEFT_RIGHT.contains(fromWalls)) {
               return false;
            }
         }
         else if (charMoveDir == Facing._8_OCLOCK) {
            if (TerrainWall.DIAG_LEFT_LEFT.contains(fromWalls) ||
                TerrainWall.VERT_LEFT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_LEFT_LEFT.contains(fromWalls)) {
               return false;
            }
         }
         else if (charMoveDir == Facing._2_OCLOCK) {
            if (TerrainWall.DIAG_LEFT_RIGHT.contains(fromWalls) ||
                TerrainWall.VERT_RIGHT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_LEFT_RIGHT.contains(fromWalls)) {
               return false;
            }
         }
         else if (charMoveDir == Facing._10_OCLOCK) {
            if (TerrainWall.DIAG_RIGHT_LEFT.contains(fromWalls) ||
                TerrainWall.VERT_LEFT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_RIGHT_LEFT.contains(fromWalls)) {
               return false;
            }
         }
         else if (charMoveDir == Facing._4_OCLOCK) {
            if (TerrainWall.DIAG_RIGHT_RIGHT.contains(fromWalls) ||
                TerrainWall.VERT_RIGHT.contains(fromWalls) ||
                TerrainWall.DIAG_FAR_RIGHT_RIGHT.contains(fromWalls)) {
               return false;
            }
         }
      }
      return true;

   }
   static private HashMap<Facing, Long> BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION = new HashMap<>();
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
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
               for (Object thing : _things) {
                  if (thing instanceof Character) {
                     if (fromLoc == null) {
                        return false;
                     }
                     Character character = (Character) thing;
                     // If we are a mult-hex character, don't be blocked by ourselves.
                     if (!fromLoc._things.contains(character)) {
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
         if ((toWalls & BLOCKING_WALLS_FOR_MOVEMENT_IN_DIRECTION.get(charMoveDir)) != 0) {
            return false;
         }
      }
      return true;
   }
   public byte costToEnter(ArenaLocation fromLoc, Character mover) {
      // even if we are flying, we can't traverse solid rock:
      if (_terrain == TerrainType.SOLID_ROCK) {
         return 100;
      }

      byte entryCost = 0;
      if (canEnter(fromLoc, true/*blockByCharacters*/)) {
         boolean isPenalizedInWater = mover.isPenalizedInWater();
         if (_terrain.isWater && !isPenalizedInWater) {
            // water creatures in water always have a movement cost of 1
            entryCost = 1;
         }
         else {
            entryCost = (byte) _terrain.costToEnter;
         }
         if (!_terrain.isWater && mover.isPenalizedOutOfWater()) {
            // water creatures out of water have an additional penalty of 1:
            entryCost++;
         }
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
               for (Object thing : _things) {
                  if (thing instanceof Character) {
                     // If we are a multi-hex character, don't be blocked by ourselves.
                     if (!fromLoc._things.contains(thing)) {
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
      if (entryCost > 1) {
         if (mover.isFlying()) {
            return 1;
         }
      }
      return entryCost;
   }

   public static ArenaLocation getForwardMovement(ArenaCoordinates curCoord, Facing direction, CombatMap map) {
      return map.getLocation(getForwardMovement(curCoord, direction));
   }

   public RGB getRGBColor() {
      return _terrain.color;
   }
   public int getRGBColorAsInt() {
      return _terrain.colorAsInt;
   }
   public TerrainType getTerrain() {
      return _terrain;
   }
   public String getTerrainName() {
      //return TERRAIN_NAMES[(int) (_data & TERRAIN_MASK)];
      return _terrain.name;
   }
   public byte getAttackPenaltyForTerrain(boolean attackerIsPenalizedInWater) {
      if (!attackerIsPenalizedInWater && _terrain.isWater) {
         return 0;
      }
      return _terrain.attackPenalty;
   }

   // TODO: call this:
   public byte getDefensePenaltyForTerrain(boolean attackerIsAquatic, byte defenseType) {
      if (attackerIsAquatic && _terrain.isWater) {
         return 0;
      }
      return _terrain.defensePenalty;
   }
   public void setTerrain(TerrainType terrain) {
      long walls = getWalls();
      ArenaLocation origLoc = clone();
      _data = (_data & ~TerrainType.MASK) | (terrain.value);
      _terrain = terrain;
      addWall(walls);
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public long getWalls() {
      return (_data & TerrainWall.MASK);
   }
   public long getWallsAndClosedDoors() {
      return getWalls() | getBlockingDoorOrientations();
   }
   public void removeAllWalls() {
      if (getWalls() != 0) {
         ArenaLocation origLoc = clone();
         _data = _terrain.value;
         ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
         notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
      }
   }
   public void removeAllDoors() {
      ArenaLocation origLoc;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_doors.isEmpty()) {
               return;
            }
            origLoc = clone();
            _doors.clear();
         }
      }
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public void addWall(long walls) {
      long pureWalls = (walls & TerrainWall.MASK);
      if ((_data & pureWalls) == pureWalls) {
         return;
      }
      ArenaLocation origLoc = clone();
      _data |= pureWalls;
      ObjectChanged changeNotif = new ObjectChanged(origLoc, this);
      notifyWatchers(origLoc, this, changeNotif, null/*skipList*/, null/*diag*/);
   }
   public boolean hasSameContents(ArenaLocation comp) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_data != comp._data) {
               return false;
            }
            if (_terrain != comp._terrain) {
               return false;
            }
            if (_doors.size()  != comp._doors.size()) {
               return false;
            }
            for (int d=0 ; d<_doors.size() ; d++) {
               if (!_doors.get(d).equals(comp._doors.get(d))) {
                  return false;
               }
            }
            if (_things.size() != comp._things.size()) {
               return false;
            }
            for (int t=0 ; t<_things.size() ; t++) {
               if (!_things.get(t).equals(comp._things.get(t))) {
                  return false;
               }
            }
         }
      }
      return true;
   }
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ArenaLoc {").append(_x).append(",").append(_y).append("}");
      sb.append(", things:[");
      boolean first = true;
      for (Object thing : _things) {
         if (!first) {
            sb.append("\n");
         }
         first = false;
         sb.append(thing.toString());
      }
      sb.append("]");
      return sb.toString();
   }

   public void addLocationActions(RequestAction req, Character actor)
   {
      int actionOpt = 0;
      RequestActionType actOpt;
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            for (Object thing : _things) {
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
                  name = PICKUP + (String) thing;
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
                     name += " (" + limb._limbType.name + ")";
                  }
                  req.addOption(new RequestActionOption(name, actOpt, limb._limbType, true));
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
      if (answer.startsWith(PICKUP)) {
         return true;
      }
      return false;
   }
   public boolean applyAction(Character actor, RequestAction actionReq) {
      return false;
   }

   public String getPickupItemName(Character actor, RequestAction actionReq, int itemIndex)
   {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (itemIndex < _things.size()) {
               Object thing = _things.get(itemIndex);
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
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            originalLoc = this.clone();
            if (itemIndex < _things.size()) {
               thing = _things.remove(itemIndex);
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
      return _monitoredProxy.getObjectIDString();
   }
   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers()
   {
      return _monitoredProxy.getSnapShotOfWatchers();
   }
   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      if (_monitoredProxy == null) {
         DebugBreak.debugBreak();
      }
      else {
         _monitoredProxy.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
      }
   }
   @Override
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return _monitoredProxy.registerAsWatcher(watcherObject, diag);
   }
   @Override
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return _monitoredProxy.unregisterAsWatcher(watcherObject, diag);
   }
   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return _monitoredProxy.unregisterAsWatcherAllInstances(watcherObject, diag);
   }

//   private double getHashValue() {
//      return Math.pow(2.34567, _x) + Math.pow(8.7654, _y);
//   }

//   public void setVisible(CombatMap map, ArenaLocation viewerLoc, int viewerID) {
//      boolean isVisible = (viewerLoc==null) || (map.hasLineOfSight(viewerLoc, this, false/*blockedByAnyStandingCharacter*/));
//      setVisible(isVisible, map, viewerLoc, viewerID, true/*basedOnFacing*/);
//   }

   /*
    * returns true if the visibility changed, false if the visibility didn't change
    */
   public boolean setVisible(boolean isVisible, CombatMap map, ArenaLocation viewerLoc, int viewerID, boolean basedOnFacing) {
      boolean oldVisibility = _visibleTo.contains(viewerID);
      boolean newVisibility = isVisible;
      if (sameCoordinates(viewerLoc)) {
         newVisibility = true;
      }
      else if (newVisibility && (map != null) && (viewerLoc != null) && basedOnFacing && (viewerID >= 0)) {
         for (Character viewer : viewerLoc.getCharacters()) {
            if (viewer._uniqueID == viewerID) {
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
         _visibleTo.clear();
         return true;
      }
      if (viewerID >= 0) {
         Integer viewerInt = viewerID;
         if (newVisibility ) {
            if ((map != null) && (viewerLoc != null)) {
               ArenaCoordinates visibleFrom = map.getFirstLocationInPath(this, viewerLoc);
               _visibleToCharacterFromLoc.put(viewerInt, visibleFrom);
               short dist = ArenaCoordinates.getDistance(this, visibleFrom);
               if (dist > 1) {
                  DebugBreak.debugBreak();
               }
            }
            _visibleTo.add(viewerInt);
            _viewedBy.add(viewerInt);
         }
         else {
            _visibleTo.remove(viewerInt);
            _visibleToCharacterFromLoc.remove(viewerInt);
         }
      }
      return true;
   }
   public boolean getVisible(int viewerID) {
      return _visibleTo.contains(viewerID);
   }
   public boolean isKnownBy(int viewerID) {
      return (_viewedBy.contains(viewerID));
   }
   public boolean setKnownBy(int viewerID, boolean isKnown) {
      Integer intVal = viewerID;
      if (isKnown) {
         if (_viewedBy.contains(intVal)) {
            return false;
         }
         _viewedBy.add(intVal);
         return true;
      }
      return _viewedBy.remove(intVal);
   }
   public void clearCharacterViewedHistory() {
      _viewedBy.clear();
   }

   public boolean sameContents(ArenaLocation otherLoc) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_this)) {
            if (_data != otherLoc._data) {
               return false;
            }
            if (_terrain != otherLoc._terrain) {
               return false;
            }
            if (_things.size() != otherLoc._things.size()) {
               return false;
            }
            for (int i=0 ; i<_things.size(); i++) {
               if (!_things.get(i).equals(otherLoc._things.get(i))) {
                  return false;
               }
            }
            if (_doors.size() != otherLoc._doors.size()) {
               return false;
            }
            for (int i=0 ; i<_doors.size(); i++) {
               if (!_doors.get(i).equals(otherLoc._doors.get(i))) {
                  return false;
               }
            }
            if ((_label != null) && (otherLoc._label != null)) {
               if (!_label.equals(otherLoc._label)) {
                  return false;
               }
            }
            else if ((_label != null) || (otherLoc._label != null)) {
               return false;
            }
            return true;
         }
      }
   }
   public boolean getSelectable() {
      return _selectable;
   }
   public void setSelectable(boolean selectable) {
      _selectable  = selectable;
   }
}
