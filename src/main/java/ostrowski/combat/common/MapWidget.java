package ostrowski.combat.common;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.*;
import ostrowski.protocol.SyncRequest;
import ostrowski.ui.Helper;
import ostrowski.util.Diagnostics;

import java.util.*;


public abstract class MapWidget extends Helper implements SelectionListener, IMapWidget, KeyListener
{
   static private final HashMap<Long, int[]> wallPolygons = new HashMap<>();
   static public int[] getPolygonForTwoWalls(TerrainWall wallA, TerrainWall wallB) {
      return wallPolygons.get(wallA.with(wallB));
   }

   static private void addWallPolygon(TerrainWall wallA, TerrainWall wallB, int[] polygon) {
      long key = wallA.with(wallB);
      if (wallPolygons.containsKey(key)) {
         DebugBreak.debugBreak("duplicate Polygons defined for a single wall pair: " + wallA + ", " + wallB);
      }

      if (polygon == null) {
         HashSet<Integer> points = new HashSet<>();
         int wallsFound = 0;
         for (TerrainWall terrainWall : TerrainWall.values()) {
            if ((terrainWall == wallA) || (terrainWall == wallB)) {
               points.add(terrainWall.startPoint);
               points.add(terrainWall.endPoint);
               if (wallsFound++ == 1) {
                  break;
               }
            }
         }
         // give the clockwise polygon that defines the single polygon that covers both walls:
         polygon = new int[points.size()];
         int i=0;
         for (int vertex=12 ; vertex>=0 ; vertex--) {
            if (points.contains(vertex)) {
               polygon[i++] = vertex;
            }
         }
      }
      wallPolygons.put(key, polygon);
   }
   static {
      // For each of the six vertices, connect the appropriate other lines into polygons.
      for (int i=0 ; i<12 ; i+=2) {
         // short edge lines with a connected long edge:
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i, i + 4), null);
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i + 2, i + 10), null);
         // short edge lines with a long center line:
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i, i + 6), null);
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i + 2, i + 8), null);
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i + 4, i + 10), null);
         // short edge lines with a short center line:
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i + 5, i + 11), null);
         addWallPolygon(getWallByVertexPoints(i, i + 2), getWallByVertexPoints(i + 3, i + 9), null);
         // Short center line with an adjacent parallel line:
         addWallPolygon(getWallByVertexPoints(i, i + 4), getWallByVertexPoints(i + 5, i + 11), null);
         // Short center line with a connected center line:
         addWallPolygon(getWallByVertexPoints(i, i + 4), getWallByVertexPoints(i, i + 6), null);
         addWallPolygon(getWallByVertexPoints(i, i + 4), getWallByVertexPoints(i + 4, i + 10), null);
      }
   }
   /*
    *   10 9 8      y = lowest
    *  11     7     y = low
    *  0       6    y = middle
    *   1     5     y = high
    *    2 3 4      y = highest
    */
   static TerrainWall getWallByVertexPoints(int start, int end) {
      // normalize parameters to within 0-11:
      start = ((start+12) % 12);
      end = ((end+12) % 12);
      for (TerrainWall terrainWall : TerrainWall.values()) {
         if ((terrainWall.startPoint == start) || (terrainWall.startPoint == end)) {
            if ((terrainWall.endPoint == start) || (terrainWall.endPoint == end)) {
               return terrainWall;
            }
         }
      }
      DebugBreak.debugBreak("invalid vertex points specified: " + start + ", " + end);
      return null;
   }

   private         Button                                    _centerOnSelfButton;
   protected       CombatMap                                 _combatMap;
   protected final List<IMapListener>                        _listeners;
   protected       int                                       _selfID;
   protected       byte                                      _selfTeam;
   protected       ArenaLocation                             _selfLoc               = null;
   protected       int                                       _targetID;
   protected       RequestMovement                           _movementRequest       = null;
   protected       RequestLocation                           _locationRequest       = null;
   protected       List<ArenaLocation>                       _selectableHexes       = null;
   protected       boolean                                   _isDragable            = true;
   protected       List<Orientation>                         _mouseOverOrientations = new ArrayList<>();
   protected       Character                                 _mouseOverCharacter    = null;
   protected       ArenaLocation                             _mouseOverLocation     = null;
   protected final Map<ArenaCoordinates, List<ArenaTrigger>> _eventsMap             = new HashMap<>();

   protected        Map<ArenaCoordinates, ArenaCoordinates> _routeMap  = null;
   protected        List<ArenaCoordinates>                  _path      = null;
   protected static List<ArenaLocation>                     _line      = new ArrayList<>();
   protected static RGB                                     _lineColor = null;

   protected IMapWidget.MapMode _mapMode = MapMode.DRAG;

   protected MapWidget() {
      _listeners = new ArrayList<>();

      if (CombatServer._isServer) {
         CombatServer._this.getShell().addKeyListener(this);
      }
   }

   @Override
   public void addControlGroup(Composite parent)
   {
      _centerOnSelfButton = createButton(parent, "Center view on self", 1/*hSpan*/, null/*fontData*/, this);
      _centerOnSelfButton.setEnabled(false);
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _centerOnSelfButton) {
         centerOnSelf();
      }
   }

   //* return true if the map changed, and needs to be redrawn*/
   protected abstract boolean centerOnSelf();

   protected void updateFindSelfButton() {
      if (_centerOnSelfButton == null) {
         return;
      }
      if (_centerOnSelfButton.isDisposed()) {
         return;
      }
      Character self = null;
      if ((_selfID != -1) && (_selfLoc != null)) {
         self = this._combatMap.getCombatantByUniqueID(_selfID);
      }
      if (self != null) {
         _centerOnSelfButton.setEnabled(true);
         _centerOnSelfButton.setText("Center view on " + self.getName());
      }
      else {
         _centerOnSelfButton.setEnabled(false);
         _centerOnSelfButton.setText("Center view on ---");
      }
      _centerOnSelfButton.redraw();
   }

   @Override
   public CombatMap getCombatMap() {
      return _combatMap;
   }

   @Override
   public int getSelfId() {
      return _selfID;
   }

   @Override
   public void updateTargetID(int targetID) {
      _targetID = targetID;
   }

   @Override
   public void updateArenaLocation(ArenaLocation arenaLoc)
   {
      ArenaLocation curLoc = _combatMap.getLocation(arenaLoc._x, arenaLoc._y);
      if (curLoc != null) {
         // preserve the knownBy self information, since the _visibleTo field doesn't get serialized
         ArenaLocation oldData = curLoc.clone();
         curLoc.copyData(arenaLoc);
         curLoc.setKnownBy(_selfID, oldData.isKnownBy(_selfID));
         curLoc.setVisible(oldData.getVisible(_selfID), _combatMap, null, _selfID, false);
      }
      else {
         _combatMap.addLocation(arenaLoc);
      }
   }

   @Override
   public void updateMapVisibility(MapVisibility mapVisibilty)
   {
      for (short col = 0; col < _combatMap.getSizeX(); col++) {
         for (short row = (short) (col % 2); row < _combatMap.getSizeY(); row += 2) {
            boolean isVisible = mapVisibilty.isVisible(col, row);
            ArenaLocation viewLoc = _combatMap.getLocation(col, row);
            viewLoc.setVisible(isVisible, _combatMap, _selfLoc, _selfID, true/*basedOnFacing*/);
         }
      }
   }


   @Override
   public void recomputeVisibility(Character self, Diagnostics diag) {
      if (self == null) {
         if (_combatMap.isHideViewFromLocalPlayers() && CombatServer._this.getArena().doLocalPlayersExist()) {
            // If we are hiding the view from local players, don't
            // clear this information out, because it's better to see
            // stale information than a blank screen:
            return;
         }
         _selfTeam = -1;
         _selfID   = -1;
         _targetID = -1;
         _selfLoc  = null;
      }
      else {
         ArenaCoordinates selfCoord = self.getHeadCoordinates();
         ArenaLocation selfLoc = _combatMap.getLocation(selfCoord._x, selfCoord._y);
         if (selfLoc == null) // the map may have changed (made smaller) while we were on a spot that is no longer there
         {
            _selfTeam = -1;
            _selfID   = -1;
            _targetID = -1;
            _selfLoc  = null;
         }
         else {
            if ((_selfTeam == self._teamID) &&
                (_selfID   == self._uniqueID) &&
                (_targetID == self._targetID) &&
                (_selfLoc.sameCoordinates(selfLoc))) {
               // nothing has changed.
               return;
            }
            _selfTeam = self._teamID;
            _selfID   = self._uniqueID;
            _targetID = self._targetID;
            _selfLoc  = selfLoc.clone();
         }
      }
      updateFindSelfButton();
      recomputeVisibilityOfSelf(self);
   }
   public void recomputeVisibilityOfSelf(Character self) {
      _combatMap.clearVisibility();
      _combatMap.recomputeKnownLocations(self, true/*basedOnFacing*/, true/*setVisibility*/, null/*locsToRedraw*/);
   }

   @Override
   public void requestMovement(RequestMovement locationMovement) {
      _combatMap.setAllHexesSelectable(false);
      setSelectableHexes(locationMovement.getFutureCoordinates());
      _movementRequest  = locationMovement;
      _mouseOverCharacter = getCombatMap().getCombatantByUniqueID(locationMovement.getActorID());
   }

   @Override
   public void requestLocation(RequestLocation locationMovement) {
      _combatMap.setAllHexesSelectable(false);
      _locationRequest  = locationMovement;
      setSelectableHexes(locationMovement.getSelectableCoordinates());
      // setup a cursor of a the spell effect
   }

   @Override
   public void setSelectableHexes(List<ArenaCoordinates> selectableHexes)
   {
      _selectableHexes  = _combatMap.getLocations(selectableHexes);
      for (ArenaLocation selectableHex : _selectableHexes) {
         selectableHex.setSelectable(true);
      }
      redraw();
   }

   @Override
   public void endHexSelection()
   {
      if ((_selectableHexes             != null) ||
          (_movementRequest             != null) ||
          (_locationRequest             != null) ||
          (_mouseOverCharacter          != null)
          ) {

         _combatMap.setAllHexesSelectable(true);
         _selectableHexes             = null;
         _movementRequest             = null;
         _locationRequest             = null;
         _mouseOverCharacter          = null;
         redraw();
      }
   }

   @Override
   public void setRouteMap(Map<Orientation, Orientation> newMap,
                           List<Orientation> path, boolean allowRedraw)
   {
      _routeMap = null;
      _path = null;
      if (newMap != null) {
         _routeMap = new HashMap<>();
         Set<Orientation> keys = newMap.keySet();
         for (Orientation key : keys) {
            Orientation value = newMap.get(key);
            if (value != null) {
               ArenaCoordinates valueCoord = value.getHeadCoordinates();
               ArenaCoordinates keyCoord = key.getHeadCoordinates();
               if ((valueCoord != null) && (keyCoord != null)) {
                  // We don't care about facing changes
                  if (!keyCoord.sameCoordinates(valueCoord)) {
                     _routeMap.put(keyCoord, valueCoord);
                  }
               }
            }
         }
      }
      if (path != null) {
         _path = new ArrayList<>();
         for (Orientation orient : path) {
            _path.add(orient.getHeadCoordinates());
         }
      }

      if (allowRedraw) {
         Canvas canvas = getCanvas();
         if (!canvas.isDisposed()) {
            Display display = canvas.getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(new Runnable() {
                  @Override
                  public void run() {
                     redraw();
                  }
               });
            }
         }
      }
   }

   protected static final Object TYPE_label        = new Object();
   protected static final Object TYPE_fighting     = new Object();
   protected static final Object TYPE_non_fighting = new Object();
   protected static final Object TYPE_thing        = new Object();
   protected static final Object TYPE_string       = new Object();
   protected static final Object TYPE_event        = new Object();
   protected static final Object TYPE_trigger      = new Object();

   protected ArrayList<Object> getTypeAndLabels(ArenaLocation loc) {
      ArrayList<Object> typesAndLabels = new ArrayList<>();

      String label = loc.getLabel();
      if (label == null) {
         label = "";
      }
      if ((label.length() > 0) || (loc.getThings().size() > 0)) {
         if (label.length() > 0) {
            typesAndLabels.add(TYPE_label);
            typesAndLabels.add(label);
         }
         ArrayList<Object> things = new ArrayList<>(loc.getThings());

         for (Object thing : things) {
            if (thing instanceof Character) {
               Character combatant = (Character) thing;
               if (combatant.getHeadCoordinates().sameCoordinates(loc)) {
                  typesAndLabels.add(combatant.stillFighting() ? TYPE_fighting : TYPE_non_fighting);
                  typesAndLabels.add(combatant.getName());
               }
            }
         }
         for (Object thing : things) {
            if (thing instanceof Thing) {
               typesAndLabels.add(TYPE_thing);
               typesAndLabels.add(((Thing)thing).getName());
            }
            if (thing instanceof String) {
               typesAndLabels.add(TYPE_string);
               typesAndLabels.add(thing);
            }
         }
      }
      ArenaTrigger trigger = _combatMap.getSelectedTrigger();
      if (trigger != null) {
         if (trigger.isTriggerAtLocation(loc, null/*mover*/)) {
            typesAndLabels.add(TYPE_trigger);
            typesAndLabels.add(trigger.getName());
         }
         for (ArenaEvent event : trigger.getEvents()) {
            if (event.isEventAtLocation(loc)) {
               typesAndLabels.add(TYPE_event);
               typesAndLabels.add(event.getName());
            }
         }
      }
      return typesAndLabels;
   }

   protected abstract Canvas getCanvas();

   @Override
   public void addListener(IMapListener listener)
   {
      if (!_listeners.contains(listener)) {
         _listeners.add(listener);
      }
   }

   @Override
   public void updateCombatant(Character character, boolean redraw)
   {
      if (_combatMap == null) {
         return;
      }
      _combatMap.updateCombatant(character, true/*checkTriggers*/);
      if (character._uniqueID == _selfID) {
         _selfLoc = _combatMap.getHeadLocation(character).clone();
         recomputeVisibilityOfSelf(character);
         //recomputeVisibilityByBruteForce(character);
      }
      if (redraw) {
         redraw();
      }
   }

   @Override
   public boolean updateMap(CombatMap map, int selfID, byte selfTeam,
                            List<ArenaLocation> availableLocs, int targetID)
   {
      _selfID   = selfID;
      _selfTeam = selfTeam;
      _targetID = targetID;
      boolean mapSizeChanged = (_combatMap == null) || (_combatMap.getSizeX() != map.getSizeX()) || (_combatMap.getSizeY() != map.getSizeY());
      _combatMap = map;
      if (mapSizeChanged) {
         setZoomToFit();
      }
      // clear the mouseOverLocation so if the previous position where the mouse was over
      // no longer exists, we don't throw an ArryIndexOutOfBoundsException looking for the ArenaLocation
      _mouseOverLocation = null;
      short leftmostVisibleColumn = getLeftmostVisibleColumn();
      short rightmostVisibleColumn = _combatMap.getSizeX();
      short topmostVisibleRow = getTopmostVisibleRow();
      short bottommostVisibleColumn = _combatMap.getSizeY();
      if (selfID != -1) {
         for (short col = leftmostVisibleColumn; (col < rightmostVisibleColumn) && (_selfLoc == null); col++) {
            for (short row = topmostVisibleRow; (row < bottommostVisibleColumn) && (_selfLoc == null); row++) {
               if ((row % 2) != (col % 2)) {
                  continue;
               }
               List<Character> characters = _combatMap.getLocation(col, row).getCharacters();
               for (Character character : characters) {
                  if (character._uniqueID == selfID) {
                     _selfLoc = _combatMap.getLocation(col, row).clone();
                     break;
                  }
               }
            }
         }
         if (_selfLoc != null) {
            for (short col = leftmostVisibleColumn; col < rightmostVisibleColumn; col++) {
               for (short row = topmostVisibleRow; row < bottommostVisibleColumn; row++) {
                  if ((row % 2) != (col % 2)) {
                     continue;
                  }
                  ArenaLocation viewLoc = _combatMap.getLocation(col, row);
                  if (!map.hasLineOfSight(_selfLoc, viewLoc, false/*blockedByAnyStandingCharacter*/)) {
                     viewLoc.setVisible(false, _combatMap, _selfLoc, _selfID, true/*basedOnFacing*/);
                  }
               }
            }
         }
      }
      else if (availableLocs != null) {
         for (short col = leftmostVisibleColumn; col < rightmostVisibleColumn; col++) {
            for (short row = topmostVisibleRow; row < bottommostVisibleColumn; row++) {
               if ((row % 2) != (col % 2)) {
                  continue;
               }
               ArenaLocation viewLoc = _combatMap.getLocation(col, row);
               ArenaLocation viewerLoc = null;
               boolean visible = false;
               for (ArenaLocation availableLoc : availableLocs) {
                  viewerLoc = availableLoc;
                  if (map.hasLineOfSight(viewerLoc, viewLoc, false/*blockedByAnyStandingCharacter*/)) {
                     visible = true;
                     break;
                  }
               }
               viewLoc.setVisible(visible, _combatMap, viewerLoc, _selfID, true/*basedOnFacing*/);
            }
         }
      }
      updateFindSelfButton();
      if (!mapSizeChanged) {
         redraw();
      }
      return mapSizeChanged;
   }

   protected short getTopmostVisibleRow() {
      return 0;
   }
   protected short getLeftmostVisibleColumn() {
      return 0;
   }

