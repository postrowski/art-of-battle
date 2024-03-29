package ostrowski.combat.common;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.graphics.AnimationSequence;
import ostrowski.graphics.GLView;
import ostrowski.graphics.IGLViewListener;
import ostrowski.graphics.SequenceLibrary;
import ostrowski.graphics.model.*;
import ostrowski.graphics.model.ObjHex.Terrain;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class MapWidget3D extends MapWidget implements ISelectionWatcher, IMonitoringObject
{
   private final GLView                                 view;
   private final Display                                display;
   private final HashMap<ArenaLocation, TexturedObject> locationToObjectMap = new HashMap<>();
   private       boolean                                setZoomOnLoad;
   private final HashMap<HumanBody, ArenaLocation>      mouseOverBodies     = new HashMap<>();
   private final HashMap<String, Texture>               textureByRaceName   = new HashMap<>();

   final List<Character>             watchedCharacters      = new ArrayList<>();
   final HashMap<Integer, HumanBody> characterIdToHumanBody = new HashMap<>();
   final HashMap<Integer, ObjHex>    characterIdToObjHex    = new HashMap<>();

   final         Semaphore       lock_animationsPending   = new Semaphore("_lock_animationsPending", CombatSemaphore.CLASS_MAPWIDGET3D_animationsPending);
   final         Semaphore       lock_animatedObjects     = new Semaphore("_lock_animatedObjects", CombatSemaphore.CLASS_MAPWIDGET3_animatedObjects);
   final         Semaphore       lock_locationToObjectMap = new Semaphore("_lock_locationToObjectMap", CombatSemaphore.CLASS_MAPWIDGET3_locationToObjectMap);
   private final List<HumanBody> animationsPending        = new ArrayList<>();
   private final List<HumanBody> animatedObjects          = new ArrayList<>();
   Thread animationThread = null;

   private final transient MonitoredObject  monitoredObj  = new MonitoredObject("MapWidget3D");
   private final transient MonitoringObject monitoringObj = new MonitoringObject("MapWidget3D");


   public MapWidget3D(Composite parent) {
      view = new GLView(parent, true/*withControls*/);
      Canvas canvas = view.getCanvas();
      canvas.addKeyListener(this);
      canvas.getParent().addKeyListener(this);
      canvas.getParent().getParent().addKeyListener(this);

      view.addSelectionWatcher(this);
      display = parent.getDisplay();
      CombatServer.registerMapWidget3D(this);

      for (String race : SequenceLibrary.availableRaces) {
         try {
            Texture texture = view.getTextureLoader().getTexture("res/bodyparts/texture_" + race + "male.png");
            if (texture != null) {
               textureByRaceName.put(race, texture);
            }
         } catch (IOException e) {
         }
      }
   }

   @Override
   public void setBackgroundAlpha(int alpha) {
   }

   @Override
   public void setLayoutData(Object data) {
      view.getCanvas().setLayoutData(data);
   }

   @Override
   public void allowPan(boolean allow) {
      view.allowPan(allow);
   }

   @Override
   public void allowDrag(boolean allow) {
      view.allowDrag(allow);
   }

   public void checkAnimation() {
      if (animationThread != null) {
         return;
      }
      synchronized (lock_animatedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_animatedObjects)) {
            if (animatedObjects.size() == 0) {
               return;
            }
         }
      }

      animationThread = new Thread() {
         @Override
         public void run() {
            try {
               Thread.currentThread().setName("AnimationThread");
               while (true) {
                  List<HumanBody> itemsToRemove = new ArrayList<>();
                  synchronized (lock_animatedObjects) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_animatedObjects)) {
                        if (animatedObjects.size() == 0) {
                           return;
                        }

                        for (HumanBody human : animatedObjects) {
                           if (!human.advanceAnimation()) {
                              itemsToRemove.add(human);
                              Set<Entry<Integer, HumanBody>> set = characterIdToHumanBody.entrySet();
                              Character character = null;
                              for (Entry<Integer, HumanBody> pair : set) {
                                 if (pair.getValue() == human) {
                                    character = CombatServer._this.map.getCombatMap().getCombatantByUniqueID(pair.getKey());
                                    break;
                                 }
                              }
                              if (character != null) {
                                 updateHumanFromCharacter(character, character.getOrientation(), human);
                              }
                           }
                        }
                        animatedObjects.removeAll(itemsToRemove);
                     }
                  }
                  synchronized (lock_animationsPending) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_animationsPending)) {
                        animationsPending.removeAll(itemsToRemove);
                        System.out.println("clearing pending animations.");
                        lock_animationsPending.notifyAll();
                        System.out.println("all pending animations cleared.");
                     }
                  }
                  Thread.sleep(10);
                  Display.getDefault().asyncExec(MapWidget3D.this::redraw);
               }
            } catch (InterruptedException e) {
            } catch (IllegalMonitorStateException e) {
               System.out.println(e);
            } finally {
               animationThread = null;
            }
         }
      };

      animationThread.setName("animationThread");
      animationThread.start();
   }

   @Override
   public void recomputeVisibilityOfSelf(Character self) {
      super.recomputeVisibilityOfSelf(self);
      forceMapRecreation();
   }

   @Override
   public void redraw() {
      view.drawScene(display);
   }

   @Override
   public void setZoomToFit() {
      if (combatMap == null) {
         return;
      }
      short centerX = (short) (combatMap.getSizeX() / 2);
      short centerY = (short) (combatMap.getSizeY() / 2);
      if ((centerY % 2) != (centerX % 2)) {
         centerY++;
      }
      ArenaLocation centerLoc = combatMap.getLocation(centerX, centerY);
      // set the camera height equal to half the map length, and the camera position off the bottom edge of the map.
      TexturedObject centerObj;
      synchronized (locationToObjectMap) {
         lock_locationToObjectMap.check();
         centerObj = locationToObjectMap.get(centerLoc);
      }
      if (centerObj == null) {
         setZoomOnLoad = true;
         return;
      }
      Tuple3 centerPos = centerObj.models.get(0).data.getFace(0).getVertex(0);
      view.cameraPosition = new Tuple3(-centerPos.getX(), view.cameraPosition.getY(), centerPos.getZ() * -3);
      view.setHeightScaleByApproximateHeightInInches((centerPos.getX() / 6)/*desiredHeightInInches*/);
   }

   @Override
   public void requestMovement(RequestMovement locationMovement) {
      super.requestMovement(locationMovement);
      setOpacityOfAllHexes();
      view.setWatchMouseMove(true);
   }

   @Override
   public void requestLocation(RequestLocation locationMovement) {
      super.requestLocation(locationMovement);
      setOpacityOfAllHexes();
      view.setWatchMouseMove(true);
   }

   @Override
   public void setSelectableHexes(List<ArenaCoordinates> selectableHexes) {
      super.setSelectableHexes(selectableHexes);
      setOpacityOfAllHexes();
   }

   @Override
   public void endHexSelection() {
      super.endHexSelection();
      setOpacityOfAllHexes();
      view.setWatchMouseMove(false);
   }

   private void setOpacityOfAllHexes() {
      synchronized (locationToObjectMap) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_locationToObjectMap)) {
            for (ArenaLocation loc : locationToObjectMap.keySet()) {
               TexturedObject texturedObj = locationToObjectMap.get(loc);
               if (loc.getSelectable()) {
                  texturedObj.opacity = 1.0f;
               }
               else {
                  texturedObj.opacity = 0.5f;
               }
            }
         }
      }
   }

   @Override
   public boolean updateMap(CombatMap map, int selfID, byte selfTeam, List<ArenaLocation> availableLocs, int targetID) {
      synchronized (locationToObjectMap) {
         lock_locationToObjectMap.check();
         locationToObjectMap.clear();
      }
      boolean result = super.updateMap(map, selfID, selfTeam, availableLocs, targetID);
      //      TexturedObject model = null;
      //      view.addModel(model);
      view.clearModels();
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
      view.setMapExtents(map.getSizeX(), map.getSizeY());

      if (setZoomOnLoad) {
         setZoomToFit();
         setZoomOnLoad = false;
      }
      return result;
   }

   private static Terrain getMapTerrainForLocation(ArenaLocation loc) {
      switch (loc.getTerrain()) {
         case BUSHES:        return Terrain.BUSH;
         case DENSE_BUSH:    return Terrain.BUSH_DENSE;
         case DIRT:          return Terrain.DIRT;
         case FLOOR:         return Terrain.MARBLE;
         case GRASS:         return Terrain.GRASS;
         case GRAVEL:        return Terrain.GRAVEL;
         case ICE:           return Terrain.ICE;
         case MUD:           return Terrain.MUD;
         case SOLID_ROCK:    return Terrain.ROCK;
         case TREE_TRUNK:    return Terrain.TREE;
         case WATER:         return Terrain.WATER;
         case WATER_DEEP:    return Terrain.WATER_DEEP;
         case WATER_SHALLOW: return Terrain.WATER_SHALLOW;
         case PAVERS:        return Terrain.PAVERS;
//         case FUTURE_1:      return Terrain.RESERVED1;
//         case FUTURE_2:      return Terrain.RESERVED2;
      }
      DebugBreak.debugBreak();
      return Terrain.GRASS;
   }

   @Override
   //* return true if the map changed, and needs to be redrawn*/
   protected boolean centerOnSelf() {
      Character self = combatMap.getCombatantByUniqueID(this.selfID);
      if (self != null) {
         centerViewOn(self);
      }
      return true;
   }

   public void centerViewOn(Character chr) {
      ArenaCoordinates headCoord = chr.getHeadCoordinates();
      Facing curFacing = chr.getFacing();
      ArenaCoordinates forwardLoc = ArenaCoordinates.getForwardMovement(headCoord, curFacing);
      int deltaX = forwardLoc.x - headCoord.x;
      int deltaY = forwardLoc.y - headCoord.y;
      int hexesBehind = -3;
      Tuple3 center = this.view.getHexLocation(headCoord.x + (deltaX * hexesBehind), headCoord.y + (deltaY * hexesBehind), 0/*z*/);
      this.view.cameraPosition = center.multiply(-1);
      int xRot = curFacing.value * -60;
      int yRot = -20;
      this.view.setCameraAngle(xRot, yRot);

      this.view.setHeightScaleByApproximateHeightInInches(90f/*desiredHeightInInches*/);
   }

   @Override
   protected Canvas getCanvas() {
      return view.getCanvas();
   }

   @Override
   public boolean ObjectSelected(TexturedObject object, GLView view, boolean selectionOn) {
      return false;
   }

   @Override
   public void onMouseDown(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      clearMouseOverBodies();
      for (IMapListener listener : listeners) {
         if ((object.relatedObject instanceof ArenaLocation)) {
            listener.onMouseDown((ArenaLocation) object.relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   @Override
   public void onMouseMove(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      List<Orientation> newMouseOverOrientations = new ArrayList<>();
      ArenaLocation loc = (ArenaLocation) object.relatedObject;
      Orientation destinationOrientation = null;
      if (loc != null) {
         if (movementRequest != null) {
            destinationOrientation = movementRequest.getBestFutureOrientation(loc, angleFromCenter, normalizedDistFromCenter);
            if (destinationOrientation != null) {
               newMouseOverOrientations = movementRequest.getRouteToFutureOrientation(destinationOrientation);
            }
         }
      }
      setMouseOverOrientations(newMouseOverOrientations, destinationOrientation);
   }

   public boolean setMouseOverOrientations(List<Orientation> newMouseOverOrientations, Orientation destinationOrientation) {
      boolean orientationChanged;
      if ((newMouseOverOrientations != null) && (mouseOverOrientations != null)) {
         if (newMouseOverOrientations.size() == mouseOverOrientations.size()) {
            orientationChanged = !newMouseOverOrientations.containsAll(mouseOverOrientations)
                                                                                                       || !mouseOverOrientations.containsAll(newMouseOverOrientations);
         }
         else {
            orientationChanged = true;
         }
      }
      else {
         // one of these is null, so if the other is not null, it has changed
         orientationChanged = ((newMouseOverOrientations != null) || (mouseOverOrientations != null));
      }
      if (!orientationChanged) {
         return false;
      }

      clearMouseOverBodies();
      mouseOverOrientations = newMouseOverOrientations;
      if ((mouseOverOrientations != null) && (movementRequest != null)) {
         Character mover = combatMap.getCombatantByUniqueID(movementRequest.getActorID());
         if (mover != null) {
            for (Orientation newOrientation : mouseOverOrientations) {
               float opacity = 0.35f;
               if (destinationOrientation.equals(newOrientation)) {
                  opacity = 0.65f;
               }
               ArenaLocation loc = this.combatMap.getLocation(newOrientation.getHeadCoordinates());
               synchronized (locationToObjectMap) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_locationToObjectMap)) {
                     TexturedObject object = this.locationToObjectMap.get(loc);
                     for (ObjModel model : object.models) {
                        if (model.data instanceof ObjHex) {
                           HumanBody human = addCharacterToHex(view, loc, (ObjHex) model.data, mover, newOrientation, true/*isGhost*/);
                           human.setOpacity(opacity);
                           mouseOverBodies.put(human, loc);
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
      for (HumanBody human : mouseOverBodies.keySet()) {
         ArenaLocation loc = mouseOverBodies.get(human);
         TexturedObject object;
         synchronized (locationToObjectMap) {
            lock_locationToObjectMap.check();
            object = this.locationToObjectMap.get(loc);
         }
         for (ObjModel model : object.models) {
            if (model.data instanceof ObjHex) {
               ObjHex objHex = (ObjHex) model.data;
               synchronized (objHex.lock_humans) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(objHex.lock_humans)) {
                     for (HumanBody humanInHex : objHex.humans) {
                        if (humanInHex.opacity != 1f) {
                           objHex.humans.remove(humanInHex);
                           // there should only be one human in this hex that is the mover
                           break; // prevent Concurrent Modification Exception
                        }
                     }
                  }
               }
            }
         }
      }
      mouseOverBodies.clear();
   }

   @Override
   public void onMouseDrag(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      for (IMapListener listener : listeners) {
         if ((object.relatedObject instanceof ArenaLocation)) {
            listener.onMouseDrag((ArenaLocation) object.relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   @Override
   public void onMouseUp(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      for (IMapListener listener : listeners) {
         if ((object.relatedObject instanceof ArenaLocation)) {
            listener.onMouseUp((ArenaLocation) object.relatedObject, event, angleFromCenter, normalizedDistFromCenter);
         }
      }
   }

   @Override
   public String getObjectIDString() {
      return monitoredObj.getObjectIDString();
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return monitoringObj.registerMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return monitoringObj.unregisterMonitoredObject(watchedObject, diag);
   }

   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return monitoringObj.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return monitoringObj.getSnapShotOfWatchedObjects();
   }

   public String getPositionName(Position position) {
      switch (position) {
         case CROUCHING:   return "crouch";
         case KNEELING:    return "kneel";
         case PRONE_BACK:  return "back";
         case PRONE_FRONT: return "front";
         case SITTING:     return "sit";
         case STANDING:    return "stand";
      }
      DebugBreak.debugBreak();
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
         display.asyncExec(() -> monitoredObjectChangedOnUIThread(originalWatchedObjectFinal, modifiedWatchedObjectFinal,
                                                            changeNotificationFinal, skipListFinal, diagFinal));
      }
   }
   public void monitoredObjectChangedOnUIThread(IMonitorableObject originalWatchedObject,
                                                IMonitorableObject modifiedWatchedObject,
                                                Object changeNotification,
                                                Vector<IMonitoringObject> skipList, Diagnostics diag) {

      if (modifiedWatchedObject instanceof Character) {
         Character oldChr = (Character) originalWatchedObject;
         Character newChr = (Character) modifiedWatchedObject;
         HumanBody human = characterIdToHumanBody.get(oldChr.uniqueID);
         RequestAction lastAction = newChr.lastAction;
         newChr.lastAction = null;
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
                  switch (attackStyle.handsRequired) {
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
            synchronized (lock_animationsPending) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_animationsPending)) {
                  animationsPending.add(human);
               }
            }
            checkAnimation();
         }
         else {
            updateHumanFromCharacter(newChr, newChr.getOrientation(), human);
         }

         if (!oldChr.getHeadCoordinates().sameCoordinates(newChr.getHeadCoordinates())) {
            ObjHex oldHex = characterIdToObjHex.get(oldChr.uniqueID);
            synchronized (oldHex.lock_humans) {
               oldHex.lock_humans.check();
               oldHex.humans.remove(human);
            }
            synchronized (locationToObjectMap) {
               try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_locationToObjectMap)) {
                  TexturedObject newHex = locationToObjectMap.get(newChr.getHeadCoordinates());
                  if (newHex != null) {
                     for (ObjModel model : newHex.models) {
                        ObjHex objHex = (ObjHex) model.data;
                        synchronized (oldHex.lock_humans) {
                           objHex.lock_humans.check();
                           objHex.humans.add(human);
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
         synchronized (locationToObjectMap) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_locationToObjectMap)) {
               object = locationToObjectMap.get(loc);//_view.getObjectByRelatedObject(modifiedWatchedObject);
               if (object != null) {
                  locationToObjectMap.remove(loc);
               }
            }
         }
         if (object != null) {
            view.removeObject(object);
            addHexToMap(loc);
         }
      }
   }

   private void forceMapRecreation() {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      Display display = Display.getDefault();
      if (!display.isDisposed()) {
         display.asyncExec(() -> updateMap(combatMap, selfID, selfTeam, null, targetID));
      }
   }

   private void addHexToMap(ArenaLocation loc) {
      boolean isVisible = (selfID == -1) || loc.getVisible(selfID);
      boolean isKnown = (selfID == -1) || loc.isKnownBy(selfID);

      if (!isVisible && (loc == selfLoc)) {
         isVisible = true;
      }
      Terrain terrain = getMapTerrainForLocation(loc);
      float opacity = isVisible ? 1.0f : 0.5f;
      if (!isKnown) {
         terrain = Terrain.GRAVEL;
      }
      TexturedObject obj = view.addHex(loc.x, loc.y, 0/*z*/, terrain, opacity, (loc.x * combatMap.getSizeY()) + loc.y, loc.getLabel());
      obj.relatedObject = loc;
      synchronized (locationToObjectMap) {
         lock_locationToObjectMap.check();
         locationToObjectMap.put(loc, obj);
      }
      long walls = 0;
      List<Door> doors = new ArrayList<>();
      List<Object> things = new ArrayList<>();
      if (isKnown)
      {
         synchronized (loc) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc.lock_this)) {
               walls = loc.getWalls();
               doors.addAll(loc.getDoors());
               things.addAll(loc.getThings());
            }
         }
      }

      for (ObjModel model : obj.models) {
         ObjHex objHex = (ObjHex) model.data;
         for (TerrainWall terrainWall : TerrainWall.values()) {
            if (terrainWall.contains(walls)) {
               int pointStart = (terrainWall.startPoint + 4) % 12;
               int pointEnd = (terrainWall.endPoint + 4) % 12;
               objHex.addWall(pointStart, pointEnd, 85f/*wallHeight*/, 4f/*thickness*/, false/*hasDoor*/, false/*doorIsOpen*/);
            }
         }

         for (Door door : doors) {
            int pointStart = (door.orientation.startPoint + 4) % 12;
            int pointEnd = (door.orientation.endPoint + 4) % 12;
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
                  if (!watchedCharacters.contains(chr)) {
                     watchedCharacters.add(chr);
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
                     addCharacterToHex(view, loc, objHex, chr, chr.getOrientation(), false/*isGhost*/);
                  }
                  // even for non-head locations, fill in the floor under every hex of the character
                  int floorColor;
                  if (chr.teamID == Enums.TEAM_ALPHA) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_GREEN : SWT.COLOR_DARK_GREEN;
                  }
                  else if (chr.teamID == Enums.TEAM_BETA) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_MAGENTA : SWT.COLOR_DARK_MAGENTA;
                  }
                  else if (chr.teamID == Enums.TEAM_INDEPENDENT) {
                     floorColor = chr.stillFighting() ? SWT.COLOR_YELLOW : SWT.COLOR_DARK_YELLOW;
                  }
                  else {
                     floorColor = chr.stillFighting() ? SWT.COLOR_RED : SWT.COLOR_DARK_RED;
                  }
                  objHex.addFloor(view, 1, floorColor, 0.5f/*opacity*/);
               }
               else {
                  if (thing instanceof Thing) {
                     if (thing instanceof Limb) {
                        String raceName = "human";
                        Texture humanTexture = textureByRaceName.get(raceName);
                        HumanBody human = new HumanBody(humanTexture, view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, raceName, true/*isMale*/);
                        TexturedObject limb;
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
                              ostrowski.graphics.objects3d.Thing thing3d;
                              thing3d = new ostrowski.graphics.objects3d.Thing(weapon, null/*weaponPart*/,
                                                                               view, false/*_invertNormals*/,
                                                                               lengthFactor, lengthFactor);
                              objHex.addTexturedObject(thing3d);
                           } catch (IOException e) {
                              e.printStackTrace();
                           }
                        }
                     }
                     else if (thing instanceof Shield) {
                        ostrowski.graphics.objects3d.Thing.Shield shield = convertShield((Shield) thing);
                        try {
                           float lengthFactor = 1f;
                           ostrowski.graphics.objects3d.Thing thing3d;
                           thing3d = new ostrowski.graphics.objects3d.Thing(shield, view, false/*_invertNormals*/,
                                                                            lengthFactor, lengthFactor);
//                           thing3d = new ostrowski.graphics.objects3d.Thing(ostrowski.graphics.objects3d.Thing.Dice.d12,
//                                                                            view, lengthFactor * 60, "");
                           objHex.addTexturedObject(thing3d);
                        } catch (IOException e) {
                           e.printStackTrace();
                        }
                     }
                  }
               }
            }
         }
         else {
            objHex.addFloor(view, 1, SWT.COLOR_GRAY, 0.33f/*opacity*/);
         }
      }
   }

   private static ostrowski.graphics.objects3d.Thing.Shield convertShield(Shield thing) {
      if (!thing.isReal()) {
         return null;
      }
      String shieldName = thing.getName();
      if ((shieldName.equals(Shield.NAME_Buckler)) ||
          (shieldName.equals(Shield.NAME_Small)) ||
          (shieldName.equals(Shield.NAME_Medium)) ||
          (shieldName.equals(Shield.NAME_Large)) ||
          (shieldName.equals(Shield.NAME_Tower))) {
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
      if ((weaponName.equals(Weapon.NAME_Axe)) ||
          (weaponName.equals(Weapon.NAME_BastardSword)) ||
          (weaponName.equals(Weapon.NAME_BastardSword_Fine)) ||
          (weaponName.equals(Weapon.NAME_Broadsword)) ||
          (weaponName.equals(Weapon.NAME_Club)) ||
          (weaponName.equals(Weapon.NAME_Dagger)) ||
          (weaponName.equals(Weapon.NAME_Flail)) ||
          (weaponName.equals(Weapon.NAME_GreatAxe)) ||
          (weaponName.equals(Weapon.NAME_Halberd)) ||
          (weaponName.equals(Weapon.NAME_Javelin)) ||
          (weaponName.equals(Weapon.NAME_Katana)) ||
          (weaponName.equals(Weapon.NAME_Katana_Fine)) ||
          (weaponName.equals(Weapon.NAME_Knife)) ||
          (weaponName.equals(Weapon.NAME_Longsword)) ||
          (weaponName.equals(Weapon.NAME_Longsword_Fine)) ||
          (weaponName.equals(Weapon.NAME_Mace)) ||
          (weaponName.equals(Weapon.NAME_Maul)) ||
          (weaponName.equals(Weapon.NAME_MorningStar)) ||
          (weaponName.equals(Weapon.NAME_Nunchucks)) ||
          (weaponName.equals(Weapon.NAME_PickAxe)) ||
          (weaponName.equals(Weapon.NAME_Quarterstaff)) ||
          (weaponName.equals(Weapon.NAME_Rapier)) ||
          (weaponName.equals(Weapon.NAME_Sabre)) ||
          (weaponName.equals(Weapon.NAME_Shortsword)) ||
          (weaponName.equals(Weapon.NAME_Spear)) ||
          (weaponName.equals(Weapon.NAME_ThrowingAxe)) ||
          (weaponName.equals(Weapon.NAME_TwoHandedSword)) ||
          (weaponName.equals(Weapon.NAME_TwoHandedSword_Fine)) ||
          (weaponName.equals(Weapon.NAME_WarHammer))) {
         if (weaponName.endsWith(", Fine")) {
            weaponName = weaponName.substring(0, weaponName.length() - 6);
         }
         weaponName = weaponName.replace(" ", "");
         weaponName = weaponName.replace("-", "");
         return ostrowski.graphics.objects3d.Thing.Weapon.valueOf(weaponName);
      }
      if (weaponName.equals(Weapon.NAME_BowShortbow)) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Shortbow_idle;
      }
      if (weaponName.equals(Weapon.NAME_BowLongbow)) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Longbow_idle;
      }
      if (weaponName.equals(Weapon.NAME_BowComposite)) {
         return ostrowski.graphics.objects3d.Thing.Weapon.Bow_idle;
      }

