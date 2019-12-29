package ostrowski.combat.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.mage.SpellSpiderWeb;
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.DoorState;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Potion;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.combat.protocol.request.RequestArenaEntrance;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.ArenaTrigger;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.BattleTerminatedException;
import ostrowski.combat.server.ClientProxy;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.ObjectChanged;
import ostrowski.protocol.SerializableObject;
import ostrowski.util.AnglePair;
import ostrowski.util.AnglesList;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.MonitoredObject;
import ostrowski.util.MonitoringObject;
import ostrowski.util.SemaphoreAutoTracker;

public class CombatMap extends SerializableObject implements Enums, IMonitorableObject, IMonitoringObject, Cloneable {
   transient private MonitoringObject _mapWatcher;
   transient private MonitoredObject  _locBook;

   private String                     _name                     = "default";
   private short                      _level;
   private short                      _sizeY;
   private short                      _sizeX;
   private ArenaLocation[][]          _locations                = null;
   private boolean                    _hideViewFromLocalPlayers = true;

   private byte                       _maxCombatantsPerTeam     = CombatServer.MAX_COMBATANTS_PER_TEAM;
   private byte                       _teamCount                = CombatServer.MAX_TEAMS;
   private ArenaLocation[][]          _startPoints              = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
   private String[][]                 _stockAiName              = new String[_teamCount][_maxCombatantsPerTeam];
   private String[][]                 _stockCharName            = new String[_teamCount][_maxCombatantsPerTeam];
   private final ArrayList<ArenaTrigger> _triggers              = new ArrayList<>();
   private ArenaTrigger               _selectedTrigger;
   transient private Diagnostics      _diag;