//   public void recomputeVisibilityByBruteForce(Character character) {
//       for (short col = 0; col < _combatMap.getSizeX(); col++) {
//          for (short row = (short) (col % 2); row < _combatMap.getSizeY(); row += 2) {
//             ArenaLocation loc = _combatMap.getLocation(col, row);
//             loc.setVisible(_combatMap, _selfLoc, character._uniqueID);
//          }
//       }
//   }

   @Override
   public void setHideViewFromLocalPlayers(boolean hideViewFromLocalPlayers) {
      _combatMap.setHideViewFromLocalPlayers(hideViewFromLocalPlayers);
      redraw();
   }

   @Override
   public void setFocusForCharacter(Character actingCharacter, SyncRequest request) {
      if (actingCharacter._uniqueID == _selfID) {
         if (centerOnSelf()) {
            redraw();
         }
         return;
      }

      if (request instanceof RequestMovement) {
         requestMovement((RequestMovement) request);
      }
      else if (request instanceof RequestLocation) {
         requestLocation((RequestLocation) request);
      }
      recomputeVisibility(actingCharacter, null/*diag*/);
      centerOnSelf();
      redraw();
   }

   @Override
   public void setLine(ArenaCoordinates startCoord, ArenaCoordinates endCoord, RGB lineColor) {
      if ((startCoord == null) || (endCoord == null)) {
         _line.clear();
      }
      else {
         _line = _combatMap.getLOSPath(startCoord, endCoord, false/*trimPath*/);
         ArenaLocation endLocation = _combatMap.getLocation(endCoord);
         if (!_line.contains(endLocation)) {
            _line.add(endLocation);
         }
      }
      _lineColor = lineColor;
   }
   @Override
   public List<ArenaLocation> getLine() {
      return _line;
   }
   @Override
   public List<ArenaLocation> getWallLine() {
      return _line;
   }

   @Override
   public void setWallLine(ArenaLocation start, double startAngleFromCenter, ArenaLocation end, double endAngleFromCenter, RGB color) {
   }

   @Override
   public void allowPan(boolean allow) { _isDragable = allow; }
   @Override
   public void allowDrag(boolean allow) { }
   @Override
   public void setMode(MapMode mode) { _mapMode = mode;}


   // KeyListener implementation:
   @Override
   public void keyPressed(KeyEvent arg0) {}
   @Override
   public void keyReleased(KeyEvent arg0) {
      if (_movementRequest != null) {
         _movementRequest.moveByKeystroke(arg0, _combatMap);
         if (_movementRequest.isAnswered()) {
            if (CombatServer._isServer) {
               CombatServer._this.getArena().completeMove(_movementRequest);
            }
         }
      }
   }

}
