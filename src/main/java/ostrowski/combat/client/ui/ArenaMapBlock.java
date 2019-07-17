/*
 * Created on May 26, 2006
 *
 */
package ostrowski.combat.client.ui;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.IMapListener;
import ostrowski.combat.common.IMapWidget;
import ostrowski.combat.common.MapWidget2D;
import ostrowski.combat.common.MapWidget3D;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;
import ostrowski.util.Diagnostics;

public class ArenaMapBlock extends Helper
{
   private final short _width  = 600;
   private final short _height = 400;
   private IMapWidget          _map;
   public ArenaMapBlock() {
   }
   public void buildBlock(Composite parent, int hSpan) {
      if (CombatServer._uses3dMap) {
         _map = new MapWidget3D(parent);
      }
      else {
         _map = new MapWidget2D(parent);
      }
      GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.minimumHeight = _height;
      data.minimumWidth  = _width;
      data.horizontalSpan = hSpan;
      data.horizontalAlignment = GridData.FILL;
      _map.setLayoutData(data);
   }
   public void addControlGroup(Composite parent) {
      _map.addControlGroup(parent);
   }

   public void updateMap(CombatMap map, int selfID, byte selfTeam, ArrayList<ArenaLocation> availableLocs, int targetID) {
      if (_map.updateMap(map, selfID, selfTeam, availableLocs, targetID)) {
         _map.setZoomToFit();
      }
   }
   public void updateTargetID(int targetID) {
      _map.updateTargetID(targetID);
   }
   public void addListener(IMapListener listener) {
      _map.addListener(listener);
   }
   public void redraw() {
      if (_map != null) {
         _map.redraw();
      }
   }
   public void requestMovement(RequestMovement locationMovement) {
      _map.requestMovement(locationMovement);
   }
   public void requestLocation(RequestLocation locationMovement) {
      _map.requestLocation(locationMovement);
   }
   public void endHexSelection() {
      _map.endHexSelection();
   }
   public CombatMap getCombatMap() {
      return _map.getCombatMap();
   }
   public void updateCombatant(Character character) {
      _map.updateCombatant(character, true/*redraw*/);
   }
   public void setRouteMap(HashMap<Orientation, Orientation> newMap, ArrayList<Orientation> path) {
      _map.setRouteMap(newMap, path, true/*allowRedraw*/);
   }
   public void updateArenaLocation(ArenaLocation arenaLoc)
   {
      _map.updateArenaLocation(arenaLoc);
   }
   public void updateMapVisibility(MapVisibility mapVisibilty) {
      _map.updateMapVisibility(mapVisibilty);
   }
   public void recomputeVisibility(Character self, Diagnostics diag) {
      _map.recomputeVisibility(self, diag);
   }
}