   public CombatMap() {
      _locBook    = new MonitoredObject("CombatMap._locBook");
      _mapWatcher = new MonitoringObject("CombatMap._mapWatcher", _locBook);
      IMonitorableObject._monitoredObj._objectIDString = this.getClass().getName() + "._locBook";
      IMonitoringObject._monitoringObj._objectIDString = this.getClass().getName() + "._mapWatcher";
      IMonitoringObject._monitoringObj._forwardObject = _locBook;
   }
   public CombatMap(short sizeX, short sizeY, Diagnostics diag) {
      this();
      _diag = diag;
      _locations = new ArenaLocation[sizeX][sizeY];
      _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
      _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
      _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
      _level = 0;
      _sizeX = sizeX;
      _sizeY = sizeY;
      for (short col = 0 ; col<sizeX ; col++) {
         for (short row = (short) (col%2) ; row<sizeY ; row += 2) {
            _locations[col][row] = new ArenaLocation(col, row);
            _locations[col][row].registerAsWatcher(this, _diag);
         }
      }
   }
   public ArrayList<ArenaTrigger> getTriggers() {
      return _triggers;
   }
   public void setSize(short newX, short newY) {
      if ((newX<_sizeX) || (newY<_sizeY)) {
         // If the map got smaller, clear all the starting points.
         for (byte team=0 ; team<_teamCount ; team++) {
            for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur ++) {
               ArenaLocation startLoc = _startPoints[team][cur];
               if (startLoc != null) {
                  if ((startLoc._x < newX) || (startLoc._y < newY)) {
                     _startPoints[team][cur] = null;
                  }
               }
            }
         }
         clearStartingPointLabels();
         clearCharacterStartingLocations();
      }
      ArenaLocation[][] newLocations = new ArenaLocation[newX][newY];
      for (short col = 0 ; col<newX ; col++) {
         for (short row = (short) (col%2) ; row<newY ; row += 2) {
            if ((col<_sizeX) && (row<_sizeY)) {
               newLocations[col][row] = _locations[col][row];
            }
            else {
               newLocations[col][row] = new ArenaLocation(col, row);
               newLocations[col][row].registerAsWatcher(this, _diag);
            }
         }
      }
      _locations = newLocations;
      _sizeX = newX;
      _sizeY = newY;
   }
   public void clearCharacterStartingLocations() {
      _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
      _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
      _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
   }
   public short getLevel() { return _level;}
   public short getSizeY() { return _sizeY;}
   public short getSizeX() { return _sizeX;}
   public byte getAvailableCombatantIndexOnTeam(byte team) {
      if (team == -1) {
         return -1;
      }

      for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
         ArenaLocation loc = _startPoints[team][cur];
         if ((loc != null) && (loc.getCharacters().size() == 0)) {
            return cur;
         }
      }
      return -1;
   }
   public byte[] getAvailableCombatantsOnTeams() {
      byte[] roomOnTeam = new byte[TEAM_NAMES.length];
      for (byte team=0 ; team<_teamCount ; team++) {
         roomOnTeam[team] = 0;
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            ArenaLocation loc = _startPoints[team][cur];
            if ((loc != null) && (loc.getCharacters().size() == 0)) {
               roomOnTeam[team]++;
            }
         }
      }
      return roomOnTeam;
   }
   public Map<Byte, List<RequestArenaEntrance.TeamMember>> getRemoteTeamMembersByTeams() {
      Map<Byte, List<RequestArenaEntrance.TeamMember>> teamMembersByTeam = new HashMap<>();
      for (byte team=0 ; team<_teamCount ; team++) {
         List<RequestArenaEntrance.TeamMember> teamMembers = new ArrayList<>();
         teamMembersByTeam.put(team, teamMembers);
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            ArenaLocation loc = _startPoints[team][cur];
            if (loc != null) {
               String name = _stockCharName[team][cur];
               boolean available = CombatServer._REMOTE_AI_NAME.equals(_stockAiName[team][cur]);
               Character character = null;
               if (loc.getCharacters().isEmpty()) {
                  teamMembers.add(new RequestArenaEntrance.TeamMember(team, name, character, cur, available));
               }
//               if (false) {
//                  List<Character> chars = loc.getCharacters();
//                  if (!chars.isEmpty() && available) {
//                     character = chars.get(0);
//                  }
//                  if (available && (character != null)) {
//                     boolean foundWaiting = false;
//                     for (Character chr : charactersWaitingToConnect) {
//                        if (chr._uniqueID == character._uniqueID) {
//                           foundWaiting = true;
//                           break;
//                        }
//                     }
//                     if (!foundWaiting) {
//                        available = false;
//                        character = null;
//                     }
//                  }
//                  teamMembers.add(new RequestArenaEntrance.TeamMember(team, name, character, cur, available));
//               }
            }
         }
      }
      return teamMembersByTeam;
   }
   public List<ArenaLocation> getAvailablePlayerLocs()
   {
      List<ArenaLocation> availLocs = new ArrayList<>();
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            ArenaLocation loc = _startPoints[team][cur];
            if ((loc != null) && (loc.getCharacters().size() == 0)) {
               availLocs.add(loc);
            }
         }
      }
      return availLocs;
   }

   public byte getCombatantsCount() {
      byte count = 0;
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            ArenaLocation loc = _startPoints[team][cur];
            if (loc != null) {
               count++;
            }
         }
      }
      return count;
   }
   public ArenaLocation[][] getStartingPoints() {
      return _startPoints;
   }
   public ArenaLocation[] getStartingPoints(byte teamID) {
      return _startPoints[teamID];
   }
   public boolean removeCharacter(Character character) {
      boolean removed = false;
      List<ArenaLocation> locs = getLocations(character);
      for (ArenaLocation loc : locs) {
         if (loc != null) {
            if (loc.remove(character)) {
               removed = true;
            }
         }
         for (IAreaSpell spell : loc.getActiveSpells()) {
            spell.affectCharacterOnExit(character);
         }
      }

      return removed;
   }
   public void addCharacter(Character character) {
      List<ArenaLocation> locs = getLocations(character);
      for (ArenaLocation loc : locs) {
         if (loc != null) {
            loc.addThing(character);
         }
         for (IAreaSpell spell : loc.getActiveSpells()) {
            spell.affectCharacterOnEntry(character);
         }
      }
   }
   public byte getTeamCount() { return _teamCount; }

   public void clearItems() {
      for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            // remove everything from the location.
            _locations[col][row].clearItems();
         }
      }
   }
   public void clearCharacterViewedHistory() {
      for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            _locations[col][row].clearCharacterViewedHistory();
         }
      }
   }
   public void setAllHexesSelectable(boolean enabled) {
      for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            _locations[col][row].setSelectable(enabled);
         }
      }
   }

   public boolean addCharacter(Character combatant, ArenaLocation startLoc, ClientProxy clientProxy) {
      removeCharacter(combatant);
      if (startLoc != null) {
         if (startLoc.getCharacters().size() == 0) {
            // If we can't get into a particular location, try changing our facing.
            // This can happen when a multi-hex creature starts too close to the edge of the map, or near a wall.
            Facing startFacing = getStartingFacing(startLoc);
            for (int offset : new int[] {0, 5, 1, 4, 2, 3}) {
               Facing facing = startFacing.turn(offset);
               if (combatant.setHeadLocation(startLoc, facing, this, _diag)) {
                  if (CombatServer._isServer) {
                     // Make the combatant a watcher of this map:
                     registerAsWatcher(combatant._mapWatcher, _diag);
                  }
                  return true;
               }
            }
         }
      }
      return false;
   }

   public void clearVisibility() {
      // Set all locations as 'not visibile'
      for (short col = 0; col < getSizeX(); col++) {
         for (short row = (short) (col % 2); row < getSizeY(); row += 2) {
            ArenaLocation viewLoc = getLocation(col, row);
            viewLoc.setVisible(false/*isVisible*/, this, null/*viewerLoc*/, -1/*viwerID*/, false/*basedOnFacing*/);
         }
      }
   }

   public void recomputeKnownLocations(Character self, boolean basedOnFacing,
                                       boolean setVisibility, Collection<ArenaCoordinates> locsToRedraw) {
      Rules.diag("recomputeKnownLocations for " + ((self == null) ? "null" : self.getName()));
      if (self == null) {
         return;
      }
      if ((!self.getCondition().isConscious()) || (!self.getCondition().isAlive())) {
         return;
      }
      ArenaLocation originalLoc = getLocation(self.getHeadCoordinates());
//      short distTopLeft     = ArenaCoordinates.getDistance(originalLoc, getLocation((short)0,                 (short)0));
//      short distTopRight    = ArenaCoordinates.getDistance(originalLoc, getLocation((short)(((_sizeX-1)/2)*2),(short)0));
//      short distBottomLeft  = ArenaCoordinates.getDistance(originalLoc, getLocation((short)0,                 (short)(((_sizeY-1)/2)*2)));
//      short distBottomRight = ArenaCoordinates.getDistance(originalLoc, getLocation((short)(((_sizeX-1)/2)*2),(short)(((_sizeY-1)/2)*2)));
      //int maxDist = Math.max(Math.max(distTopRight, distTopLeft), Math.max(distBottomRight, distBottomLeft)) + 1;
      //int maxDist = Math.max(_sizeX, _sizeY);

      setVisibilityOrKnownBy(self, setVisibility, basedOnFacing, locsToRedraw, originalLoc, originalLoc);

      AnglesList angleBlockedVisually = new AnglesList();
      // we start out at the point in the direction FACING_8_OCLOCK from the start point, so that
      // we walk in direction FACING_NOON to our second location.
      // This means that we start our coordinates with 0 in the direction of 8 O'Clock.

      int[] hexBaseDims = MapWidget2D.getHexDimensions(originalLoc._x/*column*/, originalLoc._y/*row*/, 10/*sizePerHex*/,
                                                     0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
      int baseCoordX = (hexBaseDims[MapWidget2D.X_SMALLEST] + hexBaseDims[MapWidget2D.X_LARGEST]) / 2;
      int baseCoordY = (hexBaseDims[MapWidget2D.Y_SMALLEST] + hexBaseDims[MapWidget2D.Y_LARGEST]) / 2;

      if (basedOnFacing && !self.hasPeripheralVision()) {
         //           PI/2
         //  .78       ^         2.3
         //    (-1,-1) | (+1,-1)
         //            |
         //  0 <-------+-------> PI
         //            |
         //    (-1,+1) | (+1,+1)
         //  5.5       v         3.9
         //          3*PI/2

         // The left edge of the blocked region, is actually the right-edge of the visible area
         // Start out with the 'leftEdge' being the angle off to the right of the character,
         // as if the character had a facing of 'NOON', then adjust for the actual facing of the character.
         double leftEdge = Math.PI;
         leftEdge += (self.getFacing().value * Math.PI) / 3.0; // 60 degrees per facing above 0.

         double rightEdge = leftEdge + Math.PI;
         // subtract a tiny bit off of the left edge so we see slightly more than 180 degrees.
         leftEdge -= .01;
         rightEdge += .01;

         angleBlockedVisually.add(new AnglePair(leftEdge, rightEdge));
      }

      double angleAtCorner[] = new double[12];
      boolean[] xFlags = new boolean[] {true, false};
      boolean[] yFlags = new boolean[] {true, false};
      for (boolean positiveX : xFlags) {
         for (short xAbsDelta=0 ; xAbsDelta<_sizeX ; xAbsDelta++) {
            if ((xAbsDelta == 0) && !positiveX) {
               continue;
            }
            short xDelta = (short) (positiveX ? xAbsDelta : 0-xAbsDelta);
            short x = (short) (originalLoc._x + xDelta);
            if ((x < 0) || (x>=_sizeX)) {
               break;
            }
            for (boolean positiveY : yFlags) {
               for (short yAbsDelta=(short) (xAbsDelta%2) ; yAbsDelta<_sizeY ; yAbsDelta += 2) {
                  short yDelta = (short) (positiveY ? yAbsDelta : 0-yAbsDelta);
                  short y = (short) (originalLoc._y + yDelta);
                  if ((y < 0) || (y>=_sizeY)) {
                     break;
                  }

                  // At this point, we know we are still on the map.
                  ArenaLocation loc = getLocation(x, y);

                  // compute the maximum and minimum angles to each point of the target hex.
                  // This will be used to see if this hex need to be considered, and if this hex blocks
                  // visibility, these angles will be added to the blocking angles list.
                  int[] hexDims = MapWidget2D.getHexDimensions(loc._x/*column*/, loc._y/*row*/, 10/*sizePerHex*/,
                                                               0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
                  int centerCoordX = (hexDims[MapWidget2D.X_SMALLEST] + hexDims[MapWidget2D.X_LARGEST]) / 2;
                  int centerCoordY = (hexDims[MapWidget2D.Y_SMALLEST] + hexDims[MapWidget2D.Y_LARGEST]) / 2;
                  // Math.atan2 returns a value between -PI and +PI, going counter-clockwise:
                  // with -PI/2 on the left, 0 on the bottom, PI/2 on the right, -PI and +PI at top
                  double centerAngle = ((3*Math.PI)/2) - Math.atan2(centerCoordX-baseCoordX, centerCoordY-baseCoordY);
                  if (centerAngle > (2*Math.PI)) {
                     centerAngle -= 2*Math.PI;
                  }
                  if ((loc._x == 2) && (loc._y == 6)) {
                     loc._y = (short) (loc._x + 4);
                  }
                  AnglePair thisHexsFullAngle = new AnglePair(centerAngle, centerAngle);
                  long walls = loc.getWallsAndClosedDoors();
//                  if (loc._x == 20) {
//                     if (loc._y == 56) {
//                        walls = walls;
//                     }
//                  }
                  //           PI/2
                  //  .78       ^         2.3
                  //    (-1,-1) | (+1,-1)
                  //            |
                  //  0 <-------+-------> PI
                  //            |
                  //    (-1,+1) | (+1,+1)
                  //  5.5       v         3.9
                  //          3*PI/2
                  for (int i=0 ; i<12 ; i+=2) {
                     int x1 = hexDims[i];
                     int y1 = hexDims[i+1];
                     double angle = ((3*Math.PI)/2) - Math.atan2(x1-baseCoordX, y1-baseCoordY);
                     angleAtCorner[i] = AnglePair.normalizeAngleRadians(angle);
                     if ((xDelta != 0) || (yDelta != 0)) {
                        thisHexsFullAngle.adjustToIncludeAngle(angleAtCorner[i]);
                     }
                     if (walls != 0) {
                        int x2 = hexDims[(i+2)%12];
                        int y2 = hexDims[(i+3)%12];
                        angleAtCorner[i+1] = ((3*Math.PI)/2) - Math.atan2(((x1+x2)/2.0)-baseCoordX, ((y1+y2)/2.0)-baseCoordY);
                        angleAtCorner[i+1] = AnglePair.normalizeAngleRadians(angleAtCorner[i+1]);
                     }
                  }
                  AnglesList blockingAngles = new AnglesList();
                  if (walls != 0) {
                     /*
                      *   10 9 8      y = lowest
                      *  11     7     y = low
                      *  0       6    y = middle
                      *   1     5     y = high
                      *    2 3 4      y = highest
                      */
                     // check for each wall type, one at a time
                     for (TerrainWall terrainWall : TerrainWall.values()) {
                        // Check if this hex has this orientation of a wall
                        if (terrainWall.contains(walls)) {
                           // yes, it has this type of wall, find the start & end points for this wall
                           // then lookup the angle to those endpoints
                           double angle1 = angleAtCorner[terrainWall.startPoint];
                           double angle2 = angleAtCorner[terrainWall.endPoint];
                           double minAngle = Math.min(angle1, angle2);
                           double maxAngle = Math.max(angle1, angle2);
                           // now figure out the angle it blocks
                           AnglePair blockingWallAngle = new AnglePair(minAngle, maxAngle);
                           if (blockingWallAngle.width() > Math.PI) {
                              blockingWallAngle = new AnglePair(maxAngle, minAngle);
                           }
                           if (!thisHexsFullAngle.containsCompletely(blockingWallAngle)) {
                              if ((xDelta != 0) || (yDelta != 0)) {
                                 DebugBreak.debugBreak("corner of hex doesnt exist in hex.");
                              }
                           }
                           // add this angle to the list of angles this hex blocks
                           blockingAngles.add(blockingWallAngle);
                        }
                     }
                  }

                  if (loc.getTerrain() == TerrainType.SOLID_ROCK) {
                     blockingAngles.add(thisHexsFullAngle);
                  }

                  if (loc.getTerrain() == TerrainType.TREE_TRUNK) {
                     AnglePair treeTrunkAngle = thisHexsFullAngle.adjustWidth(.85);
                     blockingAngles.add(treeTrunkAngle);
                  }
                  if (loc.getTerrain() == TerrainType.DENSE_BUSH) {
                     AnglePair angle1 = new AnglePair(thisHexsFullAngle._startAngle, centerAngle);
                     AnglePair angle2 = new AnglePair(centerAngle, thisHexsFullAngle._stopAngle);
                     angle1 = angle1.adjustWidth(.40);
                     angle2 = angle2.adjustWidth(.40);
                     blockingAngles.add(angle1);
                     blockingAngles.add(angle2);
                  }
                  // when we check if this hex is visible, we only care about the center section
                  // Furthermore, if this hex contains a tree trunk, the angle it will block
                  // is only the core half of this hex.
                  thisHexsFullAngle = thisHexsFullAngle.adjustWidth(.50);
                  if (!angleBlockedVisually.containsCompletely(thisHexsFullAngle)) {
                     // This hex is not completely blocked, so it's visible
                     // figure out the hex that is one hex closer to us, and use that as our fromLoc.
                     // This is computed based on the centerAngle from us to the location.
                     ArenaLocation fromLoc = null;
                     if ((xAbsDelta == 0) && (yAbsDelta == 0)) {
                        fromLoc = loc;
                     }
                     else {
                        Facing fromDir = Facing.getByValue((byte)(((centerAngle / (Math.PI / 3)) + 2) % 6));
                        short fromLocX = (short)(x + fromDir.moveX);
                        short fromLocY = (short)(y + fromDir.moveY);
                        fromLoc = getLocation(fromLocX, fromLocY);
                     }
                     setVisibilityOrKnownBy(self, setVisibility, basedOnFacing, locsToRedraw, loc, fromLoc);
                  }
                  else {
                     loc.setVisible(false, this, originalLoc, self._uniqueID, false/*basedOnFacing*/);
                  }
                  // add the angles that this hex blocks to the list of all angles blocked
                  angleBlockedVisually.add(blockingAngles);
               }
            }
         }
      }
   }

   public boolean addCharacter(Character combatant, byte team, byte combatantIndexOnTeam, ClientProxy clientProxy) {
      ArenaLocation startLoc = getStartingLocation(team, combatantIndexOnTeam);
      return addCharacter(combatant, startLoc, clientProxy);
   }
   public boolean setStockCharacter(String stockCombatantName, String ai, byte team, byte cur) {
      if ((team < _teamCount) && (cur < _maxCombatantsPerTeam)) {
         _stockCharName[team][cur] = stockCombatantName;
         if (ai.startsWith("AI - ")) {
            ai = ai.replace("AI - ", "");
         }
         _stockAiName[team][cur] = ai;
         return true;
      }
      return false;
   }
   public String[] getStockCharacters(byte team) {
       return _stockCharName[team];
    }
   public String[] getStockAIName(byte team) {
       return _stockAiName[team];
    }
   public void setAllCombatantsAsAI() {
      for (int team = 0 ; team<_teamCount ; team++) {
         for (int combatant = 0 ; combatant<_maxCombatantsPerTeam ; combatant++) {
            if (_stockAiName[team][combatant] != null) {
               if ((!_stockAiName[team][combatant].equals(AI_Type.NORM.name)) &&
                   (!_stockAiName[team][combatant].equals(AI_Type.GOD.name))) {
                  _stockAiName[team][combatant] = AI_Type.NORM.name;
               }
            }
         }
      }
   }

   public void addThing(short x, short y, Object thing) {
      _locations[x][y].addThing(thing);
   }
   public String getName() {
      return _name;
   }
   public void setName(String newName) {
      _name = newName;
   }

   public ArenaLocation getLocation(short x, short y) {
      if ((x >= 0) && (y >= 0) && (x < getSizeX()) && (y < getSizeY())) {
         return _locations[x][y];
      }
      return null;
   }
   public ArenaLocation getLocationQuick(short x, short y) {
      return _locations[x][y];
   }
   public ArenaLocation getLocation(ArenaCoordinates coordinate) {
      if (coordinate == null) {
         return null;
      }
      if (coordinate instanceof ArenaLocation) {
         return (ArenaLocation) coordinate;
      }
      return getLocation(coordinate._x, coordinate._y);
   }
   public List<ArenaLocation> getLocations(List<ArenaCoordinates> coordinates) {
      List<ArenaLocation> locations = new ArrayList<>();
      for (ArenaCoordinates coordinate : coordinates) {
         locations.add(getLocation(coordinate));
      }
      return locations;
   }

   public ArenaLocation getHeadLocation(Character character) {
      ArenaLocation loc = character.getLimbLocation(LimbType.HEAD, this);
      return getLocation(loc._x, loc._y);
   }
   public List<ArenaLocation> getLocations(Character character) {
      List<ArenaLocation> locations = new ArrayList<>();
      for (ArenaCoordinates coord : character.getCoordinates()) {
         locations.add(getLocation(coord._x, coord._y));
      }
      return locations;
   }
   public boolean addLocation(ArenaLocation arenaLocation) {
      if (_locations[arenaLocation._x][arenaLocation._y] != null) {
         return false;
      }
      _locations[arenaLocation._x][arenaLocation._y] = arenaLocation;
      return true;
   }
   public boolean removeLocation(ArenaLocation arenaLocation) {
      if (_locations[arenaLocation._x][arenaLocation._y] == null) {
         return false;
      }
      _locations[arenaLocation._x][arenaLocation._y] = null;
      return true;
   }

   public boolean dropThing(Object thing, short xLoc, short yLoc) {
      ArenaLocation location = _locations[xLoc][yLoc];
      if (location != null) {
         location.addThing(thing);
         return true;
      }
      return false;
   }

   public boolean dropSomething(Character actor, Object thing) {
      ArenaCoordinates coord = actor.getHeadCoordinates();
      ArenaLocation location = _locations[coord._x][coord._y];
      if (location != null) {
          location.addThing(thing);
          return true;
      }
      return false;
   }
   public ArrayList<Character> getCombatants() {
      ArrayList<Character> list = new ArrayList<>();
      for (short col = 0 ; col<_sizeX ; col++) {
          for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
              List<Character> characters = _locations[col][row].getCharacters();
              if (characters.size() > 0) {
                  list.addAll(characters);
              }
          }
       }
       return list;
    }
   public void removeAllCombatants() {
      for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            List<Character> characters = _locations[col][row].getCharacters();
            while (characters.size() > 0) {
               _locations[col][row].remove(characters.remove(0));
            }
         }
      }
   }
   public ArrayList<ArenaLocation> getLocationsWithObjects() {
     ArrayList<ArenaLocation> list = new ArrayList<>();
     for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            ArenaLocation location = _locations[col][row];
            synchronized (location) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(location._lock_this)) {
                  if ((location.getThings() != null) &&
                      (location.getThings().size() > 0)) {
                     list.add(location);
                  }
                  else if ((location.getDoors() != null) &&
                           (location.getDoors().size() > 0)) {
                     list.add(location);
                  }
               }
            }
         }
      }
      return list;
   }

   public static final String SEPARATOR_MAIN = ":";
   public boolean serializeFromString(String source) {
      StringTokenizer st = new StringTokenizer(source, SEPARATOR_MAIN);
      if (!st.hasMoreElements()) {
         return false;
      }
      _name = st.nextToken();
      if (!st.hasMoreElements()) {
         return false;
      }
      _level = Short.parseShort(st.nextToken());
      if (!st.hasMoreElements()) {
         return false;
      }
      _sizeX = Short.parseShort(st.nextToken());
      if (!st.hasMoreElements()) {
         return false;
      }
      _sizeY = Short.parseShort(st.nextToken());
      if (!st.hasMoreElements()) {
         return false;
      }

      _hideViewFromLocalPlayers = Boolean.parseBoolean(st.nextToken());

      _locations = new ArenaLocation[_sizeX][_sizeY];
      for (short col=0 ; col<_sizeX ; col++) {
         for (short row=(short) (col%2) ; row<_sizeY ; row += 2) {
            _locations[col][row] = new ArenaLocation(col, row);
            _locations[col][row].registerAsWatcher(this, _diag);
            if (!st.hasMoreElements()) {
               return false;
            }
            _locations[col][row].setData(Integer.parseInt(st.nextToken()));
         }
      }
      _teamCount = CombatServer.MAX_TEAMS;
      _maxCombatantsPerTeam = CombatServer.MAX_COMBATANTS_PER_TEAM;
      if (!st.hasMoreElements()) {
         _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
         _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
         _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
         return true;
      }
      _teamCount = Byte.parseByte(st.nextToken());
      if (!st.hasMoreElements()) {
          _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
          _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
          _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
          return true;
       }
      _maxCombatantsPerTeam = Byte.parseByte(st.nextToken());
      _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
      _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
      _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
      if (!st.hasMoreElements()) {
          return true;
       }
      for (byte team=0 ; team<_teamCount ; team++) {
         if (!st.hasMoreElements()) {
            return false;
         }
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur ++) {
            if (!st.hasMoreElements()) {
               return false;
            }
            short startX = Short.parseShort(st.nextToken());
            if (!st.hasMoreElements()) {
               return false;
            }
            short startY = Short.parseShort(st.nextToken());
            if ((startX != -1) && (startY != -1)) {
               _startPoints[team][cur] = _locations[startX][startY];
               _startPoints[team][cur].setLabel(getLabel(team, cur));
            }
            if (!st.hasMoreElements()) {
               return false;
            }
            String ai = st.nextToken();
            if (!st.hasMoreElements()) {
               return false;
            }
            String charName = st.nextToken();
            setStockCharacter(charName, ai, team, cur);
         }
      }
      return true;
   }
   public String serializeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append(_name).append(SEPARATOR_MAIN);
      sb.append(_level).append(SEPARATOR_MAIN);
      sb.append(_sizeX).append(SEPARATOR_MAIN);
      sb.append(_sizeY).append(SEPARATOR_MAIN);
      sb.append(_hideViewFromLocalPlayers).append(SEPARATOR_MAIN);
      for (short col = 0 ; col<_sizeX ; col++) {
          for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
            sb.append(_locations[col][row].getData()).append(SEPARATOR_MAIN);
         }
      }

      sb.append(_teamCount).append(SEPARATOR_MAIN);
      sb.append(_maxCombatantsPerTeam).append(SEPARATOR_MAIN);
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur ++) {
            ArenaLocation startLoc = _startPoints[team][cur];
            if (startLoc != null ) {
                sb.append(startLoc._x).append(SEPARATOR_MAIN);
                sb.append(startLoc._y).append(SEPARATOR_MAIN);
            }
            else {
                sb.append(-1).append(SEPARATOR_MAIN);
                sb.append(-1).append(SEPARATOR_MAIN);
            }
            if (_stockAiName[team][cur] == null) {
               sb.append("Off").append(SEPARATOR_MAIN);
            }
            else {
               sb.append(_stockAiName[team][cur]).append(SEPARATOR_MAIN);
            }
            if (_stockCharName[team][cur] == null) {
               sb.append(" ").append(SEPARATOR_MAIN);
            }
            else {
               sb.append(_stockCharName[team][cur]).append(SEPARATOR_MAIN);
            }
         }
      }
      return sb.toString();
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_level, out);
         writeToStream(getSizeX(), out);
         writeToStream(getSizeY(), out);
         writeToStream(_hideViewFromLocalPlayers, out);
         for (short col=0 ; col<getSizeX() ; col++) {
            for (short row=(short)(col%2) ; row<getSizeY(); row+=2) {
               getLocation(col,row).serializeContentsToStream(out);
            }
         }
         writeToStream(_teamCount, out);
         writeToStream(_maxCombatantsPerTeam, out);
         for (int team = 0 ; team<_teamCount ; team++) {
            for (int combatant = 0 ; combatant<_maxCombatantsPerTeam ; combatant++) {
               if (_startPoints[team][combatant] != null) {
                  writeToStream(_startPoints[team][combatant]._x, out);
                  writeToStream(_startPoints[team][combatant]._y, out);
               }
               else {
                  writeToStream((short)(-1), out);
                  writeToStream((short)(-1), out);
               }
               writeToStream(_stockAiName[team][combatant],   out);
               writeToStream(_stockCharName[team][combatant], out);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _level = in.readShort();
         short sizeX = in.readShort();
         short sizeY = in.readShort();
         _hideViewFromLocalPlayers = in.readBoolean();
         setSize(sizeX, sizeY);
         for (short col=0 ; col<_sizeX ; col++) {
            for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
               getLocation(col,row).serializeContentsFromStream(in);
            }
         }
         _teamCount = in.readByte();
         _maxCombatantsPerTeam = in.readByte();
         _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
         _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
         _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
         for (byte team = 0 ; team<_teamCount ; team++) {
            for (byte combatant = 0 ; combatant<_maxCombatantsPerTeam ; combatant++) {
               short x = in.readShort();
               short y = in.readShort();
               if ((x != -1) && (y != -1)) {
                  _startPoints[team][combatant]   = getLocation(x, y);
               }
               String ai       = readString(in);
               String charName = readString(in);
               setStockCharacter(charName, ai, team, combatant);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      return "ComatMap: " + _sizeX +"x" + _sizeY;
   }
   @Override
   public CombatMap clone() {
       CombatMap newObj = new CombatMap(_sizeX, _sizeY, null);
       newObj._level = _level;
       newObj._hideViewFromLocalPlayers = _hideViewFromLocalPlayers;
       newObj._name = _name;
       for (short col = 0 ; col<_sizeX ; col++) {
           for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
             newObj._locations[col][row].copyData(_locations[col][row]);
          }
       }
       newObj._teamCount = _teamCount;

       newObj._maxCombatantsPerTeam = _maxCombatantsPerTeam;
       newObj._startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
       newObj._stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
       newObj._stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
       for (byte team=0 ; team<_teamCount ; team++) {
          for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
             if (_startPoints != null) {
                ArenaLocation startPoint = _startPoints[team][cur];
                if (startPoint != null) {
                   newObj._startPoints[team][cur] = newObj._locations[startPoint._x][startPoint._y];
                }
             }
             if (_stockAiName != null) {
                newObj._stockAiName[team][cur] = _stockAiName[team][cur];
             }
             if (_stockCharName != null) {
                newObj._stockCharName[team][cur] = _stockCharName[team][cur];
             }
          }
       }
       newObj._triggers.clear();
       for (ArenaTrigger trigger : _triggers) {
          newObj._triggers.add(trigger.clone());
       }
       return newObj;
   }

   public boolean canSee(Character fromChar, Character toChar, boolean considerFacing, boolean blockedByAnyStandingCharacter) {
      if (considerFacing && fromChar.hasPeripheralVision()) {
         considerFacing = false;
      }
      return canSee(fromChar.getOrientation(), toChar, considerFacing, blockedByAnyStandingCharacter, fromChar._uniqueID/*markAsKnownByCharacterUniqueID*/);
   }
   /**
    * This method is used to figue out if a possible future orientation would be able to see the toChar, from a given fromOrientation
    * It does not mark the destination location as being known (if they were not already known)
    * @param fromOrientation
    * @param toChar
    * @param considerFacing
    * @param blockedByAnyStandingCharacter
    * @return
    */
   public boolean couldSee(Orientation fromOrientation, Character toChar, boolean considerFacing, boolean blockedByAnyStandingCharacter) {
      return canSee(fromOrientation, toChar, considerFacing, blockedByAnyStandingCharacter, -1 /*markAsKnownByCharacterUniqueID*/);
   }
   public boolean couldSee(Orientation fromOrientation, Orientation toOrientation, boolean considerFacing, boolean blockedByAnyStandingCharacter) {
      return canSee(fromOrientation, toOrientation, considerFacing, blockedByAnyStandingCharacter, -1 /*markAsKnownByCharacterUniqueID*/);
   }

   public boolean canSee(Orientation fromOrientation, Character toChar, boolean considerFacing,
                         boolean blockedByAnyStandingCharacter, int markAsKnownByCharacterUniqueID) {
      return canSee(fromOrientation, toChar.getOrientation(), considerFacing, blockedByAnyStandingCharacter, markAsKnownByCharacterUniqueID);
   }
   public boolean canSee(Orientation fromOrientation, Orientation toOrientation, boolean considerFacing,
                         boolean blockedByAnyStandingCharacter, int markAsKnownByCharacterUniqueID) {
      ArenaLocation headLoc = getLocation(fromOrientation.getHeadCoordinates());
      for (ArenaLocation toLoc : getLocations(toOrientation.getCoordinates())) {
         toLoc = _locations[toLoc._x][toLoc._y];
         if (canSeeLocation(fromOrientation, headLoc, toLoc, considerFacing, blockedByAnyStandingCharacter, markAsKnownByCharacterUniqueID)) {
            return true;
         }
      }
      return false;
   }
   public boolean canSeeLocation(Orientation fromOrientation, ArenaLocation headLoc, ArenaLocation toLoc,
                                  boolean considerFacing, boolean blockedByAnyStandingCharacter,
                                  int markAsKnownByCharacterUniqueID) {
      if (!considerFacing || isFacing(fromOrientation, toLoc)) {
         if (hasLineOfSight(headLoc, toLoc, blockedByAnyStandingCharacter)) {
            if (markAsKnownByCharacterUniqueID != -1) {
               if (!toLoc.isKnownBy(markAsKnownByCharacterUniqueID)) {
                  toLoc.setVisible(true, this, headLoc, markAsKnownByCharacterUniqueID, true/*basedOnFacing*/);
               }
            }
            return true;
         }
      }
      return false;
   }
   public boolean hasLineOfSight(ArenaLocation fromLoc, ArenaCoordinates toLoc,
                                 boolean blockedByAnyStandingCharacter) {
      int characterCount = countCharactersBetween(fromLoc, toLoc, true/*onlyCountStandingCharacters*/);
      if (characterCount == -1) {
         // There is a wall between the fromLoc and toLoc
         return false;
      }
      if (blockedByAnyStandingCharacter && (characterCount>0)) {
         return false;
      }
      return true;
   }
   /**
    * This method returns a count of the number of character between the fromLoc and the toLoc, non-inclusive.
    * If a wall is present between the from and to locations, a -1 is returned. If the toChar occupies multiple
    * locations, then it returns the least-obstructed count to any of the toChar's hexes.
    * @param fromChar
    * @param toChar
    * @param onlyCountStandingCharacters
    * @return
    */
   public int countCharactersBetween(Character fromChar, Character toChar, boolean onlyCountStandingCharacters) {
      if (fromChar._uniqueID == toChar._uniqueID) {
         return 0;
      }
      ArenaLocation fromLoc = getLocation(fromChar.getHeadCoordinates());
      int minCharCount = -1;
      for (ArenaLocation toLoc : getLocations(toChar)) {
         int count = countCharactersBetween(fromLoc, toLoc, true/*onlyCountStandingCharacters*/);
         if (minCharCount == -1) {
            minCharCount = count;
         }
         else if ((count != -1) && (minCharCount > count)) {
            minCharCount = count;
         }
      }
      return minCharCount;
   }
   /**
    * This method returns a count of the number of character between the fromLoc and the toLoc, non-inclusive.
    * If a wall is present between the from and to locations, a -1 is returned.
    * @param fromLoc
    * @param toCoord
    * @param onlyCountStandingCharacters
    * @return
    */
   private int countCharactersBetween(ArenaLocation fromLoc, ArenaCoordinates toCoord, boolean onlyCountStandingCharacters) {
      Character computeVisibilityFromCharacter = null;
      boolean setVisibility = false;
      boolean basedOnFacing = false;
      Collection<ArenaCoordinates> locsToRedraw = null;

      int charCount = 0;
      if (fromLoc.sameCoordinates(toCoord)) {
         setVisibilityOrKnownBy(computeVisibilityFromCharacter, setVisibility, basedOnFacing, locsToRedraw, fromLoc, fromLoc);
         return charCount;
      }
      ArrayList<ArenaLocation> path = getLOSPath(fromLoc, toCoord, false/*trimPath*/);
      // getLOSPath doesn't add the start or end points to the path.
      // we start our search from the fromLoc, so that's accounted for, but need
      // to add the toCoord so we check that last hex transition.
      path.add(getLocation(toCoord));
      // There may be cases where a point 2 hexes away has 3 entries in the List
      // This means that either [0] or [1] could be taken to get to [2].
      // So if we can't get enter location [0], we set the skippedHex flag
      // and check location [1]. If that isn't adjacent, then we won't be
      // able to enter that one either. If skippedHex is already true, then
      // we return with a failure indication (-1).
      boolean skippedHex = false;
      ArenaLocation curLoc = getLocation(fromLoc);
      ArenaLocation prevLoc = null;
      for (ArenaLocation testLoc : path) {
         if (curLoc.sameCoordinates(testLoc)) {
            setVisibilityOrKnownBy(computeVisibilityFromCharacter, setVisibility, basedOnFacing, locsToRedraw, curLoc, fromLoc);
            continue;
         }
         // canExit will return false if the 2 hexes aren't adjacent, or a wall in curLoc blocks blocks the way
         if (!curLoc.canExit(testLoc) &&
             ((prevLoc == null) || !(prevLoc.canExit(testLoc)))) {
            if (skippedHex) {
               return -1;
            }
            skippedHex = true;
            continue;
         }
         setVisibilityOrKnownBy(computeVisibilityFromCharacter, setVisibility, basedOnFacing, locsToRedraw, testLoc, fromLoc);
         // canEnter will return false if the 2 hexes aren't adjacent, or a wall in testLoc blocks blocks the way
         if (!testLoc.canEnter(curLoc, false/*blockByCharacters*/) &&
             ((prevLoc == null) || (curLoc == prevLoc) || !testLoc.canEnter(prevLoc, false/*blockByCharacters*/))) {
            if (testLoc.sameCoordinates(toCoord)) {
//               int terrain = toLoc.getTerrain();
//               if ((terrain == TERRAIN_SOLID_ROCK) || (terrain == TERRAIN_TREE_TRUNK))
               // 11/12/2014 - comment out this return because it allows someone to attack another person,
               // if the target is hiding behind a single wall in their own hex.
               //return charCount;
            }
            if (skippedHex) {
               return -1;
            }
            skippedHex = true;
            continue;
         }
         if (testLoc.sameCoordinates(toCoord)) {
            // destination reached.
            return charCount;
         }
         if (!testLoc.sameCoordinates(fromLoc)) {
            charCount += testLoc.getCharacterCount(onlyCountStandingCharacters, computeVisibilityFromCharacter);
         }
         prevLoc = curLoc;
         curLoc = testLoc;
         skippedHex = false;
      }
      // If skippedHex is still true, then we skipped the last hex, which is the toLoc,
      // and we are not allowed to skip that hex!
      if (skippedHex) {
         return -1;
      }
      return charCount;
   }
   private void setVisibilityOrKnownBy(Character computeVisibilityFromCharacter, boolean setVisibility, boolean basedOnFacing,
                                       Collection<ArenaCoordinates> locsToRedraw, ArenaLocation loc, ArenaLocation fromLoc) {
      if (computeVisibilityFromCharacter != null) {
         boolean redrawLoc = false;
         if (setVisibility) {
            redrawLoc = loc.setVisible(true/*isVisible*/, this, fromLoc, computeVisibilityFromCharacter._uniqueID, basedOnFacing);
         }
         else {
            redrawLoc = loc.setKnownBy(computeVisibilityFromCharacter._uniqueID, true/*isKnown*/);
         }
         if (redrawLoc &&  (locsToRedraw!= null)) {
            locsToRedraw.add(loc);
         }
      }
   }

   public void setAllLocationsAsKnownBy(Character godCharacter) {
      for (short col=0 ; col<_sizeX ; col++) {
         for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
            _locations[col][row].setKnownBy(godCharacter._uniqueID, true/*isKnown*/);
         }
      }
   }

   /**
    * This method returns a straight line of hexes from fromLoc to toLoc, regardless of terrain or obstacles.
    * @param fromCoord
    * @param toCoord
    * @param trimPath
    * @return
    */
   public ArrayList<ArenaLocation> getLOSPath(ArenaCoordinates fromCoord, ArenaCoordinates toCoord, boolean trimPath) {
      // This method does NOT put the fromCoord or the toCoord in the path!
      ArrayList<ArenaLocation> path = new ArrayList<>();
      int deltaX = toCoord._x - fromCoord._x;
      int deltaY = toCoord._y - fromCoord._y;
      int absX = Math.abs(deltaX);
      int absY = Math.abs(deltaY);
      if (deltaX == 0) {
         if (deltaY != 0) {
            if (deltaY > 0) {
               deltaY = 2;
            }
            else {
               deltaY = -2;
            }
            for (short y=(short)(fromCoord._y+deltaY) ; y!=toCoord._y ; y+=deltaY) {
               ArenaLocation loc = getLocation(fromCoord._x, y);
               if (loc == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  path.add(loc);
               }
            }
         }
         for (ArenaLocation loc : path) {
            if (loc.sameCoordinates(fromCoord) || loc.sameCoordinates(toCoord)) {
               DebugBreak.debugBreak("added from or to location to path");
            }
         }
         return path;
      }
      if (deltaX == deltaY) {
         if (deltaX > 0) {
            for (short i=1 ; i<deltaX ; i++) {
               ArenaLocation loc = getLocation((short) (fromCoord._x + i), (short) (fromCoord._y + i));
               if (loc == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  path.add(loc);
               }
            }
         }
         else {
            for (short i=-1 ; i>deltaX ; i--) {
               ArenaLocation loc = getLocation((short) (fromCoord._x + i), (short) (fromCoord._y + i));
               if (loc == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  path.add(loc);
               }
            }
         }
         for (ArenaLocation loc : path) {
            if (loc.sameCoordinates(toCoord) || loc.sameCoordinates(fromCoord)) {
               DebugBreak.debugBreak("added from or to location to path");
               break;
            }
         }
         return path;
      }
      if (deltaX == -deltaY) {
         if (deltaX > 0) {
            for (short i=1 ; i<deltaX ; i++) {
               ArenaLocation loc = getLocation((short) (fromCoord._x + i), (short) (fromCoord._y - i));
               if (loc == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  path.add(loc);
               }
            }
         }
         else {
            for (short i=-1 ; i>deltaX ; i--) {
               ArenaLocation loc = getLocation((short) (fromCoord._x + i), (short) (fromCoord._y - i));
               if (loc == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  path.add(loc);
               }
            }
         }
         for (ArenaLocation loc : path) {
            if (loc.sameCoordinates(fromCoord) || loc.sameCoordinates(toCoord)) {
               DebugBreak.debugBreak("added from or to location to path");
            }
         }
         return path;
      }
      int[] hexDims = MapWidget2D.getHexDimensions(fromCoord._x/*column*/, fromCoord._y/*row*/, 10/*sizePerHex*/, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
      int centerCoordFromX = (hexDims[MapWidget2D.X_SMALLEST] + hexDims[MapWidget2D.X_LARGEST]) / 2;
      int centerCoordFromY = (hexDims[MapWidget2D.Y_SMALLEST] + hexDims[MapWidget2D.Y_LARGEST]) / 2;

            hexDims = MapWidget2D.getHexDimensions(toCoord._x/*column*/, toCoord._y/*row*/, 10/*sizePerHex*/, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
      int centerCoordToX = (hexDims[MapWidget2D.X_SMALLEST] + hexDims[MapWidget2D.X_LARGEST]) / 2;
      int centerCoordToY = (hexDims[MapWidget2D.Y_SMALLEST] + hexDims[MapWidget2D.Y_LARGEST]) / 2;

      // There can only be two possible edges that we could pass through, based on the angle:
      byte pointIndex = -1;

      if (deltaX < 0) {
         if (absX > absY) {
            pointIndex = 0;
         }
         else if (deltaY < 0) {
            pointIndex = 5;
         }
         else {
            pointIndex = 1;
         }
      }
      else {
         if (absX > absY) {
            pointIndex = 3;
         }
         else if (deltaY < 0) {
            pointIndex = 4;
         }
         else {
            pointIndex = 2;
         }
      }

      // when pointIndex is 0, facing1 is 8 O'Clock. when pointIndex is 1, facing1 is 6 O'Clock
      Facing facing1 = Facing._8_OCLOCK.turn(0 - pointIndex);
      Facing facing2 = facing1.turnRight();

      byte moveX1 = facing1.moveX;
      byte moveY1 = facing1.moveY;
      byte moveX2 = facing2.moveX;
      byte moveY2 = facing2.moveY;

      // Math.atan2 returns a value between -PI and +PI, going counter-clockwise:
      // with -PI/2 on the left, 0 on the bottom, PI/2 on the right, -PI and +PI at top
      double centerAngle = Math.atan2(centerCoordToX-centerCoordFromX, centerCoordToY-centerCoordFromY);
      if (centerAngle < 0) {
         centerAngle += 2*Math.PI;
      }
      ArenaLocation curLocation = getLocation(fromCoord);
      while (!curLocation.sameCoordinates(toCoord)) {
         hexDims = MapWidget2D.getHexDimensions(curLocation._x/*column*/, curLocation._y/*row*/, 10/*sizePerHex*/, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
         int descisionPointX = hexDims[pointIndex*2];
         int descisionPointY = hexDims[(pointIndex*2)+1];
         double descisionPointAngle = Math.atan2(descisionPointX-centerCoordFromX, descisionPointY-centerCoordFromY);
         if (descisionPointAngle < 0) {
            descisionPointAngle += 2*Math.PI;
         }
         int moveX = moveX1;
         int moveY = moveY1;
         if (centerAngle < descisionPointAngle) {
            moveX = moveX2;
            moveY = moveY2;
         }
         ArenaLocation nextLocation = getLocation((short)(curLocation._x + moveX), (short)(curLocation._y + moveY));
         if (nextLocation == null) {
            // Make sure that when we are at the top or bottom edge, that we don't leave the map
            // when the other equally valid choice would keep us on the map
            if (centerAngle < descisionPointAngle) {
               moveX = moveX1;
               moveY = moveY1;
            }
            else {
               moveX = moveX2;
               moveY = moveY2;
            }

            nextLocation = getLocation((short)(curLocation._x + moveX), (short)(curLocation._y + moveY));

            if (nextLocation == null) {
               DebugBreak.debugBreak();
               break;
            }
         }

         if (nextLocation.sameCoordinates(toCoord)) {
            break;
         }
         path.add(nextLocation);
         curLocation = nextLocation;
      }

      for (ArenaLocation loc : path) {
         if (loc.sameCoordinates(fromCoord) || loc.sameCoordinates(toCoord)) {
            DebugBreak.debugBreak("added from or to location to path");
         }
      }

      return path;
   }
   /**
    * This method returns a straight line of hexes from fromLoc to toLoc, regardless of terrain or obstacles.
    * @param fromCoord
    * @param toCoord
    * @param trimPath
    * @return
    */
   public ArrayList<ArenaLocation> getLOSPath_OLD(ArenaCoordinates fromCoord, ArenaCoordinates toCoord, boolean trimPath) {
       ArrayList<ArenaLocation> path = new ArrayList<>();
       int xDist = toCoord._x - fromCoord._x;
       int yDist = toCoord._y - fromCoord._y;
       int pointsToCheck = (Math.abs(xDist) + Math.abs(yDist)) * 2;
       double xMove = ((double)xDist) / pointsToCheck;
       double yMove = ((double)yDist) / pointsToCheck;
       short lastX = -100;
       short lastY = -100;
       for (int point=0 ; point<=pointsToCheck ; point++) {
           double xLoc = fromCoord._x + (point*xMove);
           double yLoc = fromCoord._y + (point*yMove);
           short newX = (short) Math.round(xLoc);
           short newY = getYLocation(yLoc, newX);
           if ((newX == lastX) && (newY == lastY)) {
              continue;
           }
           lastX = newX;
           lastY = newY;

           ArenaLocation testLoc = getLocation(newX, newY);
           if (testLoc == null) {
              DebugBreak.debugBreak();
           }
           else {
              // test the last element in the current path to see if it's the same
              if ((path.size() == 0) || (!path.get(path.size()-1).sameCoordinates(testLoc))) {
                 if (trimPath) {
                    if (path.size() > 1) {
                       ArenaLocation twoStepsAgo = path.get(path.size() - 2);
                       // Is the hex from two steps back and this one adjacent?
                       if (ArenaCoordinates.getFacingToLocation(twoStepsAgo, testLoc) != null) {
                          // If so, remove the intermediate step than is not needed, since
                          // we can go from the hex two steps back directly to this one.
                          path.remove(path.size()-1);
                       }
                    }
                 }
                 path.add(testLoc);
              }
           }
       }
       return path;
   }
   /**
    * This method uses the same logic as the getPath, in that it starts with a straight line from
    * the head of the fromChar to the head of the toChar, regardless of obstacles and terrain,
    * but instead of computing the entire path, it returns just the location in the path that is
    * not the fromLoc (unless the fromLoc is equal to the toLoc)
    * @param fromChar
    * @param toChar
    * @return
    */
   public ArenaLocation getFirstLocationInPath(Character fromChar, Character toChar) {
      ArenaLocation fromLoc = getLocation(fromChar.getHeadCoordinates());
      ArenaLocation toLoc = getLocation(toChar.getHeadCoordinates());
      return getFirstLocationInPath(fromLoc, toLoc);
   }
   /**
    * This method uses the same logic as the getPath, in that it starts with a straight line from
    * the fromLoc to the toLoc, regardless of obstacles and terrain, but instead of computing the
    * entire path, it returns just the location in the path that is not the fromLoc (unless the
    * fromLoc is equal to the toLoc)
    * @param fromLoc
    * @param toLoc
    * @return
    */
   public ArenaLocation getFirstLocationInPath(ArenaLocation fromLoc, ArenaLocation toLoc) {
      if (fromLoc.sameCoordinates(toLoc)) {
         return fromLoc;
      }
      int xDist = toLoc._x - fromLoc._x;
      int yDist = toLoc._y - fromLoc._y;
      if ((xDist == 0) && (yDist == 0)) {
         return fromLoc;
      }
      int pointsToCheck = (Math.abs(xDist) + Math.abs(yDist)) * 2;
      double xMove = ((double)xDist) / pointsToCheck;
      double yMove = ((double)yDist) / pointsToCheck;
      short lastX = fromLoc._x;
      short lastY = fromLoc._y;
      for (int point = 0 ; point<=pointsToCheck ; point++) {
         double xLoc = fromLoc._x + (point*xMove);
         double yLoc = fromLoc._y + (point*yMove);
         short newX = (short) Math.round(xLoc);
         short newY = getYLocation(yLoc, newX);
         if ((newX == lastX) && (newY == lastY)) {
            continue;
         }
         lastX = newX;
         lastY = newY;

         ArenaLocation testLoc = getLocation(newX, newY);
         if (testLoc == null) {
            DebugBreak.debugBreak();
         }
         else {
            if (testLoc._x != newX) {
               DebugBreak.debugBreak();
               testLoc = getLocation(newX, newY);
            }
            if (testLoc._y != newY) {
               DebugBreak.debugBreak();
               testLoc = getLocation(newX, newY);
            }
            return testLoc;
         }
      }
      return null;
   }
   private short getYLocation(double yLoc, short newX)
   {
      short newY;
      if ((newX%2) == 0) {
         newY = (short) (Math.round(yLoc/2) * 2);
      }
      else {
         newY = (short) ((Math.round((yLoc+1)/2) * 2)-1);
      }
      // It could be this location is off the map
      if (newY >= getSizeY()) {
         newY -= 2;
      }
      return newY;
   }
   public boolean setStartingLocation(byte team, byte curCombatantIndex, ArenaLocation loc)
   {
      if (loc == null) {
         return false;
      }
      if (curCombatantIndex >= _maxCombatantsPerTeam) {
         return false;
      }
      ArenaLocation oldStart = _startPoints[team][curCombatantIndex];
      if (oldStart != null) {
         oldStart = _locations[oldStart._x][oldStart._y];
         oldStart.setLabel(null);
      }
      loc.setLabel(getLabel(team, curCombatantIndex));
      loc = _locations[loc._x][loc._y];
      loc.setLabel(getLabel(team, curCombatantIndex));
      _startPoints[team][curCombatantIndex] = loc;
      return true;
   }
   public ArenaLocation clearStartingLocation(byte team, byte curCombatantIndex)
   {
       if (curCombatantIndex >= _maxCombatantsPerTeam) {
         return null;
      }
       ArenaLocation oldStart = _startPoints[team][curCombatantIndex];
       if (oldStart != null) {
           oldStart = _locations[oldStart._x][oldStart._y];
           oldStart.setLabel(null);
       }
       _startPoints[team][curCombatantIndex] = null;
       return oldStart;
   }
   public void clearStartingPointLabels() {
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            if (_startPoints[team][cur] != null) {
               _startPoints[team][cur].setLabel(null);
            }
         }
      }
   }
   public void showStartingPointLabels() {
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            if (_startPoints[team][cur] != null) {
               _startPoints[team][cur].setLabel(getLabel(team, cur));
            }
         }
      }
   }
   static public String getLabel(byte team, byte cur) {
       return (TEAM_NAMES[team] + (cur+1));
   }
   public ArenaLocation getStartingLocation(byte team, byte curCombatantIndex) {
      if (team < _startPoints.length) {
         if (curCombatantIndex < _startPoints[team].length) {
            ArenaLocation startLoc = _startPoints[team][curCombatantIndex];
            if (startLoc != null) {
                return _locations[startLoc._x][startLoc._y];
            }
         }
      }
      return null;
   }
   public Facing getStartingFacing(ArenaLocation startingLocation) {
       // always face the center hex.
       ArenaLocation centerHex = getLocation((short)(_sizeX/2), (short)((_sizeY/2) + ((((_sizeX/2)%2) != ((_sizeY/2)%2)) ? 1 : 0)));

       if (startingLocation.sameCoordinates(centerHex)) {
          return Facing.NOON;
       }
       ArrayList<ArenaLocation> path = getLOSPath(startingLocation, centerHex, false/*trimPath*/);
       if (path.size() == 0) {
          return Facing.NOON;
       }
       ArenaLocation firstLocationTowardCenter = path.get(0);
       if (firstLocationTowardCenter.sameCoordinates(startingLocation)) {
         firstLocationTowardCenter = path.get(1);
      }
       return ArenaCoordinates.getFacingToLocation(startingLocation, firstLocationTowardCenter);
   }

   public boolean isFacing(Character attacker, Character defender) {
      for (ArenaCoordinates dest : defender.getCoordinates()) {
         if (isFacing(attacker, dest)) {
            return true;
         }
      }
      return false;
   }
   public boolean isFacing(Character attacker, ArenaCoordinates destination) {
      return isFacing(attacker.getOrientation(), destination);
   }
   public boolean isFacing(Orientation attackerOrient, ArenaCoordinates destination) {
      ArenaCoordinates headCoord = attackerOrient.getHeadCoordinates();
      if ((destination != null) && (headCoord != null)) {
         int xDiff = destination._x - headCoord._x;
         int yDiff = destination._y - headCoord._y;
         Facing attackerFacing = attackerOrient.getFacing();
         if (attackerFacing == Facing.NOON) {
            return (yDiff <= 0);
         }
         if (attackerFacing == Facing._6_OCLOCK) {
            return (yDiff >= 0);
         }
         if (attackerFacing == Facing._2_OCLOCK) {
            return (yDiff <= (xDiff*3));
         }
         if (attackerFacing == Facing._8_OCLOCK) {
            return (yDiff >= (xDiff*3));
         }
         if (attackerFacing == Facing._4_OCLOCK) {
            return (yDiff >= (-xDiff*3));
         }
         if (attackerFacing == Facing._10_OCLOCK) {
            return (yDiff <= (-xDiff*3));
         }
      }
      DebugBreak.debugBreak();
      return false;
   }

   public void updateCombatant(Character newCharacter, boolean checkTriggers)
   {
       List<ArenaLocation> newCharLocs = getLocations(newCharacter);
       boolean characterChangedLocations = false;
       for (ArenaLocation newLoc : newCharLocs) {
          if (newLoc == null) {
             DebugBreak.debugBreak();
          }
          List<Character> chars = newLoc.getCharacters();
          boolean characterFound = false;
          for (Character newLocCharacter : chars) {
             if (newLocCharacter._uniqueID == newCharacter._uniqueID) {
                characterFound = true;
                newLocCharacter.copyData(newCharacter);
                break;
             }
          }
          if (!characterFound) {
             characterChangedLocations = true;
             newLoc.addThing(newCharacter);

             if (checkTriggers) {
                if (CombatServer._isServer) {
                   if (_triggers != null) {
                      for (ArenaTrigger trigger : _triggers) {
                         if (trigger.isTriggerAtLocation(newLoc, newCharacter, this)) {
                            trigger.trigger(newCharacter);
                         }
                      }
                   }
                   for (IAreaSpell spell : newLoc.getActiveSpells()) {
                      spell.affectCharacterOnEntry(newCharacter);
                   }
                }
             }
          }
       }
       if (!characterChangedLocations) {
          // the character didn't move
          return;
       }
// TODO: figure out how to get the neighboring hexes to the new locations
//       // most likely, the character was just 1-hex away before this move
//       // so first just check the neighboring hexes
//       for (int col=(newCharacter._locX-1) ; col<=(newCharacter._locX+1) ; col++) {
//          if ((col >= 0) && (col<_sizeX)) {
//             for (int row=(newCharacter._locY-1) ; row<=(newCharacter._locY+1); row+=2) {
//                if (col == newCharacter._locX) {
//                   if (row==(newCharacter._locY-1)) {
//                      row--;
//                   }
//                   else if (row==(newCharacter._locY+1)) {
//                      row++;
//                   }
//                }
//                if ((row >= 0) && (row<_sizeY)) {
//                   List<Character> characters = _locations[col][row].getCharacters();
//                   for (Character charInHex : characters) {
//                      if (charInHex._uniqueID == newCharacter._uniqueID) {
//                         if ((newCharacter._locX != col) || (newCharacter._locY != row)) {
//                            _locations[col][row].remove(charInHex);
//                            return;
//                         }
//                      }
//                   }
//                }
//             }
//          }
//       }

       // find the old character location.
       for (short col=0 ; col<_sizeX ; col++) {
          for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
             List<Character> characters = _locations[col][row].getCharacters();
             for (Character charInHex : characters) {
                if (charInHex._uniqueID == newCharacter._uniqueID) {
                   if (!newCharLocs.contains(_locations[col][row])) {
                      _locations[col][row].remove(charInHex);
                      for (IAreaSpell spell : _locations[col][row].getActiveSpells()) {
                         spell.affectCharacterOnExit(newCharacter);
                      }
                   }
                }
             }
          }
       }
   }

   public List<Byte> getTeamsAvailable()
   {
      List<Byte> teams = new ArrayList<>();
      for (byte team=0 ; team<3 ; team++) {
         for (byte cur=0 ; cur<_startPoints[team].length ; cur++) {
            if (_startPoints[team][cur] != null) {
               if (_startPoints[team][cur].getCharacters().size() == 0) {
                  teams.add(team);
                  // This break exits the cur for loop, putting us back into the team for loop
                  break;
               }
            }
         }
      }
      return teams;
   }

   private static final String CLOSE_DOOR  = "close door ";
   private static final String OPEN_DOOR   = "open door ";
   private static final String LOCK_DOOR   = "lock door ";
   private static final String UNLOCK_DOOR = "unlock door ";
   private static final String ASSIST_CUT_PRE  = "cut ";
   private static final String ASSIST_CUT_POST = " out of spider web ";
   private static final String ASSIST_POTION_PRE  = "give ";
   private static final String ASSIST_POTION_POST = " your readied potion ";
   private static final String TO_YOUR_FRONT = "in front of you";
   private static final String TO_YOUR_RIGHT = "to your right";
   private static final String TO_YOUR_LEFT  = "to your left";

   private static final HashMap<String, HashMap<String, RequestActionType>>
                             MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE = new HashMap<>();
   static {
      HashMap<String, RequestActionType> mapClose  = new HashMap<>();
      mapClose.put(TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_CLOSE);
      mapClose.put(TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_CLOSE);
      mapClose.put(TO_YOUR_LEFT,  RequestActionType.OPT_LOCATION_ACTION_LEFT_CLOSE);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(CLOSE_DOOR, mapClose);

      HashMap<String, RequestActionType> mapOpen   = new HashMap<>();
      mapOpen.put(TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_OPEN);
      mapOpen.put(TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_OPEN);
      mapOpen.put(TO_YOUR_LEFT,  RequestActionType.OPT_LOCATION_ACTION_LEFT_OPEN);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(OPEN_DOOR, mapOpen);

      HashMap<String, RequestActionType> mapLock   = new HashMap<>();
      mapLock.put(TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_LOCK);
      mapLock.put(TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_LOCK);
      mapLock.put(TO_YOUR_LEFT,  RequestActionType.OPT_LOCATION_ACTION_LEFT_LOCK);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(LOCK_DOOR, mapLock);

      HashMap<String, RequestActionType> mapUnlock = new HashMap<>();
      mapUnlock.put(TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_UNLOCK);
      mapUnlock.put(TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_UNLOCK);
      mapUnlock.put(TO_YOUR_LEFT,  RequestActionType.OPT_LOCATION_ACTION_LEFT_UNLOCK);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(UNLOCK_DOOR, mapUnlock);

      HashMap<String, RequestActionType> mapAssist = new HashMap<>();
      mapAssist.put(TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_ASSIST);
      mapAssist.put(TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_ASSIST);
      mapAssist.put(TO_YOUR_LEFT,  RequestActionType.OPT_LOCATION_ACTION_LEFT_ASSIST);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(ASSIST_CUT_PRE, mapAssist);
      MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.put(ASSIST_POTION_PRE, mapAssist);
   }

   private static RequestActionType getLocActionType(String directionDesc, String actionDesc) {
      return MAP_DIR_TO_MAP_ACTION_TO_ACTTYPE.get(actionDesc).get(directionDesc);
   }

   public void addLocationActions(RequestAction actionReq, Character actor)
   {
      ArenaCoordinates location = actor.getHeadCoordinates();
      ArenaLocation curLoc = _locations[location._x][location._y];
      if (curLoc != null) {
         curLoc.addLocationActions(actionReq, actor);
      }
      Facing curFacing = actor.getFacing();
      ArenaLocation forwardLoc = ArenaLocation.getForwardMovement(curLoc, curFacing, this);
      ArenaLocation rightLoc   = ArenaLocation.getForwardMovement(curLoc, curFacing.turnRight(), this);
      ArenaLocation leftLoc    = ArenaLocation.getForwardMovement(curLoc, curFacing.turnLeft(), this);

      if (forwardLoc != null) {
         addAdjacentLocationAction(actionReq, actor, curLoc, forwardLoc, TO_YOUR_FRONT, RequestActionType.OPT_LOCATION_ACTION_FORWARD_OPEN, true/*includeHalfAngles*/);
      }
      if (rightLoc != null) {
         addAdjacentLocationAction(actionReq, actor, curLoc,   rightLoc, TO_YOUR_RIGHT, RequestActionType.OPT_LOCATION_ACTION_RIGHT_OPEN,   false/*includeHalfAngles*/);
      }
      if (leftLoc != null) {
         addAdjacentLocationAction(actionReq, actor, curLoc,    leftLoc,  TO_YOUR_LEFT, RequestActionType.OPT_LOCATION_ACTION_LEFT_OPEN,    false/*includeHalfAngles*/);
      }
   }

   private static void addAdjacentLocationAction(RequestAction actionReq, Character actor, ArenaLocation fromLoc, ArenaLocation toLoc,
                                                 String doorDirectionDescription, RequestActionType reqIdBase, boolean includeHalfAngles) {
      Door door = getDoorBetween(fromLoc, toLoc, includeHalfAngles);
      if (door != null) {
         if (door.isOpen()) {
            RequestActionType reqActType = getLocActionType(doorDirectionDescription, CLOSE_DOOR);
            actionReq.addOption(new RequestActionOption(CLOSE_DOOR + doorDirectionDescription, reqActType, LimbType.BODY, true/*enabled*/));
            //actionReq.addOption(reqIdBase + RequestAction.OPT_DOOR_ACTION_CLOSE, CLOSE_DOOR + doorDirectionDescription, true/*enabled*/);
         }
         else {
            if (door.isLocked()) {
               boolean hasKey = actor.hasKey(door._keyCode);
               RequestActionType reqActType = getLocActionType(doorDirectionDescription, UNLOCK_DOOR);
               actionReq.addOption(new RequestActionOption(UNLOCK_DOOR + doorDirectionDescription, reqActType, LimbType.BODY, hasKey/*enabled*/));
               //actionReq.addOption(reqIdBase + RequestAction.OPT_DOOR_ACTION_UNLOCK, UNLOCK_DOOR + doorDirectionDescription, hasKey/*enabled*/);
            }
            else {
               RequestActionType reqActType = getLocActionType(doorDirectionDescription, OPEN_DOOR);
               actionReq.addOption(new RequestActionOption(OPEN_DOOR + doorDirectionDescription, reqActType, LimbType.BODY, true/*enabled*/));
               //actionReq.addOption(reqIdBase + RequestAction.OPT_DOOR_ACTION_OPEN, OPEN_DOOR + doorDirectionDescription, true/*enabled*/);
               boolean hasKey = actor.hasKey(door._keyCode);
               reqActType = getLocActionType(doorDirectionDescription, LOCK_DOOR);
               actionReq.addOption(new RequestActionOption(UNLOCK_DOOR + doorDirectionDescription, reqActType, LimbType.BODY, hasKey/*enabled*/));
               //actionReq.addOption(reqIdBase + RequestAction.OPT_DOOR_ACTION_LOCK, LOCK_DOOR + doorDirectionDescription, hasKey/*enabled*/);
            }
         }
      }
      boolean edgeReady = false;
      boolean healingPotionReady = false;
      for (Limb limb : actor.getLimbs()) {
         if (!edgeReady) {
            if (limb.getActionsNeededToReady() == 0) {
               Weapon weap = limb.getWeapon(actor);
               if (weap != null) {
                  for (WeaponStyleAttack style : weap.getAttackStyles()) {
                     if (style.getDamageType() == ostrowski.combat.common.enums.DamageType.CUT) {
                        edgeReady = true;
                        break;
                     }
                  }
               }
            }
         }
         if (!healingPotionReady) {
            Thing thing = limb.getHeldThing();
            if (thing instanceof Potion) {
               Potion potion = (Potion) thing;
               if (potion.isHealing()) {
                  healingPotionReady = true;
               }
            }
         }
         else {
            break;
         }
      }
      if (edgeReady || healingPotionReady) {
         List<Character> chars = toLoc.getCharacters();
         for (Character chr : chars) {
            if (!actor.isEnemy(chr)) {
               if (edgeReady) {
                  Set<IHolder> holders = chr.getHolders();
                  boolean spiderWebSpellFound = false;
                  for (IHolder holder : holders) {
                     if (holder instanceof SpellSpiderWeb) {
                        if (holder.getHoldingLevel() > 0) {
                           spiderWebSpellFound = true;
                           break;
                        }
                     }
                  }
                  if (spiderWebSpellFound) {
                     RequestActionType reqActType = getLocActionType(doorDirectionDescription, ASSIST_CUT_PRE);
                     actionReq.addOption(new RequestActionOption(ASSIST_CUT_PRE + chr.getName() + ASSIST_CUT_POST + doorDirectionDescription, reqActType, LimbType.BODY, true/*enabled*/));
                     //actionReq.addOption(reqIdBase + RequestAction.OPT_ASSIST, ASSIST_CUT_PRE + chr.getName() + ASSIST_CUT_POST + doorDirectionDescription, true/*enabled*/);
                  }
               }
               if (healingPotionReady) {
                  if (chr.getCondition().isAlive() && !chr.getCondition().isConscious()) {
                     RequestActionType reqActType = getLocActionType(doorDirectionDescription, ASSIST_POTION_PRE);
                     actionReq.addOption(new RequestActionOption(ASSIST_POTION_PRE + chr.getName() + ASSIST_POTION_POST + doorDirectionDescription, reqActType, LimbType.BODY, true/*enabled*/));
                     //actionReq.addOption(reqIdBase + RequestAction.OPT_ASSIST, ASSIST_POTION_PRE + chr.getName() + ASSIST_POTION_POST + doorDirectionDescription, true/*enabled*/);
                  }
               }
            }
         }
      }
   }
   private static Door getDoorBetween(ArenaLocation fromLoc, ArenaLocation toLoc, boolean includeHalfAngles) {
      Facing dirToOtherLoc = ArenaCoordinates.getFacingToLocation(fromLoc, toLoc);
      Door door = getDoorInFacing(fromLoc, dirToOtherLoc, includeHalfAngles);
      if (door != null) {
         return door;
      }
      door = getDoorInFacing(toLoc, dirToOtherLoc.turn(3), includeHalfAngles);
      if (door != null) {
         return door;
      }
      // check for doors in the middle of the loLoc
      synchronized (toLoc) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(toLoc._lock_this)) {
            for (Door toLocDoor : toLoc.getDoors()) {
               if (toLocDoor._orientation.contains(TerrainWall.TERRAIN_ALL_CENTER_WALLS)) {
                  return toLocDoor;
               }
            }
         }
      }
      return null;
   }
   private static Door getDoorInFacing(ArenaLocation loc, Facing dirToOtherLoc, boolean includeHalfAngles) {
      synchronized (loc) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this)) {
            ArrayList<Door> doors = loc.getDoors();
            if (doors.isEmpty()) {
               return null;
            }
            long doorsToFind = DOOR_DIRECTIONS_AHEAD.get(dirToOtherLoc).bitMask;
            if (includeHalfAngles) {
               doorsToFind |= DOOR_DIRECTIONS_HALF_RIGHT.get(dirToOtherLoc).bitMask;
               doorsToFind |= DOOR_DIRECTIONS_HALF_RIGHT.get(dirToOtherLoc.turnLeft()).bitMask;
            }
            for (Door door : doors) {
               if (door._orientation.contains(doorsToFind)) {
                  return door;
               }
            }
            return null;
         }
      }
   }
   public String getActionDescription(Character actor, RequestAction actionReq)
   {
      ArenaLocation location = getLocation(actor.getHeadCoordinates());
      if (location == null) {
         return null;
      }
      String desc =  location.getActionDescription(actor, actionReq);
      if (desc != null) {
         return desc;
      }
      String answer = actionReq.getAnswer();
      String pronoun = actor.getHisHer();
      if (answer.startsWith(CLOSE_DOOR)) {
         String direction = answer.substring(CLOSE_DOOR.length());
         return actor.getName() + " closes the door " + direction.replace("your", pronoun);
      }
      if (answer.startsWith(OPEN_DOOR)) {
         String direction = answer.substring(OPEN_DOOR.length());
         return actor.getName() + " opens the door " + direction.replace("your", pronoun);
      }
      if (answer.startsWith(LOCK_DOOR)) {
         String direction = answer.substring(LOCK_DOOR.length());
         return actor.getName() + " locks the door " + direction.replace("your", pronoun);
      }
      if (answer.startsWith(UNLOCK_DOOR)) {
         String direction = answer.substring(UNLOCK_DOOR.length());
         return actor.getName() + " unlocks the door " + direction.replace("your", pronoun);
      }
      if (answer.startsWith(ASSIST_CUT_PRE)) {
         String characterPlusString = answer.substring(ASSIST_CUT_PRE.length());
         int index = characterPlusString.indexOf(ASSIST_CUT_POST);
         String characterName = characterPlusString.substring(0,index).trim();
         String postString = characterPlusString.substring(index).trim();
         String direction = postString.substring(ASSIST_CUT_PRE.length());
         return actor.getName() + " cuts " + characterName + " out of the web " + direction.replace("your", pronoun);
      }
      if (answer.startsWith(ASSIST_POTION_PRE)) {
         String characterPlusString = answer.substring(ASSIST_POTION_PRE.length());
         int index = characterPlusString.indexOf(ASSIST_POTION_POST);
         String characterName = characterPlusString.substring(0,index).trim();
         String postString = characterPlusString.substring(index).trim();
         String direction = postString.substring(ASSIST_POTION_PRE.length());
         return actor.getName() + " gives " + characterName + " a potion " + direction.replace("your", pronoun);
      }
      return null;
   }
   public boolean isPickupItem(Character actor, RequestAction actionReq)
   {
      ArenaLocation location = getLocation(actor.getHeadCoordinates());
      if (location != null) {
         return location.isPickupItem(actor, actionReq);
      }
      return false;
   }
   public String getPickupItemName(Character actor, RequestAction actionReq, int itemIndex)
   {
      ArenaLocation location = getLocation(actor.getHeadCoordinates());
      if (location != null) {
         return location.getPickupItemName(actor, actionReq, itemIndex);
      }
      return null;
   }
   public Object pickupItem(Character actor, RequestAction actionReq, int itemIndex, Diagnostics diag)
   {
      ArenaLocation location = getLocation(actor.getHeadCoordinates());
      if (location != null) {
         return location.pickupItem(actor, actionReq, itemIndex, diag);
      }
      return null;
   }
   public boolean isLocationAction(Character actor, RequestAction actionReq)
   {
      String answer = actionReq.getAnswer();
      if (answer.startsWith(CLOSE_DOOR) ||
          answer.startsWith(OPEN_DOOR) ||
          answer.startsWith(LOCK_DOOR) ||
          answer.startsWith(UNLOCK_DOOR) ||
          answer.startsWith(ASSIST_CUT_PRE) ||
          answer.startsWith(ASSIST_POTION_PRE) ) {
         return true;
      }
      return false;
   }
   public boolean applyAction(Character actor, RequestAction actionReq) throws BattleTerminatedException
   {
      ArenaLocation location = getLocation(actor.getHeadCoordinates());
//      location = _locations[location._x][location._y];
      if (location == null) {
         return false;
      }
      if (location.applyAction(actor, actionReq)) {
         return true;
      }
      if (isLocationAction(actor, actionReq)) {

         Facing curFacing = actor.getFacing();
         String answer = actionReq.getAnswer();

         Door door = null;
         ArenaLocation toLoc = null;
         boolean includeHalfAngles = false;
         Facing toLocFacing = curFacing;
         if (answer.endsWith(TO_YOUR_FRONT)) {
            includeHalfAngles = true;
         }
         else if (answer.endsWith(TO_YOUR_RIGHT)) {
            toLocFacing = curFacing.turnRight();
         }
         else if (answer.endsWith(TO_YOUR_LEFT)) {
            toLocFacing = curFacing.turnLeft();
         }
         else {
            return false;
         }
         toLoc = ArenaLocation.getForwardMovement(location, toLocFacing, this);
         if (toLoc != null) {
            if (answer.startsWith(ASSIST_CUT_PRE)) {
               List<Character> chars = toLoc.getCharacters();
               for (Character chr : chars) {
                  if (!actor.isEnemy(chr)) {
                     Set<IHolder> holders = chr.getHolders();
                     for (IHolder holder : holders) {
                        if (holder instanceof SpellSpiderWeb) {
                           ((SpellSpiderWeb) holder).reduceHoldingLevel((byte)5);
                           chr.setHoldLevel(holder, holder.getHoldingLevel());
                        }
                     }
                  }
               }
            }
            else if (answer.startsWith(ASSIST_POTION_PRE)) {
               Potion potion = null;
               Limb limbWithPotion = null;
               for (Limb limb : actor.getLimbs()) {
                  Thing thing = limb.getHeldThing();
                  if (thing instanceof Potion) {
                     potion = (Potion) thing;
                     if (potion.isHealing()) {
                        limbWithPotion = limb;
                        break;
                     }
                  }
               }
               if ((limbWithPotion != null) && (potion != null)) {
                  List<Character> chars = toLoc.getCharacters();
                  for (Character chr : chars) {
                     Condition cond = chr.getCondition();
                     if (!actor.isEnemy(chr) && cond.isAlive() && !cond.isConscious()) {
                        potion.apply(chr, CombatServer._this.getArena());
                        chr.awaken();
                        limbWithPotion.setHeldThing(null, actor);
                     }
                  }
               }
            }
            else {
               door = getDoorBetween(location, toLoc, includeHalfAngles);
               if (door != null) {
                  ArenaLocation doorLoc = toLoc;
                  synchronized (location) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(location._lock_this)) {
                        if (location.getDoors().contains(door)) {
                           doorLoc = location;
                        }
                     }
                  }

                  ArenaLocation origDoorLoc = doorLoc.clone();
                  boolean results = false;
                  if (answer.startsWith(CLOSE_DOOR)) {
                     results = door.close();
                  }
                  else if (answer.startsWith(OPEN_DOOR)) {
                     results = door.open();
                  }
                  else if (answer.startsWith(LOCK_DOOR)) {
                     results = door.lock();
                  }
                  else if (answer.startsWith(UNLOCK_DOOR)) {
                     results = door.unlock();
                  }
                  if (results) {
                     ObjectChanged changeNotification = new ObjectChanged(origDoorLoc, doorLoc);
                     doorLoc.notifyWatchers(origDoorLoc, doorLoc, changeNotification , null/*skipList*/, null/*diag*/);
                     //notifyWatchers(origDoorLoc, doorLoc, changeNotification , null/*skipList*/, null/*diag*/);
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   /*    VERT_LEFT              10,  2
    *    VERT_CENTER             9,  3
    *    VERT_RIGHT              8,  4
    *    HORIZONTAL_TOP         10,  8
    *    HORIZONTAL_CENTER       0,  6
    *    HORIZONTAL_BOTTOM       2,  4           10 9 8      y = lowest
    *    DIAG_RIGHT_LEFT         0, 10          11     7     y = low
    *    DIAG_RIGHT_CENTER       2,  8          0       6    y = middle
    *    DIAG_RIGHT_RIGHT        4,  6           1     5     y = hi
    *    DIAG_LEFT_LEFT          2,  0            2 3 4      y = highest
    *    DIAG_LEFT_CENTER        4, 10
    *    DIAG_LEFT_RIGHT         6,  8
    *    DIAG_FAR_RIGHT_LEFT     0,  8
    *    DIAG_FAR_RIGHT_CENTER   1,  7
    *    DIAG_FAR_RIGHT_RIGHT    2,  6
    *    DIAG_FAR_LEFT_LEFT      0,  4
    *    DIAG_FAR_LEFT_CENTER   11,  5
    *    DIAG_FAR_LEFT_RIGHT    10,  6
    */
   static final HashMap<Facing, TerrainWall> DOOR_DIRECTIONS_AHEAD = new HashMap<>();
   static final HashMap<Facing, TerrainWall> DOOR_DIRECTIONS_HALF_RIGHT = new HashMap<>();
   {
      DOOR_DIRECTIONS_AHEAD.put(Facing.NOON      , TerrainWall.HORIZONTAL_TOP);
      DOOR_DIRECTIONS_AHEAD.put(Facing._2_OCLOCK , TerrainWall.DIAG_LEFT_RIGHT);
      DOOR_DIRECTIONS_AHEAD.put(Facing._4_OCLOCK , TerrainWall.DIAG_RIGHT_RIGHT);
      DOOR_DIRECTIONS_AHEAD.put(Facing._6_OCLOCK , TerrainWall.HORIZONTAL_BOTTOM);
      DOOR_DIRECTIONS_AHEAD.put(Facing._8_OCLOCK , TerrainWall.DIAG_LEFT_LEFT);
      DOOR_DIRECTIONS_AHEAD.put(Facing._10_OCLOCK, TerrainWall.DIAG_RIGHT_LEFT);

      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing.NOON      , TerrainWall.DIAG_FAR_LEFT_RIGHT);
      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing._2_OCLOCK , TerrainWall.VERT_RIGHT);
      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing._4_OCLOCK , TerrainWall.DIAG_FAR_RIGHT_RIGHT);
      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing._6_OCLOCK , TerrainWall.DIAG_FAR_LEFT_LEFT);
      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing._8_OCLOCK , TerrainWall.VERT_LEFT);
      DOOR_DIRECTIONS_HALF_RIGHT.put(Facing._10_OCLOCK, TerrainWall.DIAG_FAR_RIGHT_LEFT);

   }

   @Override
   public String getObjectIDString()
   {
      return _locBook.getObjectIDString();
   }
   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers()
   {
      return _locBook.getSnapShotOfWatchers();
   }
   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      _locBook.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }
   @Override
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      RegisterResults retVal = _locBook.registerAsWatcher(watcherObject, diag);
//      for (short col = 0 ; col<_sizeX ; col++) {
//         for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
//            ArenaLocation location = getLocation(col, row);
//            ObjectInfo notification = new ObjectInfo(location);
//            watcherObject.monitoredObjectChanged(location, location, notification, null/*skipList*/, diag);
//         }
//      }
      return retVal;
   }
   @Override
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return _locBook.unregisterAsWatcher(watcherObject, diag);
   }
   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return _locBook.unregisterAsWatcherAllInstances(watcherObject, diag);
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects()
   {
      return _mapWatcher.getSnapShotOfWatchedObjects();
   }
   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      _mapWatcher.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }
   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return _mapWatcher.registerMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return _mapWatcher.unregisterMonitoredObject(watchedObject, diag);
   }
   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return _mapWatcher.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
   public boolean serializeToFile(File destFile)
   {
      Document charDoc = getXmlObject(null/*includeKnownByUniqueIDInfo*/);

      try (FileOutputStream fos = new FileOutputStream(destFile)) {
         DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
         DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
         LSSerializer serializer = impl.createLSSerializer();
         LSOutput lso = impl.createLSOutput();
         lso.setByteStream(fos);
         serializer.write(charDoc, lso);
      } catch (IOException |ClassNotFoundException |InstantiationException |IllegalAccessException |ClassCastException e) {
         e.printStackTrace();
      }
      return false;
   }
   public Document getXmlObject(ArrayList<Integer> includeKnownByUniqueIDInfo) {
      // Create a builder factory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true/*validating*/);

      // Create the builder and parse the file
      Document mapDoc = null;
      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         mapDoc = builder.newDocument();
         Element element = getXmlObject(mapDoc, includeKnownByUniqueIDInfo, "\n");
         mapDoc.appendChild(element);
      } catch (ParserConfigurationException e) {
         e.printStackTrace();
      }
      return mapDoc;
   }
   public Element getXmlObject(Document parentDoc, ArrayList<Integer> includeKnownByUniqueIDInfo, String newLine) {
      Element mainElement = parentDoc.createElement("Arena");
      mainElement.setAttribute("Name", getName());
      mainElement.setAttribute("level", String.valueOf(getLevel()));
      mainElement.setAttribute("width", String.valueOf(getSizeX()));
      mainElement.setAttribute("height", String.valueOf(getSizeY()));
      mainElement.setAttribute("hideViewFromLocalPlayers", String.valueOf(_hideViewFromLocalPlayers));

      if (includeKnownByUniqueIDInfo != null) {
         Element uniqueIdElement = parentDoc.createElement("uniqueIds");
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(uniqueIdElement);
         int index=0;
         for (Integer id : includeKnownByUniqueIDInfo) {
            Element columnElement = parentDoc.createElement("id");
            columnElement.setAttribute("uniqueID", String.valueOf(id));
            columnElement.setAttribute("index", String.valueOf(index++));
            uniqueIdElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            uniqueIdElement.appendChild(columnElement);
         }
         uniqueIdElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      boolean writeDataAsColumns = false;
      if (writeDataAsColumns) {
         Element columnsElement = parentDoc.createElement("columns");
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(columnsElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         for (short col = 0 ; col<_sizeX ; col++) {
            Element columnElement = parentDoc.createElement("col");
            columnElement.setAttribute("id", String.valueOf(col));
            StringBuilder sb = new StringBuilder();
            for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
               sb.append(_locations[col][row].getData()).append(SEPARATOR_MAIN);
               if (includeKnownByUniqueIDInfo != null) {
                  int i=1;
                  int mask = 0;
                  for (Integer uniqueID : includeKnownByUniqueIDInfo) {
                     if (_locations[col][row].isKnownBy(uniqueID)) {
                        mask += i;
                     }
                     i*=2;
                  }
                  sb.append(mask).append(SEPARATOR_MAIN);
               }
            }
            columnElement.setTextContent(sb.toString());
            columnsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            columnsElement.appendChild(columnElement);
         }
         columnsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      else {
         Element rowsElement = parentDoc.createElement("rows");
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(rowsElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         for (short row = 0 ; row<_sizeY ; row++) {
            Element rowElement = parentDoc.createElement("row");
            rowElement.setAttribute("id", String.valueOf(row));
            StringBuilder sb = new StringBuilder();
            for (short col = (short)(row%2) ; col<_sizeX ; col += 2) {
               sb.append(_locations[col][row].getData()).append(SEPARATOR_MAIN);
               if (includeKnownByUniqueIDInfo != null) {
                  int i=1;
                  int mask = 0;
                  for (Integer uniqueID : includeKnownByUniqueIDInfo) {
                     if (_locations[col][row].isKnownBy(uniqueID)) {
                        mask += i;
                     }
                     i*=2;
                  }
                  sb.append(mask).append(SEPARATOR_MAIN);
               }
            }
            rowElement.setTextContent(sb.toString());
            rowsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            rowsElement.appendChild(rowElement);
         }
         rowsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      {
         Element teamsElement = parentDoc.createElement("teams");
         teamsElement.setAttribute("count", String.valueOf(_teamCount));
         teamsElement.setAttribute("maxCombatantsPerTeam", String.valueOf(_maxCombatantsPerTeam));
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(teamsElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine));

         for (byte team=0 ; team<_teamCount ; team++) {
            Element teamElement = parentDoc.createElement("team");
            teamElement.setAttribute("id", String.valueOf(team));
            teamsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            teamsElement.appendChild(teamElement);
            for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur ++) {
               Element combatantElement = parentDoc.createElement("combatant");
               combatantElement.setAttribute("id", String.valueOf(cur));
               if (_startPoints == null) {
                  DebugBreak.debugBreak();
                  continue;
               }

               ArenaLocation startLoc = _startPoints[team][cur];
               if (startLoc != null ) {
                  combatantElement.setAttribute("startX", String.valueOf(startLoc._x));
                  combatantElement.setAttribute("startY", String.valueOf(startLoc._y));
               }
               if (_stockAiName[team][cur] == null) {
                  combatantElement.setAttribute("AI", "Off");
               }
               else {
                  combatantElement.setAttribute("AI", _stockAiName[team][cur]);
               }
               if (_stockCharName[team][cur] == null) {
                  combatantElement.setAttribute("charName", " ");
               }
               else {
                  combatantElement.setAttribute("charName", (_stockCharName[team][cur]));
               }
               teamElement.appendChild(parentDoc.createTextNode(newLine + "      "));
               teamElement.appendChild(combatantElement);
            }
            teamElement.appendChild(parentDoc.createTextNode(newLine + "    "));
         }
         teamsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      {
         Element triggersElement = parentDoc.createElement("triggers");
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(triggersElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine));

         for (ArenaTrigger event : _triggers) {
            Element eventElement = event.getXmlNode(parentDoc, newLine + "    ");
            triggersElement.appendChild(parentDoc.createTextNode(newLine + "    "));
            triggersElement.appendChild(eventElement);
         }
         triggersElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      {
         Element itemsElement = parentDoc.createElement("items");
         mainElement.appendChild(parentDoc.createTextNode(newLine + "  "));
         mainElement.appendChild(itemsElement);
         mainElement.appendChild(parentDoc.createTextNode(newLine));

         for (short col = 0 ; col<_sizeX ; col++) {
            for (short row = (short)(col%2) ; row<_sizeY ; row += 2) {
               ArenaLocation location = _locations[col][row];
               synchronized (location) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(location._lock_this)) {
                     for (Object thing : location.getThings()) {
                        // Don't include characters, because they are serialized elsewhere
                        if (thing instanceof Character) {
                           continue;
                        }
                        Element itemElement = parentDoc.createElement("item");
                        if (thing instanceof Thing) {
                           itemElement.setAttribute("name", ((Thing)thing).getName());
                           Race race = ((Thing)thing).getRacialBase();
                           if (race != null) {
                              itemElement.setAttribute("raceBase", race.getName());
                           }
                        }
                        else {// if (thing instanceof String) {
                           itemElement.setAttribute("name", thing.toString());
                        }
                        itemElement.setAttribute("col", String.valueOf(col));
                        itemElement.setAttribute("row", String.valueOf(row));
                        itemsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
                        itemsElement.appendChild(itemElement);
                     }
                     for (Door door : location.getDoors()) {
                        Element doorElement = parentDoc.createElement("door");
                        doorElement.setAttribute("orient", String.valueOf(door._orientation.bitMask));
                        doorElement.setAttribute("key", door._keyCode);
                        doorElement.setAttribute("state", String.valueOf(door._doorState.value));
                        doorElement.setAttribute("col", String.valueOf(col));
                        doorElement.setAttribute("row", String.valueOf(row));
                        itemsElement.appendChild(parentDoc.createTextNode(newLine + "    "));
                        itemsElement.appendChild(doorElement);
                     }
                  }
               }
            }
         }
         itemsElement.appendChild(parentDoc.createTextNode(newLine + "  "));
      }
      return mainElement;
   }
   public boolean serializeFromFile(File sourceFile)
   {
      if (sourceFile.getAbsolutePath().endsWith(".xml")) {
         Document charDoc =  parseXmlFile(sourceFile, false/*validating*/);
         if (charDoc != null) {
            return serializeFromXmlObject(charDoc.getDocumentElement());
         }
      }
      return false;
   }

   public boolean serializeFromXmlObject(Node mapDoc)
   {
      NamedNodeMap namedNodeMap = mapDoc.getAttributes();
      if (namedNodeMap == null) {
         return false;
      }
      _name = namedNodeMap.getNamedItem("Name").getNodeValue();
      Node levelNode = namedNodeMap.getNamedItem("level");
      if (levelNode != null) {
         _level = Short.parseShort(levelNode.getNodeValue());
      }
      else {
         _level = 0;
      }
      short sizeX = Short.parseShort(namedNodeMap.getNamedItem("width").getNodeValue());
      short sizeY = Short.parseShort(namedNodeMap.getNamedItem("height").getNodeValue());
      setSize((short)0, (short)0);
      setSize(sizeX, sizeY);

      _hideViewFromLocalPlayers = false;
      Node node = namedNodeMap.getNamedItem("hideViewFromLocalPlayers");
      if (node != null) {
         String hide = node.getNodeValue();
         if (hide != null) {
            _hideViewFromLocalPlayers = Boolean.parseBoolean(hide);
         }
      }


      _teamCount = CombatServer.MAX_TEAMS;
      _maxCombatantsPerTeam = CombatServer.MAX_COMBATANTS_PER_TEAM;
      _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
      _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
      _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];

      _triggers.clear();
      ArrayList<Integer> includeKnownByUniqueIDInfo = null;

      NodeList children = mapDoc.getChildNodes();
      for (int i=0 ; i<children.getLength() ; i++) {
         Node child = children.item(i);
         if (child.getNodeName().equals("uniqueIds")) {
            includeKnownByUniqueIDInfo = new ArrayList<>();
            NodeList grandChildren = child.getChildNodes();
            for (int j=0 ; j<grandChildren.getLength() ; j++) {
               Node grandChild = grandChildren.item(j);
               NamedNodeMap attributes = grandChild.getAttributes();
               if (attributes != null) {
                  Integer uniqueID = Integer.parseInt(attributes.getNamedItem("uniqueID").getNodeValue());
                  int index = Integer.parseInt(attributes.getNamedItem("index").getNodeValue());
                  while (includeKnownByUniqueIDInfo.size() <= index) {
                     includeKnownByUniqueIDInfo.add(null);
                  }
                  includeKnownByUniqueIDInfo.set(index, uniqueID);
               }
            }
         }
         else if (child.getNodeName().equals("columns")) {
            NodeList grandChildren = child.getChildNodes();
            for (int col=0 ; col<grandChildren.getLength() ; col++) {
               Node grandChild = grandChildren.item(col);
               NamedNodeMap attributes = grandChild.getAttributes();
               if (attributes != null) {
                  short colId = Short.parseShort(attributes.getNamedItem("id").getNodeValue());
                  String colData = grandChild.getTextContent();
                  StringTokenizer st = new StringTokenizer(colData, SEPARATOR_MAIN);
                  for (short row=(short) (colId%2) ; row<_sizeY ; row += 2) {
                     _locations[colId][row] = new ArenaLocation(colId, row);
                     _locations[colId][row].registerAsWatcher(this, _diag);
                     if (!st.hasMoreElements()) {
                        return false;
                     }
                     _locations[colId][row].setData(Integer.parseInt(st.nextToken()));
                     if (includeKnownByUniqueIDInfo != null) {
                        if (!st.hasMoreElements()) {
                           return false;
                        }
                        int mask = Integer.parseInt(st.nextToken());
                        int a=1;
                        for (Integer uniqueID : includeKnownByUniqueIDInfo) {
                           if ((mask & a) != 0) {
                              _locations[colId][row].setKnownBy(uniqueID.intValue(), true);
                           }
                           a *= 2;
                        }
                     }
                  }
               }
            }
         }
         else if (child.getNodeName().equals("rows")) {
            NodeList grandChildren = child.getChildNodes();
            for (int row=0 ; row<grandChildren.getLength() ; row++) {
               Node grandChild = grandChildren.item(row);
               NamedNodeMap attributes = grandChild.getAttributes();
               if (attributes != null) {
                  short rowId = Short.parseShort(attributes.getNamedItem("id").getNodeValue());
                  String rowData = grandChild.getTextContent();
                  StringTokenizer st = new StringTokenizer(rowData, SEPARATOR_MAIN);
                  for (short col=(short) (rowId%2) ; col<_sizeX ; col += 2) {
                     _locations[col][rowId] = new ArenaLocation(col, rowId);
                     _locations[col][rowId].registerAsWatcher(this, _diag);
                     if (!st.hasMoreElements()) {
                        return false;
                     }
                     _locations[col][rowId].setData(Integer.parseInt(st.nextToken()));
                     if (includeKnownByUniqueIDInfo != null) {
                        if (!st.hasMoreElements()) {
                           return false;
                        }
                        int mask = Integer.parseInt(st.nextToken());
                        int a=1;
                        for (Integer uniqueID : includeKnownByUniqueIDInfo) {
                           if ((mask & a) != 0) {
                              _locations[col][rowId].setKnownBy(uniqueID.intValue(), true);
                           }
                           a *= 2;
                        }
                     }
                  }
               }
            }
         }
         else if (child.getNodeName().equals("teams")) {
            NamedNodeMap childAttributes = child.getAttributes();
            if (childAttributes != null) {
               _teamCount = Byte.parseByte(childAttributes.getNamedItem("count").getNodeValue());
               _maxCombatantsPerTeam = Byte.parseByte(childAttributes.getNamedItem("maxCombatantsPerTeam").getNodeValue());

               // allow for an increase in team count or combatants per team
               _teamCount = (byte) Math.max(_teamCount, CombatServer.MAX_TEAMS);
               _maxCombatantsPerTeam = (byte) Math.max(_maxCombatantsPerTeam, CombatServer.MAX_COMBATANTS_PER_TEAM);

               _startPoints = new ArenaLocation[_teamCount][_maxCombatantsPerTeam];
               _stockAiName = new String[_teamCount][_maxCombatantsPerTeam];
               _stockCharName = new String[_teamCount][_maxCombatantsPerTeam];
               NodeList grandChildren = child.getChildNodes();
               for (int col=0 ; col<grandChildren.getLength() ; col++) {
                  Node grandChild = grandChildren.item(col);
                  NamedNodeMap grandChildAttributes = grandChild.getAttributes();
                  if (grandChildAttributes != null) {
                     byte teamId = Byte.parseByte(grandChildAttributes.getNamedItem("id").getNodeValue());
                     NodeList greatGrandChildren = grandChild.getChildNodes();
                     for (int teamIndex=0 ; teamIndex<greatGrandChildren.getLength() ; teamIndex++) {
                        Node greatGrandChild = greatGrandChildren.item(teamIndex);
                        NamedNodeMap greatGrandChildattrs = greatGrandChild.getAttributes();
                        if (greatGrandChildattrs != null) {
                           byte combatantId = Byte.parseByte(greatGrandChildattrs.getNamedItem("id").getNodeValue());

                           Node startXNode = greatGrandChildattrs.getNamedItem("startX");
                           Node startYNode = greatGrandChildattrs.getNamedItem("startY");
                           if ((startXNode != null) && (startYNode != null)) {
                              short startX = Short.parseShort(startXNode .getNodeValue());
                              short startY = Short.parseShort(startYNode.getNodeValue());
                              _startPoints[teamId][combatantId] = _locations[startX][startY];
                              _startPoints[teamId][combatantId].setLabel(getLabel(teamId, combatantId));
                           }
                           _stockAiName[teamId][combatantId] = greatGrandChildattrs.getNamedItem("AI").getNodeValue();
                           _stockCharName[teamId][combatantId] = greatGrandChildattrs.getNamedItem("charName").getNodeValue();
                        }
                     }
                  }
               }
            }
         }
         else if (child.getNodeName().equals("triggers")) {
            NodeList grandChildren = child.getChildNodes();
            for (int index=0 ; index<grandChildren.getLength() ; index++) {
               Node grandChild = grandChildren.item(index);
               ArenaTrigger newEvent = ArenaTrigger.getArenaTrigger(grandChild);
               if (newEvent != null) {
                  _triggers.add(newEvent);
               }
            }
         }
         else if (child.getNodeName().equals("items")) {
            NodeList grandChildren = child.getChildNodes();
            for (int index=0 ; index<grandChildren.getLength() ; index++) {
               Node grandChild = grandChildren.item(index);

               NamedNodeMap attributes = grandChild.getAttributes();
               if (attributes != null) {
                  Thing thing = null;
                  String name = null;
                  Door door = null;
                  if (grandChild.getNodeName().equals("item")) {
                     name = attributes.getNamedItem("name").getNodeValue();
                     Race raceBase = null;
                     Node raceNode = attributes.getNamedItem("raceBase");
                     if (raceNode != null) {
                        raceBase = Race.getRace(raceNode.getNodeValue(), Gender.MALE);
                     }
                     else {
                        raceBase = Race.getRace(Race.NAME_Human, Gender.MALE);
                     }
                     thing = Thing.getThing(name, raceBase);
                  }
                  else if (grandChild.getNodeName().equals("door")) {
                     String orientation = attributes.getNamedItem("orient").getNodeValue();
                     String keyCode     = attributes.getNamedItem("key").getNodeValue();
                     String stateStr   = attributes.getNamedItem("state").getNodeValue();
                     if ((orientation != null) && (stateStr != null)) {
                        long doorOrientation = 0;
                        DoorState doorState = DoorState.Closed;
                        try {
                           doorOrientation = Long.parseLong(orientation);
                        }
                        catch (NumberFormatException e) {
                           for (TerrainWall wall : TerrainWall.values()) {
                              if (wall.name().equals(orientation)) {
                                 doorOrientation = wall.bitMask;
                                 break;
                              }
                           }
                        }
                        try {
                           doorState = DoorState.getByValue(Integer.parseInt(stateStr));
                        }
                        catch (NumberFormatException e) {
                           for (DoorState state : DoorState.values()) {
                              if (state.name().equals(stateStr)) {
                                 doorState = state;
                                 break;
                              }
                           }
                        }
                        door = new Door(doorState, keyCode, TerrainWall.getByBitMask(doorOrientation));
                     }
                  }
                  String rowStr = attributes.getNamedItem("row").getNodeValue();
                  String colStr = attributes.getNamedItem("col").getNodeValue();
                  if ((rowStr != null) && (colStr != null)) {
                     short row = Short.parseShort(rowStr);
                     short col = Short.parseShort(colStr);
                     if ((row >= 0) && (col >= 0) && (row < getSizeY()) && (col < getSizeX())) {
                        if (thing != null) {
                           _locations[col][row].addThing(thing);
                        }
                        else if (door != null) {
                           _locations[col][row].addDoor(door);
                        }
                        else if ((name != null) && (name.length() > 0)) {
                           _locations[col][row].addThing(name);
                        }
                     }
                  }
               }
            }
         }
      }
      return true;
   }
   // Parses an XML file and returns a DOM document.
   // If validating is true, the contents is validated against the DTD
   // specified in the file.
   public static Document parseXmlFile(File sourceFile, boolean validating) {
       try {
           // Create a builder factory
           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           factory.setValidating(validating);

           // Create the builder and parse the file
           Document doc = factory.newDocumentBuilder().parse(sourceFile);
           return doc;
       } catch (SAXException e) {
           // A parsing error occurred; the xml input is not valid
       } catch (ParserConfigurationException e) {
       } catch (IOException e) {
       }
       return null;
   }
   public ArrayList<ArenaLocation> getAdjacentLocations(ArenaLocation location) {
      ArrayList<ArenaLocation> locs = new ArrayList<>();
      for (Facing facing : Facing.values()) {
         ArenaLocation loc = ArenaLocation.getForwardMovement(location, facing, this);
         if (loc != null) {
            locs.add(loc);
         }
      }
      return locs;
   }
   public Character getCombatantByUniqueID(int uniqueID) {
      for (short col=0 ; col<_sizeX ; col++) {
         for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
            List<Character> characters = _locations[col][row].getCharacters();
            for (Character charInHex : characters) {
               if (charInHex._uniqueID == uniqueID) {
                  return charInHex;
               }
            }
         }
      }
      return null;
   }
   public ArenaTrigger getSelectedTrigger() {
      return _selectedTrigger;
   }
   public void setSelectedTrigger(ArenaTrigger trigger) {
      _selectedTrigger = trigger;
   }
   public boolean hasExitTriggers() {
      for (ArenaTrigger trigger : _triggers) {
         if (trigger.hasExitEvent()) {
            return true;
         }
      }
      return false;
   }

   public boolean equals(CombatMap other) {
      if (_name == null) {
         if (other._name != null) {
            return false;
         }
      }
      else {
         if (!_name.equals(other._name)) {
            return false;
         }
      }
      if (_level != other._level) {
         return false;
      }
      if (_sizeY != other._sizeY) {
         return false;
      }
      if (_sizeX != other._sizeX) {
         return false;
      }
      if (_hideViewFromLocalPlayers != other._hideViewFromLocalPlayers) {
         return false;
      }
      for (short col = 0 ; col<_sizeX ; col++) {
         for (short row = (short) (col%2) ; row<_sizeY ; row += 2) {
//            if (!_locations[col][row].equals(other._locations[col][row])) {
//               return false;
//            }
            if (!_locations[col][row].sameContents(other._locations[col][row])) {
               return false;
            }
         }
      }
      if (_maxCombatantsPerTeam != other._maxCombatantsPerTeam) {
         return false;
      }
      if (_teamCount != other._teamCount) {
         return false;
      }
      for (byte team=0 ; team<_teamCount ; team++) {
         for (byte cur=0 ; cur<_maxCombatantsPerTeam ; cur++) {
            if ((_startPoints[team][cur] != null)   && !_startPoints[team][cur].sameCoordinates(other._startPoints[team][cur])) {
               return false;
            }
            if ((_stockAiName[team][cur] != null)   && !_stockAiName[team][cur].equals(other._stockAiName[team][cur])) {
               return false;
            }
            if ((_stockCharName[team][cur] != null) && !_stockCharName[team][cur].equals(other._stockCharName[team][cur])) {
               return false;
            }
         }
      }
      if (_triggers.size() != other._triggers.size()) {
         return false;
      }
      for (int i=0 ; i<_triggers.size() ; i++) {
         if (!_triggers.get(i).equals(other._triggers.get(i))) {
            return false;
         }
      }
      return true;
   }
   public boolean isHideViewFromLocalPlayers() {
      return _hideViewFromLocalPlayers;
   }
   public void setHideViewFromLocalPlayers(boolean hideViewFromLocalPlayers) {
      _hideViewFromLocalPlayers = hideViewFromLocalPlayers;
   }

   public void onNewRound(Battle battle) {
      for (short col=0 ; col<_sizeX ; col++) {
         for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
            List<Character> characters = _locations[col][row].getCharacters();
            for (Character charInHex : characters) {
               for (IAreaSpell spell : _locations[col][row].getActiveSpells()) {
                  spell.affectCharacterOnRoundStart(charInHex);
               }
            }
         }
      }
   }

   public void onEndRound(Battle battle) {
      for (short col=0 ; col<_sizeX ; col++) {
         for (short row=(short)(col%2) ; row<_sizeY; row+=2) {
            List<Character> characters = _locations[col][row].getCharacters();
            for (Character charInHex : characters) {
               for (IAreaSpell spell : _locations[col][row].getActiveSpells()) {
                  spell.affectCharacterOnRoundEnd(charInHex);
               }
            }
         }
      }
   }

}
