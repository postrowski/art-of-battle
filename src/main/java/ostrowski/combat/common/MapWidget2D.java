/*
 * Created on May 24, 2006
 */
package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.*;
import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.TerrainWall;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.things.*;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.server.*;
import ostrowski.util.SemaphoreAutoTracker;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;

public class MapWidget2D extends MapWidget implements Listener, SelectionListener
{
   static final int         MIN_SIZE_PER_HEX         = 8;
   private int              _sizePerHex              = 30;
   private int              _widgetWidth             = -1;
   private int              _widgetHeight            = -1;

   private final Canvas     _canvas;

   private static ImageData ZOOM_CONTROL_IMAGE_DATA  = null;
   private static Image     ZOOM_CONTROL_IMAGE       = null;

   private static Point     ZOOM_IN_BUTTON_CENTER    = null;
   private static Point     ZOOM_OUT_BUTTON_CENTER   = null;
   private static Point     ZOOM_RESET_BUTTON_CENTER = null;
   private static int       ZOOM_BUTTON_RADIUS       = 0;

   private static Cursor    _currentCursor           = null;
   private static Cursor    _fillCursor              = null;
   private static Cursor    _brushCursor             = null;
   private static Cursor    _wallCursor              = null;
   private static Cursor    _handOpenCursor          = null;
   private static Cursor    _handClosedCursor        = null;

   private short            _zoom                    = 0;
   private short            _maxZoom                 = 10;
   private int[]            _sizePerHexForZoom       = null;

   private short            _top                     = 0;
   private short            _left                    = 0;

   private boolean          _resizeOnFirstDraw       = true;
   private Point            _dragStart               = null; // will be null when drag is not taking place.
   private Point            _dragEnd                 = null; // will be null when drag is not taking place.
   private ImageData        _imageDataCopy;
   private CombatMap        _hashMapIsForThisMap     = null;

   static {
      ZOOM_CONTROL_IMAGE_DATA = getImageData("/res/zoomControl.png");
   }

   private static ImageData getImageData(String resourceName) {
      try (InputStream stream = MapWidget2D.class.getResourceAsStream(resourceName))
      {
         if (stream == null) {
            DebugBreak.debugBreak("can't find resource " + resourceName);
            return null;
         }
         return new ImageData(stream);
      } catch (IOException e) {
         DebugBreak.debugBreak("can't load resource " + resourceName);
         return null;
      }
   }

   public MapWidget2D(Composite parent) {
      _canvas = new Canvas(parent, SWT.BORDER);

      ZOOM_CONTROL_IMAGE    = new Image(parent.getDisplay(), ZOOM_CONTROL_IMAGE_DATA);

      _fillCursor       = new Cursor(parent.getDisplay(), getImageData("/res/paintBucket.png"),    0, 28);
      _brushCursor      = new Cursor(parent.getDisplay(), getImageData("/res/paintBrush.png"),     0, 19);
      _wallCursor       = new Cursor(parent.getDisplay(), getImageData("/res/paintBrushWall.png"), 0, 19);
      _handOpenCursor   = new Cursor(parent.getDisplay(), getImageData("/res/handOpen.png"),       10, 10);
      _handClosedCursor = new Cursor(parent.getDisplay(), getImageData("/res/handClosed.png"),     10, 10);

      _canvas.addListener(SWT.Paint, this);
      _canvas.addListener(SWT.MouseDown, this);
      _canvas.addListener(SWT.MouseUp, this);
      _canvas.addListener(SWT.MouseMove, this);
      _canvas.addListener(SWT.MouseDoubleClick, this);
      _canvas.addListener(SWT.MouseVerticalWheel, this);
      _canvas.addListener(SWT.MouseEnter, this);
      _canvas.addListener(SWT.MouseExit, this);

      _canvas.addKeyListener(this);
      _canvas.getParent().addKeyListener(this);
      _canvas.getParent().getParent().addKeyListener(this);
   }

   @Override
   public void setLayoutData(Object data) {
      _canvas.setLayoutData(data);
   }

//   public boolean setZoomToFit()
//   {
//      Object layoutData = _canvas.getLayoutData();
//      if (layoutData instanceof GridData) {
//         GridData gridData = (GridData) layoutData;
//         short width = (short) gridData.minimumWidth;
//         short height = (short) gridData.minimumHeight;
//         setZoomToFit(width, height);
//         return true;
//      }
//      return false;
//   }

   @Override
   public void setZoomToFit()
   {
      int oldSize = _sizePerHex;
      int oldZoom = _zoom;
       Rectangle rect = _canvas.getBounds();
      _widgetWidth = rect.width;
      _widgetHeight = rect.height;

      if (rect.width > 0) {
         _resizeOnFirstDraw = false;
      }

      // minimum hex size is MIN_SIZE_PER_HEX.
      _sizePerHex = MIN_SIZE_PER_HEX;
      if (_combatMap != null) {
         while (++_sizePerHex < (_widgetWidth / 2)) {
            int[] bounds = getHexDimensions(_combatMap.getSizeX(), _combatMap.getSizeY(), false/*cacheResults*/);
            if ((bounds[X_LARGEST] > _widgetWidth) || (bounds[Y_LARGEST] > _widgetHeight)) {
               _sizePerHex--;
               break;
            }
         }
      }
      _maxZoom = 10;
      _zoom = 0;
      _top  = 0;
      _left = 0;
      _sizePerHexForZoom = new int[_maxZoom];
      _sizePerHexForZoom[0] = _sizePerHex;
      for (int i = 0; i < (_maxZoom - 1); i++) {
         _sizePerHexForZoom[i + 1] = (_sizePerHexForZoom[i] * 4) / 3;
      }
      if ((oldSize != _sizePerHex) || (oldZoom != _zoom)) {
         redraw();
      }
   }

