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
   private          Button                                    centerOnSelfButton;
   protected        CombatMap                                 combatMap;
   protected final  List<IMapListener>                        listeners;
   protected        int                                       selfID;
   protected        byte                                      selfTeam;
   protected        ArenaLocation                             selfLoc               = null;
   protected        int                                       targetID;
   protected        RequestMovement                           movementRequest       = null;
   protected        RequestLocation                           locationRequest       = null;
   protected        Set<ArenaLocation>                        selectableHexes       = null;
   protected        boolean                                   isDragable            = true;
   protected        List<Orientation>                         mouseOverOrientations = new ArrayList<>();
   protected        Character                                 mouseOverCharacter    = null;
   protected        ArenaLocation                             mouseOverLocation     = null;
   protected final  Map<ArenaCoordinates, List<ArenaTrigger>> eventsMap             = new HashMap<>();
   protected        Map<ArenaCoordinates, ArenaCoordinates>   routeMap              = null;
   protected        List<ArenaCoordinates>                    path                  = null;
   protected static List<ArenaLocation>                       line                  = new ArrayList<>();
   protected static RGB                                       lineColor             = null;
   protected        IMapWidget.MapMode       mapMode       = MapMode.DRAG;
   static private final HashMap<Long, int[]> WALL_POLYGONS = new HashMap<>();
   static public int[] getPolygonForTwoWalls(TerrainWall wallA, TerrainWall wallB) {
      return WALL_POLYGONS.get(wallA.with(wallB));
   }

   static private void addWallPolygon(TerrainWall wallA, TerrainWall wallB, int[] polygon) {
      long key = wallA.with(wallB);
      if (WALL_POLYGONS.containsKey(key)) {
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
      WALL_POLYGONS.put(key, polygon);
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

   protected MapWidget() {
      listeners = new ArrayList<>();

      if (CombatServer.isServer) {
         CombatServer._this.getShell().addKeyListener(this);
      }
   }

   @Override
   public void addControlGroup(Composite parent)
   {
      centerOnSelfButton = createButton(parent, "Center view on self", 1/*hSpan*/, null/*fontData*/, this);
      centerOnSelfButton.setEnabled(false);
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == centerOnSelfButton) {
         centerOnSelf();
      }
   }

   //* return true if the map changed, and needs to be redrawn*/
   protected abstract boolean centerOnSelf();

   protected void updateFindSelfButton() {
      if (centerOnSelfButton == null) {
         return;
      }
      if (centerOnSelfButton.isDisposed()) {
         return;
      }
      Character self = null;
      if ((selfID != -1) && (selfLoc != null)) {
         self = this.combatMap.getCombatantByUniqueID(selfID);
      }
      if (self != null) {
         centerOnSelfButton.setEnabled(true);
         centerOnSelfButton.setText("Center view on " + self.getName());
      }
      else {
         centerOnSelfButton.setEnabled(false);
         centerOnSelfButton.setText("Center view on ---");
      }
      centerOnSelfButton.redraw();
   }

   @Override
   public CombatMap getCombatMap() {
      return combatMap;
   }

   @Override
   public int getSelfId() {
      return selfID;
   }

   @Override
   public void updateTargetID(int targetID) {
      this.targetID = targetID;
   }

   @Override
   public void updateArenaLocation(ArenaLocation arenaLoc)
   {
      ArenaLocation curLoc = combatMap.getLocation(arenaLoc.x, arenaLoc.y);
      if (curLoc != null) {
         // preserve the knownBy self information, since the visibleTo field doesn't get serialized
         ArenaLocation oldData = curLoc.clone();
         curLoc.copyData(arenaLoc);
         curLoc.setKnownBy(selfID, oldData.isKnownBy(selfID));
         curLoc.setVisible(oldData.getVisible(selfID), combatMap, null, selfID, false);
      }
      else {
         combatMap.addLocation(arenaLoc);
      }
   }

   @Override
   public void updateMapVisibility(MapVisibility mapVisibilty)
   {
      for (short col = 0; col < combatMap.getSizeX(); col++) {
         for (short row = (short) (col % 2); row < combatMap.getSizeY(); row += 2) {
            boolean isVisible = mapVisibilty.isVisible(col, row);
            ArenaLocation viewLoc = combatMap.getLocation(col, row);
            viewLoc.setVisible(isVisible, combatMap, selfLoc, selfID, true/*basedOnFacing*/);
         }
      }
   }


   @Override
   public void recomputeVisibility(Character self, Diagnostics diag) {
      if (self == null) {
         if (combatMap.isHideViewFromLocalPlayers() && CombatServer._this.getArena().doLocalPlayersExist()) {
            // If we are hiding the view from local players, don't
            // clear this information out, because it's better to see
            // stale information than a blank screen:
            return;
         }
         selfTeam = -1;
         selfID = -1;
         targetID = -1;
         selfLoc = null;
      }
      else {
         ArenaCoordinates selfCoord = self.getHeadCoordinates();
         ArenaLocation selfLoc = combatMap.getLocation(selfCoord.x, selfCoord.y);
         if (selfLoc == null) // the map may have changed (made smaller) while we were on a spot that is no longer there
         {
            selfTeam = -1;
            selfID = -1;
            targetID = -1;
            this.selfLoc = null;
         }
         else {
            if ((selfTeam == self.teamID) &&
                (selfID == self.uniqueID) &&
                (targetID == self.targetID) &&
                (this.selfLoc.sameCoordinates(selfLoc))) {
               // nothing has changed.
               return;
            }
            selfTeam = self.teamID;
            selfID = self.uniqueID;
            targetID = self.targetID;
            this.selfLoc = selfLoc.clone();
         }
      }
      updateFindSelfButton();
      recomputeVisibilityOfSelf(self);
   }
   public void recomputeVisibilityOfSelf(Character self) {
      combatMap.clearVisibility();
      combatMap.recomputeKnownLocations(self, true/*basedOnFacing*/, true/*setVisibility*/, null/*locsToRedraw*/);
   }

   @Override
   public void requestMovement(RequestMovement locationMovement) {
      setSelectableHexes(locationMovement.getFutureCoordinates());
      movementRequest = locationMovement;
      mouseOverCharacter = getCombatMap().getCombatantByUniqueID(locationMovement.getActorID());
   }

   @Override
   public void requestLocation(RequestLocation locationMovement) {
      locationRequest = locationMovement;
      setSelectableHexes(locationMovement.getSelectableCoordinates());
      // setup a cursor of a the spell effect
   }

   @Override
   public void setSelectableHexes(List<ArenaCoordinates> selectableHexes)
   {
      combatMap.setAllHexesSelectable(false);
      this.selectableHexes = new HashSet<>();
      this.selectableHexes.addAll(combatMap.getLocations(selectableHexes));
      for (ArenaLocation selectableHex : this.selectableHexes) {
         selectableHex.setSelectable(true);
      }
      redraw();
   }

   @Override
   public void endHexSelection()
   {
      if ((selectableHexes != null) ||
          (movementRequest != null) ||
          (locationRequest != null) ||
          (mouseOverCharacter != null)
          ) {

         combatMap.setAllHexesSelectable(true);
         selectableHexes = null;
         movementRequest = null;
         locationRequest = null;
         mouseOverCharacter = null;
         redraw();
      }
   }

   @Override
   public void setRouteMap(Map<Orientation, Orientation> newMap,
                           List<Orientation> path, boolean allowRedraw)
   {
      routeMap = null;
      this.path = null;
      if (newMap != null) {
         routeMap = new HashMap<>();
         Set<Orientation> keys = newMap.keySet();
         for (Orientation key : keys) {
            Orientation value = newMap.get(key);
            if (value != null) {
               ArenaCoordinates valueCoord = value.getHeadCoordinates();
               ArenaCoordinates keyCoord = key.getHeadCoordinates();
               if ((valueCoord != null) && (keyCoord != null)) {
                  // We don't care about facing changes
                  if (!keyCoord.sameCoordinates(valueCoord)) {
                     routeMap.put(keyCoord, valueCoord);
                  }
               }
            }
         }
      }
      if (path != null) {
         this.path = new ArrayList<>();
         for (Orientation orient : path) {
            this.path.add(orient.getHeadCoordinates());
         }
      }

      if (allowRedraw) {
         Canvas canvas = getCanvas();
         if (!canvas.isDisposed()) {
            Display display = canvas.getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(this::redraw);
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

   protected List<Object> getTypeAndLabels(ArenaLocation loc) {
      List<Object> typesAndLabels = new ArrayList<>();

      String label = loc.getLabel();
      if (label == null) {
         label = "";
      }
      if ((label.length() > 0) || (loc.getThings().size() > 0)) {
         if (label.length() > 0) {
            typesAndLabels.add(TYPE_label);
            typesAndLabels.add(label);
         }
         List<Object> things = new ArrayList<>(loc.getThings());

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
      ArenaTrigger trigger = combatMap.getSelectedTrigger();
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
      if (!listeners.contains(listener)) {
         listeners.add(listener);
      }
   }

   @Override
   public void updateCombatant(Character character, boolean redraw)
   {
      if (combatMap == null) {
         return;
      }
      combatMap.updateCombatant(character, true/*checkTriggers*/);
      if (character.uniqueID == selfID) {
         selfLoc = combatMap.getHeadLocation(character).clone();
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
      this.selfID = selfID;
      this.selfTeam = selfTeam;
      this.targetID = targetID;
      boolean mapSizeChanged = (combatMap == null) || (combatMap.getSizeX() != map.getSizeX()) || (combatMap.getSizeY() != map.getSizeY());
      combatMap = map;
      if (mapSizeChanged) {
         setZoomToFit();
      }
      // clear the mouseOverLocation so if the previous position where the mouse was over
      // no longer exists, we don't throw an ArryIndexOutOfBoundsException looking for the ArenaLocation
      mouseOverLocation = null;
      short leftmostVisibleColumn = getLeftmostVisibleColumn();
      short rightmostVisibleColumn = combatMap.getSizeX();
      short topmostVisibleRow = getTopmostVisibleRow();
      short bottommostVisibleColumn = combatMap.getSizeY();
      if (selfID != -1) {
         for (short col = leftmostVisibleColumn; (col < rightmostVisibleColumn) && (selfLoc == null); col++) {
            for (short row = topmostVisibleRow; (row < bottommostVisibleColumn) && (selfLoc == null); row++) {
               if ((row % 2) != (col % 2)) {
                  continue;
               }
               List<Character> characters = combatMap.getLocation(col, row).getCharacters();
               for (Character character : characters) {
                  if (character.uniqueID == selfID) {
                     selfLoc = combatMap.getLocation(col, row).clone();
                     break;
                  }
               }
            }
         }
         if (selfLoc != null) {
            for (short col = leftmostVisibleColumn; col < rightmostVisibleColumn; col++) {
               for (short row = topmostVisibleRow; row < bottommostVisibleColumn; row++) {
                  if ((row % 2) != (col % 2)) {
                     continue;
                  }
                  ArenaLocation viewLoc = combatMap.getLocation(col, row);
                  if (!map.hasLineOfSight(selfLoc, viewLoc, false/*blockedByAnyStandingCharacter*/)) {
                     viewLoc.setVisible(false, combatMap, selfLoc, this.selfID, true/*basedOnFacing*/);
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
               ArenaLocation viewLoc = combatMap.getLocation(col, row);
               ArenaLocation viewerLoc = null;
               boolean visible = false;
               for (ArenaLocation availableLoc : availableLocs) {
                  viewerLoc = availableLoc;
                  if (map.hasLineOfSight(viewerLoc, viewLoc, false/*blockedByAnyStandingCharacter*/)) {
                     visible = true;
                     break;
                  }
               }
               viewLoc.setVisible(visible, combatMap, viewerLoc, this.selfID, true/*basedOnFacing*/);
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
//       for (short col = 0; col < combatMap.getSizeX(); col++) {
//          for (short row = (short) (col % 2); row < combatMap.getSizeY(); row += 2) {
//             ArenaLocation loc = combatMap.getLocation(col, row);
//             loc.setVisible(combatMap, selfLoc, character.uniqueID);
//          }
//       }
//   }

   @Override
   public void setHideViewFromLocalPlayers(boolean hideViewFromLocalPlayers) {
      combatMap.setHideViewFromLocalPlayers(hideViewFromLocalPlayers);
      redraw();
   }
   @Override
   public void setKnownByAllPlayers(boolean knownByAllPlayers) {
      combatMap.setKnownByAllPlayers(knownByAllPlayers);
      redraw();
   }

   @Override
   public void setFocusForCharacter(Character actingCharacter, SyncRequest request) {
      if (actingCharacter.uniqueID == selfID) {
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
         line.clear();
      }
      else {
         line = combatMap.getLOSPath(startCoord, endCoord, false/*trimPath*/);
         ArenaLocation endLocation = combatMap.getLocation(endCoord);
         if (!line.contains(endLocation)) {
            line.add(endLocation);
         }
      }
      MapWidget.lineColor = lineColor;
   }
   @Override
   public List<ArenaLocation> getLine() {
      return line;
   }
   @Override
   public List<ArenaLocation> getWallLine() {
      return line;
   }

   @Override
   public void setWallLine(ArenaLocation start, double startAngleFromCenter, ArenaLocation end, double endAngleFromCenter, RGB color) {
   }

   @Override
   public void allowPan(boolean allow) { isDragable = allow; }
   @Override
   public void allowDrag(boolean allow) { }
   @Override
   public void setMode(MapMode mode) { mapMode = mode;}


   // KeyListener implementation:
   @Override
   public void keyPressed(KeyEvent arg0) {}
   @Override
   public void keyReleased(KeyEvent arg0) {
      if (movementRequest != null) {
         movementRequest.moveByKeystroke(arg0, combatMap);
         if (movementRequest.isAnswered()) {
            if (CombatServer.isServer) {
               CombatServer._this.getArena().completeMove(movementRequest);
            }
         }
      }
   }

}
