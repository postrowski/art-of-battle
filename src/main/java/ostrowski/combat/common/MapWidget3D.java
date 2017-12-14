package ostrowski.combat.common;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Head;
import ostrowski.combat.common.things.Leg;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Tail;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.things.Wing;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.graphics.AnimationSequence;
import ostrowski.graphics.GLView;
import ostrowski.graphics.IGLViewListener;
import ostrowski.graphics.SequenceLibrary;
import ostrowski.graphics.model.ISelectionWatcher;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjHex;
import ostrowski.graphics.model.ObjHex.Terrain;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.CombatSemaphore;
import ostrowski.util.Diagnostics;
import ostrowski.util.IMonitorableObject;
import ostrowski.util.IMonitoringObject;
import ostrowski.util.MonitoredObject;
import ostrowski.util.MonitoringObject;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoTracker;
import ostrowski.util.SemaphoreAutoUntracker;

public class MapWidget3D extends MapWidget implements ISelectionWatcher, IMonitoringObject
{
   private final GLView                                 _view;
   private final Display                                _display;
   private final HashMap<ArenaLocation, TexturedObject> _locationToObjectMap = new HashMap<>();
   private boolean                                _setZoomOnLoad;
   private final HashMap<HumanBody, ArenaLocation>      _mouseOverBodies     = new HashMap<>();
   private final HashMap<String, Texture> _textureByRaceName = new HashMap<>();

   public MapWidget3D(Composite parent) {
      _view = new GLView(parent);
      Canvas canvas = _view.getCanvas();
      canvas.addKeyListener(this);
      canvas.getParent().addKeyListener(this);
      canvas.getParent().getParent().addKeyListener(this);

      _view.addSelectionWatcher(this);
      _display = parent.getDisplay();
      CombatServer.registerMapWidget3D(this);

      for (String race : SequenceLibrary._availableRaces) {
         try {
            Texture texture = _view.getTextureLoader().getTexture("res/bodyparts/texture_"+race+"male.png");
            if (texture != null) {
               _textureByRaceName.put(race, texture);
            }
         } catch (IOException e) {
         }
      }
   }

   @Override
   public void setLayoutData(Object data) {
      _view.getCanvas().setLayoutData(data);
   }

   @Override
   public void allowPan(boolean allow) {
      _view.allowPan(allow);
   }

   @Override
   public void allowDrag(boolean allow) {
      _view.allowDrag(allow);
   }

   Semaphore                    _lock_animationsPending   = new Semaphore("_lock_animationsPending", CombatSemaphore.CLASS_MAPWIDGET3D_animationsPending);
   Semaphore                    _lock_animatedObjects     = new Semaphore("_lock_animatedObjects", CombatSemaphore.CLASS_MAPWIDGET3_animatedObjects);
   Semaphore                    _lock_locationToObjectMap = new Semaphore("_lock_locationToObjectMap", CombatSemaphore.CLASS_MAPWIDGET3_locationToObjectMap);
   private final ArrayList<HumanBody> _animationsPending = new ArrayList<>();
   private final ArrayList<HumanBody> _animatedObjects   = new ArrayList<>();
   Thread                       _animationThread   = null;

