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
   private final MonitoringObject _mapWatcher;
   private final MonitoredObject  _mapBook;
   private final Character        _owner;
   private final CombatMap        _viewOfMap;
   private final CombatMap        _realMap;
   private final MapVisibility    _mapVisibility;

   public ArenaLocationBook(Character owner, CombatMap map, Diagnostics diag) {
      IMonitorableObject._monitoredObj._objectIDString = this.getClass().getName() + "for " + owner.getObjectIDString();
      IMonitoringObject._monitoringObj._objectIDString = this.getClass().getName() + "for " + owner.getObjectIDString();
      _owner = owner;
      _realMap = map;
      _viewOfMap = (CombatMap) map.clone();
      _mapVisibility = new MapVisibility(_realMap);
      recomputeVisibility(diag);
      _mapBook = new MonitoredObject("ArenaLocationBook._mapBook for " + _owner.getObjectIDString());
      _mapWatcher = new MonitoringObject("ArenaLocationBook.mapWatcher for " + _owner.getObjectIDString(), _mapBook);
      _owner.registerAsWatcher(this, null/*diag*/);
      _realMap.registerAsWatcher(this, diag);
      registerAsWatcher(owner, diag);
   }

   //   public void setLocation(ArenaLocation newLocation, byte newFacing, CombatMap map, Diagnostics diag)
   //   {
   //   }

   @SuppressWarnings("unused")
   private void recomputeVisibility(Diagnostics diag) {
      if (false) {
         // This method is called any time the owner moves,
         // or a door is opened or closed
         MapVisibility origObj = (MapVisibility) _mapVisibility.clone();

         ArenaLocation ownerHeadLoc = _realMap.getHeadLocation(_owner);
         boolean visibilityChanged = false;
         for (short col = 0; col < _realMap.getSizeX(); col++) {
            for (short row = (short) (col % 2); row < _realMap.getSizeY(); row += 2) {
               ArenaLocation realLoc = _realMap.getLocation(col, row);
               ArenaLocation viewLoc = _realMap.getLocation(col, row);
               boolean hasLineOfSight = _realMap.hasLineOfSight(ownerHeadLoc, realLoc, false/*blockedByAnyStandingCharacter*/);
               // setVisibile returns true when the visibility changes
               if (_mapVisibility.setVisible(col, row, hasLineOfSight)) {
                  viewLoc.setVisible(hasLineOfSight, _realMap, ownerHeadLoc, _owner._uniqueID, true/*basedOnFacing*/);
                  visibilityChanged = true;
               }
            }
         }
         if (visibilityChanged) {// && (_mapBook != null)) {
            MapVisibility newObj = (MapVisibility) _mapVisibility.clone();
            ObjectChanged objChanged = new ObjectChanged(origObj, newObj);
            notifyWatchers(this, this, objChanged, null/*skipList*/, diag);
         }
      }
   }

   @Override
   public String getObjectIDString() {
      return _mapBook.getObjectIDString();
   }

   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers() {
      return _mapBook.getSnapShotOfWatchers();
   }

   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                              Vector<IMonitoringObject> skipList, Diagnostics diag) {
      _mapBook.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   @Override
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return _mapBook.registerAsWatcher(watcherObject, diag);
   }

   @Override
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return _mapBook.unregisterAsWatcher(watcherObject, diag);
   }

   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag) {
      return _mapBook.unregisterAsWatcherAllInstances(watcherObject, diag);
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return _mapWatcher.getSnapShotOfWatchedObjects();
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
               if (_mapVisibility.isVisible(origLoc._x, origLoc._y)) {
                  // If the location is visible, make sure its up to date with the
                  // viewed version
                  ArenaLocation viewOfLocation = _viewOfMap.getLocation(origLoc._x, origLoc._y);
                  ArenaLocation realLocation = _realMap.getLocation(origLoc._x, origLoc._y);
                  if (!viewOfLocation.hasSameContents(realLocation)) {
                     ObjectChanged changeLocation = new ObjectChanged(viewOfLocation, realLocation);
                     notifyWatchers(viewOfLocation, realLocation, changeLocation, null/*skipList*/, diag);
                     // update the view version
                     _viewOfMap.getLocation(origLoc._x, origLoc._y).copyData(modLoc);
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
         _mapWatcher.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
      }
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _mapWatcher.registerMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _mapWatcher.unregisterMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return _mapWatcher.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
