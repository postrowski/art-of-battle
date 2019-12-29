package ostrowski.combat.common;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.Diagnostics;

public interface IMapWidget extends KeyListener
{
   enum MapMode {
      NONE,
      DRAG,
      PAINT_TERRAIN,
      PAINT_WALL,
      FILL,
      LINE
   }

   void setLayoutData(Object data);
   void addControlGroup(Composite parent);

   boolean updateMap(CombatMap map, int selfID, byte selfTeam, List<ArenaLocation> availableLocs, int targetID);
   CombatMap getCombatMap();

   void setZoomToFit();
   void redraw();

   void addListener(IMapListener listener);

   void setRouteMap(Map<Orientation, Orientation> newMap, List<Orientation> path, boolean b);
   void requestMovement(RequestMovement locationMovement);
   void requestLocation(RequestLocation locationMovement);
   void setSelectableHexes(List<ArenaCoordinates> selectableCoordinates);
   void endHexSelection();

   int getSelfId();
   void setFocusForCharacter(Character actingCharacter, SyncRequest req);
   void updateTargetID(int targetID);
   void updateCombatant(Character character, boolean b);
   void updateArenaLocation(ArenaLocation arenaLoc);

   void updateMapVisibility(MapVisibility mapVisibilty);
   void recomputeVisibility(Character self, Diagnostics diag);

   void setHideViewFromLocalPlayers(boolean selection);

   List<ArenaLocation> getLine();
   List<ArenaLocation> getWallLine();
   void setLine(ArenaCoordinates startCoord, ArenaCoordinates endCoord, RGB lineColor);
   void setWallLine(ArenaLocation start, double startAngleFromCenter, ArenaLocation end, double endAngleFromCenter, RGB color);
   void allowPan(boolean allow);
   void allowDrag(boolean allow);
   void setMode(MapMode mode);
   void applyAnimations();
}
