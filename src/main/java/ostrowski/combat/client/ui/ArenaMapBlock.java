/*
 * Created on May 26, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;
import ostrowski.util.Diagnostics;

import java.util.HashMap;
import java.util.List;

public class ArenaMapBlock extends Helper
{
   private final static short WIDTH  = 600;
   private final static short HEIGHT = 400;
   private IMapWidget         map;
   public ArenaMapBlock() {
   }
   public void buildBlock(Composite parent, int hSpan) {
      if (CombatServer.uses3dMap) {
         map = new MapWidget3D(parent);
      }
      else {
         map = new MapWidget2D(parent);
      }
      GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.minimumHeight = HEIGHT;
      data.minimumWidth  = WIDTH;
      data.horizontalSpan = hSpan;
      data.horizontalAlignment = GridData.FILL;
      map.setLayoutData(data);
   }
   public void addControlGroup(Composite parent) {
      map.addControlGroup(parent);
   }

   public void updateMap(CombatMap map, int selfID, byte selfTeam, List<ArenaLocation> availableLocs, int targetID) {
      if (this.map.updateMap(map, selfID, selfTeam, availableLocs, targetID)) {
         this.map.setZoomToFit();
      }
   }
   public void updateTargetID(int targetID) {
      map.updateTargetID(targetID);
   }
   public void addListener(IMapListener listener) {
      map.addListener(listener);
   }
   public void redraw() {
      if (map != null) {
         map.redraw();
      }
   }
   public void requestMovement(RequestMovement locationMovement) {
      map.requestMovement(locationMovement);
   }
   public void requestLocation(RequestLocation locationMovement) {
      map.requestLocation(locationMovement);
   }
   public void endHexSelection() {
      map.endHexSelection();
   }
   public CombatMap getCombatMap() {
      return map.getCombatMap();
   }
   public void updateCombatant(Character character) {
      map.updateCombatant(character, true/*redraw*/);
   }
   public void setRouteMap(HashMap<Orientation, Orientation> newMap, List<Orientation> path) {
      map.setRouteMap(newMap, path, true/*allowRedraw*/);
   }
   public void updateArenaLocation(ArenaLocation arenaLoc)
   {
      map.updateArenaLocation(arenaLoc);
   }
   public void updateMapVisibility(MapVisibility mapVisibilty) {
      map.updateMapVisibility(mapVisibilty);
   }
   public void recomputeVisibility(Character self, Diagnostics diag) {
      map.recomputeVisibility(self, diag);
   }
}