   public int getFontSizeByZoomLevel() {
      switch (_zoom) {
         case 0: return 10;
         case 1: return 11;
         case 2: return 12;
         case 3: return 13;
         case 4: return 14;
         case 5: return 16;
         case 6: return 17;
         case 7: return 19;
         case 8: return 20;
         case 9: return 22;
         case 10: return 24;
      }
      return 10;
   }
   @Override
   public void handleEvent(Event event)
   {
      if (event.type == SWT.Paint) {
         if (_resizeOnFirstDraw) {
            setZoomToFit();
         }
         if (_combatMap != null) {
            Display display = event.display;
            Image image = new Image(display, _canvas.getBounds());
            Font font = new Font(display, "", getFontSizeByZoomLevel(), SWT.BOLD);

            // Setup an off-screen GC, onto which all drawing is done.
            // This will later be transferred to the events CG (real screen)
            // This allows double-buffering of the image, to reduce flashing in any animation
            GC gcImage = new GC(image);
            gcImage.setFont(font);
//            short minCol = -1;
//            short maxCol = -1;
//            short minRow = -1;
//            short maxRow = -1;
//            short start = (short) Math.min(minCol, minRow);
//            short end   = (short) Math.max(_combatMap.getSizeX(), _combatMap.getSizeY());
//            for (short i=start ; i< end ; i++) {
//               int[] bounds = getHexDimensions(i,i, false/*cacheResults*/);
//               if (-1 == minCol) {
//                  if ((bounds[X_SMALLEST] <= event.x) &&
//                      (bounds[X_LARGEST]  >= event.x)) {
//                     minCol = i;
//                  }
//               }
//               if (-1 == minRow) {
//                  if ((bounds[Y_LARGEST]  >= event.y) &&
//                      (bounds[Y_SMALLEST] <= event.y)) {
//                     minRow = i;
//                  }
//               }
//               if ((minRow != -1) && (minCol != -1)) {
//                  break;
//               }
//            }
//            int rightEdge = event.x + event.width;
//            int bottomEdge = event.y + event.height;
//            for (short i=end ; i>= start ; i--) {
//               int[] bounds = getHexDimensions(i,i, false/*cacheResults*/);
//               if (-1 == maxCol) {
//                  if ((bounds[X_SMALLEST] <= rightEdge) &&
//                      (bounds[X_LARGEST]  >= rightEdge)) {
//                     maxCol = (short) (i+1);
//                  }
//               }
//               if (-1 == maxRow) {
//                  if ((bounds[Y_LARGEST]  >= bottomEdge) &&
//                      (bounds[Y_SMALLEST] <= bottomEdge)) {
//                     maxRow = i;
//                  }
//               }
//               if ((maxRow != -1) && (maxCol != -1)) {
//                  break;
//               }
//            }
//            minCol = (short) Math.max(minCol, _left - 1);
//            maxCol = (short) Math.min(maxCol, _combatMap.getSizeX());
//            minRow = (short) Math.max(minRow, _top - 1);
//            maxRow = (short) Math.min(maxRow, _combatMap.getSizeY());
//            if (maxRow == -1) {
//               maxRow = _combatMap.getSizeY();
//            }
//            if (maxCol == -1) {
//               maxCol = _combatMap.getSizeX();
//            }

//            int[] bounds = getHexDimensions((short)1000, (short)1000, false/*chacheResults*/);
//            double colWidth = bounds[4] / 1000.0;
//            double rowHeight = bounds[1] / 1000.0;
//            short minCol = (short) Math.max((event.x / colWidth),                   _left - 1);
//            short maxCol = (short) Math.min(((event.x + event.width) / colWidth + 1),   _combatMap.getSizeX());
//            short minRow = (short) Math.max((event.y / rowHeight),                  _top - 1);
//            short maxRow = (short) Math.min(((event.y + event.height) / rowHeight + 1), _combatMap.getSizeY());
//            if (minCol >0) minCol--;
//            if (minRow >0) minRow--;
            if (_isDragable && (_dragStart != null) && (_dragEnd != null) && (_imageDataCopy != null)) {
               Image copyImage = new Image(display, _imageDataCopy);
               int dX = _dragEnd.x - _dragStart.x;
               int dY = _dragEnd.y - _dragStart.y;
               drawZoomControls(gcImage);
               event.gc.drawImage(copyImage, event.x/*srcX*/, event.y/*srcY*/, event.width/*srcWidth*/, event.height/*srcHeight*/,
                                             event.x + dX/*dstX*/, event.y + dY/*dstY*/, event.width/*dstWidth*/, event.height/*dstHeight*/);
               copyImage.dispose();
            }
            else {
               int[] bounds = getHexDimensions((short)10000, (short)10000, false/*chacheResults*/);
               double colWidth = bounds[4] / 10000.0;
               double rowHeight = bounds[1] / 10000.0;
               short minCol = (short) (((event.x / colWidth) + _left) - 1);
               short maxCol = (short) Math.min(Math.round(((event.x + event.width) / colWidth)  + _left+ 2),  _combatMap.getSizeX());
               short minRow = (short) (((event.y / rowHeight) + _top) -1);
               short maxRow = (short) Math.min(Math.round(((event.y + event.height) / rowHeight) + _top + 2), _combatMap.getSizeY());
               if (minCol <0) {
                  minCol = 0;
               }
               if (minRow <0) {
                  minRow = 0;
               }

//               if (_isDragable && (_dragStart != null) && (_imageDataCopy == null)) {
//                  // If we are going to use this as a drag image source, make it as big as possible.
//                  minCol = 0;
//                  minRow = 0;
//                  maxCol = _combatMap.getSizeX();
//                  maxRow = _combatMap.getSizeY();
//               }

               Map<Integer, Color> cachedColorsMap = new HashMap<>();
               Map<Color, Pattern> cachedPatternMap = new HashMap<>();

               boolean isHideViewFromLocalPlayers = _combatMap.isHideViewFromLocalPlayers();
               for (short col = minCol; col < maxCol; col++) {
                  short row = minRow;
                  if ((row % 2) != (col % 2)) {
                     row++;
                  }
                  for (; row < maxRow; row += 2) {
                     // only redraw hexes that are in the redraw-area.
                     // Since all we care about is the y coordinated of these hexes,
                     // we cache them in the 'locAtRow' array, so we can re-use
                     // them on subsequent columns
                     ArenaLocation loc = _combatMap.getLocationQuick(col, row);
                     drawHex(loc, gcImage, display, cachedColorsMap, cachedPatternMap);
                     boolean isKnown = (_selfID == -1) || loc.isKnownBy(_selfID);
                     if (isKnown) {
                        drawWall(_combatMap.getLocation(col, row), gcImage, display,
                                 _sizePerHex, 0 /*offsetX*/, 0 /*offsetY*/, _left, _top, _selfID, isHideViewFromLocalPlayers);
                     }
                  }
               }
               // since the text often extends outside of the bounds defined by getHexDimensions,
               // we just always re-draw all text.

               for (short col = _left; col < _combatMap.getSizeX(); col++) {
                  short row = _top;
                  if ((row % 2) != (col % 2)) {
                     row++;
                  }
                  for ( ; row < _combatMap.getSizeY(); row+=2) {
                     labelHex(_combatMap.getLocation(col, row), gcImage, display);
                  }
               }

               if (_isDragable && (_dragStart != null) && (_imageDataCopy == null)) {
                  _imageDataCopy = (ImageData) image.getImageData().clone();
               }

               drawZoomControls(gcImage);

               // Draw the off-screen buffer to the screen
               //event.gc.drawImage(image, 0, 0);
               event.gc.drawImage(image, event.x/*srcX*/, event.y/*srcY*/, event.width/*srcWidth*/, event.height/*srcHeight*/,
                                         event.x/*dstX*/, event.y/*dstY*/, event.width/*dstWidth*/, event.height/*dstHeight*/);

               // Clean up
               for (Color clr : cachedColorsMap.values()) {
                  clr.dispose();
               }
               for (Pattern pattern : cachedPatternMap.values()) {
                  pattern.dispose();
               }
            }
            font.dispose();
            image.dispose();
            gcImage.dispose();
         }
      }
      else if (event.type == SWT.MouseDown) {
         if (_currentCursor == _handOpenCursor) {
            _currentCursor = _handClosedCursor;
            if ((event.widget.getDisplay() != null) && (event.widget.getDisplay().getActiveShell() != null)) {
               event.widget.getDisplay().getActiveShell().setCursor(_currentCursor);
            }
         }
      }
      else if (event.type == SWT.MouseUp) {
         if (_currentCursor == _handClosedCursor) {
            _currentCursor = _handOpenCursor;
            if ((event.widget.getDisplay() != null) && (event.widget.getDisplay().getActiveShell() != null)) {
               event.widget.getDisplay().getActiveShell().setCursor(_currentCursor);
            }
         }
      }
      // Don't do else if, because there is another MouseDown & MouseUp handler later:
      if (event.type == SWT.MouseEnter) {
         switch (_mapMode) {
            case DRAG:          _currentCursor = _handOpenCursor; break;
            case FILL:          _currentCursor = _fillCursor;     break;
            case PAINT_TERRAIN: _currentCursor = _brushCursor;    break;
            case PAINT_WALL:    _currentCursor = _wallCursor;     break;
            case LINE:          _currentCursor = null;            break;
         }
         if ((event.widget.getDisplay() != null) && (event.widget.getDisplay().getActiveShell() != null)) {
            event.widget.getDisplay().getActiveShell().setCursor(_currentCursor);
         }
      }
      else if (event.type == SWT.MouseExit) {
         if (_currentCursor != null) {
            _currentCursor = null;
            if ((event.widget.getDisplay() != null) && (event.widget.getDisplay().getActiveShell() != null)) {
               event.widget.getDisplay().getActiveShell().setCursor(null);
            }
         }
      }
      else if (event.type == SWT.MouseVerticalWheel) {
         _zoom += event.count / 3;
         resetOnNewZoomLevel(new Point(event.x, event.y));
      }
      else if (event.type == SWT.MouseMove) {

         if (_isDragable && (_dragStart != null)) {
            _dragEnd = new Point(event.x, event.y);
            _canvas.redraw();
            return;
         }
         List<Orientation> oldOrientations = _mouseOverOrientations;
         _mouseOverOrientations = new ArrayList<>();
         ArenaLocation loc = findLoc(event.x, event.y);
         double angle    = 0;
         double distance = 0;
         if (loc != null) {
            angle    = getAngleFromCenter(loc, event.x, event.y);
            distance = getDistanceFromCenter(loc, event.x, event.y);
            if (_movementRequest != null) {
//               Orientation mouseOverOrientation = _movementRequest.getBestOrientation(loc, angle, distance);
//               if (mouseOverOrientation != null) {
//                  _mouseOverOrientations.add(mouseOverOrientation);
//               }
//               else
               {
                  Orientation futureOrientation = _movementRequest.getBestFutureOrientation(loc, angle, distance);
                  if (futureOrientation != null) {
                     _mouseOverOrientations = _movementRequest.getRouteToFutureOrientation(futureOrientation);
                  }
               }
            }
         }
         boolean orientationChanged;
         if ((oldOrientations != null) && (_mouseOverOrientations != null)) {
            if (oldOrientations.size() == _mouseOverOrientations.size()) {
               orientationChanged = !oldOrientations.containsAll(_mouseOverOrientations) || !_mouseOverOrientations.containsAll(oldOrientations);
            }
            else {
               orientationChanged = true;
            }
         }
         else {
            // one of these is null, so if the other is not null, it has changed
            orientationChanged = ((oldOrientations != null) || (_mouseOverOrientations != null));
         }
         if (!orientationChanged) {
            if ((_mouseOverCharacter != null) && (_mouseOverOrientations != null) && (_mouseOverOrientations.size() > 0)) {
               if (_mouseOverOrientations.contains(_mouseOverCharacter.getOrientation())) {
                  orientationChanged = true;
               }
            }
         }
         if ((loc != _mouseOverLocation) || orientationChanged) {
            if ((event.stateMask & SWT.BUTTON1) != 0) {
               for (IMapListener listener : _listeners) {
                  listener.onMouseDrag(_mouseOverLocation, event, angle, distance);
               }
            }
            int minX = 1000;
            int minY = 1000;
            int maxX = 0;
            int maxY = 0;
            if (_mouseOverLocation != null) {
               int[] bounds = getHexDimensions(_mouseOverLocation);
               minX = bounds[X_SMALLEST];
               minY = bounds[Y_SMALLEST];
               maxX = bounds[X_LARGEST];
               maxY = bounds[Y_LARGEST];
            }
            boolean redrawSurroundHexes = false;
            if (redrawSurroundHexes && (loc != null)) {
               for (Facing dir : Facing.values()) {
                  ArenaLocation adjLoc = _combatMap.getLocation((short)(loc._x + dir.moveX), (short)(loc._y + dir.moveY));
                  if (adjLoc != null) {
                     int[] bounds = getHexDimensions(adjLoc);
                     minX = Math.min(minX, bounds[X_SMALLEST]);
                     minY = Math.min(minY, bounds[Y_SMALLEST]);
                     maxX = Math.max(maxX, bounds[X_LARGEST]);
                     maxY = Math.max(maxY, bounds[Y_LARGEST]);
                  }
               }
            }
            _mouseOverLocation = loc;
            if (_mouseOverLocation != null) {
               if ((event.stateMask & SWT.BUTTON1) != 0) {
                  for (IMapListener listener : _listeners) {
                     listener.onMouseDrag(_mouseOverLocation, event, angle, distance);
                  }
               }
               int[] bounds = getHexDimensions(_mouseOverLocation);
               minX = Math.min(minX, bounds[X_SMALLEST]);
               minY = Math.min(minY, bounds[Y_SMALLEST]);
               maxX = Math.max(maxX, bounds[X_LARGEST]);
               maxY = Math.max(maxY, bounds[Y_LARGEST]);
            }
            for (IMapListener listener : _listeners) {
               listener.onMouseMove(_mouseOverLocation, event, angle, distance);
            }

            if (orientationChanged) {
               if (oldOrientations != null) {
                  for (Orientation oldOrientation : oldOrientations) {
                     if (oldOrientation == null) {
                        DebugBreak.debugBreak();
                     }
                     else {
                        for (ArenaCoordinates oldCoord : oldOrientation.getCoordinates()) {
                           int[] bounds = getHexDimensions(oldCoord);
                           minX = Math.min(minX, bounds[X_SMALLEST]);
                           minY = Math.min(minY, bounds[Y_SMALLEST]);
                           maxX = Math.max(maxX, bounds[X_LARGEST]);
                           maxY = Math.max(maxY, bounds[Y_LARGEST]);
                        }
                     }
                  }
               }
               if (_mouseOverOrientations != null) {
                  for (Orientation mouseOverOrientation : _mouseOverOrientations) {
                     if (mouseOverOrientation == null) {
                        DebugBreak.debugBreak();
                     }
                     else {
                        for (ArenaCoordinates newCoord : mouseOverOrientation.getCoordinates()) {
                           int[] bounds = getHexDimensions(newCoord);
                           minX = Math.min(minX, bounds[X_SMALLEST]);
                           minY = Math.min(minY, bounds[Y_SMALLEST]);
                           maxX = Math.max(maxX, bounds[X_LARGEST]);
                           maxY = Math.max(maxY, bounds[Y_LARGEST]);
                        }
                     }
                  }
               }
            }
            redraw(minX, minY, (maxX - minX), (maxY - minY), true);
         }
      }
      else if ((event.type == SWT.MouseDown) || (event.type == SWT.MouseUp)) {
         if (event.button == 1) {
            // left mouse button
            if ((event.x > ((Math.min(ZOOM_IN_BUTTON_CENTER.x, ZOOM_OUT_BUTTON_CENTER.x) - ZOOM_BUTTON_RADIUS)))
                && (event.y > (ZOOM_IN_BUTTON_CENTER.y - ZOOM_BUTTON_RADIUS))) {
               if (isInButton(event, ZOOM_IN_BUTTON_CENTER)) {
                  if (event.type == SWT.MouseDown) {
                     _zoom++;
                     resetOnNewZoomLevel(null);
                  }
                  return;
               }
               if (isInButton(event, ZOOM_OUT_BUTTON_CENTER)) {
                  if (event.type == SWT.MouseDown) {
                     _zoom--;
                     resetOnNewZoomLevel(null);
                  }
                  return;
               }
               if (isInButton(event, ZOOM_RESET_BUTTON_CENTER)) {
                  if (event.type == SWT.MouseDown) {
                     _zoom = 0;
                     _left = 0;
                     _top = 0;
                     resetOnNewZoomLevel(null);
                     setZoomToFit();
                  }
                  return;
               }
            }
            if (_isDragable) {
               if (event.type == SWT.MouseDown) {
                  boolean mouseOverSelectableHex = false;
                  if (_selectableHexes != null) {
                     for (ArenaLocation selectableHex : _selectableHexes) {
                        int[] bounds = getHexDimensions(selectableHex);
                        if ((event.x > bounds[X_SMALLEST]) && (event.y > bounds[Y_SMALLEST]) &&
                            (event.x < bounds[X_LARGEST]) && (event.y < bounds[Y_LARGEST])) {
                           mouseOverSelectableHex = true;
                           break;
                        }
                     }
                  }
                  if (!mouseOverSelectableHex) {
                     _dragStart = new Point(event.x, event.y);
                     _canvas.redraw();
                     return;
                  }
               }
               // must be SWT.MouseUp
               if (_dragStart != null) {
                  if (_dragEnd != null) {
                     // complete the drag operation:
                     int dX = _dragEnd.x - _dragStart.x;
                     int dY = _dragEnd.y - _dragStart.y;
                     if ((dX != 0) || (dY != 0)) {
                        int[] loc_1_1 = getHexDimensions((short)1/*column*/, (short)1/*row*/, _sizePerHex, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
                        int[] loc_2_2 = getHexDimensions((short)2/*column*/, (short)2/*row*/, _sizePerHex, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
                        _top -= dY / (loc_2_2[1] - loc_1_1[1]);
                        _left -= dX / (loc_2_2[0] - loc_1_1[0]);
                        _top  = (short) Math.max(0, _top);
                        _left = (short) Math.max(0, _left);
                        resetOnNewZoomLevel(null);
                     }
                  }

                  _imageDataCopy = null;
                  _dragStart = null;
                  if (_dragEnd != null) {
                     _dragEnd = null;
                     return;
                  }
               }
            }
         }
         double angle    = 0.0;
         double distance = 0.0;
         ArenaLocation loc = findLoc(event.x, event.y);
         if (loc != null) {
            angle    = getAngleFromCenter(loc, event.x, event.y);
            distance = getDistanceFromCenter(loc, event.x, event.y);
         }
         for (IMapListener listener : _listeners) {
            if (event.type == SWT.MouseDown) {
               if (event.button == 1) {
                  listener.onMouseDown(loc, event, angle, distance);
               }
               else if (event.button == 3) {
                  listener.onRightMouseDown(loc, event, angle, distance);
               }
            }
            else if (event.type == SWT.MouseUp) {
               if (event.button == 1) {
                  listener.onMouseUp(loc, event, angle, distance);
               }
               else if (event.button == 3) {
                  listener.onRightMouseUp(loc, event, angle, distance);
               }
            }
         }
      }
   }

//   @Override
//   public void allowPan(boolean allow) {
//      super.allowPan(allow);
//   }
//   @Override
//   public void allowDrag(boolean allow) {
//      super.allowDrag(allow);
//   }
//
   private static boolean isInButton(Event event, Point buttonCenter) {
      int dx = buttonCenter.x - event.x;
      int dy = buttonCenter.y - event.y;
      int d2 = (dx * dx) + (dy * dy);
      return (d2 < (ZOOM_BUTTON_RADIUS * ZOOM_BUTTON_RADIUS));
   }

   public void drawHex(ArenaLocation loc, GC gc, Display display, Map<Integer, Color> cachedColorsMap, Map<Color, Pattern> cachedPatternMap)
   {
      if (loc == null) {
         return;
      }
      boolean isMouseOver = (loc == _mouseOverLocation);
//      boolean isVisible = ((_selfID != -1) || !_combatMap.isHideViewFromLocalPlayers()) && loc.getVisible(_selfID);
      boolean isVisible = (_selfID == -1) || loc.getVisible(_selfID);
      boolean isKnown = (_selfID == -1) || loc.isKnownBy(_selfID);
      boolean hexSelectable = true;
      if (_selectableHexes != null) {
         hexSelectable = loc.getSelectable();
//         for (ArenaCoordinates selectableHex : _selectableHexes) {
//            if ((selectableHex._x == loc._x) && (selectableHex._y == loc._y)) {
//               hexSelectable = HEX_ENABLED;
//               break;
//            }
//         }
      }
      List<ArenaTrigger> triggers = new ArrayList<>();
      List<ArenaEvent> events = new ArrayList<>();
      ArenaTrigger trigger = _combatMap.getSelectedTrigger();
      if (trigger != null) {
         if (trigger.isTriggerAtLocation(loc, null/*mover*/)) {
            triggers.add(trigger);
         }
         for (ArenaEvent event : trigger.getEvents()) {
            if (event.isEventAtLocation(loc)) {
               events.add(event);
            }
         }
      }
      else {
         if (_hashMapIsForThisMap != _combatMap) {
            _hashMapIsForThisMap = _combatMap;
            _eventsMap.clear();
            for (ArenaTrigger trig : _combatMap.getTriggers()) {
               for (ArenaCoordinates trigLoc : trig.getTriggerCoordinates()) {
                  List<ArenaTrigger> triggersAtLoc = _eventsMap.computeIfAbsent(trigLoc, k -> new ArrayList<>());
                  triggersAtLoc.add(trig);
               }
            }
         }
         List<ArenaTrigger> triggersAtLoc = _eventsMap.get(loc);
         if (triggersAtLoc != null) {
            events = new ArrayList<>();
            for (ArenaTrigger trig : triggersAtLoc) {
               for (ArenaEvent event : trig.getEvents()) {
                  if (event.getType().equals(ArenaEvent.EVENT_TYPE_NEW_MAP)) {
                     events.add(event);
                  }
               }
            }
         }
      }
      boolean showKnownButNotVisibleChars = false;
      // If we have a character within 3 hexes of us, always show them, even if they are behind us.
      if (_selfLoc != null) {
         if (ArenaCoordinates.getDistance(_selfLoc, loc) < 4) {
            showKnownButNotVisibleChars = true;
         }
      }
      List<Orientation> completionOrientations = null;
      List<Orientation> cancelOrientations = null;
      if (_movementRequest != null) {
         completionOrientations = _movementRequest.getCompletionOrientations();
         cancelOrientations = _movementRequest.getCancelOrientations();
      }
      drawHex(loc, gc, display, _sizePerHex, /*offsetX*/ /*offsetY*/ _left/*offsetCol*/, _top/*offsetRow*/,
              isMouseOver, _selfID, _targetID, _selfTeam, hexSelectable, isVisible, isKnown, /*borderFade*/
              _routeMap, _path, _mouseOverOrientations, completionOrientations, cancelOrientations,
              _mouseOverCharacter, _locationRequest, triggers, events, showKnownButNotVisibleChars, cachedColorsMap, cachedPatternMap);
   }

   public static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                              int offsetX, int offsetY)
   {
      drawHex(loc, gc, display, sizePerHex, offsetX, offsetY, (short) 0/*offsetCol*/,
              (short) 0/*offsetRow*/, false/*isMouseOver*/, -1/*selfID*/, -1/*targetID*/,
              (byte) -1/*selfTeam*/, true/*hexSelectable*/, true/*isVisible*/, true/*isKnown*/, 50,
              null/*routeMap*/, null/*path*/, null/*selectionOrientation*/,
              null/*completionOrientations*/, null/*cancelOrientations*/,
              null/*_mouseOverCharacter*/, null/*locationRequest*/,
              null/*triggers*/, null/*events*/, false/*showKnownButNotVisibleChars*/, null/*cachedColorsMap*/,
              null/*cachedPatternMap*/, 0/*rotation*/);
   }
   public static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                              int offsetX, int offsetY, double rotation)
   {
      drawHex(loc, gc, display, sizePerHex, offsetX, offsetY, (short) 0/*offsetCol*/,
              (short) 0/*offsetRow*/, false/*isMouseOver*/, -1/*selfID*/, -1/*targetID*/,
              (byte) -1/*selfTeam*/, true/*hexSelectable*/, true/*isVisible*/, true/*isKnown*/, 50,
              null/*routeMap*/, null/*path*/, null/*selectionOrientation*/,
              null/*completionOrientations*/, null/*cancelOrientations*/,
              null/*_mouseOverCharacter*/, null/*locationRequest*/,
              null/*triggers*/, null/*events*/, false/*showKnownButNotVisibleChars*/, null/*cachedColorsMap*/,
              null/*cachedPatternMap*/, rotation);
   }

