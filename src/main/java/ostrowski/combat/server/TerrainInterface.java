package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.IMapWidget;
import ostrowski.combat.common.IMapWidget.MapMode;
import ostrowski.combat.common.MapWidget2D;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.things.Door;
import ostrowski.combat.common.things.DoorState;
import ostrowski.combat.common.things.Thing;
import ostrowski.graphics.IGLViewListener;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class TerrainInterface extends Helper implements SelectionListener, ModifyListener, Listener, Enums, IGLViewListener
{
   private short         currentTerrain   = -1;
   private long          currentWall      = -1;
   private Button[]      terrainButtons;
   private Button        clearButton;
   private Button        fillButton;
   private boolean       fillActive       = false;
   private Button        lineButton;
   private boolean       lineActive       = false;
   private Button        wallLineButton;
   private boolean       wallLineActive   = false;
   private Button[]      wallButtons;
   private Button        isHalfHeight;
   private Button        isDoor;
   private Label         isDoorLabel;
   private Button        isOpen;
   private Label         isOpenLabel;
   private Button        isLockable;
   private Label         isLockableLabel;
   private Button        isLocked;
   private Label         isLockedLabel;
   private Text          doorKeyDesc;
   private Label         doorKeyDescLabel;
   private ArenaLocation lineStart;
   private ArenaLocation lineEnd;
   private double        lineStartAngleFromCenter;
   private double        lineEndAngleFromCenter;
   private Text          itemDesc;
   private Button        setItems;
   private boolean       setItemsActive   = false;
   private Button        clearItems;
   private boolean       clearItemsActive = false;
   double rotation = 0;
   private IMapWidget mapInterface;

   public boolean allowPan() {
      return ((currentTerrain == -1) && (currentWall == -1));
   }
   public void disableCurrentEdits() {
      if (currentTerrain != -1) {
         terrainButtons[currentTerrain].redraw();
         currentTerrain = -1;
      }
      if (currentWall != -1) {
         currentWall = -1;
         redrawWalls();
      }
   }
   public void buildBlock(Composite parentComposite) {
      Composite buttonsBlock  = new Composite(parentComposite, SWT.TRAIL);
      GridLayout grid = new GridLayout(4, false);
      buttonsBlock.setLayout(grid);
      GridData data;
      {
         Composite terrainButtonsBlock = createGroup(buttonsBlock, "Terrain", 3/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
         terrainButtons = new Button[TerrainType.values().length];
         for (TerrainType terrain : TerrainType.values()) {
            terrainButtons[terrain.value] = new Button(terrainButtonsBlock, SWT.PUSH);
            terrainButtons[terrain.value].setText(terrain.name + " ");
            terrainButtons[terrain.value].setAlignment(SWT.RIGHT);
            terrainButtons[terrain.value].addListener(SWT.Paint, this);
            data = new GridData();
            data.minimumWidth = 120;
            data.grabExcessHorizontalSpace = true;
            terrainButtons[terrain.value].setLayoutData(data);
            terrainButtons[terrain.value].addSelectionListener(this);
         }
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         clearButton = makeButton(terrainButtonsBlock, "Clear");
         fillButton = makeButton(terrainButtonsBlock, "Paint / Fill");
         lineButton = makeButton(terrainButtonsBlock, "Draw Line");
      }
      {
         Composite wallsDoorsBlock = createGroup(buttonsBlock, "Walls / Doors", 2/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
         {
            Composite wallButtonsBlock = new Composite(wallsDoorsBlock, SWT.TRAIL);
            grid = new GridLayout(3, false);
            grid.verticalSpacing = 1;
            grid.horizontalSpacing = 1;
            wallButtonsBlock.setLayout(grid);
            wallButtons = new Button[19];
            for (int wall = 0; wall < wallButtons.length ; wall++) {
               wallButtons[wall] = new Button(wallButtonsBlock, SWT.PUSH);
               wallButtons[wall].addListener(SWT.Paint, this);
               data = new GridData();
               data.minimumWidth = 35;
               if (wall == 0) {
                  data.horizontalSpan = 3;
               }
               data.horizontalAlignment = SWT.CENTER;
               data.grabExcessHorizontalSpace = true;
               wallButtons[wall].setLayoutData(data);
               wallButtons[wall].addSelectionListener(this);
            }
         }
//         new Label(wallsDoorsBlock, 0);
//         new Label(wallsDoorsBlock, 0);
         {
            Composite wallBlock = createGroup(wallsDoorsBlock, "Wall info", 3/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
            isHalfHeight = new Button(wallBlock, SWT.CHECK);
            createLabel(wallBlock, "this wall is half height.", SWT.LEFT, 2/*hSpan*/, null);
            isHalfHeight.addSelectionListener(this);
            isDoor = new Button(wallBlock, SWT.CHECK);
            isDoorLabel = createLabel(wallBlock, "this wall section has a door.", SWT.LEFT, 2/*hSpan*/, null);
            isOpen = new Button(wallBlock, SWT.CHECK);
            isOpenLabel = createLabel(wallBlock, "the door is open.", SWT.LEFT, 2/*hSpan*/, null);
            isLockable = new Button(wallBlock, SWT.CHECK);
            isLockableLabel = createLabel(wallBlock, "the door can be locked.", SWT.LEFT, 2/*hSpan*/, null);
            isLocked = new Button(wallBlock, SWT.CHECK);
            isLockedLabel = createLabel(wallBlock, "the door is locked.", SWT.LEFT, 2/*hSpan*/, null);
            doorKeyDescLabel = createLabel(wallBlock, "Key description:", SWT.RIGHT, 2/*hSpan*/, null);
            doorKeyDesc = createText(wallBlock, "", true/*editable*/, 1/*hSpan*/);
            isDoor.addSelectionListener(this);
            isOpen.addSelectionListener(this);
            isLockable.addSelectionListener(this);
            isLocked.addSelectionListener(this);
         }
         wallLineButton = makeButton(wallsDoorsBlock, "Draw wall");
      }
      {
         Composite itemsBlock = createGroup(buttonsBlock, "Items on floor", 3/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
         grid = new GridLayout(1, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         itemsBlock.setLayout(grid);
         createLabel(itemsBlock, "Item description:", SWT.LEFT, 1/*hSpan*/, null);
         itemDesc = createText(itemsBlock, "", true/*editable*/, 1/*hSpan*/);
         setItems = makeButton(itemsBlock, "Set Location");
         clearItems = makeButton(itemsBlock, "Clear Location(s)");
      }
   }
   private Button makeButton(Composite parent, String text) {
      Button newButton = new Button(parent, SWT.PUSH);
      newButton.setAlignment(SWT.CENTER);
      newButton.setSize(100, 20);
      newButton.setText(text);
      newButton.addListener(SWT.Paint, this);
      GridData data = new GridData();
      data.minimumWidth  = 120;
      data.horizontalAlignment = SWT.CENTER;
      data.grabExcessHorizontalSpace = true;
      newButton.setLayoutData(data);
      newButton.addSelectionListener(this);
      return newButton;
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      boolean halfHeightWall = isHalfHeight.getSelection();
      boolean isDoor = !halfHeightWall && this.isDoor.getSelection();
      boolean isLockableDoor = isDoor && isLockable.getSelection();
      boolean isOpenDoor = isDoor && isOpen.getSelection();

      if (halfHeightWall) {
         this.isDoor.setSelection(false);
      }
      if (!isDoor) {
         isOpen.setSelection(false);
         isLockable.setSelection(false);
         isLocked.setSelection(false);
      }
      this.isDoor.setEnabled(!halfHeightWall);
      isDoorLabel.setEnabled(!halfHeightWall);
      isOpen.setEnabled(isDoor);
      isOpenLabel.setEnabled(isDoor);
      isLockable.setEnabled(isDoor);
      isLockableLabel.setEnabled(isDoor);
      isLocked.setEnabled(isLockableDoor && !isOpenDoor);
      isLockedLabel.setEnabled(isLockableDoor && !isOpenDoor);
      doorKeyDesc.setEnabled(isLockableDoor);
      doorKeyDescLabel.setEnabled(isLockableDoor);

      if ((e.widget == this.isDoor) || (e.widget == isHalfHeight) || (e.widget == isOpen)) {
         // These affect the way the door looks, must redraw the hex:
         redrawWalls();
      }
      else if ((e.widget == isLockable) || (e.widget == isLocked)) {
         // These don't affect the appearance, so there is nothing to do
      }
      else if (e.widget == clearButton) {
         fillActive = false;
         wallLineActive = false;
         lineActive = false;
         setItemsActive = false;
         clearItemsActive = false;
         if (currentTerrain != -1) {
            terrainButtons[currentTerrain].redraw();
         }
         currentTerrain = -1;
         fillButton.redraw();
         lineButton.redraw();
         wallLineButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == fillButton) {
         fillActive = !fillActive;
         wallLineActive = false;
         lineActive = false;
         setItemsActive = false;
         clearItemsActive = false;
         fillButton.redraw();
         lineButton.redraw();
         wallLineButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == lineButton) {
         lineActive = !lineActive;
         wallLineActive = false;
         fillActive = false;
         setItemsActive = false;
         clearItemsActive = false;
         lineButton.redraw();
         wallLineButton.redraw();
         fillButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == wallLineButton) {
         wallLineActive = !wallLineActive;
         lineActive = false;
         fillActive = false;
         setItemsActive = false;
         clearItemsActive = false;
         lineButton.redraw();
         wallLineButton.redraw();
         fillButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         if (currentTerrain != -1) {
            terrainButtons[currentTerrain].redraw();
         }
         currentTerrain = -1;
         currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == setItems) {
         setItemsActive = !setItemsActive;
         clearItemsActive = false;
         fillActive = false;
         wallLineActive = false;
         lineActive = false;
         wallLineButton.redraw();
         lineButton.redraw();
         fillButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         if (currentTerrain != -1) {
            terrainButtons[currentTerrain].redraw();
         }
         currentTerrain = -1;
         currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == clearItems) {
         clearItemsActive = !clearItemsActive;
         setItemsActive = false;
         fillActive = false;
         wallLineActive = false;
         lineActive = false;
         lineButton.redraw();
         wallLineButton.redraw();
         fillButton.redraw();
         setItems.redraw();
         clearItems.redraw();
         if (currentTerrain != -1) {
            terrainButtons[currentTerrain].redraw();
         }
         currentTerrain = -1;
         currentWall = -1;
         redrawWalls();
      }
      else {
         for (short i = 0; i < terrainButtons.length ; i++) {
            if (e.widget == terrainButtons[i]) {
               terrainButtons[i].redraw();
               if (currentTerrain != -1) {
                  terrainButtons[currentTerrain].redraw();
               }
               currentTerrain = i;
               fillButton.redraw();
               lineButton.redraw();
               redrawWalls();
               wallLineActive = false;
               wallLineButton.redraw();
               setItemsActive = false;
               setItems.redraw();
               clearItemsActive = false;
               clearItems.redraw();
               currentWall = -1;
               break;
            }
         }
         for (int i = 0; i < wallButtons.length ; i++) {
            if (e.widget == wallButtons[i]) {
               if (currentTerrain != -1) {
                  terrainButtons[currentTerrain].redraw();
               }
               currentTerrain = -1;
               fillButton.redraw();
               lineButton.redraw();
               if (i==0) {
                  currentWall = 0;
               }
               else {
                  currentWall = TerrainWall.VERT_LEFT.bitMask << (i - 1);
               }
               redrawWalls();
               setItemsActive = false;
               setItems.redraw();
               clearItemsActive = false;
               clearItems.redraw();
               wallLineActive = false;
               wallLineButton.redraw();
               break;
            }
         }
      }
      if (mapInterface != null) {
         if (fillActive) {
            mapInterface.setMode(MapMode.FILL);
         }
         else if (lineActive) {
            if (currentWall > 0) {
               mapInterface.setMode(MapMode.LINE);
            }
            else {
               mapInterface.setMode(MapMode.NONE);
            }
         }
         else if (currentTerrain != -1) {
            mapInterface.setMode(MapMode.PAINT_TERRAIN);
         }
         else if (currentWall != -1) {
            mapInterface.setMode(MapMode.PAINT_WALL);
         }
         else {
            mapInterface.setMode(MapMode.DRAG);
         }
      }
      if (mapInterface != null) {
         mapInterface.allowPan(allowPan());
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
   }

   @Override
   public void handleEvent(Event event) {
      if (event.type == SWT.Paint) {
         if (event.widget instanceof Button) {
            Button eventButton = ((Button)(event.widget));
            Rectangle bounds = eventButton.getBounds();
            Color oldBg = event.gc.getBackground();
            Color oldFg = event.gc.getForeground();
            Color bgColor = null;
            Color fgColor = null;
            try {
               int sizePerHex = 20;
               int offsetX = 2;
               int offsetY = 2;
               for (int buttonIndex = 0; buttonIndex < wallButtons.length ; buttonIndex++) {
                  if (eventButton == wallButtons[buttonIndex]) {
                     long wallOrientation = TerrainWall.VERT_LEFT.bitMask << (buttonIndex-1);
                     if (((currentWall == 0) && (buttonIndex == 0)) ||
                         (currentWall == wallOrientation)) {
                        event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
                        event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_GRAY));
                        event.gc.fillRectangle(2, 2, event.width-4, event.height-4);
                     }
                     // center the hex inside the button:
                     offsetX = (eventButton.getSize().x - sizePerHex)/2;
                     ArenaLocation loc = new ArenaLocation((short)0,(short)0);
                     loc.setTerrain(TerrainType.FLOOR);
                     MapWidget2D.drawHex(loc, event.gc, event.display, sizePerHex, offsetX, offsetY, rotation);
                     if (buttonIndex != 0) {
                        if (isDoor.getSelection() || isHalfHeight.getSelection()) {
                           DoorState state = DoorState.CLOSED;
                           if (isHalfHeight.getSelection()) {
                              state = DoorState.HALF_HEIGHT_WALL;
                           }
                           if (isOpen.getSelection()) {
                              state = DoorState.OPEN;
                           }
                           else if (isLocked.getSelection()) {
                              state = DoorState.LOCKED;
                           }

                           Door door = new Door(state, doorKeyDesc.getText(), TerrainWall.getByBitMask(wallOrientation));
                           loc.addDoor(door);
                        }
                        else {
                           loc.addWall(wallOrientation);
                        }
                        MapWidget2D.drawWall(loc, event.gc, event.display, sizePerHex, offsetX, offsetY, (short)0, (short)0, -1/*selfID*/, false/*hideViewFromLocalPlayers*/, rotation);
                     }
                     return;
                  }
               }
               if ((eventButton == fillButton) || (eventButton == lineButton) ||
                   (eventButton == setItems) || (eventButton == clearItems) || (eventButton == wallLineButton)) {
                  boolean fill = false;
                  if (currentTerrain != -1) {
                     if (eventButton == fillButton) {
                        fill = fillActive;
                     }
                     if (eventButton == lineButton) {
                        fill = lineActive;
                     }
                  }
                  if (eventButton == setItems) {
                     fill = setItemsActive;
                  }
                  if (eventButton == clearItems) {
                     fill = clearItemsActive;
                  }
                  if (eventButton == wallLineButton) {
                     fill = wallLineActive;
                  }

                  if (fill) {
                     RGB fillColor;
                     if ((eventButton == fillButton) || (eventButton == lineButton)) {
                        fillColor = TerrainType.getByValue(currentTerrain).color;
                     }
                     else {
                        fillColor = new RGB(64, 64, 64);
                     }
                     RGB textColor = getTextColorOnBackground(fillColor);
                     bgColor = new Color(event.display, fillColor);
                     fgColor = new Color(event.display, textColor);
                     event.gc.setBackground(bgColor);
                     event.gc.setForeground(fgColor);
                     event.gc.fillRectangle(2, 2, event.width-4, event.height-4);

                     String text = eventButton.getText();
                     Point extent = event.gc.stringExtent(text);
                     // Center the text in the button horizontally and vertically
                     int xOffset = (bounds.width-extent.x)/2;
                     int yOffset = (bounds.height-extent.y)/2;
                     event.gc.drawText(text, xOffset, yOffset);
                  }
                  return;
               }
               for (short buttonIndex = 0; buttonIndex < terrainButtons.length ; buttonIndex++) {
                  if (eventButton == terrainButtons[buttonIndex]) {
                     boolean fill = (currentTerrain == buttonIndex);
                     if (fill) {
                        event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_DARK_GRAY));
                        event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_WHITE));
                        event.gc.fillRectangle(2, 2, event.width-4, event.height-4);

                        String text = eventButton.getText();
                        Point extent = event.gc.stringExtent(text);
                        // Right-justify the text
                        int xOffset = (bounds.width-extent.x) - 3;
                        // Center the text in the button vertically
                        int yOffset = (bounds.height-extent.y)/2;
                        event.gc.drawText(text, xOffset, yOffset);
                     }
                     ArenaLocation loc = new ArenaLocation((short)0,(short)0);
                     loc.setTerrain(TerrainType.getByValue(buttonIndex));
                     MapWidget2D.drawHex(loc, event.gc, event.display, sizePerHex, offsetX, offsetY);
                     return;
                  }
               }
            }
            finally {
               event.gc.setBackground(oldBg);
               event.gc.setForeground(oldFg);

               if (bgColor != null) {
                  bgColor.dispose();
               }
               if (fgColor != null) {
                  fgColor.dispose();
               }
            }
         }
      }
   }
   private static RGB getTextColorOnBackground(RGB backgroundColor) {
      int highColorCount = 0;
      if (backgroundColor.red > 80)   { highColorCount++;  if (backgroundColor.red > 160) {
         highColorCount++;
      }}
      if (backgroundColor.green > 80) { highColorCount++;  if (backgroundColor.green > 160) {
         highColorCount++;
      }}
      if (backgroundColor.blue > 80)  { highColorCount++;  if (backgroundColor.blue > 160) {
         highColorCount++;
      }}
      if (highColorCount > 3) {
         return new RGB(0, 0, 0);
      }
      return new RGB(255, 255, 255);
   }

   private void redrawWalls()
   {
      // redraw all the walls
      for (Button element : wallButtons) {
         element.redraw();
      }
   }

   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter, IMapWidget mapWidget) {
      if ((lineActive && (currentTerrain > 0)) || (wallLineActive && (currentWall > 0))) {
         if (lineStart == null) {
            lineStart = loc;
            lineStartAngleFromCenter = angleFromCenter;
         }
         if (lineEnd != loc) {
            if (loc != null) {
               lineEnd = loc;
               lineEndAngleFromCenter = angleFromCenter;

               if (lineActive && (currentTerrain > 0)) {
                  mapWidget.setLine(lineStart, lineEnd, TerrainType.getByValue(currentTerrain).color);
               }
               else { // this is true: (wallLineActive && (currentWall > 0))
                  mapWidget.setWallLine(lineStart, lineStartAngleFromCenter, lineEnd, lineEndAngleFromCenter,
                                        TerrainType.getByValue(currentTerrain).color);
               }

               if (mapWidget instanceof MapWidget2D) {
                  MapWidget2D mapWidget2d = (MapWidget2D) mapWidget;
                  int[] bounds = mapWidget2d.getHexDimensions(lineStart.x, lineStart.y, true/*cacheResults*/);

                  Point minLoc = new Point(bounds[MapWidget2D.X_SMALLEST], bounds[MapWidget2D.Y_SMALLEST]);
                  Point maxLoc = new Point(bounds[MapWidget2D.X_LARGEST],  bounds[MapWidget2D.Y_LARGEST]);
                  if (lineEnd != null) {
                     bounds = mapWidget2d.getHexDimensions(lineEnd.x, lineEnd.y, true/*cacheResults*/);
                     minLoc = new Point(Math.min(minLoc.x, bounds[MapWidget2D.X_SMALLEST]), Math.min(minLoc.y, bounds[MapWidget2D.Y_SMALLEST]));
                     maxLoc = new Point(Math.max(maxLoc.x, bounds[MapWidget2D.X_LARGEST]),  Math.max(maxLoc.y, bounds[MapWidget2D.Y_LARGEST]));
                  }
                  if (lineStart != null) {
                     bounds = mapWidget2d.getHexDimensions(lineStart.x, lineStart.y, true/*cacheResults*/);
                     minLoc = new Point(Math.min(minLoc.x, bounds[MapWidget2D.X_SMALLEST]), Math.min(minLoc.y, bounds[MapWidget2D.Y_SMALLEST]));
                     maxLoc = new Point(Math.max(maxLoc.x, bounds[MapWidget2D.X_LARGEST]),  Math.max(maxLoc.y, bounds[MapWidget2D.Y_LARGEST]));
                  }
                  minLoc.y -= bounds[MapWidget2D.Y_LARGEST];
                  maxLoc.y += bounds[MapWidget2D.Y_LARGEST];
                  mapWidget2d.redraw(minLoc.x, minLoc.y, (maxLoc.x - minLoc.x), (maxLoc.y - minLoc.y), false);
                  Rules.diag("{"+minLoc.x+","+minLoc.y+"}-{"+maxLoc.x+","+maxLoc.y+"}");
               }
            }
         }
      }
      else {
         onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter, mapWidget, null/*locationsToRedraw*/);
      }
   }
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter,
                           IMapWidget mapWidget, List<ArenaCoordinates> locationsToRedraw) {
      if (currentTerrain > -1) {
         if (lineActive || wallLineActive) {
            lineStart = loc;
            lineStartAngleFromCenter = angleFromCenter;
            lineEnd = loc;
            lineEndAngleFromCenter = angleFromCenter;
            mapWidget.setLine(null, null, null);
            mapWidget.setWallLine(null, 0, null, 0, null);
         }
      }
   }
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter,
                         IMapWidget mapWidget, List<ArenaCoordinates> locationsToRedraw) {
      List<ArenaLocation> lineLocs = null;
      if ((currentTerrain > -1) && lineActive) {
         lineLocs = mapWidget.getLine();
      }
      if (wallLineActive) {
         lineLocs = mapWidget.getWallLine();
      }
      if (lineLocs != null) {
         for (ArenaLocation lineLoc : lineLocs) {
            ArenaLocation mapLoc;
            if ((loc != null) && lineActive) {
               // copy the terrain data to the map
               lineLoc.setTerrain(TerrainType.getByValue(currentTerrain));
               // This list contains the actual map locations
               mapLoc = lineLoc;
            }
            else {
               // since the list of ArenaLocations is a list of copies,
               // we must get the actual location from the map itself before we modify it
               mapLoc = mapWidget.getCombatMap().getLocation(lineLoc.x, lineLoc.y);
               // copy the wall data to the map
               mapLoc.addWall(lineLoc.getWalls());
            }
            if (locationsToRedraw != null) {
               locationsToRedraw.add(mapLoc);
            }
         }
         lineStart = null;
         lineStartAngleFromCenter = 0;
         lineEnd = null;
         lineEndAngleFromCenter = 0;
         if (lineActive) {
            mapWidget.setLine(null, null, null);
         }
         if (wallLineActive) {
            mapWidget.setWallLine(null, 0, null, 0, null);
         }
      }
      if (loc != null) {
         if (locationsToRedraw != null) {
            locationsToRedraw.add(loc);
         }

         if (setItemsActive) {
            Thing thing = Thing.getThing(itemDesc.getText(), null);
            if (thing != null) {
               loc.addThing(thing);
            }
            else {
               loc.addThing(itemDesc.getText());
            }
         }
         else if (clearItemsActive) {
            synchronized (loc) {
               loc.lock_this.check();
               loc.getThings().clear();
            }
         }
         else if (currentTerrain > -1) {
            if (fillActive) {
               fillMap(loc, currentTerrain, mapWidget.getCombatMap(), locationsToRedraw);
               fillActive = false;
               mapInterface.setMode(MapMode.PAINT_TERRAIN);
               fillButton.redraw();
            }
            else if (!lineActive && !wallLineActive) {
               loc.setTerrain(TerrainType.getByValue(currentTerrain));
               if (currentTerrain == TerrainType.TREE_TRUNK.value) {
                  for (Facing dir : Facing.values()) {
                     ArenaLocation adjacentLoc = mapWidget.getCombatMap().getLocation((short)(loc.x + dir.moveX), (short) (loc.y + dir.moveY));
                     if (adjacentLoc != null) {
                        if ((adjacentLoc.getTerrain() != TerrainType.SOLID_ROCK) && (adjacentLoc.getTerrain() != TerrainType.TREE_TRUNK)) {
                           adjacentLoc.setTerrain(TerrainType.DENSE_BUSH);
                           if (locationsToRedraw != null) {
                              locationsToRedraw.add(adjacentLoc);
                           }
                        }
                     }
                  }
               }
            }
         }
         if (currentWall > -1) {
            if (currentWall == 0) {
               loc.removeAllWalls();
               loc.removeAllDoors();
            }
            else {
               if (isDoor.getSelection() || isHalfHeight.getSelection()) {
                  DoorState state = DoorState.CLOSED;
                  if (isHalfHeight.getSelection()) {
                     state = DoorState.HALF_HEIGHT_WALL;
                  }
                  else if (isOpen.getSelection()) {
                     state = DoorState.OPEN;
                  }
                  else if (isLocked.getSelection()) {
                     state = DoorState.LOCKED;
                  }

                  Door door = new Door(state, doorKeyDesc.getText(), TerrainWall.getByBitMask(currentWall));
                  loc.addDoor(door);
                  // clear the door flags, so the next click won't also be a door
                  // unless the user explicitly makes it one.
                  isDoor.setSelection(false);
                  isOpen.setSelection(false);
                  isOpen.setEnabled(false);
                  isOpenLabel.setEnabled(false);
                  isLockable.setSelection(false);
                  isLockable.setEnabled(false);
                  isLockableLabel.setEnabled(false);
                  isLocked.setSelection(false);
                  isLocked.setEnabled(false);
                  isLockedLabel.setEnabled(false);
                  redrawWalls();
               }
               else {
                  loc.addWall(currentWall);
               }
            }
         }
      }
   }
   private static void fillMap(ArenaLocation loc, short newTerrain, CombatMap map, List<ArenaCoordinates> locationsToRedraw) {
      List<ArenaLocation> locsLeftToProcess = new ArrayList<>();
      locsLeftToProcess.add(loc);

      TerrainType originalTerrain = loc.getTerrain();
      if (originalTerrain.value == newTerrain) {
         return;
      }
      while (locsLeftToProcess.size() > 0) {
         ArenaLocation testLoc = locsLeftToProcess.remove(0);
         if ((testLoc == null) || (originalTerrain != testLoc.getTerrain())) {
            continue;
         }
         testLoc.setTerrain(TerrainType.getByValue(newTerrain));
         if (locationsToRedraw != null) {
            locationsToRedraw.add(testLoc);
         }
         locsLeftToProcess.add(map.getLocation(testLoc.x, (short)(testLoc.y + 2)));
         locsLeftToProcess.add(map.getLocation(testLoc.x, (short)(testLoc.y - 2)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc.x + 1), (short)(testLoc.y + 1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc.x - 1), (short)(testLoc.y + 1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc.x + 1), (short)(testLoc.y - 1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc.x - 1), (short)(testLoc.y - 1)));
      }
   }

   @Override
   public void viewAngleChanged(float newXFacingInRadians, float newYFacingInRadians) {
      rotation = -newXFacingInRadians;
      redrawWalls();
   }
   public void setMap(IMapWidget map) {
      mapInterface = map;
   }

}
