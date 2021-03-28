package ostrowski.combat.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Rules;
import ostrowski.combat.server.CombatServer;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing;

import java.util.ArrayList;
import java.util.List;

public class RollingDie extends Dialog implements PaintListener
{
   private Shell        shell           = null;
 //  private final GLView  view;
   private       Thread animationThread = null;
   //private TexturedObject die;
   private final Point centerLoc;
   public final  Point      offsetFromParent = null; // if this is null, we are not pinned to the parent, otherwise we are.
   public       Thing       die;
   public static RollingDie _this;

   public RollingDie(Shell parentShell, int style) {
      super (parentShell, SWT.MODELESS | SWT.NO_TRIM | style);

      _this = this;

      centerLoc = new Point(0, 0);

      animationThread = new Thread() {
         @Override
         public void run() {
            try {
               Thread.currentThread().setName("AnimationThread");
               long startTime = System.currentTimeMillis();
               long endTime = startTime + (10 * 3000); // stay in loop for 30 seconds;
               while (endTime > System.currentTimeMillis()) {
                  Thread.sleep(40); // ~25 redraws per second
                  //Rules.diag("in loop, about to call run()");
                  Display.getDefault().asyncExec(() -> {
                     die.facingOffset = new Tuple3(die.facingOffset.getX() + 10,
                                                   die.facingOffset.getY() + 0,
                                                   die.facingOffset.getZ() + 0);
                     redraw();
                  });
               }
               Display.getDefault().asyncExec(() -> {
                  Rules.diag("done loop in run");
                  _this.close();
               });

            } catch (InterruptedException | IllegalMonitorStateException e) {
               System.out.println(e);
            } finally {
               animationThread = null;
            }
         }
      };

//      GLView view = new GLView(parentShell, false/*withControls*/);
//      view.cameraPosition = new Tuple3(600, 600, 600);
//      view.setCameraAngle(225, 35);
//      view.clearModels();
//      try {
//         die = new Thing(Dice.d12, view, 10/*sizeFactor*/, "Blue");
//
//         die.locationOffset = new Tuple3(0, 0, 0);
//         view.addModel(die);
//      } catch (IOException e) {
//         e.printStackTrace();
//      }

      parentShell.getDisplay();
      //_animationThread.start();
      animationThread = null;
   }

   public static Region getRegion(double radius, Point center, double startAngle, double stopAngle) {
      radius += 0.5;
      List<Point> points = new ArrayList<>();
      points.add(center);
      double stepInRadians = 1.0/radius;
      for (double angle = startAngle ; angle < stopAngle ; angle += stepInRadians) {
         points.add(new Point((int)(center.x + (radius * Math.sin(angle))),
                              (int) (center.y  + (radius * Math.cos(angle)))));
      }
      int[] polygon = new int[points.size() * 2];
      for (int i=0 ; i< points.size() ; i++) {
         polygon[i*2] = points.get(i).x;
         polygon[(i*2)+1] = points.get(i).y;
         if (polygon[i*2] > center.x) {
            polygon[i*2]++;
         }
         if (polygon[(i*2)+1] > center.y) {
            polygon[(i*2)+1]++;
         }
      }
      Region region = new Region();
      region.add(polygon);
      return region;
   }

   enum DIRS {
      NORTH(0, -1),
      NORTHEAST(1, -1),
      EAST(1, 0),
      SOUTHEAST(1, 1),
      SOUTH(0, 1),
      SOUTHWEST(-1, 1),
      WEST(-1, 0),
      NORTHWEST(-1,-1);