   public void checkAnimation() {
      if (_animationThread != null) {
         return;
      }
      synchronized (_lock_animatedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_animatedObjects)) {
            if (_animatedObjects.size() == 0) {
               return;
            }
         }
      }

      _animationThread = new Thread() {
         @Override
         public void run() {
            try {
               Thread.currentThread().setName("AnimationThread");
               while (true) {
                  List<HumanBody> itemsToRemove = new ArrayList<>();
                  synchronized (_lock_animatedObjects) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_animatedObjects)) {
                        if (_animatedObjects.size() == 0) {
                           return;
                        }

                        for (HumanBody human : _animatedObjects) {
                           if (!human.advanceAnimation()) {
                              itemsToRemove.add(human);
                              Set<Entry<Integer, HumanBody>> set = _characterIdToHumanBody.entrySet();
                              Character character = null;
                              for (Entry<Integer, HumanBody> pair : set) {
                                 if (pair.getValue() == human) {
                                    character = CombatServer._this._map.getCombatMap().getCombatantByUniqueID(pair.getKey());
                                    break;
                                 }
                              }
                              if (character != null) {
                                 updateHumanFromCharacter(character, character.getOrientation(), human);
                              }
                           }
                        }
                        _animatedObjects.removeAll(itemsToRemove);
                     }
                  }
                  synchronized (_lock_animationsPending) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_animationsPending)) {
                        _animationsPending.removeAll(itemsToRemove);
                        System.out.println("clearing pending animations.");
                        _lock_animationsPending.notifyAll();
                        System.out.println("all pending animations cleared.");
                     }
                  }
                  Thread.sleep(10);
                  Display.getDefault().asyncExec(new Runnable() {
                     @Override
                     public void run() {
                        redraw();
                     }
                  });
               }
            } catch (InterruptedException e) {
            } catch (IllegalMonitorStateException e) {
               System.out.println(e.toString());
            } finally {
               _animationThread = null;
            }
         }
      };

      _animationThread.setName("animationThread");
      _animationThread.start();
   }

   @Override
   public void recomputeVisibilityOfSelf(Character self) {
      super.recomputeVisibilityOfSelf(self);
      forceMapRecreation();
   }

   @Override
   public void redraw() {
      _view.drawScene(_display);
   }

   @Override
   public void setZoomToFit() {
      if (_combatMap == null) {
         return;
      }
      short centerX = (short) (_combatMap.getSizeX() / 2);
      short centerY = (short) (_combatMap.getSizeY() / 2);
      if ((centerY % 2) != (centerX % 2)) {
         centerY++;
      }
      ArenaLocation centerLoc = _combatMap.getLocation(centerX, centerY);
      // set the camera height equal to half the map length, and the camera position off the bottom edge of the map.
      TexturedObject centerObj;
      synchronized (_locationToObjectMap) {
         _lock_locationToObjectMap.check();
         centerObj = _locationToObjectMap.get(centerLoc);
      }
      if (centerObj == null) {
         _setZoomOnLoad = true;
         return;
      }
      Tuple3 centerPos = centerObj._models.get(0)._data.getFace(0).getVertex(0);
      _view._cameraPosition = new Tuple3(-centerPos.getX(), _view._cameraPosition.getY(), centerPos.getZ() * -3);
      _view.setHeightScaleByApproximateHeightInInches((centerPos.getX() / 6)/*desiredHeightInInches*/);
   }

   @Override
   public void requestMovement(RequestMovement locationMovement) {
      super.requestMovement(locationMovement);
      setOpacityOfAllHexes();
      _view.setWatchMouseMove(true);
   }

   @Override
   public void setSelectableHexes(ArrayList<ArenaCoordinates> selectableHexes) {
      super.setSelectableHexes(selectableHexes);
      setOpacityOfAllHexes();
   }

   @Override
   public void endHexSelection() {
      super.endHexSelection();
      setOpacityOfAllHexes();
      _view.setWatchMouseMove(false);
   }

   private void setOpacityOfAllHexes() {
      synchronized (_locationToObjectMap) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationToObjectMap)) {
            for (ArenaLocation loc : _locationToObjectMap.keySet()) {
               TexturedObject texturedObj = _locationToObjectMap.get(loc);
               if (loc.getSelectable()) {
                  texturedObj._opacity = 1.0f;
               }
               else {
                  texturedObj._opacity = 0.5f;
               }
            }
         }
      }
   }

   @Override
   public boolean updateMap(CombatMap map, int selfID, byte selfTeam, ArrayList<ArenaLocation> availableLocs, int targetID) {
      synchronized (_locationToObjectMap) {
         _lock_locationToObjectMap.check();
         _locationToObjectMap.clear();
      }
      boolean result = super.updateMap(map, selfID, selfTeam, availableLocs, targetID);
      //      TexturedObject model = null;
      //      _view.addModel(model);
      _view.clearModels();
      for (short x = 0; x < map.getSizeX(); x++) {
         for (short y = 0; y < map.getSizeY(); y++) {
            ArenaLocation loc = map.getLocation(x, y);
            if (loc == null) {
               continue;
            }
            loc.registerAsWatcher(this, null);
            addHexToMap(loc);
         }
      }
      _view.setMapExtents(map.getSizeX(), map.getSizeY());

      if (_setZoomOnLoad) {
         setZoomToFit();
         _setZoomOnLoad = false;
      }
      return result;
   }

   private static Terrain getMapTerrainForLocation(ArenaLocation loc) {
      if (loc.getTerrain() == TerrainType.BUSHES) {
         return Terrain.BUSH;
      }
      if (loc.getTerrain() == TerrainType.DENSE_BUSH) {
         return Terrain.BUSH_DENSE;
      }
      if (loc.getTerrain() == TerrainType.DIRT) {
         return Terrain.DIRT;
      }
      if (loc.getTerrain() == TerrainType.FLOOR) {
         return Terrain.MARBLE;
      }
      if (loc.getTerrain() == TerrainType.GRASS) {
         return Terrain.GRASS;
      }
      if (loc.getTerrain() == TerrainType.GRAVEL) {
         return Terrain.GRAVEL;
      }
      if (loc.getTerrain() == TerrainType.ICE) {
         return Terrain.ICE;
      }
      if (loc.getTerrain() == TerrainType.MUD) {
         return Terrain.MUD;
      }
      if (loc.getTerrain() == TerrainType.SOLID_ROCK) {
         return Terrain.ROCK;
      }
      if (loc.getTerrain() == TerrainType.TREE_TRUNK) {
         return Terrain.TREE;
      }
      if (loc.getTerrain() == TerrainType.WATER) {
         return Terrain.WATER;
      }
      if (loc.getTerrain() == TerrainType.WATER_DEEP) {
         return Terrain.WATER_DEEP;
      }
      if (loc.getTerrain() == TerrainType.WATER_SHALLOW) {
         return Terrain.WATER_SHALLOW;
      }
      if (loc.getTerrain() == TerrainType.PAVERS) {
         return Terrain.PAVERS;
      }
      //if (loc.getTerrain() == Enums.Terrain.FUTURE_1)      return Terrain.RESERVED1;
      //if (loc.getTerrain() == Enums.Terrain.FUTURE_2)      return Terrain.RESERVED2;
      return Terrain.GRASS;
   }

   @Override
   //* return true if the map changed, and needs to be redrawn*/
   protected boolean centerOnSelf() {
      Character self = _combatMap.getCombatantByUniqueID(this._selfID);
      if (self != null) {
         centerViewOn(self);
      }
      return true;
   }

   public void centerViewOn(Character chr) {
      ArenaCoordinates headCoord = chr.getHeadCoordinates();
      Facing curFacing = chr.getFacing();
      ArenaCoordinates forwardLoc = ArenaCoordinates.getForwardMovement(headCoord, curFacing);
      int deltaX = forwardLoc._x - headCoord._x;
      int deltaY = forwardLoc._y - headCoord._y;
      int hexesBehind = -3;
      Tuple3 center = this._view.getHexLocation(headCoord._x + (deltaX * hexesBehind), headCoord._y + (deltaY * hexesBehind), 0/*z*/);
      this._view._cameraPosition = center.multiply(-1);
      int xRot = curFacing.value * -60;
      int yRot = -20;
      this._view.setCameraAngle(xRot, yRot);

      this._view.setHeightScaleByApproximateHeightInInches(90f/*desiredHeightInInches*/);
   }

   @Override
   protected Canvas getCanvas() {
      return _view.getCanvas();
   }

   @Override
   public boolean ObjectSelected(TexturedObject object, GLView view, boolean selectionOn) {
      return false;
   }

   @Override
   public void onMouseDown(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      clearMouseOverBodies();
      for (IMapListener listener : _listeners) {
         if ((object._relatedObject != null) && (object._relatedObject instanceof ArenaLocation)) {
            listener.onMouseDown((ArenaLocation) object._relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   @Override
   public void onMouseMove(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      ArrayList<Orientation> newMouseOverOrientations = new ArrayList<>();
      ArenaLocation loc = (ArenaLocation) object._relatedObject;
      Orientation destinationOrientation = null;
      if (loc != null) {
         if (_movementRequest != null) {
            destinationOrientation = _movementRequest.getBestFutureOrientation(loc, angleFromCenter, normalizedDistFromCenter);
            if (destinationOrientation != null) {
               newMouseOverOrientations = _movementRequest.getRouteToFutureOrientation(destinationOrientation);
            }
         }
      }
      setMouseOverOrientations(newMouseOverOrientations, destinationOrientation);
   }

   public boolean setMouseOverOrientations(List<Orientation> newMouseOverOrientations, Orientation destinationOrientation) {
      boolean orientationChanged;
      if ((newMouseOverOrientations != null) && (_mouseOverOrientations != null)) {
         if (newMouseOverOrientations.size() == _mouseOverOrientations.size()) {
            orientationChanged = !newMouseOverOrientations.containsAll(_mouseOverOrientations)
                                                                                                       || !_mouseOverOrientations.containsAll(newMouseOverOrientations);
         }
         else {
            orientationChanged = true;
         }
      }
      else {
         // one of these is null, so if the other is not null, it has changed
         orientationChanged = ((newMouseOverOrientations != null) || (_mouseOverOrientations != null));
      }
      if (!orientationChanged) {
         return false;
      }

      clearMouseOverBodies();
      _mouseOverOrientations = newMouseOverOrientations;
      if ((_mouseOverOrientations != null) && (_movementRequest != null)) {
         Character mover = _combatMap.getCombatantByUniqueID(_movementRequest.getActorID());
         if (mover != null) {
            for (Orientation newOrientation : _mouseOverOrientations) {
               float opacity = 0.35f;
               if (destinationOrientation.equals(newOrientation)) {
                  opacity = 0.65f;
               }
               ArenaLocation loc = this._combatMap.getLocation(newOrientation.getHeadCoordinates());
               synchronized (_locationToObjectMap) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationToObjectMap)) {
                     TexturedObject object = this._locationToObjectMap.get(loc);
                     for (ObjModel model : object._models) {
                        if (model._data instanceof ObjHex) {
                           HumanBody human = addCharacterToHex(_view, loc, (ObjHex) model._data, mover, newOrientation, true/*isGhost*/);
                           human.setOpacity(opacity);
                           _mouseOverBodies.put(human, loc);
                        }
                     }
                  }
               }
            }
         }
      }
      redraw();
      return true;
   }

   public void clearMouseOverBodies() {
      for (HumanBody human : _mouseOverBodies.keySet()) {
         ArenaLocation loc = _mouseOverBodies.get(human);
         TexturedObject object;
         synchronized (_locationToObjectMap) {
            _lock_locationToObjectMap.check();
            object = this._locationToObjectMap.get(loc);
         }
         for (ObjModel model : object._models) {
            if (model._data instanceof ObjHex) {
               ObjHex objHex = (ObjHex) model._data;
               synchronized (objHex._lock_humans) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(objHex._lock_humans)) {
                     for (HumanBody humanInHex : objHex._humans) {
                        if (humanInHex._opacity != 1f) {
                           objHex._humans.remove(humanInHex);
                           // there should only be one human in this hex that is the mover
                           break; // prevent Concurrent Modification Exception
                        }
                     }
                  }
               }
            }
         }
      }
      _mouseOverBodies.clear();
   }

   @Override
   public void onMouseDrag(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      for (IMapListener listener : _listeners) {
         if ((object._relatedObject != null) && (object._relatedObject instanceof ArenaLocation)) {
            listener.onMouseDrag((ArenaLocation) object._relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   @Override
   public void onMouseUp(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      for (IMapListener listener : _listeners) {
         if ((object._relatedObject != null) && (object._relatedObject instanceof ArenaLocation)) {
            listener.onMouseUp((ArenaLocation) object._relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   private final transient MonitoredObject  _monitoredObj  = new MonitoredObject("MapWidget3D");
   private final transient MonitoringObject _monitoringObj = new MonitoringObject("MapWidget3D");

   @Override
   public String getObjectIDString() {
      return _monitoredObj.getObjectIDString();
   }

   public Vector<IMonitoringObject> getSnapShotOfWatchers() {
      return _monitoredObj.getSnapShotOfWatchers();
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.registerMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return _monitoringObj.getSnapShotOfWatchedObjects();
   }

   public String getPositionName(Position position) {
      if (position.value == Position.CROUCHING.value) {
         return "crouch";
      }
      if (position.value == Position.KNEELING.value) {
         return "kneel";
      }
      if (position.value == Position.PRONE_BACK.value) {
         return "back";
      }
      if (position.value == Position.PRONE_FRONT.value) {
         return "front";
      }
      if (position.value == Position.SITTING.value) {
         return "sit";
      }
      if (position.value == Position.STANDING.value) {
         return "stand";
      }
      return "";
   }

   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                      Vector<IMonitoringObject> skipList, Diagnostics diag) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      Display display = Display.getDefault();
      if (!display.isDisposed()) {
         final IMonitorableObject originalWatchedObjectFinal = originalWatchedObject;
         final IMonitorableObject modifiedWatchedObjectFinal = modifiedWatchedObject;
         final Object changeNotificationFinal = changeNotification;
         final Vector<IMonitoringObject> skipListFinal = skipList;
         final Diagnostics diagFinal = diag;
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               monitoredObjectChangedOnUIThread(originalWatchedObjectFinal, modifiedWatchedObjectFinal,
                                                changeNotificationFinal, skipListFinal, diagFinal);
            }
         });
      }
   }
   public void monitoredObjectChangedOnUIThread(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                      Vector<IMonitoringObject> skipList, Diagnostics diag) {

      if (modifiedWatchedObject instanceof Character) {
         Character oldChr = (Character) originalWatchedObject;
         Character newChr = (Character) modifiedWatchedObject;
         HumanBody human = _characterIdToHumanBody.get(oldChr._uniqueID);
         RequestAction lastAction = newChr._lastAction;
         newChr._lastAction = null;
         Position oldPos = oldChr.getPosition();
         Position newPos = newChr.getPosition();
         String sequenceName = null;
         if (oldPos != newPos) {
            sequenceName = getPositionName(oldPos) + "_to_" + getPositionName(newPos);
         }
         else {
            if (lastAction != null) {
               if (lastAction.isAttack()) {
                  WeaponStyleAttack attackStyle = lastAction.getWeaponStyleAttack(newChr);
                  switch (attackStyle._handsRequired) {
                     case 0: sequenceName = "attack_kick"; break;
                     case 1: sequenceName = "attack_sword_shield"; break;
                     case 2: sequenceName = "attack_greataxe"; break;
                  }
                  if (sequenceName != null) {
                     sequenceName += "_1"; // attack actions
                     sequenceName += "_0"; // actions needed to re-ready weapon
                  }
               }
               else if (lastAction.isReadyWeapon()) {
                  updateHumanFromCharacter(newChr, newChr.getOrientation(), human);
                  sequenceName = "readyWeapon";
               }
            }
         }
         ArenaCoordinates oldHeadLoc = oldChr.getHeadCoordinates();
         ArenaCoordinates newHeadLoc = newChr.getHeadCoordinates();
         if (!oldHeadLoc.sameCoordinates(newHeadLoc)) {
            //sequenceName = "Walking";
         }
         AnimationSequence seq = SequenceLibrary.getAnimationSequenceByName(oldChr.getRace().getName(), sequenceName);
         if (seq != null) {
            human.addAnimationSequence(seq);
            synchronized (_lock_animationsPending) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_animationsPending)) {
                  _animationsPending.add(human);
               }
            }
            checkAnimation();
         }
         else {
            updateHumanFromCharacter(newChr, newChr.getOrientation(), human);
         }

         if (human != null) {
            if (!oldChr.getHeadCoordinates().sameCoordinates(newChr.getHeadCoordinates())) {
               ObjHex oldHex = _characterIdToObjHex.get(oldChr._uniqueID);
               synchronized (oldHex._lock_humans) {
                  oldHex._lock_humans.check();
                  oldHex._humans.remove(human);
               }
               synchronized (_locationToObjectMap) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationToObjectMap)) {
                     TexturedObject newHex = _locationToObjectMap.get(newChr.getHeadCoordinates());
                     if (newHex != null) {
                        for (ObjModel model : newHex._models) {
                           ObjHex objHex = (ObjHex) model._data;
                           synchronized (oldHex._lock_humans) {
                              objHex._lock_humans.check();
                              objHex._humans.add(human);
                           }
                        }
                     }
                  }
               }
            }
         }
         forceMapRecreation();
      }
      else if (modifiedWatchedObject instanceof ArenaLocation) {
         ArenaLocation loc = (ArenaLocation) modifiedWatchedObject;
         TexturedObject object;
         synchronized (_locationToObjectMap) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationToObjectMap)) {
               object = _locationToObjectMap.get(loc);//_view.getObjectByRelatedObject(modifiedWatchedObject);
               if (object != null) {
                  _locationToObjectMap.remove(loc);
               }
            }
         }
         if (object != null) {
            _view.removeObject(object);
            addHexToMap(loc);
         }
      }
   }

   private void forceMapRecreation() {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      Display display = Display.getDefault();
      if (!display.isDisposed()) {
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               updateMap(_combatMap, _selfID, _selfTeam, null, _targetID);
            }
         });
      }
   }

   List<Character>             _watchedCharacters      = new ArrayList<>();
   HashMap<Integer, HumanBody> _characterIdToHumanBody = new HashMap<>();
   HashMap<Integer, ObjHex>    _characterIdToObjHex    = new HashMap<>();

   private void addHexToMap(ArenaLocation loc) {
      boolean isVisible = (_selfID == -1) || loc.getVisible();
      boolean isKnown = (_selfID == -1) || loc.isKnownBy(_selfID);

      if (!isVisible && (loc == _selfLoc)) {
         isVisible = true;
      }
      Terrain terrain = getMapTerrainForLocation(loc);
      float opacity = isVisible ? 1.0f : 0.5f;
      if (!isKnown) {
         terrain = Terrain.GRAVEL;
      }
      TexturedObject obj = _view.addHex(loc._x, loc._y, 0/*z*/, terrain, opacity, (loc._x * _combatMap.getSizeY()) + loc._y, loc.getLabel());
      obj._relatedObject = loc;
      synchronized (_locationToObjectMap) {
         _lock_locationToObjectMap.check();
         _locationToObjectMap.put(loc, obj);
      }
      long walls = 0;
      ArrayList<Door> doors = new ArrayList<>();
      ArrayList<Object> things = new ArrayList<>();
      if (isKnown)
      {
         synchronized (loc) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this)) {
               walls = loc.getWalls();
               doors.addAll(loc.getDoors());
               things.addAll(loc.getThings());
            }
         }
      }

      for (ObjModel model : obj._models) {
         ObjHex objHex = (ObjHex) model._data;
         for (TerrainWall terrainWall : TerrainWall.values()) {
            if (terrainWall.contains(walls)) {
               int pointStart = (terrainWall.startPoint + 4) % 12;
               int pointEnd = (terrainWall.endPoint + 4) % 12;
               objHex.addWall(pointStart, pointEnd, 85f/*wallHeight*/, 4f/*thickness*/, false/*hasDoor*/, false/*doorIsOpen*/);
            }
         }

         for (Door door : doors) {
            int pointStart = (door._orientation.startPoint + 4) % 12;
            int pointEnd = (door._orientation.endPoint + 4) % 12;
            objHex.addWall(pointStart, pointEnd, 85f/*wallHeight*/, 4f/*thickness*/, true/*hasDoor*/, door.isOpen()/*doorIsOpen*/);
         }
         if (loc.getTerrain() == TerrainType.TREE_TRUNK) {
            objHex.addTree(18f, 100f);
         }
         else if (loc.getTerrain() == TerrainType.BUSHES) {
            objHex.addBush(30f, 15f, false);
         }
         else if (loc.getTerrain() == TerrainType.DENSE_BUSH) {
            objHex.addBush(30f, 30f, true);
         }
         else if (loc.getTerrain() == TerrainType.SOLID_ROCK) {
            objHex.addSolidRock(60f/*rockHeight*/);
         }

         if (isVisible) {
            for (Object thing : things) {
               if (thing instanceof Character) {
                  Character chr = (Character) thing;
                  if (!_watchedCharacters.contains(chr)) {
                     _watchedCharacters.add(chr);
                     chr.registerAsWatcher(this, null);
                  }
                  // If a multi-hex character is present, only deal with the head position:
                  boolean skipChar = false;
                  if (chr.getOrientation().getCoordinates().size() > 1) {
                     if (!chr.getHeadCoordinates().sameCoordinates(loc)) {
                        skipChar = true;
                     }
                  }
                  if (!skipChar) {
                     addCharacterToHex(_view, loc, objHex, chr, chr.getOrientation(), false/*isGhost*/);
                  }
                  // even for non-head locations, fill in the floor under every hex of the character
                  int floorColor = SWT.COLOR_GRAY;
                  if (chr._teamID == Enums.TEAM_ALPHA) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN;
                  }
                  else if (chr._teamID == Enums.TEAM_BETA) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_MAGENTA : SWT.COLOR_DARK_MAGENTA;
                  }
                  else if (chr._teamID == Enums.TEAM_INDEPENDENT) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_YELLOW : SWT.COLOR_DARK_YELLOW;
                  }
                  else {
                     floorColor = chr.stillFighting() ? SWT.COLOR_RED : SWT.COLOR_DARK_RED;
                  }
                  objHex.addFloor(_view, 1, floorColor, 0.5f/*opacity*/);
               }
               else {
                  if (thing instanceof Thing) {
                     if (thing instanceof Limb) {
                        String raceName = "human";
                        Texture humanTexture = _textureByRaceName.get(raceName);
                        HumanBody human = new HumanBody(humanTexture, _view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, raceName, true/*isMale*/);
                        TexturedObject limb = null;
                        if (thing instanceof Hand) {
                           limb = human.getModelHand(true/*rightSide*/);
                        }
                        else if (thing instanceof Leg) {
                           limb = human.getModelLeg(true/*rightSide*/);
                        }
                        else if (thing instanceof Head) {
                           limb = human.getModelHead();
                        }
                        else if (thing instanceof Tail) {
                           limb = human.getModelTail();
                        }
                        else if (thing instanceof Wing) {
                           limb = human.getModelWing(true/*rightSide*/);
                        }
                        else {
                           continue; // what is this?
                        }

                        if (limb == null) {
                           continue; // may be a tail or wing, which is not yet supported
                        }
                        objHex.addTexturedObject(limb);
                     }
                     else if (thing instanceof Weapon) {
                        ostrowski.graphics.objects3d.Thing.Weapon weapon = convertWeapon((Weapon) thing);
                        if (weapon != null) {
                           try {
                              float lengthFactor = 1.0f;
                              ostrowski.graphics.objects3d.Thing thing3d = new ostrowski.graphics.objects3d.Thing(weapon, null/*weaponPart*/, _view,
                                                                                                                  false/*_invertNormals*/, lengthFactor,
                                                                                                                  lengthFactor);
                              if (thing3d != null) {
                                 objHex.addTexturedObject(thing3d);
                              }
//                              else {
//                                 ObjModel weaponModel = ObjLoader.loadObj("res/weapons/" + weapon.name() + ".obj", _view, lengthFactor, lengthFactor);
//                                 if (weaponModel != null) {
//                                    objHex.addObject(weaponModel._data);
//                                 }
//                              }
                           } catch (IOException e) {
                              e.printStackTrace();
                           }
                        }
                     }
                     else if (thing instanceof Shield) {
                        ostrowski.graphics.objects3d.Thing.Shield shield = convertShield((Shield) thing);
                        try {
                           float lengthFactor = 1f;
                           ostrowski.graphics.objects3d.Thing thing3d = new ostrowski.graphics.objects3d.Thing(shield, _view, false/*_invertNormals*/,
                                                                                                               lengthFactor, lengthFactor);
//                           thing3d = new ostrowski.graphics.objects3d.Thing(ostrowski.graphics.objects3d.Thing.Dice.d12,
//                                                                            _view, lengthFactor * 60, "");
                           if (thing3d != null) {
                              objHex.addTexturedObject(thing3d);
                           }
//                           else {
//                              ObjModel weaponModel = ObjLoader.loadObj("res/weapons/" + shield.name() + ".obj", _view, lengthFactor, lengthFactor);
//                              if (weaponModel != null) {
//                                 objHex.addObject(weaponModel._data);
//                              }
//                           }
                        } catch (IOException e) {
                           e.printStackTrace();
                        }
                     }
                  }
               }
            }
         }
         else {
            objHex.addFloor(_view, 1, SWT.COLOR_GRAY, 0.33f/*opacity*/);
         }
      }
   }

   private static ostrowski.graphics.objects3d.Thing.Shield convertShield(Shield thing) {
      if (!thing.isReal()) {
         return null;
      }
      String shieldName = thing.getName();
      if ((shieldName == Shield.NAME_Buckler) || (shieldName == Shield.NAME_Small) || (shieldName == Shield.NAME_Medium) || (shieldName == Shield.NAME_Large)
          || (shieldName == Shield.NAME_Tower)) {
         shieldName = shieldName.replace(" ", "");
         shieldName = shieldName.replace("-", "");
         return ostrowski.graphics.objects3d.Thing.Shield.valueOf(shieldName);
      }
      return null;
   }

   private static ostrowski.graphics.objects3d.Thing.Weapon convertWeapon(Weapon thing) {
      if (!thing.isReal()) {
         return null;
      }
      String weaponName = thing.getName();
      if ((weaponName == Weapon.NAME_Axe) || (weaponName == Weapon.NAME_BastardSword) || (weaponName == Weapon.NAME_BastardSword_Fine)
          || (weaponName == Weapon.NAME_Broadsword) || (weaponName == Weapon.NAME_Club) || (weaponName == Weapon.NAME_Dagger)
          || (weaponName == Weapon.NAME_Flail) || (weaponName == Weapon.NAME_GreatAxe) || (weaponName == Weapon.NAME_Halberd)
          || (weaponName == Weapon.NAME_Javelin) || (weaponName == Weapon.NAME_Katana) || (weaponName == Weapon.NAME_Katana_Fine)
          || (weaponName == Weapon.NAME_Knife) || (weaponName == Weapon.NAME_Longsword) || (weaponName == Weapon.NAME_Longsword_Fine)
          || (weaponName == Weapon.NAME_Mace) || (weaponName == Weapon.NAME_Maul) || (weaponName == Weapon.NAME_MorningStar)
          || (weaponName == Weapon.NAME_Nunchucks) || (weaponName == Weapon.NAME_PickAxe) || (weaponName == Weapon.NAME_Quarterstaff)
          || (weaponName == Weapon.NAME_Rapier) || (weaponName == Weapon.NAME_Sabre) || (weaponName == Weapon.NAME_Shortsword)
          || (weaponName == Weapon.NAME_Spear) || (weaponName == Weapon.NAME_ThrowingAxe) || (weaponName == Weapon.NAME_TwoHandedSword)
          || (weaponName == Weapon.NAME_TwoHandedSword_Fine) || (weaponName == Weapon.NAME_WarHammer)) {
         if (weaponName.endsWith("_Fine")) {
            weaponName = weaponName.substring(0, weaponName.length() - 5);
         }
         if (weaponName.endsWith(", Fine")) {
            weaponName = weaponName.substring(0, weaponName.length() - 6);
         }
         weaponName = weaponName.replace(" ", "");
         weaponName = weaponName.replace("-", "");
         return ostrowski.graphics.objects3d.Thing.Weapon.valueOf(weaponName);
      }
      if (weaponName == Weapon.NAME_BowShortbow) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Shortbow_idle;
      }
      if (weaponName == Weapon.NAME_BowLongbow) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Longbow_idle;
      }
      if (weaponName == Weapon.NAME_BowComposite) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Bow_idle;
      }

      if ((weaponName == Weapon.NAME_BlowGun) || (weaponName == Weapon.NAME_Crossbow) || (weaponName == Weapon.NAME_CrossbowHeavy)
          || (weaponName == Weapon.NAME_CrossbowLight) || (weaponName == Weapon.NAME_Sling) || (weaponName == Weapon.NAME_StaffSling)
          || (weaponName == Weapon.NAME_ThreePartStaff) || (weaponName == Weapon.NAME_ThrowingStar)) {
         return null;
      }

      //      if ((weaponName == Weapon.NAME_Claws) ||
      //          (weaponName == Weapon.NAME_Fangs) ||
      //          (weaponName == Weapon.NAME_HeadButt) ||
      //          (weaponName == Weapon.NAME_HornGore) ||
      //          (weaponName == Weapon.NAME_KarateKick) ||
      //          (weaponName == Weapon.NAME_Punch) ||
      //          (weaponName == Weapon.NAME_TailStrike) ||
      //          (weaponName == Weapon.NAME_Tusks))
      //         return null;
      return null;
   }

   private HumanBody addCharacterToHex(GLView view, ArenaLocation loc, ObjHex objHex, Character chr,
                                       Orientation orientation, boolean isGhost) {
      HumanBody human = null;
      if (!isGhost) {
         human = _characterIdToHumanBody.get(chr._uniqueID);
      }

      if (human == null) {
         float lengthFactor;
         float aveSize = (chr.getBuildBase() + chr.getAdjustedStrength()) / 2;
         if (aveSize > 0) {
            lengthFactor = (float) Math.pow(1.01, aveSize);
         }
         else {
            lengthFactor = (float) Math.pow(1.04, aveSize);
         }
         float widthFactor = (float) Math.pow(1.05, chr.getAdjustedStrength());
         Race race = chr.getRace();
         lengthFactor *= race._lengthMod3d;
         widthFactor  *= race._widthMod3d;

         String raceName = chr.getRace().getName();
         if (chr.getRace().getLegCount() > 2) {
            raceName = "wolf";
         }

         Texture texture = _textureByRaceName.get(raceName.toLowerCase());
         // if that race doesn't have an entry, use 'human'
         if (texture == null) {
            texture = _textureByRaceName.get("human");
         }

         human = new HumanBody(texture, _view, lengthFactor, widthFactor, raceName, chr.getGender().equals(Gender.MALE));
         if (!isGhost) {
            _characterIdToHumanBody.put(chr._uniqueID, human);
         }
      }

      updateHumanFromCharacter(chr, orientation, human);
      // TODO: insert "walking" animation here:
      objHex.addHuman(human, orientation.getFacing().value);
      if (!isGhost) {
         ObjHex oldLocation = _characterIdToObjHex.get(chr._uniqueID);
         if (oldLocation != null) {
            synchronized (oldLocation._lock_humans) {
               oldLocation._lock_humans.check();
               oldLocation._humans.remove(human);
            }
         }

         _characterIdToObjHex.put(chr._uniqueID, objHex);
         // If a character takes up multiple hexes, only put the name on the head hex.
         if (orientation.getHeadCoordinates().sameCoordinates(loc)) {
            Message nameMessage = new Message();
            nameMessage._text = chr.getName();
            int colorBase = 255;
            if (!chr.getCondition().isConscious()) {
               colorBase = 64;
            }
            switch (chr._teamID) {
               case Enums.TEAM_ALPHA:
                  nameMessage._colorRGB = new RGB(00, colorBase, 00);
                  break;
               case Enums.TEAM_BETA:
                  nameMessage._colorRGB = new RGB(colorBase, 00, 00);
                  break;
               case Enums.TEAM_INDEPENDENT:
                  nameMessage._colorRGB = new RGB(00, 00, colorBase);
                  break;
            }
            nameMessage._visible = true;
            if (!human._messages.contains(nameMessage)) {
               human._messages.add(nameMessage);
            }
         }
      }
      return human;
   }

   private static void updateHumanFromCharacter(Character chr, Orientation orientation, HumanBody human) {
      human.setFacing(chr.getFacing().value);
      human._relatedObject = chr;
      Hand rightHand = (Hand) chr.getLimb(LimbType.HAND_RIGHT);
      Hand leftHand = (Hand) chr.getLimb(LimbType.HAND_LEFT);
      if (chr.getLimb(LimbType.LEG_RIGHT) == null) {
         human.removeLeg(true/*rightLeg*/);
      }
      if (chr.getLimb(LimbType.LEG_LEFT) == null) {
         human.removeLeg(false/*rightLeg*/);
      }

      boolean leftHandUsingBox = false;
      Thing rightHandHeldThing = null;
      String rightHandHeldThingName = null;
      if ((rightHand == null) || (rightHand.isSevered())) {
         human.removeArm(true/*rightArm*/);
      }
      else {
         rightHandHeldThing = rightHand.getHeldThing();
         if (rightHandHeldThing != null) {
            rightHandHeldThingName = rightHandHeldThing.getName();
            if ((rightHandHeldThing instanceof MissileWeapon) && rightHandHeldThing.getName().startsWith("Bow, ")) {
               if (rightHandHeldThingName.equals(Weapon.NAME_BowLongbow)) {
                  rightHandHeldThingName = "Longbow";
               }
               else if (rightHandHeldThingName.equals(Weapon.NAME_BowShortbow)) {
                  rightHandHeldThingName = "Shortbow";
               }
               else if (rightHandHeldThingName.equals(Weapon.NAME_BowComposite)) {
                  rightHandHeldThingName = "Bow";
               }

               if (!rightHandHeldThingName.equals(rightHandHeldThing.getName())) {
                  int state = rightHand.getPreparedState();
                  if (state < 2) {
                     human.setHeldThing(true/*rightHand*/, "Arrow");
                  }
                  else {
                     human.setHeldThing(true/*rightHand*/, (ostrowski.graphics.objects3d.Thing.Weapon)null);
                  }
                  switch (state) {
                     case 0: rightHandHeldThingName += "_ready"; break; // bow drawn
                     case 1: rightHandHeldThingName += "_idle"; break;  // arrow notched
                     case 2: rightHandHeldThingName += "_idle"; break;  // arrow ready
                     case 3: rightHandHeldThingName += "_idle"; break;  // bow unready
                  }
               }
               leftHandUsingBox = true;
               human.setHeldThing(false/*rightHand*/, rightHandHeldThingName);
            }
            else {
               human.setHeldThing(true/*rightHand*/, rightHandHeldThingName);
            }
         }
         else {
            human.setHeldThing(true/*rightHand*/, "");
         }
      }
      if ((leftHand == null) || (leftHand.isSevered())) {
         human.removeArm(false/*rightArm*/);
      }
      else {
         if (!leftHandUsingBox) {
            human.setHeldThing(false/*rightHand*/, leftHand.getHeldThingName());
         }
      }
      if (chr.getArmor().isReal()) {
         human.setArmor(chr.getArmor().getName());
      }
      else if (chr.getArmor().getName() == ostrowski.combat.common.things.Armor.NAME_NoArmor) {
         human.setArmor("No Armor");
      }
      else {
         human.setArmor(null);
      }

      if (orientation.getPosition() == Position.CROUCHING) {
         human.setPosition(HumanBody.Position.Crouching);
      }
      if (orientation.getPosition() == Position.KNEELING) {
         human.setPosition(HumanBody.Position.Kneeling);
      }
      if (orientation.getPosition() == Position.PRONE_BACK) {
         human.setPosition(HumanBody.Position.LayingOnBack);
      }
      if (orientation.getPosition() == Position.PRONE_FRONT) {
         human.setPosition(HumanBody.Position.LayingOnFront);
      }
      if (orientation.getPosition() == Position.SITTING) {
         human.setPosition(HumanBody.Position.Sitting);
      }
      if (orientation.getPosition() == Position.STANDING)
      {
         human.setPosition(HumanBody.Position.Standing);
         if (rightHandHeldThing != null) {
            Skill skill = chr.getBestSkill((Weapon) rightHandHeldThing);
            if (skill.getType() == SkillType.Bow) {
               switch (rightHand.getPreparedState()) {
                  case 3: human.setKeyFrame("ready bow_unready"); break; // bow unready
                  case 2: human.setKeyFrame("ready bow_unready"); break; // arrow ready
                  case 1: human.setKeyFrame("ready bow_notched"); break; // arrow notched
                  case 0: human.setKeyFrame("ready bow_raised"); break;  // bow drawn
               }
               //human.setKeyFrame("ready greataxe");
            }
            if (skill.getType() == SkillType.TwoHanded_AxeMace) {
               human.setKeyFrame("ready greataxe");
            }
            else if ((skill.getType() == SkillType.TwoHanded_Sword) || (skill.getType() == SkillType.Quarterstaff)) {
               human.setKeyFrame("ready twohanded");
            }
//            else if (skill.getType() == SkillType.Bow) {
//               switch (rightHand.getPreparedState()) {
//                  case 0: human.setKeyFrame("ready bow drawn"); break;// bow drawn
//                  case 1: human.setKeyFrame("ready bow notched"); break;// arrow notched
//                  case 2: human.setKeyFrame("ready bow unnotched"); break;// holding arrow
//                  case 3: human.setKeyFrame("ready bow noarrow"); break;// not yet holding arrow
//               }
//               human.setKeyFrame("ready bow");
//            }
//            else if (skill.getType() == SkillType.Fencing) {
//               human.setKeyFrame("ready fencing");
//            }
//            else if (skill.getType() == SkillType.Knife) {
//               human.setKeyFrame("ready knife");
//            }
//            else if ((skill.getType() == SkillType.Polearm) ||
//                     (skill.getType() == SkillType.Spear)) {
//               human.setKeyFrame("ready spear");
//            }
         }
         else {
            if (chr.getRace().getLegCount() == 4) {
               human.setKeyFrame("Quadraped");
            }
         }
      }
      human.setFacing(chr.getFacing().value);
      human._positionOffset = new Tuple3(0, 0, 0);
      human.setHeightBasedOnLowestLimbPoint();
      if (chr.getPosition() == Position.PRONE_BACK) {
         human._positionOffset = new Tuple3(human._positionOffset.getX(), human._positionOffset.getY(), human._positionOffset.getZ() + 40);
      }
   }

   public void addGLViewListener(IGLViewListener listener) {
      _view.addViewListener(listener);
   }

   @Override
   public void applyAnimations() {
      synchronized (_lock_animationsPending) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_animationsPending)) {
            synchronized (_lock_animatedObjects) {
               try (SemaphoreAutoTracker sat2 = new SemaphoreAutoTracker(_lock_animatedObjects)) {
                  _animatedObjects.addAll(_animationsPending);
               }
            }
            // make sure the animation thread is around to empty this list:
            checkAnimation();
            while (_animationsPending.size() > 0) {

               System.out.println("pending animations size = " + _animationsPending.size());

               try (SemaphoreAutoUntracker sau = new SemaphoreAutoUntracker(_lock_animationsPending)) {
                  _lock_animationsPending.wait(1000);
               } catch (InterruptedException e) {
               }
               if (_animationThread != null) {
                  // animation thread died. Maybe we are shutting down.
                  // stop this infinite loop.
                  break;
               }
            }
         }
      }
   }
}