   private static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                               short offsetCol, short offsetRow,
                               boolean isMouseOver, int selfID, int targetID, byte selfTeam,
                               boolean hexSelectable, boolean isVisible, boolean isKnown,
                               Map<ArenaCoordinates, ArenaCoordinates> routeMap,
                               List<ArenaCoordinates> path,
                               List<Orientation> selectionOrientations,
                               List<Orientation> completionOrientations,
                               List<Orientation> cancelOrientations,
                               Character mouseOverCharacter,
                               RequestLocation locationRequest,
                               List<ArenaTrigger> triggers,
                               List<ArenaEvent> events,
                               boolean showKnownButNotVisibleChars,
                               Map<Integer, Color> cachedColorsMap,
                               Map<Color, Pattern> cachedPatternMap)
   {
      drawHex(loc, gc, display, sizePerHex,
              0, 0, offsetCol, offsetRow,
              isMouseOver, selfID, targetID, selfTeam,
              hexSelectable, isVisible, isKnown, 90,
              routeMap,
              path,
              selectionOrientations,
              completionOrientations,
              cancelOrientations,
              mouseOverCharacter,
              locationRequest,
              triggers,
              events,
              showKnownButNotVisibleChars,
              cachedColorsMap,
              cachedPatternMap,
              0/*rotation*/);
   }

   private static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                               int offsetX, int offsetY, short offsetCol, short offsetRow,
                               boolean isMouseOver, int selfID, int targetID, byte selfTeam,
                               boolean hexSelectable, boolean isVisible, boolean isKnown, int borderFade,
                               Map<ArenaCoordinates, ArenaCoordinates> routeMap,
                               List<ArenaCoordinates> path,
                               List<Orientation> selectionOrientations,
                               List<Orientation> completionOrientations,
                               List<Orientation> cancelOrientations,
                               Character mouseOverCharacter,
                               RequestLocation locationRequest,
                               List<ArenaTrigger> triggers,
                               List<ArenaEvent> events,
                               boolean showKnownButNotVisibleChars,
                               Map<Integer, Color> cachedColorsMap,
                               Map<Color, Pattern> cachedPatternMap,
                               double rotation)
   {
      if (loc == null) {
         return;
      }
      gc.setLineStyle(SWT.LINE_SOLID);
      int[] bounds = getHexDimensions((short) (loc._x - offsetCol),
                                      (short) (loc._y - offsetRow), sizePerHex, offsetX, offsetY, true/*cacheResults*/, rotation);
      if (isMouseOver) {
         gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
         gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         gc.fillPolygon(bounds);
         gc.drawPolygon(bounds);
      }
      if (!isVisible && !isKnown) {
         gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         gc.fillPolygon(bounds);
         gc.drawPolygon(bounds);
         if (routeMap != null) {
            drawRouteLine(loc, gc, display, sizePerHex, offsetX, offsetY, offsetCol, offsetRow,
                          routeMap, path, bounds);
         }
         return;
      }
      // figure out which character to draw.
      // always draw a fighting character, if one exits (over an unconscious character)
      // and if multiple characters are fighting in this hex, draw the 'self' character.
      Character character = null;
      if (isVisible || (showKnownButNotVisibleChars /*&& isKnown*/)) { /*even if we don't know the hex, we should draw that someone is there*/
         for (Character ch : loc.getCharacters()) {
            if ((character == null) || (ch.stillFighting())) {
               character = ch;
               if (ch._uniqueID == selfID) {
                  break;
               }
            }
         }
      }
      int background = 0xffffff;
      if (character != null) {
         byte teamId = character._teamID;
         if (!character.stillFighting()) {
            background = 0x808080;
         }
         else if (teamId == Enums.TEAM_ALPHA) {
            if (character._uniqueID == selfID) {
               background = 0xffC0C0;
            }
            else if (character._uniqueID == targetID) {
               background = 0xff3030;
            }
            else {
               background = 0xC02020;
            }
         }
         else if (teamId == Enums.TEAM_BETA) {
            if (character._uniqueID == selfID) {
               background = 0xC0C0ff;
            }
            else if (character._uniqueID == targetID) {
               background = 0x3030ff;
            }
            else {
               background = 0x2020C0;
            }
         }
         else if (teamId == Enums.TEAM_INDEPENDENT) {
            if (character._uniqueID == selfID) {
               background = 0xC0ffC0;
            }
            else if (character._uniqueID == targetID) {
               background = 0x30ff30;
            }
            else {
               background = 0x20C020;
            }
         }
         else if (teamId == Enums.TEAM_UNIVERSAL) {
            background = 0xffC0C0;
         }
         else {
            background = 0xFFFFFF;
            DebugBreak.debugBreak();
         }
      }
      else {
         if ((_lineColor != null) && _line.contains(loc)) {
            background = _lineColor.red << (16 + _lineColor.green) << (8 + _lineColor.blue);
         }
         else {
            background = loc.getRGBColorAsInt();
         }
      }
      // RGB foreground = new RGB(0x80,0x80,0x80);
      if (!hexSelectable) {
         background = darkenColor(background, 44);
         //foreground = darkenColor(foreground);
      }
      int foreground = darkenColor(background, borderFade);
      if ((events != null) && (events.size() > 0)) {
         // turn this more blue
         int newRed   = (((background & 0xff0000) *3)/4) & 0xff0000;
         int newGreen = (((background & 0x00ff00) *3)/4) & 0x00ff00;
         int newBlue  = (255 - ((255 - (background & 0x0000ff))/2)) & 0x0000ff;
         background = newRed + newGreen + newBlue;
      }
      if ((triggers != null) && (triggers.size() > 0)) {
         // turn this more red
         int newRed   = (255 - (255 - ((background & 0xff0000) /2))) & 0xff0000;
         int newGreen = (((background & 0x00ff00) *3)/4) & 0x00ff00;
         int newBlue  = (((background & 0x0000ff) *3)/4) & 0x0000ff;
         background = newRed + newGreen + newBlue;
      }
      Color bgColor = null;
      Color fgColor = null;
      if (cachedColorsMap != null) {
         bgColor = cachedColorsMap.get(background);
         fgColor = cachedColorsMap.get(foreground);
         if (bgColor == null) {
            bgColor = new Color(display, getColor(background));
            cachedColorsMap.put(background, bgColor);
         }
         if (fgColor == null) {
            fgColor = new Color(display, getColor(foreground));
            cachedColorsMap.put(foreground, fgColor);
         }
      }
      else {
         bgColor = new Color(display, getColor(background));
         fgColor = new Color(display, getColor(foreground));
      }
      gc.setBackground(bgColor);
      gc.setForeground(fgColor);
      if (isVisible) {
         gc.fillPolygon(bounds);
         gc.drawPolygon(bounds);
      }
      else if (isKnown) {
         // overlay a dither pattern to grey-out the hex:
//         background = new RGB(255, 255, 255);
//         foreground = new RGB(  0,   0,   0);
//         PaletteData palette = new PaletteData(new RGB[] {background, foreground});
//         int scanlinePad = 2;
//         byte[] data = new byte[] {1, 1, 1, 1};
//         ImageData imageData = new ImageData(2/*width*/, 2/*height*/, 1/*depth*/, palette, scanlinePad, data );
//         Image dither = new Image(display, imageData);
//         gc.setBackgroundPattern(new Pattern(display, dither));
//         gc.setForegroundPattern(new Pattern(display, dither));

         Pattern notCurrentlyVisibleFillPattern = null;
         if (cachedPatternMap != null) {
            notCurrentlyVisibleFillPattern = cachedPatternMap.get(bgColor);
            if (notCurrentlyVisibleFillPattern == null) {
               notCurrentlyVisibleFillPattern = new Pattern(display, 0,0, 1,2, display.getSystemColor(SWT.COLOR_DARK_GRAY), bgColor);
               cachedPatternMap.put(bgColor, notCurrentlyVisibleFillPattern);
            }
         }
         else {
            notCurrentlyVisibleFillPattern = new Pattern(display, 0,0, 1,2, display.getSystemColor(SWT.COLOR_DARK_GRAY), bgColor);
         }

         gc.setBackgroundPattern(notCurrentlyVisibleFillPattern);
         gc.fillPolygon(bounds);
         if (cachedPatternMap == null) {
            notCurrentlyVisibleFillPattern.dispose();
         }
      }
      if (cachedColorsMap == null) {
         bgColor.dispose();
         fgColor.dispose();
      }

      if (isVisible) {
         // draw any weapons laying on the ground
         // TODO: assume the size 1/2 the hex width, but this should be an attribute of the weapon.
         int itemIndex = 0;
         synchronized (loc) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this)) {
               for (Object obj : loc.getThings()) {
                  DrawnObject drawnThing = null;
                  if (obj instanceof Thing) {
                     if (obj instanceof Limb) {
                        if (obj instanceof Hand) {
                           drawnThing = ((Limb)obj).drawThing((int) (sizePerHex/2.5), (int) (sizePerHex/2.5), new RGB(0,0,0), new RGB(192,192,192));
                        }
                        else if (obj instanceof Leg) {
                           drawnThing = ((Limb)obj).drawThing(sizePerHex/2, sizePerHex/2, new RGB(0,0,0), new RGB(192,192,192));
                        }
                        else if (obj instanceof Wing) {
                           drawnThing = ((Limb)obj).drawThing(sizePerHex/2, sizePerHex/2, new RGB(0,0,0), new RGB(192,192,192));
                        }
                     }
                     else {
                        Thing thing = (Thing) obj;
                        drawnThing = thing.drawThing(sizePerHex/3, new RGB(0,0,0), new RGB(64,64,64));
                     }
                  }
                  if (drawnThing != null) {
                     // create a pseudo-random orientation of this item, so it will always be in the random spot
                     // each time we draw it.
                     Random pseudoRandom = new Random((loc._x * 65536) + loc._y + (itemIndex++ * 1000));
                     int centerX = (bounds[0] + bounds[(2 * 3)]) / 2;
                     int centerY = (bounds[1] + bounds[(2 * 3) + 1]) / 2;
                     int width  = bounds[X_LARGEST] - bounds[X_SMALLEST];
                     int height = bounds[Y_LARGEST] - bounds[Y_SMALLEST];
                     int offX = (int) ((width/4) * pseudoRandom.nextDouble() * pseudoRandom.nextDouble());
                     int offY = (int) ((height/4) * pseudoRandom.nextDouble() * pseudoRandom.nextDouble());
                     drawnThing.rotatePoints(pseudoRandom.nextDouble() * Math.PI * 2);
                     drawnThing.offsetPoints(offX, offY);
                     drawnThing.rotatePoints(pseudoRandom.nextDouble() * Math.PI * 2);
                     drawnThing.offsetPoints(centerX, centerY);
                     drawnThing.draw(gc, display);
                  }
               }
            }
         }
      }
      int futureBackground = 0xE0E0E0;
      int futureForeground = 0x404040;
      if ((selectionOrientations != null)
               && (selectionOrientations.size() > 0)
               && isMouseOver
               && (mouseOverCharacter != null)) {
         if (mouseOverCharacter != null) {
            Orientation selectionOrientation = selectionOrientations.get(0);
//            completionOrientations = new ArrayList<>();
//            completionOrientations.add(mouseOverCharacter.getOrientation());
            boolean drawCheckMark = (completionOrientations != null) && completionOrientations.contains(selectionOrientation);
            boolean drawCancelMark = (cancelOrientations != null) && cancelOrientations.contains(selectionOrientation);
            if (drawCheckMark || drawCancelMark) {
               if (selectionOrientation.isInLocation(loc)) {
                  selectionOrientation.drawCharacter(gc, display, bounds, loc, background, foreground, mouseOverCharacter);
               }
               if (drawCheckMark) {
                  drawCheckMark(gc, display, bounds);
               }
               else {
                  drawCancelMark(gc, display, bounds);
               }
            } else {
               if (selectionOrientation.isInLocation(loc)) {
                  selectionOrientation.drawCharacter(gc, display, bounds, loc, futureBackground, futureForeground, mouseOverCharacter);
               }
            }
         }
      }
      else if (isMouseOver && (locationRequest != null)) {
         for (ArenaCoordinates answerLoc : locationRequest.getSelectableCoordinates()) {
            if (answerLoc.sameCoordinates(loc)) {
               drawImageOnHex(loc, bounds, gc, display, locationRequest.getCursorResourceName());
               break;
            }
         }
      }
      else if (character != null) {
         Orientation orient = character.getOrientation();
         if (orient != null) {
            orient.drawCharacter(gc, display, bounds, loc, background, foreground, character);
            if ((mouseOverCharacter == character) && (character.isInCoordinates(loc)) && isMouseOver) {
               drawCheckMark(gc, display, bounds);
            }
         }
      }
      if ((selectionOrientations != null) && (selectionOrientations.size() > 0)
           && !isMouseOver && (mouseOverCharacter != null)) {
         for (Orientation selectionOrientation : selectionOrientations) {
            if (selectionOrientation == null) {
               DebugBreak.debugBreak();
            }
            else if (selectionOrientation.getCoordinates().contains(loc)) {
               selectionOrientation.drawCharacter(gc, display, bounds, loc, futureBackground, futureForeground, mouseOverCharacter);
            }
         }
      }
      if (routeMap != null) {
         drawRouteLine(loc, gc, display, sizePerHex, offsetX, offsetY, offsetCol, offsetRow,
                       routeMap, path, bounds);
      }
   }
   /*
    *   10 9 8      y = lowest
    *  11     7     y = low
    *  0       6    y = middle
    *   1     5     y = high
    *    2 3 4      y = highest
    */

   private static void drawCancelMark(GC gc, Display display, int[] bounds)
   {
      Color bgColor;
      Color fgColor;
      // This is the same orientation as the characters current orientation,
      // so draw ax 'X' to indicate that if they click on this,
      // they would cancel movement, and let them choose another operation.
      int[] xMark = new int[2*8];
      // top left point
      xMark[0]   = (bounds[0] + (bounds[2 * 5  ] * 3)) / 4;
      xMark[1] = (bounds[1] + bounds[(2 * 5) + 1]) / 2;
      // top middle point
      xMark[2]   = (bounds[2 * 5 ] + bounds[2 * 4  ]) / 2;
      xMark[(2) + 1] = ((bounds[1] * 4) + bounds[(2 * 5) + 1]) / 5;
      // top right point
      xMark[2*2]   = (bounds[2*3  ]  + (bounds[2*4  ]*3))/4;
      xMark[(2*2)+1] =  xMark[1];
      // right middle point
      xMark[2*3]   = (bounds[2] + bounds[2 * 3  ]) / 2;
      xMark[(2*3)+1] =  bounds[1];
      // bottom right point
      xMark[2*4]   = xMark[2*2];
      xMark[(2*4)+1] =  (bounds[(2*2)+1] + bounds[(2*3)+1])/2;
      // bottom middle point
      xMark[2*5]   =  xMark[2];
      xMark[(2*5)+1] = ((bounds[1] * 4) + bounds[(2) + 1]) / 5;
      // bottom left point
      xMark[2*6]   = xMark[0];
      xMark[(2*6)+1] = xMark[(2*4)+1];
      // left middle point
      xMark[2*7]   = (bounds[0] + bounds[2 * 2  ]) / 2;
      xMark[(2*7)+1] =  xMark[(2*3)+1];

      bgColor = new Color(display, getColor(0xff0000)); // red
      fgColor = new Color(display, getColor(0x000000));
      gc.setBackground(bgColor);
      gc.setForeground(fgColor);
      gc.fillPolygon(xMark);
      gc.drawPolygon(xMark);
      bgColor.dispose();
      fgColor.dispose();
   }
   private static void drawCheckMark(GC gc, Display display, int[] bounds)
   {
      Color bgColor;
      Color fgColor;
      // This is the same orientation as the characters current orientation,
      // so draw a check mark or a stop sign to indicate that if they click on this,
      // they would terminate their movement.
      int[] checkMark = new int[2*4];
      // left point
      checkMark[0]   = (bounds[0] + bounds[2]) / 2;
      checkMark[1] =  bounds[1];
      // bottom point
      checkMark[2]   = ((bounds[2] * 3) + bounds[2 * 2  ]) / 4;
      checkMark[(2) + 1] = (bounds[1] + (bounds[(2) + 1] * 3)) / 4;
      // right (high) point
      checkMark[2*2]   = (bounds[2*3  ]   + bounds[2*4  ])/2;
      checkMark[(2*2)+1] = (bounds[(2*3)+1]   + bounds[(2*4)+1])/2;
      // middle point
      checkMark[2*3]   = ((bounds[2] * 3) + bounds[2 * 2  ]) / 4;
      checkMark[(2*3)+1] = ((bounds[1] * 2) + bounds[(2) + 1]) / 3;

      bgColor = new Color(display, getColor(0x00ff00)); // green
      fgColor = new Color(display, getColor(0x000000));
      gc.setBackground(bgColor);
      gc.setForeground(fgColor);
      gc.fillPolygon(checkMark);
      gc.drawPolygon(checkMark);
      bgColor.dispose();
      fgColor.dispose();
   }

   /**
    * @param loc
    * @param gc
    * @param display
    * @param sizePerHex
    * @param offsetX
    * @param offsetY
    * @param offsetCol
    * @param offsetRow
    * @param routeMap
    * @param bounds
    */
   private static void drawRouteLine(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                                     int offsetX, int offsetY, short offsetCol, short offsetRow,
                                     Map<ArenaCoordinates, ArenaCoordinates> routeMap,
                                     List<ArenaCoordinates> path, int[] bounds)
   {
      ArenaCoordinates comeFrom = routeMap.get(loc);
      if (comeFrom != null) {
         int x1 = (bounds[X_SMALLEST] + bounds[X_LARGEST]) / 2;
         int y1 = (bounds[Y_SMALLEST] + bounds[Y_LARGEST]) / 2;
         int[] frombounds = getHexDimensions((short) (comeFrom._x - offsetCol),
                                             (short) (comeFrom._y - offsetRow), sizePerHex,
                                             offsetX, offsetY, true/*cacheResults*/);
         int centerX = (frombounds[X_SMALLEST] + frombounds[X_LARGEST]) / 2;
         int centerY = (frombounds[Y_SMALLEST] + frombounds[Y_LARGEST]) / 2;
         int x2 = (x1 + centerX) / 2;
         int y2 = (y1 + centerY) / 2;
         Color fgColor = ((path!=null) && path.contains(loc)) ? new Color(display, new RGB(255, 255, 255))
                                                              : new Color(display, new RGB(200, 0, 0));
         gc.setForeground(fgColor);
         gc.drawLine(x1, y1, x2, y2);
         fgColor.dispose();
      }
   }

   @Override
   protected Canvas getCanvas() {
      return _canvas;
   }

   public static int darkenColor(int color, int fade) {
      return ((((color & 0xff0000) * fade) / 100) & 0xff0000) +
             ((((color & 0x00ff00) * fade) / 100) & 0x00ff00) +
             ((((color & 0x0000ff) * fade) / 100) & 0x0000ff);
   }
   public static RGB darkenColor(RGB color, int fade) {
      return new RGB((color.red * fade) / 100, (color.green * fade) / 100, (color.blue * fade) / 100);
   }
   public static RGB invertColor(RGB color) {
      return new RGB(255-color.red, 255-color.green, 255-color.blue);
   }

   private void labelHex(ArenaLocation loc, GC gc, Display display)
   {
      if (loc != null) {
//         boolean isVisible = ((_selfID != -1) || !_combatMap.isHideViewFromLocalPlayers()) && loc.getVisible(_selfID);
         boolean isVisible = (_selfID == -1) || loc.getVisible(_selfID);
         if (!isVisible) {
            return;
         }
         int[] bounds = getHexDimensions(loc);
         Point textStart = getTextStartPoint(bounds);
         int x = textStart.x;
         int y = textStart.y;
         List<Object> labelsAndColors = getTypeAndLabels(loc);
         while (labelsAndColors.size() > 0) {
            Object type = labelsAndColors.remove(0);
            String labelText  = (String) labelsAndColors.remove(0);
            if (type == TYPE_fighting) {
               drawOutlinedText(gc, display, x, y, labelText, SWT.COLOR_WHITE);
               y += getFontSizeByZoomLevel() + 1;
            }
            if (type == TYPE_non_fighting) {
               drawOutlinedText(gc, display, x, y, labelText, SWT.COLOR_GRAY);
               y += getFontSizeByZoomLevel() + 1;
            }
            if ((type == TYPE_thing) || (type == TYPE_string))  {
               drawOutlinedText(gc, display, x, y, labelText, SWT.COLOR_GREEN);
               y += getFontSizeByZoomLevel() + 1;
            }
            if ((type == TYPE_label) || (type == TYPE_event) || (type == TYPE_trigger)) {
               gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
               gc.drawText(labelText, x, y, true/*transparent background*/);
               y += getFontSizeByZoomLevel() + 1;
            }
         }
      }
   }

   private static Point getTextStartPoint(int[] bounds) {
      return new Point(((bounds[X_SMALLEST] + bounds[Y_SMALLEST - 1]) - 1) / 2,
                       ((bounds[Y_SMALLEST] + bounds[X_SMALLEST + 1]) - 1) / 2);
   }

   private static void drawOutlinedText(GC gc, Display display, int x, int y, String text, int swtColorIDForeground) {
      gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
      for (int xOff=-1 ; xOff<=1 ; xOff++) {
         for (int yOff=-1 ; yOff<=1 ; yOff++) {
            if ((xOff != 0) || (yOff != 0)) {
               gc.drawText(text, x+xOff, y+yOff, true/*transparent background*/);
            }
         }
      }
      gc.setForeground(display.getSystemColor(swtColorIDForeground));
      gc.drawText(text, x, y, true/*transparent background*/);
   }

   public void drawZoomControls(GC gc)
   {
      org.eclipse.swt.graphics.Point bottomRightPoint = this._canvas.getSize();
      ZOOM_BUTTON_RADIUS = ZOOM_CONTROL_IMAGE_DATA.height / 2;
      int leftEdge = bottomRightPoint.x - ZOOM_CONTROL_IMAGE_DATA.width - 10;
      int topEdge = bottomRightPoint.y - ZOOM_CONTROL_IMAGE_DATA.height - 10;
      int rightEdge = leftEdge + ZOOM_CONTROL_IMAGE_DATA.width;
      int buttonCenterY = topEdge + ZOOM_BUTTON_RADIUS;
      ZOOM_OUT_BUTTON_CENTER = new Point(leftEdge + ZOOM_BUTTON_RADIUS, buttonCenterY);
      ZOOM_IN_BUTTON_CENTER = new Point(rightEdge - ZOOM_BUTTON_RADIUS, buttonCenterY);
      ZOOM_RESET_BUTTON_CENTER = new Point((leftEdge + rightEdge) / 2, buttonCenterY);

      gc.drawImage(ZOOM_CONTROL_IMAGE, leftEdge, topEdge);
   }
   public static void drawWall(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                               int offsetX, int offsetY, short offsetCol, short offsetRow, int selfID,
                               boolean hideViewFromLocalPlayers)
   {
      drawWall(loc, gc, display, sizePerHex, offsetX, offsetY, offsetCol, offsetRow, selfID, hideViewFromLocalPlayers, 0.0/*rotation*/);
   }

   public static void drawWall(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                               int offsetX, int offsetY, short offsetCol, short offsetRow, int selfID,
                               boolean hideViewFromLocalPlayers, double rotation) {
//      boolean isVisible = ((selfID != -1) || !hideViewFromLocalPlayers) && loc.getVisible(_selfID);
      boolean isVisible = (selfID == -1) || loc.getVisible(selfID);
      if (!isVisible && !loc.isKnownBy(selfID)) {
         return;
      }

      int[] hexBounds = getHexDimensions((short) (loc._x - offsetCol),
                                         (short) (loc._y - offsetRow), sizePerHex, offsetX, offsetY, true/*cacheResults*/, rotation);
      /*
       *   10 9 8      y = lowest
       *  11     7     y = low
       *  0       6    y = middle
       *   1     5     y = high
       *    2 3 4      y = highest
       */

      List<Door> doors = new ArrayList<>(loc.getDoors());
      long walls = loc.getWalls();
      if ((walls == 0) && (doors.size() == 0)) {
         // no walls or doors in this hex.
         // But there might be spells to draw:
         drawSpells(loc, hexBounds, gc, display);
         return;
      }
      Color oldBg = gc.getBackground();
      Color oldFg = gc.getForeground();

      if (walls != 0) {
         // When selfID == -1, we are drawing the wall buttons.
         // In this case, we never want to draw the hidden area 'behind' a wall.
         if (selfID != -1) {
            Map<Facing, Boolean> vis = getVisibilityOfAdjacentHexes(loc, selfID);
/*            boolean ext[] = getArrayOfShouldWallsExtendIntoAdjacentHexes(loc, selfID);

            gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            for (int[] element : wallLocs) {
               if ((element[0] & walls) != 0) {
                  int fillPoint = computeFillPoint(ext, element[0]);
                  if (fillPoint != -1) {
                     drawHidden(hexBounds, gc, element[1], element[2], fillPoint);
                  }
               }
            }
*/
            gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
            gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
            for (TerrainWall terrainWall : TerrainWall.values()) {
               if (terrainWall.contains(walls)) {
                  int fillPoint = computeFillPoint(vis, terrainWall);
                  if (fillPoint != -1) {
                     drawHidden(hexBounds, gc, terrainWall.startPoint, terrainWall.endPoint, fillPoint);
                  }
               }
            }
            for (Door door : doors) {
               int fillPoint = computeFillPoint(vis, door._orientation);
               if (!door.isOpen()) {
                  drawHidden(hexBounds, gc, door._orientation.startPoint, door._orientation.endPoint, fillPoint);
               }
            }
         }
      }
      gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
      for (TerrainWall terrainWall : TerrainWall.values()) {
         if (terrainWall.contains(walls)) {
            drawWall(hexBounds, gc, terrainWall.startPoint, terrainWall.endPoint, terrainWall.thickness);
            // Are there more than one walls in this hex?
            if ((terrainWall.bitMask ^ walls) != 0) {
               // Yes, there are. Find all combinations of the current wall
               // with each of the OTHER walls in this hex, and get the
               // polygon that is the joined shape
               for (TerrainWall terrainWall2 : TerrainWall.values()) {
                  if (terrainWall != terrainWall2) {
                     if (terrainWall2.contains(walls)) {
                        int[] polygon = MapWidget.getPolygonForTwoWalls(terrainWall, terrainWall2);
                        if (polygon != null) {
                           drawPolygons(hexBounds, gc, polygon);
                        }
                     }
                  }
               }
            }
         }
      }

      gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_RED));
      gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_RED));
      for (Door door : doors) {
         drawDoor(hexBounds, gc, door._orientation.startPoint, door._orientation.endPoint, door._orientation.thickness, door.isOpen());
      }
      drawSpells(loc, hexBounds, gc, display);
      gc.setBackground(oldBg);
      gc.setForeground(oldFg);
   }

   private static void drawImageOnHex(ArenaLocation loc, int[] hexBounds,
                                      GC gc, Display display, String imageResourceName) {
      if (imageResourceName != null) {
         int x1 = getPoint(hexBounds, 10,  true/*getXValue*/);
         int y1 = getPoint(hexBounds, 10, false/*getXValue*/);
         int x2 = getPoint(hexBounds,  4,  true/*getXValue*/);
         int y2 = getPoint(hexBounds,  4, false/*getXValue*/);
         /*
          *   10 9 8      y = lowest
          *  11     7     y = low
          *  0       6    y = middle
          *   1     5     y = high
          *    2 3 4      y = highest
          */
         ImageData imageData = getImageDataResourceByName(imageResourceName);
         gc.drawImage(getImageResourceByName(imageResourceName, display),
                      0/*srcX*/, 0/*srcY*/, imageData.width/*srcWidth*/, imageData.height/*srcHeight*/,
                      x1/*dstX*/, y1/*dstY*/, (x2-x1)/*dstWidth*/, (y2-y1)/*dstHeight*/);
      }
   }
   private static void drawSpells(ArenaLocation loc, int[] hexBounds, GC gc, Display display) {
      for (IAreaSpell spell : loc.getActiveSpells()) {
         drawImageOnHex(loc, hexBounds, gc, display, spell.getImageResourceName());
      }
   }

   private static final Map<String, ImageData> imageDataByName = new HashMap<>();
   private static final Map<String, Image>     imageByName     = new HashMap<>();
   private static ImageData getImageDataResourceByName(String name) {
      if (imageDataByName.get(name) == null) {
         imageDataByName.put(name, getImageData(name));
      }
      return imageDataByName.get(name);
   }
   private static Image getImageResourceByName(String name, Display display) {
      if (imageByName.get(name) == null) {
         imageByName.put(name, new Image(display, getImageDataResourceByName(name)));
      }
      return imageByName.get(name);
   }

   private static int computeFillPoint(Map<Facing, Boolean> visPerDirection, TerrainWall wall) {
      if (wall.contains((TerrainWall.VERT_LEFT.with(TerrainWall.VERT_CENTER.with(TerrainWall.VERT_RIGHT))))) {
         if (visPerDirection.get(Facing._2_OCLOCK) != visPerDirection.get(Facing._10_OCLOCK)) {
            return visPerDirection.get(Facing._2_OCLOCK) ? 0 : 6;
         }
         if (visPerDirection.get(Facing._4_OCLOCK) != visPerDirection.get(Facing._8_OCLOCK)) {
            return visPerDirection.get(Facing._4_OCLOCK) ? 0 : 6;
         }
         if (visPerDirection.get(Facing._4_OCLOCK) != visPerDirection.get(Facing._10_OCLOCK)) {
            return visPerDirection.get(Facing._4_OCLOCK) ? 0 : 6;
         }
         if (visPerDirection.get(Facing._2_OCLOCK) != visPerDirection.get(Facing._8_OCLOCK)) {
            return visPerDirection.get(Facing._2_OCLOCK) ? 0 : 6;
         }
      }
      else if (wall.contains(TerrainWall.HORIZONTAL_TOP.with(TerrainWall.HORIZONTAL_CENTER.with(TerrainWall.HORIZONTAL_BOTTOM)))) {
         if (visPerDirection.get(Facing.NOON) != visPerDirection.get(Facing._6_OCLOCK)) {
            return visPerDirection.get(Facing.NOON) ? 3 : 9;
         }
      }
      else if (wall.contains(TerrainWall.DIAG_LEFT_LEFT.with(TerrainWall.DIAG_LEFT_CENTER.with(TerrainWall.DIAG_LEFT_RIGHT)))) {
         if (visPerDirection.get(Facing._2_OCLOCK) != visPerDirection.get(Facing._8_OCLOCK)) {
            return visPerDirection.get(Facing._2_OCLOCK) ? 1 : 7;
         }
      }
      else if (wall.contains(TerrainWall.DIAG_RIGHT_LEFT.with(TerrainWall.DIAG_RIGHT_CENTER.with(TerrainWall.DIAG_RIGHT_RIGHT)))) {
         if (visPerDirection.get(Facing._4_OCLOCK) != visPerDirection.get(Facing._10_OCLOCK)) {
            return visPerDirection.get(Facing._4_OCLOCK) ? 11 : 5;
         }
      }
      else if (wall.contains(TerrainWall.DIAG_FAR_RIGHT_LEFT.with(TerrainWall.DIAG_FAR_RIGHT_CENTER.with(TerrainWall.DIAG_FAR_RIGHT_RIGHT)))) {
         if (visPerDirection.get(Facing.NOON) != visPerDirection.get(Facing._6_OCLOCK)) {
            return visPerDirection.get(Facing.NOON) ? 4 : 10;
         }
         if (visPerDirection.get(Facing._4_OCLOCK) != visPerDirection.get(Facing._10_OCLOCK)) {
            return visPerDirection.get(Facing._4_OCLOCK) ? 10 : 4;
         }
         if (visPerDirection.get(Facing._4_OCLOCK) != visPerDirection.get(Facing.NOON)) {
            return visPerDirection.get(Facing._4_OCLOCK) ? 10 : 4;
         }
         if (visPerDirection.get(Facing._6_OCLOCK) != visPerDirection.get(Facing._10_OCLOCK)) {
            return visPerDirection.get(Facing._6_OCLOCK) ? 10 : 4;
         }
      }
      else if (wall.contains(TerrainWall.DIAG_FAR_LEFT_LEFT.with(TerrainWall.DIAG_FAR_LEFT_CENTER.with(TerrainWall.DIAG_FAR_LEFT_RIGHT)))) {
         if (visPerDirection.get(Facing.NOON) != visPerDirection.get(Facing._6_OCLOCK)) {
            return visPerDirection.get(Facing.NOON) ? 2 : 8;
         }
         if (visPerDirection.get(Facing._8_OCLOCK) != visPerDirection.get(Facing._2_OCLOCK)) {
            return visPerDirection.get(Facing._8_OCLOCK) ? 8 : 2;
         }
         if (visPerDirection.get(Facing._8_OCLOCK) != visPerDirection.get(Facing.NOON)) {
            return visPerDirection.get(Facing._8_OCLOCK) ? 8 : 2;
         }
         if (visPerDirection.get(Facing._6_OCLOCK) != visPerDirection.get(Facing._2_OCLOCK)) {
            return visPerDirection.get(Facing._6_OCLOCK) ? 8 : 2;
         }
      }
      return -1;
   }

   private static Map<Facing, Boolean> getVisibilityOfAdjacentHexes(ArenaCoordinates fromCoord, int viewerId) {
      Map<Facing, Boolean> isKnown = new HashMap<>();
      for (Facing dir : Facing.values()) {
         isKnown.put(dir, false);
      }
      if (CombatServer._this != null) {
         IMapWidget map = CombatServer._this._map;
         if (map != null) {
            CombatMap combatMap = map.getCombatMap();
            if (combatMap != null) {
               for (Facing dir : Facing.values()) {
                  short x = (short) (dir.moveX + fromCoord._x);
                  short y = (short) (dir.moveY + fromCoord._y);
                  ArenaLocation fromLoc = combatMap.getLocation(x, y);
                  if ((fromLoc != null) && (fromLoc.getVisible(viewerId))) {
                     isKnown.put(dir, true);
                  }
               }
            }
         }
      }
      return isKnown;
   }