      public final int x;
      public final int y;
      DIRS(int x, int y) {
         this.x = x;
         this.y = y;
      }
      public DIRS next() {
         switch (this) {
            case NORTH:     return NORTHEAST;
            case NORTHEAST: return EAST;
            case EAST:      return SOUTHEAST;
            case SOUTHEAST: return SOUTH;
            case SOUTH:     return SOUTHWEST;
            case SOUTHWEST: return WEST;
            case WEST:      return NORTHWEST;
            case NORTHWEST: return NORTH;
         }
         return null;
      }
      public DIRS prev() {
         switch (this) {
            case NORTH:     return NORTHWEST;
            case NORTHEAST: return NORTH;
            case EAST:      return NORTHEAST;
            case SOUTHEAST: return EAST;
            case SOUTH:     return SOUTHEAST;
            case SOUTHWEST: return SOUTH;
            case WEST:      return SOUTHWEST;
            case NORTHWEST: return WEST;
         }
         return null;
      }
      public DIRS reverse() {
         switch (this) {
            case NORTH:     return SOUTH;
            case NORTHEAST: return SOUTHWEST;
            case EAST:      return WEST;
            case SOUTHEAST: return NORTHWEST;
            case SOUTH:     return NORTH;
            case SOUTHWEST: return NORTHEAST;
            case WEST:      return EAST;
            case NORTHWEST: return SOUTHEAST;
         }
         return null;
      }
   }
//   private static DIRS walkEdge(DIRS fromDir, int x, int y, int backgroundColor, ImageData image) {
//      DIRS testDir = fromDir.reverse().next();
//      while (testDir != fromDir.reverse()) {
//         if (image.getPixel(x + testDir._x, y + testDir._y) != backgroundColor) {
//            return testDir;
//         }
//         testDir = testDir.next();
//      }
//      return null;
//   }

   public void redraw() {
//      view.drawScene(display);
   }

   public void open() {
      Shell parent = getParent();
      shell = new Shell(parent, SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      ControlListener parentControlListener = new ControlListener() {
         @Override
         public void controlResized(ControlEvent arg0) {
         }
         @Override
         public void controlMoved(ControlEvent arg0) {
            if (offsetFromParent != null) {
               Point parentLoc = getParent().getLocation();
               setLocation(new Point(parentLoc.x + offsetFromParent.x,
                                     parentLoc.y + offsetFromParent.y));
            }
         }
      };
      parent.addControlListener(parentControlListener);

      //new Snippet134(parent);

      shell.setText("Rolling Die");
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.makeColumnsEqualWidth = true;
      layout.horizontalSpacing = SWT.FILL;
      shell.setLayout(layout);

      //_shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));

      shell.addPaintListener(this);

      //add ability to move shell around
      Listener l = new Listener() {
         Point origin;
         @Override
         public void handleEvent(Event e) {
            switch (e.type) {
               case SWT.MouseDown:
                  origin = new Point(e.x, e.y);
                  break;
               case SWT.MouseUp:
                  origin = null;
                  break;
               case SWT.MouseMove:
                  if (origin != null) {
                     Point p = shell.getDisplay().map(shell, null, e.x, e.y);
                     setLocation(new Point(p.x - origin.x, p.y - origin.y));

                     if (CombatServer.uses3dMap) {
                        CombatServer._this.redrawMap();
                     }
                  }
                  break;
               case SWT.KeyDown:
               case SWT.KeyUp:
                  // bubble the keystrokes up to the parent dialog, so it can handle them:
                  getParent().notifyListeners(e.type, e);
                  break;
            }
         }
      };
      // Register listeners for all the events we do something with:
      shell.addListener(SWT.MouseDown, l);
      shell.addListener(SWT.MouseUp, l);
      shell.addListener(SWT.MouseMove, l);
      shell.addListener(SWT.KeyDown, l);
      shell.addListener(SWT.KeyUp, l);
      shell.pack();

      // Define the overall shape
      Region region = getRegion(25.0, new Point(25, 25), 0, 2*Math.PI);
      Rectangle size = region.getBounds();
      shell.setRegion(region);
      region.dispose();
      shell.setSize(size.width, size.height);

      shell.open();

      shell.setLocation(centerLoc);
   }

   public void close() {
      if (!shell.isDisposed()) {
         shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));
   }

   public void setLocation(Point location) {
      shell.setLocation(location);
   }
}
