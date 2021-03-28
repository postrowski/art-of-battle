/*
 * Created on Dec 3, 2006
 */
package ostrowski.combat.common;

import java.util.Vector;

import ostrowski.combat.protocol.MapVisibility;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.protocol.ObjectChanged;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.MonitoredObject;
import ostrowski.util.MonitoringObject;

public class ArenaLocationBook implements IMonitorableObject, IMonitoringObject
{
   private final MonitoringObject mapWatcher;
   private final MonitoredObject  mapBook;
   private final Character        owner;
   private final CombatMap        viewOfMap;
   private final CombatMap        realMap;
   private final MapVisibility    mapVisibility;

   public ArenaLocationBook(Character owner, CombatMap map, Diagnostics diag) {
      IMonitorableObject.monitoredObj.objectIDString = this.getClass().getName() + "for " + owner.getObjectIDString();
      IMonitoringObject._monitoringObj.objectIDString = this.getClass().getName() + "for " + owner.getObjectIDString();
      this.owner = owner;
      realMap = map;
      viewOfMap = map.clone();
      mapVisibility = new MapVisibility(realMap);
      recomputeVisibility(diag);
      mapBook = new MonitoredObject("ArenaLocationBook._mapBook for " + this.owner.getObjectIDString());
      mapWatcher = new MonitoringObject("ArenaLocationBook.mapWatcher for " + this.owner.getObjectIDString(), mapBook);
      this.owner.registerAsWatcher(this, null/*diag*/);
      realMap.registerAsWatcher(this, diag);
      registerAsWatcher(owner, diag);
   }

   //   public void setLocation(ArenaLocation newLocation, byte newFacing, CombatMap map, Diagnostics diag)
   //   {
   //   }

   private void recomputeVisibility(Diagnostics diag) {
      // This method is called any time the owner moves,
      // or a door is opened or closed
      MapVisibility origObj = mapVisibility.clone();

      ArenaLocation ownerHeadLoc = realMap.getHeadLocation(owner);
      boolean visibilityChanged = false;
      for (short col = 0; col < realMap.getSizeX(); col++) {
         for (short row = (short) (col % 2); row < realMap.getSizeY(); row += 2) {
            ArenaLocation realLoc = realMap.getLocation(col, row);
            boolean hasLineOfSight = realMap.hasLineOfSight(ownerHeadLoc, realLoc, false/*blockedByAnyStandingCharacter*/);
            // is the character looking at this hex?
            if (hasLineOfSight && !realMap.isFacing(owner, realLoc) && !owner.hasPeripheralVision()) {
               hasLineOfSight = false;
               // basedOnFacing can be set to false now that we know he IS facing the location
            }
            // setVisibile returns true when the visibility changes
            if (mapVisibility.setVisible(col, row, hasLineOfSight)) {
               realLoc.setVisible(hasLineOfSight, realMap, ownerHeadLoc, owner.uniqueID, false/*basedOnFacing*/);
               visibilityChanged = true;
            }
         }
      }
      if (visibilityChanged && (mapBook != null)) {
         MapVisibility newObj = mapVisibility.clone();
         ObjectChanged objChanged = new ObjectChanged(origObj, newObj);
         notifyWatchers(this, this, objChanged, null/*skipList*/, diag);
      }
   }

   @Override
   public String getObjectIDString() {
      return mapBook.getObjectIDString();
   }

   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers() {
      return mapBook.getSnapShotOfWatchers();
   }

   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                              Vector<IMonitoringObject> skipList, Diagnostics diag) {
      mapBook.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   @Override
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return mapBook.registerAsWatcher(watcherObject, diag);
   }

   @Override
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return mapBook.unregisterAsWatcher(watcherObject, diag);
   }

   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag) {
      return mapBook.unregisterAsWatcherAllInstances(watcherObject, diag);
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return mapWatcher.getSnapShotOfWatchedObjects();
   }

   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                      Vector<IMonitoringObject> skipList, Diagnostics diag) {
      boolean forwardNotification = true;
      if (changeNotification instanceof ObjectChanged) {
         ObjectChanged changedNotif = (ObjectChanged) changeNotification;
         Object origObj = changedNotif.getOriginalObj();
         Object modObj = changedNotif.getModifiedObj();
         if (origObj instanceof ArenaLocation) {
            if (modObj instanceof ArenaLocation) {
               ArenaLocation origLoc = (ArenaLocation) origObj;
               ArenaLocation modLoc = (ArenaLocation) modObj;
               if (origLoc.getWallsAndClosedDoors() != modLoc.getWallsAndClosedDoors()) {
                  recomputeVisibility(diag);
                  // recomuteVisibility would have sent any changes to the visible flag.
               }
//               ArenaLocation headLoc = owner.getLimbLocation(LimbType.HEAD, realMap);
//               boolean hasLineOfSight = realMap.hasLineOfSight(headLoc, origLoc, false/*blockedByAnyStandingCharacter*/);
//               if (hasLineOfSight) {
               if (mapVisibility.isVisible(origLoc.x, origLoc.y)) {
                  // If the location is visible, make sure its up to date with the
                  // viewed version
                  ArenaLocation viewOfLocation = viewOfMap.getLocation(origLoc.x, origLoc.y);
                  ArenaLocation realLocation = realMap.getLocation(origLoc.x, origLoc.y);
                  if (!viewOfLocation.hasSameContents(realLocation)) {
                     ObjectChanged changeLocation = new ObjectChanged(viewOfLocation, realLocation);
                     notifyWatchers(viewOfLocation, realLocation, changeLocation, null/*skipList*/, diag);
                     // update the view version
                     viewOfMap.getLocation(origLoc.x, origLoc.y).copyData(modLoc);
                  }
               }
               else {
                  forwardNotification = false;
               }
            }
         }
         if (origObj instanceof Character) {
            if (modObj instanceof Character) {
               Character origChar = (Character) origObj;
               Character modChar = (Character) modObj;
               // The Character.compareTo(Character o) method compares the locations of the two characters
               if (origChar.compareTo(modChar) != 0) {
                  recomputeVisibility(diag);
               }
            }
            forwardNotification = false;
         }
      }
      if (forwardNotification) {
         mapWatcher.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
      }
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return mapWatcher.registerMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return mapWatcher.unregisterMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return mapWatcher.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