/*   private static boolean[] getArrayOfShouldWallsExtendIntoAdjacentHexes(ArenaCoordinates fromCoord, int viewerId) {
      boolean[] dontExtendWalls = new boolean[6];
      IMapWidget map = CombatServer._this._map;
      if (map != null) {
         CombatMap combatMap = map.getCombatMap();
         if (combatMap != null) {
            ArenaLocation fromLoc = combatMap.getLocation(fromCoord);
            if (fromLoc != null) {
               for (byte dir=Facing.NOON ; dir<=Facing._10_OCLOCK ; dir++) {
                  short x = (short) (ArenaCoordinates.getXMoveForDirection(dir) + fromCoord._x);
                  short y = (short) (ArenaCoordinates.getYMoveForDirection(dir) + fromCoord._y);
                  ArenaLocation toLoc = combatMap.getLocation(x, y);
                  dontExtendWalls[dir] = (toLoc != null) && toLoc.canExit(fromCoord);
//                           && (ArenaLocation.canMoveBetween(fromLoc, toLoc, false/*blockByCharacters* /));
               }
            }
         }
      }
      return dontExtendWalls;
   }
*/

   private static void drawWall(int[] hexBounds, GC gc, int pointA, int pointB, int width)
   {
      double wallWidth = width / 10.0;
      int[] wallBounds = new int[12];
      get3points(hexBounds, pointA, wallWidth, wallBounds, 0/*startAt*/);
      get3points(hexBounds, pointB, wallWidth, wallBounds, 6/*startAt*/);
      gc.fillPolygon(wallBounds);
      gc.drawPolygon(wallBounds);
   }
   private static void drawPolygons(int[] hexBounds, GC gc, int[] points)
   {
      int[] polygonBounds = new int[points.length * 2];
      int i=0;
      for (int point : points) {
         polygonBounds[i++] = getPoint(hexBounds, point, true/*getXValue*/);
         polygonBounds[i++] = getPoint(hexBounds, point, false/*getYValue*/);
      }
      gc.fillPolygon(polygonBounds);
      gc.drawPolygon(polygonBounds);
   }

