/*
 * Created on May 24, 2006
 */
package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MapWidget2D extends MapWidget implements Listener, SelectionListener
{
   static final int         MIN_SIZE_PER_HEX         = 8;
   private int              _sizePerHex              = 30;
   private int              _widgetWidth             = -1;
   private int              _widgetHeight            = -1;

   private final Canvas     _canvas;

   private static final ImageData ZOOM_CONTROL_IMAGE_DATA;
   private static       Image     ZOOM_CONTROL_IMAGE       = null;

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


   static class BackgroundImageInfo {
      private int   _alpha      = 0;
      private Image _image      = null;
      private int   _sizePerHex = 0;
      private float  _stretchFactorX = 0f;
      private float  _stretchFactorY = 0f;
      private String _imagePath      = "";
      private short  _mapSizeX       = 0;
      private short  _mapSizeY       = 0;
      private CombatMap _map = null;
      private void setInfo(int sizePerHex, CombatMap map, Display display) {
         if ((_sizePerHex == sizePerHex) && (_map == map) &&
             (map == null || _imagePath.endsWith(map.getBackgroundImagePath())) &&
             (map == null || _alpha == map.getBackgroundImageAlpha()) &&
             (map == null || _mapSizeX == map.getSizeX()) &&
             (map == null || _mapSizeY == map.getSizeY())
         ) {
            return;
         }
         _sizePerHex = sizePerHex;
         _mapSizeX = (map == null) ? 0 : map.getSizeX();
         _mapSizeY = (map == null) ? 0 : map.getSizeY();
         _map = map;
         String mapBGImagePath = (map == null) ? "" : map.getBackgroundImagePath();
         _alpha = (map == null) ? 0 : map.getBackgroundImageAlpha();
         if (mapBGImagePath == null) {
            mapBGImagePath = "";
         }
         if (!_imagePath.equalsIgnoreCase(mapBGImagePath)) {
            if (_image != null) {
               _image.dispose();
               _image = null;
            }
            _imagePath = mapBGImagePath;
            if (!mapBGImagePath.isEmpty()) {
               String baseDir = System.getProperty("user.dir");
               String fileSeparator = System.getProperty("file.separator");
               baseDir = baseDir + fileSeparator + "arenas";
               File image = new File(_imagePath);
               if (!image.exists()) {
                  _imagePath = baseDir + fileSeparator + _imagePath;
                  image = new File(_imagePath);
               }
               if (image.exists()) {
                  _image = new Image(display, _imagePath);
               }
            }
         }

         if (_map != null && _image != null) {
            // subtract 1 from the map size, because a map of size 2x2 has (0,0), (0,1), (1,0), (1,1)
            int[] bottomRightBounds = getHexDimensions((short)(_mapSizeX-1), (short) (_mapSizeY - 1), _sizePerHex, 0, 0, false/*cacheResults*/);
            _stretchFactorX = ((float) _image.getBounds().width) / bottomRightBounds[X_LARGEST];
            _stretchFactorY = ((float) _image.getBounds().height) / bottomRightBounds[Y_LARGEST];
         }
      }

      public boolean isActive() {
         return _alpha > 0 && _image != null;
      }
   }
   private static final BackgroundImageInfo BACKGROUND_IMAGE_INFO = new BackgroundImageInfo();

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
   public void setBackgroundAlpha(int alpha) {
      BACKGROUND_IMAGE_INFO._alpha = alpha;
      redraw();
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
            int[] bounds = getHexDimensions((short)(_combatMap.getSizeX()-1), (short) (_combatMap.getSizeY() - 1), false/*cacheResults*/);
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
   static boolean oneShot = false;
   @Override
   public void handleEvent(Event event)
   {
      if (event.type == SWT.Paint) {
         long start = System.currentTimeMillis();
         if (_resizeOnFirstDraw) {
            setZoomToFit();
         }
         if (_combatMap != null) {
            if (oneShot) {
               return;
            }
            Display display = event.display;
            Image image = new Image(display, _canvas.getBounds());
            Font font = new Font(display, "", getFontSizeByZoomLevel(), SWT.BOLD);

            CombatMap combatMap = null;
            IMapWidget map = CombatServer._this._map;
            if (map != null) {
               combatMap = map.getCombatMap();
            }
            BACKGROUND_IMAGE_INFO.setInfo(_sizePerHex, combatMap, display);

            // Setup an off-screen GC, onto which all drawing is done.
            // This will later be transferred to the events CG (real screen)
            // This allows double-buffering of the image, to reduce flashing in any animation
            GC gcImage = new GC(image);
            gcImage.setFont(font);

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
               short minCol = (short) Math.max(((event.x / colWidth) + _left) - 1, 0);
               short maxCol = (short) Math.min(Math.round(((event.x + event.width) / colWidth)  + _left+ 1),
                                               _combatMap.getSizeX()-1);
               short minRow = (short) Math.max(((event.y / rowHeight) + _top) -1, 0);
               short maxRow = (short) Math.min(Math.round(((event.y + event.height) / rowHeight) + _top + 1),
                                               _combatMap.getSizeY()-1);

               Map<Integer, Color> cachedColorsMap = new HashMap<>();
               Map<Color, Pattern> cachedPatternMap = new HashMap<>();

               boolean isHideViewFromLocalPlayers = _combatMap.isHideViewFromLocalPlayers();
               if (BACKGROUND_IMAGE_INFO._image != null) {
                  // Collect the set of ArenaLocations that are known, and that are visible
                  List<ArenaLocation> knownLocs = new ArrayList<>();
                  List<ArenaLocation> visibleLocs = new ArrayList<>();
                  for (short col = minCol; col <= maxCol; col++) {
                     short row = minRow;
                     if ((row % 2) != (col % 2)) {
                        row++;
                     }
                     for (; row <= maxRow; row += 2) {
                        // only redraw hexes that are in the redraw-area.
                        // Since all we care about is the y coordinated of these hexes,
                        // we cache them in the 'locAtRow' array, so we can re-use
                        // them on subsequent columns
                        ArenaLocation loc = _combatMap.getLocationQuick(col, row);
                        if ((_selfID == -1) || loc.getVisible(_selfID)) {
                           visibleLocs.add(loc);
                        }
                        else if (loc.isKnownBy(_selfID)) {
                           visibleLocs.add(loc);
                           knownLocs.add(loc);
                        }
                     }
                  }
                  //int previousAlpha = event.gc.getAlpha();
                  //event.gc.setAlpha(BACKGROUND_IMAGE_INFO._alpha);
                  drawBackground(visibleLocs, BACKGROUND_IMAGE_INFO._image, _sizePerHex, gcImage, event, display, combatMap);
                  //event.gc.setAlpha(previousAlpha);
               }
               for (short col = minCol; col <= maxCol; col++) {
                  short row = minRow;
                  if ((row % 2) != (col % 2)) {
                     row++;
                  }
                  for (; row <= maxRow; row += 2) {
                     // only redraw hexes that are in the redraw-area.
                     // Since all we care about is the y coordinated of these hexes,
                     // we cache them in the 'locAtRow' array, so we can re-use
                     // them on subsequent columns
                     ArenaLocation loc = _combatMap.getLocationQuick(col, row);
                     drawHex(loc, gcImage, display, cachedColorsMap, cachedPatternMap);
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

               // Draw the off-screen buffer to the screen
               //System.out.println("drawImage at 365 (off screen to on)");
               event.gc.drawImage(image, event.x/*srcX*/, event.y/*srcY*/, event.width/*srcWidth*/, event.height/*srcHeight*/,
                                         event.x/*dstX*/, event.y/*dstY*/, event.width/*dstWidth*/, event.height/*dstHeight*/);
               drawZoomControls(event.gc);

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
         long stop = System.currentTimeMillis();
         //System.out.println("paint took "  + (stop-start) + "ms.");
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
         resetCurson(event);
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
//               else {
               Orientation futureOrientation = _movementRequest.getBestFutureOrientation(loc, angle, distance);
               if (futureOrientation != null) {
                  _mouseOverOrientations = _movementRequest.getRouteToFutureOrientation(futureOrientation);
               }
//               }
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
            if ((_mouseOverCharacter != null) && (_mouseOverOrientations != null) && (!_mouseOverOrientations.isEmpty())) {
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
            List<ArenaCoordinates> locationsToRedraw = new ArrayList<>();
            locationsToRedraw.add(_mouseOverLocation);
            boolean redrawSurroundHexes = false;
            if (redrawSurroundHexes && (loc != null)) {
               for (Facing dir : Facing.values()) {
                  ArenaLocation adjLoc = _combatMap.getLocation((short)(loc._x + dir.moveX), (short)(loc._y + dir.moveY));
                  locationsToRedraw.add(adjLoc);
               }
            }
            _mouseOverLocation = loc;
            if (_mouseOverLocation != null) {
               if ((event.stateMask & SWT.BUTTON1) != 0) {
                  for (IMapListener listener : _listeners) {
                     listener.onMouseDrag(_mouseOverLocation, event, angle, distance);
                  }
               }
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
                        locationsToRedraw.addAll(oldOrientation.getCoordinates());
                     }
                  }
               }
               if (_mouseOverOrientations != null) {
                  for (Orientation mouseOverOrientation : _mouseOverOrientations) {
                     if (mouseOverOrientation == null) {
                        DebugBreak.debugBreak();
                     }
                     else {
                        locationsToRedraw.addAll(mouseOverOrientation.getCoordinates());
                     }
                  }
               }
            }
            locationsToRedraw.removeIf(o-> o==null);
            if (!locationsToRedraw.isEmpty()) {
               int minX = 10000;
               int minY = 10000;
               int maxX = 0;
               int maxY = 0;
               for (ArenaCoordinates locationToRedraw : locationsToRedraw) {
                  int[] bounds = getHexDimensions(locationToRedraw);
                  minX = Math.min(minX, bounds[X_SMALLEST]);
                  minY = Math.min(minY, bounds[Y_SMALLEST]);
                  maxX = Math.max(maxX, bounds[X_LARGEST]);
                  maxY = Math.max(maxY, bounds[Y_LARGEST]);
               }

               redraw(minX, minY, (maxX - minX), (maxY - minY), true);
            }
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
               MapMode oldMapMode = _mapMode;
               if (event.button == 1) {
                  listener.onMouseUp(loc, event, angle, distance);
               }
               else if (event.button == 3) {
                  listener.onRightMouseUp(loc, event, angle, distance);
               }
               if (oldMapMode != _mapMode) {
                  resetCurson(event);
               }
            }
         }
      }
   }

   private void resetCurson(Event event) {
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

   static List<List<Integer>> computeRegions(List<? extends ArenaCoordinates> locs, int sizePerHex, short left, short top) {
      List<List<Integer>> regions = new ArrayList<>();
      for (ArenaCoordinates loc : locs) {
         int[] locBounds = getHexDimensions((short) (loc._x - left), (short)(loc._y - top), sizePerHex, 0, 0, false);
         List<Integer> locPoints = new ArrayList<>();
         int x = 0;
         int y = 0;
         for (int i=0 ; i<6 ; i++) {
            // If the line clips the 0-line, clip the line where it meets the border.
            int nx = locBounds[i*2];
            int ny = locBounds[i * 2 + 1];
            float dist = 1.0f;
            if (nx < 0) {
               dist = x / (x - nx);
            }
            if (ny < 0) {
               float distY = y / (y - ny);
               dist = Math.min(distY, dist);
            }
            if (dist != 1.0f) {
               nx = Math.round(dist * nx / (x-nx));
               ny = Math.round(dist * ny / (y-ny));
            }
            locPoints.add(Integer.valueOf(nx * 10_000 + ny));
            x = nx;
            y = ny;
         }
         if (!combineRegions(regions, locPoints)) {
            regions.add(locPoints);
         }
      }
      return regions;
   }

   private static boolean combineRegions(List<List<Integer>> regions, List<Integer> newRegion) {
      boolean foundRegion = false;
      for (List<Integer> region : regions) {
         regionloop:
         for (int i=0 ; i< region.size() ; i++ ) {
            Integer pointI = region.get(i);
            int newRegionSize = newRegion.size();
            for (int j = 0; j < newRegionSize; j++) {
               Integer newPointJ = newRegion.get(j);
               if (pointI.equals(newPointJ)) {
                  // assume all points are listed in counter-clockwise order
                  Integer newPointJ_minus = (j>0) ? newRegion.get(j-1) : newRegion.get(newRegionSize-1);
                  Integer pointI_plus = (i<(region.size()-1) ? region.get(i+1) : region.get(0));
                  if (pointI_plus.equals(newPointJ_minus)) {
                     for (int k = 1; k<(newRegionSize - 1) ; k++) {
                        region.add(i+k, newRegion.get((j+k) % newRegionSize));
                     }
                     reduceRegion(region, i + newRegionSize - 1);
                     newRegion.clear();
                     foundRegion = true;
                     // treat the merged-into region as the 'newRegion', to see if we can combine this now larger
                     // region with any other existing regions
                     newRegion = region;
                     break regionloop; // break out of the i loop, to look at the next region
                  }
               }
            }
         }
      }
      if (foundRegion) {
         regions.removeIf(region->region.isEmpty());
      }
      return foundRegion;
   }

   private static void reduceRegion(List<Integer> region, int i) {
      while (true) {
         Integer before = (i>0) ? region.get(i-1) : region.get(region.size()-1);
         Integer after = (i<(region.size()-1) ? region.get(i+1) : region.get(0));
         if (!before.equals(after)) {
            return;
         }
         region.remove(i);
         region.remove(i);
         i--;
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
      boolean isVisible = (_selfID == -1) || loc.getVisible(_selfID);
      boolean isKnown = (_selfID == -1) || loc.isKnownBy(_selfID);
      boolean hexSelectable = true;
      if (_selectableHexes != null) {
         hexSelectable = loc.getSelectable();
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
                  _eventsMap.computeIfAbsent(trigLoc, k -> new ArrayList<>()).add(trig);
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
      boolean showKnownButNotVisibleChars = (_selfLoc != null) &&
                                            (ArenaCoordinates.getDistance(_selfLoc, loc) <= Rules.AUTO_VISIBLE_DISTANCE);
      List<Orientation> completionOrientations = null;
      List<Orientation> cancelOrientations = null;
      if (_movementRequest != null) {
         completionOrientations = _movementRequest.getCompletionOrientations();
         cancelOrientations = _movementRequest.getCancelOrientations();
      }
      drawHex(loc, gc, display, _sizePerHex,
              0/*offsetX*/, 0/*offsetY*/, _left/*offsetCol*/, _top/*offsetRow*/,
              isMouseOver, _selfID, _targetID,
              hexSelectable, isVisible, isKnown, 10 ,
              _routeMap, _path, _mouseOverOrientations, completionOrientations, cancelOrientations,
              _mouseOverCharacter, _locationRequest, triggers, events,
              showKnownButNotVisibleChars, cachedColorsMap, cachedPatternMap, 0);
   }

   public static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                              int offsetX, int offsetY)
   {
      drawHex(loc, gc, display, sizePerHex, offsetX, offsetY, (short) 0/*offsetCol*/,
              (short) 0/*offsetRow*/, false/*isMouseOver*/, -1/*selfID*/, -1/*targetID*/,
              true/*hexSelectable*/, true/*isVisible*/, true/*isKnown*/, 50,
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
              true/*hexSelectable*/, true/*isVisible*/, true/*isKnown*/, 50,
              null/*routeMap*/, null/*path*/, null/*selectionOrientation*/,
              null/*completionOrientations*/, null/*cancelOrientations*/,
              null/*_mouseOverCharacter*/, null/*locationRequest*/,
              null/*triggers*/, null/*events*/, false/*showKnownButNotVisibleChars*/, null/*cachedColorsMap*/,
              null/*cachedPatternMap*/, rotation);
   }

   private static void drawHex(ArenaLocation loc, GC gc, Display display, int sizePerHex,
                               int offsetX, int offsetY, short offsetCol, short offsetRow,
                               boolean isMouseOver, int selfID, int targetID,
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
                                      (short) (loc._y - offsetRow), sizePerHex, offsetX, offsetY, true, rotation);
      if (!isVisible && !isKnown) {
         gc.setBackground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         //System.out.println("drawPolygon at 859: " + loc._x + "," + loc._y + " (unknown hex)");
         gc.fillPolygon(bounds);
         gc.drawPolygon(bounds);
         drawRouteLine(loc, gc, display, sizePerHex, offsetX, offsetY, offsetCol, offsetRow, routeMap, path, bounds);
         return;
      }
      if (isMouseOver) {
         gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
         gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
         int previousAlpha = gc.getAlpha();
         gc.setAlpha(128);
         //System.out.println("fillPolygon at 870: " + loc._x + "," + loc._y +" (mouseover)");
         gc.fillPolygon(bounds);
         gc.drawPolygon(bounds);
         gc.setAlpha(previousAlpha);
      }

      // If we dont have any watchers, then this location is not on the real map
      // it's probably a hex being draw on a button, so it doesn't need to draw
      // brackgrounds, contents, character, route lines, etc.
      boolean locOnMap = !loc.getSnapShotOfWatchers().isEmpty();

      // figure out which character to draw.
      // always draw a fighting character, if one exits (over an unconscious character)
      // and if multiple characters are fighting in this hex, draw the 'self' character.
      Character character = null;
      if (isVisible || showKnownButNotVisibleChars) { /*even if we don't know the hex, we should draw that someone is there*/
         for (Character ch : loc.getCharacters()) {
            if ((character == null) || (ch.stillFighting())) {
               character = ch;
               if (ch._uniqueID == selfID) {
                  break;
               }
            }
         }
      }
      int alertLayer = 0;
      int alertLayerAlpha = 0;
      int characterColor = 0;
      if (character != null) {
         characterColor = getBackgroundForCharacter(selfID, targetID, character);
         alertLayer = characterColor;
         alertLayerAlpha = 128;
      }

      int shadowAlpha = 0;
      int alpha;
      int background;
      if ((_lineColor != null) && _line.contains(loc)) {
         background = _lineColor.red << (16 + _lineColor.green) << (8 + _lineColor.blue);
         alpha = 192;
      }
      else {
         background = loc.getRGBColorAsInt();
         alpha = 255;
         if (!isVisible) {
            shadowAlpha = 140;
         }
      }
      // RGB border = new RGB(0x80,0x80,0x80);
      if (!hexSelectable) {
         shadowAlpha = 255 - (255 - shadowAlpha) * (255 - 100) / 255;
      }
      //background = darkenColor(background, shadowAlpha);
      int border = darkenColor(background, borderFade);

      if (locOnMap) {
         // When using backgrounds, always overlay the map with black hex borders
         border = darkenColor(border, (BACKGROUND_IMAGE_INFO._alpha * 100 / 255));
      }

      int alertRed   = alertLayer & 0xff0000 >> 16;
      int alertGreen = alertLayer & 0x00ff00 >> 8;
      int alertBlue  = alertLayer & 0x0000ff;
      if ((events != null) && (!events.isEmpty())) {
         // turn this more blue
         int newRed   = alertRed * 3/4;
         int newGreen = alertGreen * 3/4;
         int newBlue  = (alertBlue + 255) / 2;
         alertLayer = (newRed << 16) + (newGreen << 8) + newBlue;
         alertLayerAlpha = (alertLayerAlpha == 0) ? 100 : 180;
      }
      if ((triggers != null) && (!triggers.isEmpty())) {
         // turn this more red
         int newRed   = (alertRed + 255) / 2;
         int newGreen = alertGreen * 3/4;
         int newBlue  = alertBlue * 3/4;
         alertLayer = (newRed << 16) + (newGreen << 8) + newBlue;
         alertLayerAlpha = (alertLayerAlpha == 0) ? 102 : (alertLayer == 100) ? 190 : 220;
      }

      {
         Color bgColor;
         Color borderColor;
         Color alertLayerColor = null;
         if (cachedColorsMap != null) {
            bgColor = cachedColorsMap.get(background);
            if (bgColor == null) {
               bgColor = new Color(display, getColor(background));
               cachedColorsMap.put(background, bgColor);
            }
            borderColor = cachedColorsMap.get(border);
            if (borderColor == null) {
               borderColor = new Color(display, getColor(border));
               cachedColorsMap.put(border, borderColor);
            }
            if (alertLayerAlpha > 0) {
               alertLayerColor = cachedColorsMap.get(alertLayer);
               if (alertLayerColor == null) {
                  alertLayerColor = new Color(display, getColor(alertLayer));
                  cachedColorsMap.put(alertLayer, alertLayerColor);
               }
            }
         } else {
            bgColor = new Color(display, getColor(background));
            borderColor = new Color(display, getColor(border));
            if (alertLayerAlpha > 0) {
               alertLayerColor = new Color(display, getColor(alertLayer));
            }
         }
         gc.setBackground(bgColor);
         gc.setForeground(borderColor);
         int previousAlpha = gc.getAlpha();
         int newAlpha = (255 - BACKGROUND_IMAGE_INFO._alpha) * alpha / 255;
         if (locOnMap) {
            gc.setAlpha(newAlpha);
         }
         //System.out.println("fillPolygon at 1002: " + loc._x + "," + loc._y + " (not visible)");
         gc.drawPolygon(bounds);
         gc.fillPolygon(bounds);

         if (locOnMap && (shadowAlpha > 0)) {
            // for hexes that need to be darker when we are using background images,
            // we draw a semi-transparent dark gray overlay on top of it
            gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setAlpha(shadowAlpha);
            gc.fillPolygon(bounds);
         }
         if (isKnown && locOnMap) {
            drawWall(loc, gc, display, sizePerHex, 0, 0, offsetCol, offsetRow, selfID, false, 0.0);
         }

         if (alertLayerAlpha > 0) {
            try {
               gc.setBackground(alertLayerColor);
               gc.setForeground(alertLayerColor);
            } catch (Exception e) {
               e.printStackTrace();
            }
            gc.setAlpha(alertLayerAlpha);
            gc.fillPolygon(bounds);
         }
         // draw the hex borders an alpha that is relative to the size per hex, smaller hexes get less alpha
         gc.setAlpha(Math.min(sizePerHex * 3, 255));
         gc.drawPolygon(bounds);

         gc.setAlpha(previousAlpha);

         if (cachedColorsMap == null) {
            bgColor.dispose();
            borderColor.dispose();
            if (alertLayerColor != null) {
               alertLayerColor.dispose();
            }
         }
      }
      if (!locOnMap) {
         return;
      }

      if (isVisible) {
         // draw any weapons laying on the ground
         drawHexThings(loc, gc, display, sizePerHex, bounds);
      }
      int futureBackground = 0xE0E0E0;
      int futureForeground = 0x404040;
      if ((selectionOrientations != null) &&
          !selectionOrientations.isEmpty() &&
          isMouseOver &&
          (mouseOverCharacter != null)) {
         Orientation selectionOrientation = selectionOrientations.get(0);
         boolean drawCheckMark = (completionOrientations != null) && completionOrientations.contains(selectionOrientation);
         boolean drawCancelMark = (cancelOrientations != null) && cancelOrientations.contains(selectionOrientation);
         if (drawCheckMark || drawCancelMark) {
            if (selectionOrientation.isInLocation(loc)) {
               selectionOrientation.drawCharacter(gc, display, bounds, loc, background, border, mouseOverCharacter);
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
            orient.drawCharacter(gc, display, bounds, loc, characterColor, border, character);
            if ((mouseOverCharacter == character) && (character.isInCoordinates(loc)) && isMouseOver) {
               drawCheckMark(gc, display, bounds);
            }
         }
      }
      if ((selectionOrientations != null) &&
          !selectionOrientations.isEmpty() &&
          !isMouseOver &&
          (mouseOverCharacter != null)) {
         for (Orientation selectionOrientation : selectionOrientations) {
            if ((selectionOrientation != null) && (selectionOrientation.getCoordinates().contains(loc))) {
               selectionOrientation.drawCharacter(gc, display, bounds, loc, futureBackground, futureForeground, mouseOverCharacter);
            }
         }
      }
      drawRouteLine(loc, gc, display, sizePerHex, offsetX, offsetY, offsetCol, offsetRow, routeMap, path, bounds);
   }

   private static int getBackgroundForCharacter(int selfID, int targetID, Character character) {
      byte teamId = character._teamID;
      if (!character.stillFighting()) {
         return 0x808080;
      }
      if (teamId == Enums.TEAM_ALPHA) {
         if (character._uniqueID == selfID) {
            return 0xffC0C0;
         }
         if (character._uniqueID == targetID) {
            return 0xff3030;
         }
         return 0xC02020;
      }
      if (teamId == Enums.TEAM_BETA) {
         if (character._uniqueID == selfID) {
            return 0xC0C0ff;
         }
         if (character._uniqueID == targetID) {
            return 0x3030ff;
         }
         return 0x2020C0;
      }
      if (teamId == Enums.TEAM_INDEPENDENT) {
         if (character._uniqueID == selfID) {
            return 0xC0ffC0;
         }
         if (character._uniqueID == targetID) {
            return 0x30ff30;
         }
         return 0x20C020;
      }
      if (teamId == Enums.TEAM_UNIVERSAL) {
         return 0xffC0C0;
      }
      DebugBreak.debugBreak();
      return 0xFFFFFF;
   }

   private static void drawHexThings(ArenaLocation loc, GC gc, Display display, int sizePerHex, int[] bounds) {
      // TODO: assume the size 1/2 the hex width, but this should be an attribute of the weapon.
      int itemIndex = 0;
      synchronized (loc) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this)) {
            for (Object obj : loc.getThings()) {
               if (obj instanceof Thing) {
                  DrawnObject drawnThing = null;
                  if (obj instanceof Limb) {
                     if (obj instanceof Hand) {
                        drawnThing = ((Limb) obj).drawThing((int) (sizePerHex / 2.5), (int) (sizePerHex / 2.5), new RGB(0, 0, 0), new RGB(192, 192, 192));
                     } else if (obj instanceof Leg) {
                        drawnThing = ((Limb) obj).drawThing(sizePerHex / 2, sizePerHex / 2, new RGB(0, 0, 0), new RGB(192, 192, 192));
                     } else if (obj instanceof Wing) {
                        drawnThing = ((Limb) obj).drawThing(sizePerHex / 2, sizePerHex / 2, new RGB(0, 0, 0), new RGB(192, 192, 192));
                     }
                  } else {
                     Thing thing = (Thing) obj;
                     drawnThing = thing.drawThing(sizePerHex / 3, new RGB(0, 0, 0), new RGB(64, 64, 64));
                  }
                  if (drawnThing != null) {
                     // create a pseudo-random orientation of this item, so it will always be in the random spot
                     // each time we draw it.
                     Random pseudoRandom = new Random((loc._x * 65536) + loc._y + (itemIndex++ * 1000));
                     int centerX = (bounds[0] + bounds[(2 * 3)]) / 2;
                     int centerY = (bounds[1] + bounds[(2 * 3) + 1]) / 2;
                     int width = bounds[X_LARGEST] - bounds[X_SMALLEST];
                     int height = bounds[Y_LARGEST] - bounds[Y_SMALLEST];
                     int offX = (int) ((width / 4) * pseudoRandom.nextDouble() * pseudoRandom.nextDouble());
                     int offY = (int) ((height / 4) * pseudoRandom.nextDouble() * pseudoRandom.nextDouble());
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
   }

   private void drawBackground(List<ArenaLocation> locs, Image image, int sizePerHex,
                                      GC gcImage, Event event, Display display, CombatMap combatMap) {
      if (locs == null || locs.isEmpty()) {
         return;
      }
      List<List<Integer>> regions = computeRegions(locs, sizePerHex, _left, _top);
      Short minX = locs.get(0)._x; ArenaLocation minXLoc = locs.get(0);
      Short maxX = locs.get(0)._x; ArenaLocation maxXLoc = locs.get(0);
      Short minY = locs.get(0)._y; ArenaLocation minYLoc = locs.get(0);
      Short maxY = locs.get(0)._y; ArenaLocation maxYLoc = locs.get(0);
      for (ArenaLocation loc : locs) {
         if (minX > loc._x) { minX = loc._x; minXLoc = loc; }
         if (minY > loc._y) { minY = loc._y; minYLoc = loc; }
         if (maxX < loc._x) { maxX = loc._x; maxXLoc = loc; }
         if (maxY < loc._y) { maxY = loc._y; maxYLoc = loc; }
      }

      int[] unAdjustedBoundsMinX  = getHexDimensions(minXLoc._x, minXLoc._y, sizePerHex, 0, 0, false/*cacheResults*/);
      int[] unAdjustedBoundsMinY  = getHexDimensions(minYLoc._x, minYLoc._y, sizePerHex, 0, 0, false/*cacheResults*/);
      int[] unAdjustedBoundsMaxX  = getHexDimensions(maxXLoc._x, maxXLoc._y, sizePerHex, 0, 0, false/*cacheResults*/);
      int[] unAdjustedBoundsMaxY  = getHexDimensions(maxYLoc._x, maxYLoc._y, sizePerHex, 0, 0, false/*cacheResults*/);
      int[] adjustedBoundsMinX  = getHexDimensions((short)(minXLoc._x - _left), (short)(minXLoc._y - _top), sizePerHex, 0, 0, false/*cacheResults*/);
      int[] adjustedBoundsMinY  = getHexDimensions((short)(minYLoc._x - _left), (short)(minYLoc._y - _top), sizePerHex, 0, 0, false/*cacheResults*/);
      int[] adjustedBoundsMaxX  = getHexDimensions((short)(maxXLoc._x - _left), (short)(maxXLoc._y - _top), sizePerHex, 0, 0, false/*cacheResults*/);
      int[] adjustedBoundsMaxY  = getHexDimensions((short)(maxYLoc._x - _left), (short)(maxYLoc._y - _top), sizePerHex, 0, 0, false/*cacheResults*/);
      int mapSpaceHexXmin = (int) Math.round(unAdjustedBoundsMinX[X_SMALLEST] * BACKGROUND_IMAGE_INFO._stretchFactorX);
      int mapSpaceHexYmin = (int) Math.round(unAdjustedBoundsMinY[Y_SMALLEST] * BACKGROUND_IMAGE_INFO._stretchFactorY);
      int mapSpaceHexXmax = (int) Math.round(unAdjustedBoundsMaxX[X_LARGEST] * BACKGROUND_IMAGE_INFO._stretchFactorX);
      int mapSpaceHexYmax = (int) Math.round(unAdjustedBoundsMaxY[Y_LARGEST] * BACKGROUND_IMAGE_INFO._stretchFactorY);
      int srcX = mapSpaceHexXmin;
      int srcY = mapSpaceHexYmin;
      int srcWidth = mapSpaceHexXmax - mapSpaceHexXmin;
      int srcHeight = mapSpaceHexYmax - mapSpaceHexYmin;

      int dstX = adjustedBoundsMinX[X_SMALLEST];
      int dstY = adjustedBoundsMinY[Y_SMALLEST];
      int dstWidth = adjustedBoundsMaxX[X_LARGEST] - dstX;
      int dstHeight = adjustedBoundsMaxY[Y_LARGEST] - dstY;

      for (List<Integer> region : regions) {
         Path clippingPath = new Path(display);
         for (Integer point : region) {
            clippingPath.lineTo(point / 10_000, point % 10_000);
         }
         // tie it back to the first point
         clippingPath.lineTo(region.get(0) / 10_000, region.get(0) % 10_000);
         gcImage.setClipping(clippingPath);
         //System.out.println("drawImage at 1231 (BG)");
         gcImage.drawImage(image, srcX, srcY, srcWidth, srcHeight,
                                  dstX, dstY, dstWidth, dstHeight);
         clippingPath.dispose();
      }
      gcImage.setClipping((Path)null);
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
      ArenaCoordinates comeFrom = (routeMap == null) ? null : routeMap.get(loc);
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

   public static int darkenColor(int color, int fadePercent) {
      return ((((color & 0xff0000) * (100 - fadePercent)) / 100) & 0xff0000) +
             ((((color & 0x00ff00) * (100 - fadePercent)) / 100) & 0x00ff00) +
             ((((color & 0x0000ff) * (100 - fadePercent)) / 100) & 0x0000ff);
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
         while (!labelsAndColors.isEmpty()) {
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

      //System.out.println("drawImage at 1439 (zoom)");
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
               if (!door.isOpen() && !door.isHalfHeightWall()) {
                  int fillPoint = computeFillPoint(vis, door._orientation);
                  int previousAlpha = gc.getAlpha();
                  gc.setAlpha(255);
                  drawHidden(hexBounds, gc, door._orientation.startPoint, door._orientation.endPoint, fillPoint);
                  gc.setAlpha(previousAlpha);
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
                     if ((terrainWall != terrainWall2) && (terrainWall2.contains(walls))) {
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

      Color shortWallColor = new Color(display, getColor(0x555555));
      Color doorColor = display.getSystemColor(SWT.COLOR_DARK_RED);
      Color currentColor = null;
      for (int d=0 ; d<doors.size() ; d++) {
         Door door = doors.get(d);
         if (door.isHalfHeightWall()) {
            if (currentColor != shortWallColor) {
               gc.setBackground(shortWallColor);
               gc.setForeground(shortWallColor);
               currentColor = shortWallColor;
            }
            drawWall(hexBounds, gc, door._orientation.startPoint, door._orientation.endPoint, door._orientation.thickness);
            for (int d2=0 ; d2<doors.size() ; d2++) {
               if (d2 != d) {
                  Door door2 = doors.get(d2);
                  if (door2.isHalfHeightWall()) {
                     int[] polygon = MapWidget.getPolygonForTwoWalls(door._orientation, door2._orientation);
                     if (polygon != null) {
                        drawPolygons(hexBounds, gc, polygon);
                     }
                  }
               }
            }
            // If this hex has a full wall, and a half wall, connect the half wall to each full wall
            for (TerrainWall terrainWall2 : TerrainWall.values()) {
               if ((door._orientation != terrainWall2) && (terrainWall2.contains(walls))) {
                  int[] polygon = MapWidget.getPolygonForTwoWalls(door._orientation, terrainWall2);
                  if (polygon != null) {
                     drawPolygons(hexBounds, gc, polygon);
                  }
               }
            }
         }
         else {
            if (currentColor != doorColor) {
               gc.setBackground(doorColor);
               gc.setForeground(doorColor);
               currentColor = doorColor;
            }
            drawDoor(hexBounds, gc, door._orientation.startPoint, door._orientation.endPoint, door._orientation.thickness, door.isOpen());
         }
      }
      drawSpells(loc, hexBounds, gc, display);
      gc.setBackground(oldBg);
      gc.setForeground(oldFg);
      shortWallColor.dispose();
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
         //System.out.println("drawImage at 1601");
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
      if (_selfID != -1) {
         coordinates = coordinates.stream()
                                  .filter(loc-> !(loc instanceof ArenaLocation) || ((ArenaLocation)loc).getVisible(_selfID))
                 .collect(Collectors.toList());
      }
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
         return new int[0];
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
         double sizeY = (sizePerHex * 71d) / 81; // set aspect ratio
         double locX = (double) sizePerHex * ((column * 0.75) + .5);
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
      if ((offsetX != 0) || (offsetY != 0)) {
         return new int[] { hexDims[0]  + offsetX, hexDims[1] + offsetY,
                            hexDims[2]  + offsetX, hexDims[3] + offsetY,
                            hexDims[4]  + offsetX, hexDims[5] + offsetY,
                            hexDims[6]  + offsetX, hexDims[7] + offsetY,
                            hexDims[8]  + offsetX, hexDims[9] + offsetY,
                            hexDims[10] + offsetX, hexDims[11] + offsetY};
      }
      return hexDims;
   }

   private static final Map<Integer, double[]> MAP_OF_HEX_SIZES_TO_BASE_DIMENSIONS = new HashMap<>();
   public static double[] getHexBaseDimensions(int sizePerHex) {
      double[] results = MAP_OF_HEX_SIZES_TO_BASE_DIMENSIONS.get(sizePerHex);
      if (results == null) {
         double sizeY = (sizePerHex * 71d) / 81; // set aspect ratio
         double halfY = sizeY / 2;
         double halfX = (double) sizePerHex / 2;
         double quartX = (double) sizePerHex / 4;
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
   public final static int Y_LEFTMOST  = 1;
   public final static int Y_RIGHTMOST = 7;

   public double getAngleFromCenter(ArenaLocation loc, int x, int y) {
      return getMeasurementFromCenter(loc, x, y, true/*angle*/);
   }
   public double getDistanceFromCenter(ArenaLocation loc, int x, int y) {
      return getMeasurementFromCenter(loc, x, y, false/*angle*/);
   }
   public double getMeasurementFromCenter(ArenaLocation loc, int x, int y, boolean angle) {
      int[] bounds = getHexDimensions(loc);
      int centerX = (bounds[X_SMALLEST] + bounds[X_LARGEST]) / 2;
      int centerY = (bounds[Y_LEFTMOST] + bounds[Y_RIGHTMOST]) / 2;
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
