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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TerrainInterface extends Helper implements SelectionListener, ModifyListener, Listener, Enums, IGLViewListener
{
   private short    _currentTerrain = -1;
   private long     _currentWall    = -1;
   private Button[] _terrainButtons;
   private Button   _clearButton;
   private Button   _fillButton;
   private boolean  _fillActive     = false;
   private Button   _lineButton;
   private boolean  _lineActive     = false;
   private Button   _wallLineButton;
   private boolean  _wallLineActive = false;
   private Button[] _wallButtons;
   private Button   _isHalfHeight;
   private Button   _isDoor;
   private Label    _isDoorLabel;
   private Button   _isOpen;
   private Label    _isOpenLabel;
   private Button   _isLockable;
   private Label    _isLockableLabel;
   private Button   _isLocked;
   private Label    _isLockedLabel;
   private Text     _doorKeyDesc;
   private Label    _doorKeyDescLabel;
   private ArenaLocation _lineStart;
   private ArenaLocation _lineEnd;
   private double _lineStartAngleFromCenter;
   private double _lineEndAngleFromCenter;

   private Text     _itemDesc;
   private Button   _setItems;
   private boolean  _setItemsActive = false;
   private Button   _clearItems;
   private boolean  _clearItemsActive = false;

   public Text      _bgImageFilePath;
   public Button    _bgImageFileBtn;
   public Slider    _bgImageAlphaSlider;

   public boolean allowPan() {
      return ((_currentTerrain == -1) && (_currentWall == -1));
   }
   public void disableCurrentEdits() {
      if (_currentTerrain != -1) {
         _terrainButtons[_currentTerrain].redraw();
         _currentTerrain = -1;
      }
      if (_currentWall != -1) {
         _currentWall = -1;
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
         _terrainButtons = new Button[TerrainType.values().length];
         for (TerrainType terrain : TerrainType.values()) {
            _terrainButtons[terrain.value] = new Button(terrainButtonsBlock, SWT.PUSH);
            _terrainButtons[terrain.value].setText(terrain.name + " ");
            _terrainButtons[terrain.value].setAlignment(SWT.RIGHT);
            _terrainButtons[terrain.value].addListener(SWT.Paint, this);
            data = new GridData();
            data.minimumWidth = 120;
            data.grabExcessHorizontalSpace = true;
            _terrainButtons[terrain.value].setLayoutData(data);
            _terrainButtons[terrain.value].addSelectionListener(this);
         }
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         new Label(terrainButtonsBlock, 0);
         _clearButton = makeButton(terrainButtonsBlock, "Clear");
         _fillButton = makeButton(terrainButtonsBlock, "Paint / Fill");
         _lineButton = makeButton(terrainButtonsBlock, "Draw Line");
      }
      {
         Composite wallsDoorsBlock = createGroup(buttonsBlock, "Walls / Doors", 2/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
         {
            Composite wallButtonsBlock = new Composite(wallsDoorsBlock, SWT.TRAIL);
            grid = new GridLayout(3, false);
            grid.verticalSpacing = 1;
            grid.horizontalSpacing = 1;
            wallButtonsBlock.setLayout(grid);
            _wallButtons = new Button[19];
            for (int wall=0 ; wall<_wallButtons.length ; wall++) {
               _wallButtons[wall] = new Button(wallButtonsBlock, SWT.PUSH);
               _wallButtons[wall].addListener(SWT.Paint, this);
               data = new GridData();
               data.minimumWidth = 35;
               if (wall == 0) {
                  data.horizontalSpan = 3;
               }
               data.horizontalAlignment = SWT.CENTER;
               data.grabExcessHorizontalSpace = true;
               _wallButtons[wall].setLayoutData(data);
               _wallButtons[wall].addSelectionListener(this);
            }
         }
//         new Label(wallsDoorsBlock, 0);
//         new Label(wallsDoorsBlock, 0);
         {
            Composite wallBlock = createGroup(wallsDoorsBlock, "Wall info", 3/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
            _isHalfHeight     = new Button(wallBlock, SWT.CHECK);
            createLabel(wallBlock, "this wall is half height.", SWT.LEFT, 2/*hSpan*/, null);
            _isHalfHeight.addSelectionListener(this);
            _isDoor          = new Button(wallBlock, SWT.CHECK);
            _isDoorLabel     = createLabel(wallBlock, "this wall section has a door.", SWT.LEFT, 2/*hSpan*/, null);
            _isOpen          = new Button(wallBlock, SWT.CHECK);
            _isOpenLabel     = createLabel(wallBlock, "the door is open.", SWT.LEFT, 2/*hSpan*/, null);
            _isLockable      = new Button(wallBlock, SWT.CHECK);
            _isLockableLabel = createLabel(wallBlock, "the door can be locked.", SWT.LEFT, 2/*hSpan*/, null);
            _isLocked        = new Button(wallBlock, SWT.CHECK);
            _isLockedLabel   = createLabel(wallBlock, "the door is locked.", SWT.LEFT, 2/*hSpan*/, null);
            _doorKeyDescLabel= createLabel(wallBlock, "Key description:", SWT.RIGHT, 2/*hSpan*/, null);
            _doorKeyDesc     = createText(wallBlock, "", true/*editable*/, 1/*hSpan*/);
            _isDoor.addSelectionListener(this);
            _isOpen.addSelectionListener(this);
            _isLockable.addSelectionListener(this);
            _isLocked.addSelectionListener(this);
         }
         _wallLineButton = makeButton(wallsDoorsBlock, "Draw wall");
      }
      {
         Composite itemsBlock = createGroup(buttonsBlock, "Items on floor", 3/*columns*/, false/*sameSize*/, 1 /*hSpacing*/, 1 /*vSpacing*/);
         grid = new GridLayout(1, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         itemsBlock.setLayout(grid);
         createLabel(itemsBlock, "Item description:", SWT.LEFT, 1/*hSpan*/, null);
         _itemDesc   = createText(itemsBlock, "", true/*editable*/, 1/*hSpan*/);
         _setItems   = makeButton(itemsBlock, "Set Location");
         _clearItems = makeButton(itemsBlock, "Clear Location(s)");
      }
      {
         Composite bgImgBlock = createGroup(buttonsBlock, "Background Image", 4/*columns*/, false/*sameSize*/,
                                            1 /*hSpacing*/, 1 /*vSpacing*/);
         grid = new GridLayout(7, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         bgImgBlock.setLayout(grid);
         createLabel(bgImgBlock, "File:", SWT.LEFT, 1/*hSpan*/, null);
         _bgImageFilePath = createText(bgImgBlock, "", false/*editable*/, 4/*hSpan*/);
         _bgImageFileBtn = createButton(bgImgBlock, "choose", 2, null, this);
         createLabel(bgImgBlock, "Alpha", SWT.LEFT, 1/*hSpan*/, null);
         _bgImageAlphaSlider = new Slider(bgImgBlock, SWT.HORIZONTAL);
         GridData sliderGridData = new GridData(GridData.FILL_HORIZONTAL);
         sliderGridData.horizontalSpan = 6;
         _bgImageAlphaSlider.setLayoutData(sliderGridData);
         _bgImageAlphaSlider.setBounds(0, 30, 200, 20);
         _bgImageAlphaSlider.setMaximum(255);
         _bgImageAlphaSlider.setMinimum(0);
         _bgImageAlphaSlider.setIncrement(16);
         _bgImageAlphaSlider.addSelectionListener(this);
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
      boolean halfHeightWall = _isHalfHeight.getSelection();
      boolean isDoor = !halfHeightWall && _isDoor.getSelection();
      boolean isLockableDoor = isDoor && _isLockable.getSelection();
      boolean isOpenDoor = isDoor && _isOpen.getSelection();

      if (halfHeightWall) {
         _isDoor.setSelection(false);
      }
      if (!isDoor) {
         _isOpen.setSelection(false);
         _isLockable.setSelection(false);
         _isLocked.setSelection(false);
      }
      _isDoor.setEnabled(!halfHeightWall);
      _isDoorLabel.setEnabled(!halfHeightWall);
      _isOpen.setEnabled(isDoor);
      _isOpenLabel.setEnabled(isDoor);
      _isLockable.setEnabled(isDoor);
      _isLockableLabel.setEnabled(isDoor);
      _isLocked.setEnabled(isLockableDoor && !isOpenDoor);
      _isLockedLabel.setEnabled(isLockableDoor && !isOpenDoor);
      _doorKeyDesc.setEnabled(isLockableDoor);
      _doorKeyDescLabel.setEnabled(isLockableDoor);

      if ((e.widget == _isDoor) || (e.widget == _isHalfHeight) || (e.widget == _isOpen)) {
         // These affect the way the door looks, must redraw the hex:
         redrawWalls();
      }
      else if ((e.widget == _isLockable) || (e.widget == _isLocked)) {
         // These don't affect the appearance, so there is nothing to do
      }
      else if (e.widget == _bgImageAlphaSlider) {
         _mapInterface.setBackgroundAlpha(_bgImageAlphaSlider.getSelection());
         _mapInterface.getCombatMap().setBackgroundImageAlpha(_bgImageAlphaSlider.getSelection());
      }
      else if (e.widget == _bgImageFileBtn) {
         Shell shell = e.display.getShells()[0];
         FileDialog fd = new FileDialog(shell, SWT.OPEN);
         fd.setText("Open");
         fd.setFilterPath("Arenas");
         fd.setFilterExtensions(new String[] {"*.png;*.gif;*.jpg", "*.*"});
         fd.setFilterNames(new String[] {"Image Files (*.png;*.gif;*.jpg)", "All files(*.*)"});
         String selected = fd.open();
         if (selected != null && !selected.isEmpty()) {
            String baseDir = System.getProperty("user.dir");
            String fileSeparator = System.getProperty("file.separator");
            baseDir = baseDir + fileSeparator + "arenas";
            if (selected.startsWith(baseDir)) {
               selected = selected.substring(baseDir.length());
            }
            else {
               // copy the file to the same directory as the arenas are stored
               File file = new File(selected);
               File newFile = new File(baseDir + fileSeparator + file.getName());
               String fullPath = newFile.getAbsolutePath();
               int i=0;
               while (newFile.exists()) {
                  newFile = new File(fullPath.split(".")[0] + i++ + fullPath.split(".")[1]);
               }
               try (InputStream in = new BufferedInputStream(new FileInputStream(file));
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {

                  byte[] buffer = new byte[1024];
                  int lengthRead;
                  while ((lengthRead = in.read(buffer)) > 0) {
                     out.write(buffer, 0, lengthRead);
                     out.flush();
                  }
               } catch (IOException ex) {
                  ex.printStackTrace();
               }
               selected = newFile.getName();
            }
            _bgImageFilePath.setText(selected);
            _mapInterface.getCombatMap().setBackgroundImagePath(selected);
         }
      }
      else if (e.widget == _clearButton) {
         _fillActive = false;
         _wallLineActive = false;
         _lineActive = false;
         _setItemsActive = false;
         _clearItemsActive = false;
         if (_currentTerrain != -1) {
            _terrainButtons[_currentTerrain].redraw();
         }
         _currentTerrain = -1;
         _fillButton.redraw();
         _lineButton.redraw();
         _wallLineButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         _currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == _fillButton) {
         _fillActive = !_fillActive;
         _wallLineActive = false;
         _lineActive = false;
         _setItemsActive = false;
         _clearItemsActive = false;
         _fillButton.redraw();
         _lineButton.redraw();
         _wallLineButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         _currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == _lineButton) {
         _lineActive = !_lineActive;
         _wallLineActive = false;
         _fillActive = false;
         _setItemsActive = false;
         _clearItemsActive = false;
         _lineButton.redraw();
         _wallLineButton.redraw();
         _fillButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         _currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == _wallLineButton) {
         _wallLineActive = !_wallLineActive;
         _lineActive = false;
         _fillActive = false;
         _setItemsActive = false;
         _clearItemsActive = false;
         _lineButton.redraw();
         _wallLineButton.redraw();
         _fillButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         if (_currentTerrain != -1) {
            _terrainButtons[_currentTerrain].redraw();
         }
         _currentTerrain = -1;
         _currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == _setItems) {
         _setItemsActive = !_setItemsActive;
         _clearItemsActive = false;
         _fillActive = false;
         _wallLineActive = false;
         _lineActive = false;
         _wallLineButton.redraw();
         _lineButton.redraw();
         _fillButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         if (_currentTerrain != -1) {
            _terrainButtons[_currentTerrain].redraw();
         }
         _currentTerrain = -1;
         _currentWall = -1;
         redrawWalls();
      }
      else if (e.widget == _clearItems) {
         _clearItemsActive = !_clearItemsActive;
         _setItemsActive = false;
         _fillActive = false;
         _wallLineActive = false;
         _lineActive = false;
         _lineButton.redraw();
         _wallLineButton.redraw();
         _fillButton.redraw();
         _setItems.redraw();
         _clearItems.redraw();
         if (_currentTerrain != -1) {
            _terrainButtons[_currentTerrain].redraw();
         }
         _currentTerrain = -1;
         _currentWall = -1;
         redrawWalls();
      }
      else {
         for (short i=0 ; i<_terrainButtons.length ; i++) {
            if (e.widget == _terrainButtons[i]) {
               _terrainButtons[i].redraw();
               if (_currentTerrain != -1) {
                  _terrainButtons[_currentTerrain].redraw();
               }
               _currentTerrain = i;
               _fillButton.redraw();
               _lineButton.redraw();
               redrawWalls();
               _wallLineActive = false;
               _wallLineButton.redraw();
               _setItemsActive = false;
               _setItems.redraw();
               _clearItemsActive = false;
               _clearItems.redraw();
               _currentWall = -1;
               break;
            }
         }
         for (int i=0 ; i<_wallButtons.length ; i++) {
            if (e.widget == _wallButtons[i]) {
               if (_currentTerrain != -1) {
                  _terrainButtons[_currentTerrain].redraw();
               }
               _currentTerrain = -1;
               _fillButton.redraw();
               _lineButton.redraw();
               if (i==0) {
                  _currentWall = 0;
               }
               else {
                  _currentWall = TerrainWall.VERT_LEFT.bitMask << (i-1);
               }
               redrawWalls();
               _setItemsActive = false;
               _setItems.redraw();
               _clearItemsActive = false;
               _clearItems.redraw();
               _wallLineActive = false;
               _wallLineButton.redraw();
               break;
            }
         }
      }
      if (_mapInterface != null) {
         if (_fillActive) {
            _mapInterface.setMode(MapMode.FILL);
         }
         else if (_lineActive) {
            if (_currentWall > 0) {
               _mapInterface.setMode(MapMode.LINE);
            }
            else {
               _mapInterface.setMode(MapMode.NONE);
            }
         }
         else if (_currentTerrain != -1) {
            _mapInterface.setMode(MapMode.PAINT_TERRAIN);
         }
         else if (_currentWall != -1) {
            _mapInterface.setMode(MapMode.PAINT_WALL);
         }
         else {
            _mapInterface.setMode(MapMode.DRAG);
         }
      }
      if (_mapInterface != null) {
         _mapInterface.allowPan(allowPan());
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
               for (int buttonIndex=0 ; buttonIndex<_wallButtons.length ; buttonIndex++) {
                  if (eventButton == _wallButtons[buttonIndex]) {
                     long wallOrientation = TerrainWall.VERT_LEFT.bitMask << (buttonIndex-1);
                     if (((_currentWall == 0) && (buttonIndex == 0)) ||
                          (_currentWall == wallOrientation)) {
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
                        if (_isDoor.getSelection() || _isHalfHeight.getSelection()) {
                           DoorState state = DoorState.CLOSED;
                           if (_isHalfHeight.getSelection()) {
                              state = DoorState.HALF_HEIGHT_WALL;
                           }
                           if (_isOpen.getSelection()) {
                              state = DoorState.OPEN;
                           }
                           else if (_isLocked.getSelection()) {
                              state = DoorState.LOCKED;
                           }

                           Door door = new Door(state,_doorKeyDesc.getText(), TerrainWall.getByBitMask(wallOrientation));
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
               if ((eventButton == _fillButton) || (eventButton == _lineButton) ||
                   (eventButton == _setItems) || (eventButton == _clearItems) || (eventButton == _wallLineButton)) {
                  boolean fill = false;
                  if (_currentTerrain != -1) {
                     if (eventButton == _fillButton) {
                        fill = _fillActive;
                     }
                     if (eventButton == _lineButton) {
                        fill = _lineActive;
                     }
                  }
                  if (eventButton == _setItems) {
                     fill = _setItemsActive;
                  }
                  if (eventButton == _clearItems) {
                     fill = _clearItemsActive;
                  }
                  if (eventButton == _wallLineButton) {
                     fill = _wallLineActive;
                  }

                  if (fill) {
                     RGB fillColor;
                     if ((eventButton == _fillButton) || (eventButton == _lineButton)) {
                        fillColor = TerrainType.getByValue(_currentTerrain).color;
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
               for (short buttonIndex=0 ; buttonIndex<_terrainButtons.length ; buttonIndex++) {
                  if (eventButton == _terrainButtons[buttonIndex]) {
                     boolean fill = (_currentTerrain == buttonIndex);
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
      for (Button element : _wallButtons) {
         element.redraw();
      }
   }

   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter, IMapWidget mapWidget) {
      if ((_lineActive && (_currentTerrain > 0)) || (_wallLineActive && (_currentWall > 0))) {
         if (_lineStart == null) {
            _lineStart = loc;
            _lineStartAngleFromCenter = angleFromCenter;
         }
         if (_lineEnd != loc) {
            if (loc != null) {
               _lineEnd = loc;
               _lineEndAngleFromCenter = angleFromCenter;

               if (_lineActive && (_currentTerrain > 0)) {
                  mapWidget.setLine(_lineStart, _lineEnd, TerrainType.getByValue(_currentTerrain).color);
               }
               else { // this is true: (_wallLineActive && (_currentWall > 0))
                  mapWidget.setWallLine(_lineStart, _lineStartAngleFromCenter, _lineEnd, _lineEndAngleFromCenter,
                                        TerrainType.getByValue(_currentTerrain).color);
               }

               if (mapWidget instanceof MapWidget2D) {
                  MapWidget2D mapWidget2d = (MapWidget2D) mapWidget;
                  int[] bounds = mapWidget2d.getHexDimensions(_lineStart._x, _lineStart._y, true/*cacheResults*/);

                  Point minLoc = new Point(bounds[MapWidget2D.X_SMALLEST], bounds[MapWidget2D.Y_SMALLEST]);
                  Point maxLoc = new Point(bounds[MapWidget2D.X_LARGEST],  bounds[MapWidget2D.Y_LARGEST]);
                  if (_lineEnd != null) {
                     bounds = mapWidget2d.getHexDimensions(_lineEnd._x, _lineEnd._y, true/*cacheResults*/);
                     minLoc = new Point(Math.min(minLoc.x, bounds[MapWidget2D.X_SMALLEST]), Math.min(minLoc.y, bounds[MapWidget2D.Y_SMALLEST]));
                     maxLoc = new Point(Math.max(maxLoc.x, bounds[MapWidget2D.X_LARGEST]),  Math.max(maxLoc.y, bounds[MapWidget2D.Y_LARGEST]));
                  }
                  if (_lineStart != null) {
                     bounds = mapWidget2d.getHexDimensions(_lineStart._x, _lineStart._y, true/*cacheResults*/);
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
      if (_currentTerrain > -1) {
         if (_lineActive || _wallLineActive) {
            _lineStart = loc;
            _lineStartAngleFromCenter = angleFromCenter;
            _lineEnd = loc;
            _lineEndAngleFromCenter = angleFromCenter;
            mapWidget.setLine(null, null, null);
            mapWidget.setWallLine(null, 0, null, 0, null);
         }
      }
   }
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter,
                         IMapWidget mapWidget, List<ArenaCoordinates> locationsToRedraw) {
      List<ArenaLocation> lineLocs = null;
      if ((_currentTerrain > -1) &&_lineActive) {
         lineLocs = mapWidget.getLine();
      }
      if (_wallLineActive) {
         lineLocs = mapWidget.getWallLine();
      }
      if (lineLocs != null) {
         for (ArenaLocation lineLoc : lineLocs) {
            ArenaLocation mapLoc;
            if ((loc != null) && _lineActive) {
               // copy the terrain data to the map
               lineLoc.setTerrain(TerrainType.getByValue(_currentTerrain));
               // This list contains the actual map locations
               mapLoc = lineLoc;
            }
            else {
               // since the list of ArenaLocations is a list of copies,
               // we must get the actual location from the map itself before we modify it
               mapLoc = mapWidget.getCombatMap().getLocation(lineLoc._x, lineLoc._y);
               // copy the wall data to the map
               mapLoc.addWall(lineLoc.getWalls());
            }
            if (locationsToRedraw != null) {
               locationsToRedraw.add(mapLoc);
            }
         }
         _lineStart = null;
         _lineStartAngleFromCenter = 0;
         _lineEnd = null;
         _lineEndAngleFromCenter = 0;
         if (_lineActive) {
            mapWidget.setLine(null, null, null);
         }
         if (_wallLineActive) {
            mapWidget.setWallLine(null, 0, null, 0, null);
         }
      }
      if (loc != null) {
         if (locationsToRedraw != null) {
            locationsToRedraw.add(loc);
         }

         if (_setItemsActive) {
            Thing thing = Thing.getThing(_itemDesc.getText(), null);
            if (thing != null) {
               loc.addThing(thing);
            }
            else {
               loc.addThing(_itemDesc.getText());
            }
         }
         else if (_clearItemsActive) {
            synchronized (loc) {
               loc._lock_this.check();
               loc.getThings().clear();
            }
         }
         else if (_currentTerrain > -1) {
            if (_fillActive) {
               fillMap(loc, _currentTerrain, mapWidget.getCombatMap(), locationsToRedraw);
               _fillActive = false;
               _mapInterface.setMode(MapMode.PAINT_TERRAIN);
               _fillButton.redraw();
            }
            else if (!_lineActive && !_wallLineActive) {
               loc.setTerrain(TerrainType.getByValue(_currentTerrain));
               if (_currentTerrain == TerrainType.TREE_TRUNK.value) {
                  for (Facing dir : Facing.values()) {
                     ArenaLocation adjacentLoc = mapWidget.getCombatMap().getLocation((short)(loc._x + dir.moveX), (short) (loc._y + dir.moveY));
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
         if (_currentWall > -1) {
            if (_currentWall == 0) {
               loc.removeAllWalls();
               loc.removeAllDoors();
            }
            else {
               if (_isDoor.getSelection() || _isHalfHeight.getSelection()) {
                  DoorState state = DoorState.CLOSED;
                  if (_isHalfHeight.getSelection()) {
                     state = DoorState.HALF_HEIGHT_WALL;
                  }
                  else if (_isOpen.getSelection()) {
                     state = DoorState.OPEN;
                  }
                  else if (_isLocked.getSelection()) {
                     state = DoorState.LOCKED;
                  }

                  Door door = new Door(state, _doorKeyDesc.getText(), TerrainWall.getByBitMask(_currentWall));
                  loc.addDoor(door);
                  // clear the door flags, so the next click won't also be a door
                  // unless the user explicitly makes it one.
                  _isDoor.setSelection(false);
                  _isOpen.setSelection(false);
                  _isOpen.setEnabled(false);
                  _isOpenLabel.setEnabled(false);
                  _isLockable.setSelection(false);
                  _isLockable.setEnabled(false);
                  _isLockableLabel.setEnabled(false);
                  _isLocked.setSelection(false);
                  _isLocked.setEnabled(false);
                  _isLockedLabel.setEnabled(false);
                  redrawWalls();
               }
               else {
                  loc.addWall(_currentWall);
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
         locsLeftToProcess.add(map.getLocation(testLoc._x,   (short)(testLoc._y+2)));
         locsLeftToProcess.add(map.getLocation(testLoc._x,   (short)(testLoc._y-2)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc._x+1), (short)(testLoc._y+1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc._x-1), (short)(testLoc._y+1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc._x+1), (short)(testLoc._y-1)));
         locsLeftToProcess.add(map.getLocation((short)(testLoc._x-1), (short)(testLoc._y-1)));
      }
   }

   double rotation = 0;
   private IMapWidget _mapInterface;
   @Override
   public void viewAngleChanged(float newXFacingInRadians, float newYFacingInRadians) {
      rotation = -newXFacingInRadians;
      redrawWalls();
   }
   public void setMap(IMapWidget map) {
      _mapInterface = map;
     _mapInterface.setBackgroundAlpha(_bgImageAlphaSlider.getSelection());

     String backgroundImagePath = "";
     if (map.getCombatMap() != null) {
        backgroundImagePath = map.getCombatMap().getBackgroundImagePath();
        _bgImageAlphaSlider.setSelection(map.getCombatMap().getBackgroundImageAlpha());
        if (backgroundImagePath == null) {
           backgroundImagePath = "";
        }
     }
     _bgImageFilePath.setText(backgroundImagePath);
   }

}