//      if ((weaponName == Weapon.NAME_BlowGun)
//          || (weaponName == Weapon.NAME_Crossbow)
//          || (weaponName == Weapon.NAME_CrossbowHeavy)
//          || (weaponName == Weapon.NAME_CrossbowLight)
//          || (weaponName == Weapon.NAME_Sling)
//          || (weaponName == Weapon.NAME_StaffSling)
//          || (weaponName == Weapon.NAME_ThreePartStaff)
//          || (weaponName == Weapon.NAME_ThrowingStar)) {
//         return null;
//      }

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
         human = characterIdToHumanBody.get(chr.uniqueID);
      }

      if (human == null) {
         float lengthFactor;
         float aveSize = (chr.getBuildBase() + chr.getAdjustedStrength()) / 2f;
         if (aveSize > 0) {
            lengthFactor = (float) Math.pow(1.01, aveSize);
         }
         else {
            lengthFactor = (float) Math.pow(1.04, aveSize);
         }
         float widthFactor = (float) Math.pow(1.05, chr.getAdjustedStrength());
         Race race = chr.getRace();
         lengthFactor *= race.lengthMod3d;
         widthFactor  *= race.widthMod3d;

         String raceName = chr.getRace().getName();
         if (chr.getRace().getLegCount() > 2) {
            raceName = "wolf";
         }

         Texture texture = textureByRaceName.get(raceName.toLowerCase());
         // if that race doesn't have an entry, use 'human'
         if (texture == null) {
            texture = textureByRaceName.get("human");
         }

         human = new HumanBody(texture, this.view, lengthFactor, widthFactor, raceName, chr.getGender().equals(Gender.MALE));
         if (!isGhost) {
            characterIdToHumanBody.put(chr.uniqueID, human);
         }
      }

      updateHumanFromCharacter(chr, orientation, human);
      // TODO: insert "walking" animation here:
      objHex.addHuman(human, orientation.getFacing().value);
      if (!isGhost) {
         ObjHex oldLocation = characterIdToObjHex.get(chr.uniqueID);
         if (oldLocation != null) {
            synchronized (oldLocation.lock_humans) {
               oldLocation.lock_humans.check();
               oldLocation.humans.remove(human);
            }
         }

         characterIdToObjHex.put(chr.uniqueID, objHex);
         // If a character takes up multiple hexes, only put the name on the head hex.
         if (orientation.getHeadCoordinates().sameCoordinates(loc)) {
            Message nameMessage = new Message();
            nameMessage.text = chr.getName();
            int colorBase = 255;
            if (!chr.getCondition().isConscious()) {
               colorBase = 64;
            }
            switch (chr.teamID) {
               case Enums.TEAM_ALPHA:
                  nameMessage.colorRGB = new RGB(0, colorBase, 0);
                  break;
               case Enums.TEAM_BETA:
                  nameMessage.colorRGB = new RGB(colorBase, 0, 0);
                  break;
               case Enums.TEAM_INDEPENDENT:
                  nameMessage.colorRGB = new RGB(0, 0, colorBase);
                  break;
            }
            nameMessage.visible = true;
            if (!human.messages.contains(nameMessage)) {
               human.messages.add(nameMessage);
            }
         }
      }
      return human;
   }

   private static void updateHumanFromCharacter(Character chr, Orientation orientation, HumanBody human) {
      human.setFacing(chr.getFacing().value);
      human.relatedObject = chr;
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
      String rightHandHeldThingName;
      if ((rightHand == null) || (rightHand.isSevered())) {
         human.removeArm(true/*rightArm*/);
      }
      else {
         rightHandHeldThing = rightHand.getHeldThing();
         if (rightHandHeldThing != null) {
            rightHandHeldThingName = rightHandHeldThing.getName();
            if ((rightHandHeldThing instanceof MissileWeapon) && rightHandHeldThing.getName().startsWith("Bow, ")) {
               switch (rightHandHeldThingName) {
                  case Weapon.NAME_BowLongbow:
                     rightHandHeldThingName = "Longbow";
                     break;
                  case Weapon.NAME_BowShortbow:
                     rightHandHeldThingName = "Shortbow";
                     break;
                  case Weapon.NAME_BowComposite:
                     rightHandHeldThingName = "Bow";
                     break;
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
                     case 1: // arrow notched
                     case 2: // arrow ready
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
      else if (chr.getArmor().getName().equals(Armor.NAME_NoArmor)) {
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
      human.positionOffset = new Tuple3(0, 0, 0);
      human.setHeightBasedOnLowestLimbPoint();
      if (chr.getPosition() == Position.PRONE_BACK) {
         human.positionOffset = new Tuple3(human.positionOffset.getX(), human.positionOffset.getY(), human.positionOffset.getZ() + 40);
      }
   }

   public void addGLViewListener(IGLViewListener listener) {
      view.addViewListener(listener);
   }

   @Override
   public void applyAnimations() {
      synchronized (lock_animationsPending) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_animationsPending)) {
            synchronized (lock_animatedObjects) {
               try (SemaphoreAutoTracker sat2 = new SemaphoreAutoTracker(lock_animatedObjects)) {
                  animatedObjects.addAll(animationsPending);
               }
            }
            // make sure the animation thread is around to empty this list:
            checkAnimation();
            while (animationsPending.size() > 0) {

               System.out.println("pending animations size = " + animationsPending.size());

               try (SemaphoreAutoUntracker sau = new SemaphoreAutoUntracker(lock_animationsPending)) {
                  lock_animationsPending.wait(1000);
               } catch (InterruptedException e) {
               }
               if (animationThread != null) {
                  // animation thread died. Maybe we are shutting down.
                  // stop this infinite loop.
                  break;
               }
            }
         }
      }
   }
}