//   private static void drawWall(int[] hexBounds, Display display, GC gc, int pointA, int pointB,
//                                int width, int fillPoint)
//   {
//      drawWall(hexBounds, gc, pointA, pointB, width);
//      if (fillPoint != -1) {
//         Color oldBg = gc.getBackground();
//         Color oldFg = gc.getForeground();
//         gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
//         gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
//         int[] shadowBounds = new int[6];
//         shadowBounds[0] = hexBounds[fillPoint];
//         shadowBounds[1] = hexBounds[(fillPoint + 1) % 12];
//         shadowBounds[2] = hexBounds[pointA];
//         shadowBounds[3] = hexBounds[(pointA + 1) % 12];
//         shadowBounds[4] = hexBounds[pointB];
//         shadowBounds[5] = hexBounds[(pointB + 1) % 12];
//         gc.fillPolygon(shadowBounds);
//         gc.drawPolygon(shadowBounds);
//         gc.setBackground(oldBg);
//         gc.setForeground(oldFg);
//      }
//   }

   private static void drawHidden(int[] hexBounds, GC gc, int pointA, int pointB, int fillPoint)
   {
      // Check for the simple condition where the wall is on an edge, and the fill point is on the wall.
      // In this case, we don't want to hide any of the hex.
      int distFillToA = Math.abs(pointA - fillPoint);
      int distFillToB = Math.abs(pointB - fillPoint);
      int distAToB = Math.abs(pointB - pointA);
      if (distAToB > 6) {
         distAToB = 12 - distAToB;
      }
      if (distFillToA > 6) {
         distFillToA = 12 - distFillToA;
      }
      if (distFillToB > 6) {
         distFillToB = 12 - distFillToB;
      }
      if (distAToB == 2) {
         // Are we look at the back-side of this wall?
         if ((distFillToA < 5) && (distFillToB < 5)) {
            return;
         }
      }
      if (distAToB == 4) {
         // Are we looking parallel to this wall?
         if ((distFillToA > 4) || (distFillToB > 4)) {
            return;
         }
      }
      int[] shadowBounds = getShadowBounds(hexBounds, pointA, pointB, fillPoint);
      gc.fillPolygon(shadowBounds);
      gc.drawPolygon(shadowBounds);
   }

   private static int[] getShadowBounds(int[] hexBounds, int pointA, int pointB, int fillPoint)
   {
      boolean clockwise = false;
      for (int i = 1; i < 12; i++) {
         if (((pointA + i) % 12) == fillPoint) {
            clockwise = true;
            break;
         }
         if (((pointA + i) % 12) == pointB) {
            break;
         }
      }
      List<Integer> points = new ArrayList<>();
      int direction = 1;
      if (!clockwise) {
         direction = 11;
      }
      for (int point = pointA; point != pointB; point = (point + direction) % 12) {
         points.add(point);
      }
      points.add(pointB);
      int[] shadowBounds = new int[points.size() * 2];
      int i = 0;
      for (Integer point : points) {
         int val = point;
         shadowBounds[i++] = getPoint(hexBounds, val, true/*getXValue*/);
         shadowBounds[i++] = getPoint(hexBounds, val, false/*getXValue*/);
      }
      return shadowBounds;
   }


   private static void drawDoor(int[] hexBounds, GC gc, int pointA, int pointB, int width, boolean isOpen) {
      double wallWidth = width / 10.0;
      int[] wallBounds1 = new int[10];
      int[] wallBounds2 = new int[10];
      get3points(hexBounds, pointA, wallWidth, wallBounds1, 0/*startAt*/);
      get3points(hexBounds, pointB, wallWidth, wallBounds2, 0/*startAt*/);
      wallBounds1[6] = (int) Math.round((wallBounds1[4] * (1 - 0.25)) + (wallBounds2[0] * (0.25)));
      wallBounds1[7] = (int) Math.round((wallBounds1[5] * (1 - 0.25)) + (wallBounds2[1] * (0.25)));
      wallBounds1[8] = (int) Math.round((wallBounds1[0] * (1 - 0.25)) + (wallBounds2[4] * (0.25)));
      wallBounds1[9] = (int) Math.round((wallBounds1[1] * (1 - 0.25)) + (wallBounds2[5] * (0.25)));

      wallBounds2[6] = (int) Math.round((wallBounds2[4] * (1 - 0.25)) + (wallBounds1[0] * (0.25)));
      wallBounds2[7] = (int) Math.round((wallBounds2[5] * (1 - 0.25)) + (wallBounds1[1] * (0.25)));
      wallBounds2[8] = (int) Math.round((wallBounds2[0] * (1 - 0.25)) + (wallBounds1[4] * (0.25)));
      wallBounds2[9] = (int) Math.round((wallBounds2[1] * (1 - 0.25)) + (wallBounds1[5] * (0.25)));

      gc.fillPolygon(wallBounds1);
      gc.drawPolygon(wallBounds1);
      gc.fillPolygon(wallBounds2);
      gc.drawPolygon(wallBounds2);

      if (!isOpen) {
         int x1 = (wallBounds1[0] + wallBounds1[2] + wallBounds1[4])/3;
         int y1 = (wallBounds1[1] + wallBounds1[3] + wallBounds1[5])/3;
         int x2 = (wallBounds2[0] + wallBounds2[2] + wallBounds2[4])/3;
         int y2 = (wallBounds2[1] + wallBounds2[3] + wallBounds2[5])/3;
         gc.drawLine(wallBounds1[2], wallBounds1[3], wallBounds2[2], wallBounds2[3]);
         gc.drawLine(x1, y1, x2, y2);
      }
   }

   private static void get3points(int[] hexBounds, int point, double wallWidth, int[] results, int startAt) {
      // always return them in clockwise order
      results[startAt] = splitPoints(hexBounds, point, (point + 11) % 12, wallWidth, true/*getXValue*/);
      results[startAt + 1] = splitPoints(hexBounds, point, (point + 11) % 12, wallWidth, false/*getXValue*/);
      results[startAt + 2] = getPoint(hexBounds, point, true/*getXValue*/);
      results[startAt + 3] = getPoint(hexBounds, point, false/*getXValue*/);
      results[startAt + 4] = splitPoints(hexBounds, point, (point + 1) % 12, wallWidth, true/*getXValue*/);
      results[startAt + 5] = splitPoints(hexBounds, point, (point + 1) % 12, wallWidth, false/*getXValue*/);
   }

   private static int splitPoints(int[] hexBounds, int pointA, int pointB, double percentToB, boolean getXValue) {
      if ((percentToB < 0) || (percentToB > 1.0)) {
         DebugBreak.debugBreak();
         throw new IllegalArgumentException();
      }

      int a = getPoint(hexBounds, pointA, getXValue);
      int b = getPoint(hexBounds, pointB, getXValue);
      return (int) Math.round((a * (1 - percentToB)) + (b * (percentToB)));
   }

   private static int getPoint(int[] hexBounds, int pointA, boolean getXValue) {
      if ((pointA % 2) == 0) {
         return hexBounds[pointA + (getXValue ? 0 : 1)];
      }
      return splitPoints(hexBounds, (pointA + 11) % 12, (pointA + 1) % 12, .50, getXValue);
   }

   @Override
   public void redraw() {
      if (!_canvas.isDisposed()) {
         _canvas.redraw();
      }
   }
   public void redraw(Collection<ArenaCoordinates> coordinates) {
      if (!_canvas.isDisposed() && !_canvas.getDisplay().isDisposed()) {
         int minX = 10000;
         int minY = 10000;
         int maxX = 0;
         int maxY = 0;
         if (_combatMap != null) {
            for (ArenaCoordinates coord : coordinates) {
               ArenaLocation loc = _combatMap.getLocation(coord);
               if (loc == null) {
                  continue;
               }
               int[] bounds = getHexDimensions(loc);
               if (minX > bounds[X_SMALLEST]) {
                  minX = bounds[X_SMALLEST];
               }
               if (minY > bounds[Y_SMALLEST]) {
                  minY = bounds[Y_SMALLEST];
               }
               if (maxX < bounds[X_LARGEST]) {
                  maxX = bounds[X_LARGEST];
               }
               if (maxY < bounds[Y_LARGEST]) {
                  maxY = bounds[Y_LARGEST];
               }

               // Now check for any text that must be written to this hex,
               // and ensure that region is also covered:
               List<Object> typeAndLabels = getTypeAndLabels(loc);
               int maxWidthInCharacters = 0;
               int height = 0;

               for (Object obj : typeAndLabels) {
                  if (obj instanceof String) {
                     height += 10;
                     maxWidthInCharacters = Math.max(maxWidthInCharacters, ((String)obj).length());
                  }
               }
               if (maxWidthInCharacters > 0) {
                  // If we are displaying any text, its possible that someone moved
                  // And if they moved, we need to redraw the same area needed by this
                  // location to each of the surrounding hexes, since he came from one
                  // one of those hexes, but we don't know which one.
                  maxY += bounds[Y_LARGEST] - bounds[Y_SMALLEST];
                  maxX += bounds[X_LARGEST] - bounds[X_SMALLEST];
               }
               Point textStart = getTextStartPoint(bounds);
               int maxTextX = textStart.x + (maxWidthInCharacters * 12);
               int maxTextY = textStart.y + height;

               if (maxX < maxTextX) {
                  maxX = maxTextX;
               }
               if (maxY < maxTextY) {
                  maxY = maxTextY;
               }
            }
         }
         maxX++;
         maxY++;
         _canvas.redraw(minX, minY, (maxX - minX)/*width*/, (maxY - minY)/*height*/, true/*all(paint children in section)*/);
      }
   }

   public void redraw(int x, int y, int width, int height, boolean all)
   {
      _canvas.redraw(x, y, width, height, all);
   }


   @Override
   protected short getTopmostVisibleRow() {
      return _top;
   }
   @Override
   protected short getLeftmostVisibleColumn() {
      return _left;
   }

   public int[] getHexDimensions(ArenaCoordinates coord) {
      if (coord == null) {
         DebugBreak.debugBreak();
      }
      return getHexDimensions((short) (coord._x - _left), (short) (coord._y - _top), _sizePerHex, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
   }
   public int[] getHexDimensions(short column, short row, boolean cacheResults) {
      return getHexDimensions((short) (column - _left), (short) (row - _top), _sizePerHex, 0/*offsetX*/, 0/*offsetY*/, cacheResults);
   }

   /*
    *   5 4
    *  0   3
    *   1 2
    */
   private static final Map<Integer, Map<Short, Map<Short, int[]>>> MAP_SIZE_TO_MAP_ROW_TO_MAP_COLUMN_TO_HEX_DIMS = new HashMap<>();
   public static int[] getHexDimensions(short column, short row, int sizePerHex, int offsetX, int offsetY, boolean cacheResults, double rotation) {
      int[] baseHexDimensions = getHexDimensions(column, row, sizePerHex, offsetX, offsetY, cacheResults);
      if (rotation == 0) {
         return baseHexDimensions;
      }
      int[] rotatedHexDimensions = new int[12];
      double aveX = (baseHexDimensions[0] + baseHexDimensions[2] + baseHexDimensions[4] + baseHexDimensions[6] + baseHexDimensions[8] + baseHexDimensions[10]) / 6.0;
      double aveY = (baseHexDimensions[1] + baseHexDimensions[3] + baseHexDimensions[5] + baseHexDimensions[7] + baseHexDimensions[9] + baseHexDimensions[11]) / 6.0;
      for (int i=0 ; i<6 ; i++) {
         double x = baseHexDimensions[i*2]     - aveX;
         double y = baseHexDimensions[(i*2) + 1] - aveY;
         rotatedHexDimensions[i*2]     = (int) Math.round(( (x * Math.cos(rotation)) + (y * Math.sin(rotation))) + aveX);
         rotatedHexDimensions[(i*2) + 1] = (int) Math.round(((-x * Math.sin(rotation)) + (y * Math.cos(rotation))) + aveY);
      }
      return rotatedHexDimensions;
   }
   public static int[] getHexDimensions(short column, short row, int sizePerHex, int offsetX, int offsetY, boolean cacheResults)
   {
      Map<Short, Map<Short, int[]>> mapRowToMapColumnToHexDims = MAP_SIZE_TO_MAP_ROW_TO_MAP_COLUMN_TO_HEX_DIMS.get(sizePerHex);
      if (mapRowToMapColumnToHexDims == null) {
         mapRowToMapColumnToHexDims = new HashMap<>();
         if (cacheResults) {
            MAP_SIZE_TO_MAP_ROW_TO_MAP_COLUMN_TO_HEX_DIMS.put(sizePerHex, mapRowToMapColumnToHexDims);
         }
      }
      Map<Short, int[]> mapColumnToHexDims = mapRowToMapColumnToHexDims.get(row);
      if (mapColumnToHexDims == null) {
         mapColumnToHexDims = new HashMap<>();
         if (cacheResults) {
            mapRowToMapColumnToHexDims.put(row, mapColumnToHexDims);
         }
      }
      int[] hexDims = mapColumnToHexDims.get(column);
      if (hexDims == null) {
         double[] hexDimensions = getHexBaseDimensions(sizePerHex);
         double sizeX = sizePerHex;
         double sizeY = (sizePerHex * 71d) / 81; // set aspect ratio
         double locX = sizeX * ((column *  0.75) + .5);
         double locY = ((row+1) * sizeY) / 2;
         hexDims = new int[] { (int) Math.round(hexDimensions[0] + locX), (int) Math.round(hexDimensions[1] + locY),
                               (int) Math.round(hexDimensions[2] + locX), (int) Math.round(hexDimensions[3] + locY),
                               (int) Math.round(hexDimensions[4] + locX), (int) Math.round(hexDimensions[5] + locY),
                               (int) Math.round(hexDimensions[6] + locX), (int) Math.round(hexDimensions[7] + locY),
                               (int) Math.round(hexDimensions[8] + locX), (int) Math.round(hexDimensions[9] + locY),
                               (int) Math.round(hexDimensions[10]+ locX), (int) Math.round(hexDimensions[11]+ locY) };
         if (cacheResults) {
            mapColumnToHexDims.put(column, hexDims);
         }
      }
      if ((offsetX != 0) && (offsetY != 0)) {
         return new int[] { hexDims[0]  + offsetX, hexDims[1] + offsetY,
                            hexDims[2]  + offsetX, hexDims[3] + offsetY,
                            hexDims[4]  + offsetX, hexDims[5] + offsetY,
                            hexDims[6]  + offsetX, hexDims[7] + offsetY,
                            hexDims[8]  + offsetX, hexDims[9] + offsetY,
                            hexDims[10] + offsetX, hexDims[11] + offsetY};
      }
      if (offsetY != 0) {
         return new int[] { hexDims[0],  hexDims[1] + offsetY,
                            hexDims[2],  hexDims[3] + offsetY,
                            hexDims[4],  hexDims[5] + offsetY,
                            hexDims[6],  hexDims[7] + offsetY,
                            hexDims[8],  hexDims[9] + offsetY,
                            hexDims[10], hexDims[11] + offsetY};
      }
      if (offsetX != 0) {
         return new int[] { hexDims[0]  + offsetX, hexDims[1],
                            hexDims[2]  + offsetX, hexDims[3],
                            hexDims[4]  + offsetX, hexDims[5],
                            hexDims[6]  + offsetX, hexDims[7],
                            hexDims[8]  + offsetX, hexDims[9],
                            hexDims[10] + offsetX, hexDims[11]};
      }
      return hexDims;
   }

   private static final Map<Integer, double[]> MAP_OF_HEX_SIZES_TO_BASE_DIMENSIONS = new HashMap<>();
   public static double[] getHexBaseDimensions(int sizePerHex) {
      double[] results = MAP_OF_HEX_SIZES_TO_BASE_DIMENSIONS.get(sizePerHex);
      if (results == null) {
         double sizeX = sizePerHex;
         double sizeY = (sizePerHex * 71d) / 81; // set aspect ratio
         double halfY = sizeY / 2;
         double halfX = sizeX / 2;
         double quartX = sizeX / 4;
         results = new double[] {  -halfX, 0,      // leftmost point  0
                                  -quartX, halfY,  // left-bottom     1
                                   quartX, halfY,  // right-bottom    2
                                    halfX, 0,      // rightmost       3
                                   quartX, -halfY, // right-top       4
                                  -quartX, -halfY, // left-top        5
                                };
         MAP_OF_HEX_SIZES_TO_BASE_DIMENSIONS.put(sizePerHex, results);
      }
      return results;
   }

   public final static int X_SMALLEST = 0;
   public final static int X_LARGEST  = 6;
   public final static int Y_SMALLEST = 9;
   public final static int Y_LARGEST  = 3;

   public double getAngleFromCenter(ArenaLocation loc, int x, int y) {
      return getMeasurementFromCenter(loc, x, y, true/*angle*/);
   }
   public double getDistanceFromCenter(ArenaLocation loc, int x, int y) {
      return getMeasurementFromCenter(loc, x, y, false/*angle*/);
   }
   public double getMeasurementFromCenter(ArenaLocation loc, int x, int y, boolean angle) {
      int[] bounds = getHexDimensions(loc);
      int centerX = (bounds[0] + bounds[3 * 2]) / 2;
      int centerY = (bounds[1] + bounds[(3 * 2) + 1]) / 2;
      // normalize the distances, so a 1 means at the edge or vertex
      double yDist = ((double)(y - centerY)) / (bounds[(5*2)+1] - centerY);
      double xDist = ((double)(x - centerX)) / (bounds[0] - centerX);

      if (angle) {
         return Math.atan2(yDist, xDist);
      }
      return Math.sqrt((yDist*yDist) + (xDist*xDist));
   }
   public ArenaLocation findLoc(int x, int y)
   {
      if (_combatMap != null) {
         for (short col = _left; (col < _combatMap.getSizeX()); col++) {
            // get the first item in this column
            ArenaLocation loc = _combatMap.getLocation(col, (short) (col % 2));
            int[] bounds = getHexDimensions(loc);
            if ((bounds[X_SMALLEST] <= x) && (bounds[X_LARGEST] >= x)) {
               for (short row = _top; (row < _combatMap.getSizeY()); row++) {
                  if ((row % 2) != (col % 2)) {
                     continue;
                  }
                  loc = _combatMap.getLocation(col, row);
                  bounds = getHexDimensions(loc);
                  if ((bounds[Y_SMALLEST] <= y) && (bounds[Y_LARGEST] >= y)) {
                     if ((bounds[2] <= x) && (bounds[4] >= x)) {
                        return loc;
                     }
                     // flip the coordinates around so we only need to compare the point
                     // against the line between bounds[0,1] & bounds[10,11]
                     int centerX = (bounds[2] + bounds[4]) / 2;
                     int centerY = bounds[1];
                     int newX = (x > bounds[4]) ? (2 * centerX) - x : x;
                     int newY = (y > bounds[1]) ? (2 * centerY) - y : y;
                     // slope-intercept formula: y = Mx + b
                     double slope = ((double) (bounds[11] - bounds[1])) / (bounds[10] - bounds[0]); // slope = rise / over
                     double b = bounds[1] - (slope * bounds[0]); // b = y - Mx
                     double interceptY = (slope * newX) + b;
                     if (newY > interceptY) {
                        return loc;
                     }
                  }
               }
            }
         }
      }
      return null;
   }

   private void resetOnNewZoomLevel(Point mouseCenter) {
      if (_zoom >= _maxZoom) {
         _zoom = (short) (_maxZoom - 1);
      }
      if (_zoom < 0) {
         _zoom = 0;
      }
      int oldSizePerHex = _sizePerHex;
      if (mouseCenter == null) {
         mouseCenter = new Point(_widgetWidth /2, _widgetHeight/2);
      }
      ArenaLocation oldLoc = findLoc(mouseCenter.x, mouseCenter.y);

      _sizePerHex = _sizePerHexForZoom[_zoom];

      if (oldSizePerHex != _sizePerHex) {
         int oldHexesWide = _widgetWidth / oldSizePerHex;
         int oldHexesTall = (_widgetHeight * 160) / (oldSizePerHex * 66);
         int newHexesWide = _widgetWidth / _sizePerHex;
         int newHexesTall = (_widgetHeight * 160) / (_sizePerHex * 66);
         _left -= (newHexesWide - oldHexesWide) / 2;
         _top -= (newHexesTall - oldHexesTall) / 2;
      }
      if (_top < 0) {
         _top = 0;
      }
      if (_left < 0) {
         _left = 0;
      }

      if (_combatMap != null) {
         ArenaLocation rightMostLoc = _combatMap.getLocation((short) (_combatMap.getSizeX() - 1),
                                                             (short) ((_combatMap.getSizeX() - 1) % 2));
         ArenaLocation bottomMostLoc = _combatMap.getLocation((short) 1,
                                                              (short) (_combatMap.getSizeY() - 1));
         int[] testDimensions = getHexDimensions(rightMostLoc);
         while ((testDimensions[X_LARGEST] < (_widgetWidth - _sizePerHex)) && (_left > 0)) {
            _left--;
            testDimensions = getHexDimensions(rightMostLoc);
         }
         testDimensions = getHexDimensions(bottomMostLoc);
         while ((testDimensions[Y_LARGEST] < (_widgetHeight - _sizePerHex)) && (_top > 0)) {
            _top--;
            testDimensions = getHexDimensions(bottomMostLoc);
         }
         ArenaLocation newLoc = findLoc(mouseCenter.x, mouseCenter.y);
         if ((newLoc != null) && (oldLoc != null)) {
            _left = (short) Math.max((_left - (newLoc._x - oldLoc._x)), 0);
            _top  = (short) Math.max((_top  - (newLoc._y - oldLoc._y)), 0);
         }
      }
      redraw();
   }

   @Override
   //* return true if the map changed, and needs to be redrawn*/
   protected boolean centerOnSelf() {
      if (_selfLoc == null) {
         return false;
      }
      short oldTop = _top;
      short oldLeft = _left;
      int hexesWide = _widgetWidth / _sizePerHex;
      int hexesTall = (_widgetHeight * 160) / (_sizePerHex * 66);
      _left = (short) (_selfLoc._x - (hexesWide / 2));
      _top = (short) (_selfLoc._y - (hexesTall / 2));

      if (_top < 0) {
         _top = 0;
      }
      if (_left < 0) {
         _left = 0;
      }

      ArenaLocation rightMostLoc = _combatMap.getLocation((short) (_combatMap.getSizeX() - 1),
                                                          (short) ((_combatMap.getSizeX() - 1) % 2));
      ArenaLocation bottomMostLoc = _combatMap.getLocation((short) 1,
                                                           (short) (_combatMap.getSizeY() - 1));
      int[] testDimensions = getHexDimensions(rightMostLoc);
      while ((testDimensions[X_LARGEST] < (_widgetWidth - _sizePerHex)) && (_left > 0)) {
         _left--;
         testDimensions = getHexDimensions(rightMostLoc);
      }
      testDimensions = getHexDimensions(bottomMostLoc);
      while ((testDimensions[Y_LARGEST] < (_widgetHeight - _sizePerHex)) && (_top > 0)) {
         _top--;
         testDimensions = getHexDimensions(bottomMostLoc);
      }

      if ((oldTop == _top) && (oldLeft == _left)) {
         // nothing changed
         return false;
      }

      redraw();
      return true;
   }

   private static final Map<Integer, RGB> PALLET = new HashMap<>();
   public static RGB getColor(int colorAsint) {
      RGB color = PALLET.get(colorAsint);
      if (color == null) {
         color = new RGB((colorAsint & 0xff0000) >> 16,
                         (colorAsint & 0x00ff00) >> 8,
                         (colorAsint & 0x0000ff) >> 0);
         PALLET.put(colorAsint, color);
      }
      return color;
   }

   @Override
   public void applyAnimations() {
   }

}
