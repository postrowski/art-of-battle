package ostrowski.combat.client;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Rules;
import ostrowski.combat.server.CombatServer;
import ostrowski.graphics.GLView;
import ostrowski.graphics.objects3d.Thing;
import ostrowski.graphics.objects3d.Thing.Dice;

public class RollingDie extends Dialog implements PaintListener
{
   private Shell          _shell            = null;
   private final GLView   _view;
   private final Display  _display;
   private Thread         _animationThread  = null;
   //private TexturedObject _die;
   private Point          _centerLoc        = new Point(0, 0);
   public  Point          _offsetFromParent = null;           // if this is null, we are not pinned to the parent, otherwise we are.

   public RollingDie(Shell parentShell, int style) {
      super (parentShell, SWT.MODELESS | SWT.NO_TRIM | style);

      _centerLoc = new Point(0, 0);

      _animationThread = new Thread() {
         @Override
         public void run() {
            try {
               Thread.currentThread().setName("AnimationThread");
               long startTime = System.currentTimeMillis();
               long endTime = startTime + (10 * 1000); // stay in loop for 10 seconds;
               while (endTime > System.currentTimeMillis()) {
                  Thread.sleep(10);
                  Rules.diag("in loop, about to call run()");
                  Display.getDefault().asyncExec(new Runnable() {
                     @Override
                     public void run() {
                        redraw();
                        Rules.diag("in run()");
                     }
                  });
                  break;
               }
               Display.getDefault().asyncExec(new Runnable() {
                  @Override
                  public void run() {
                     _shell.dispose();
                  }
               });
               Rules.diag("done loop");

            } catch (InterruptedException e) {
            } catch (IllegalMonitorStateException e) {
               System.out.println(e.toString());
            } finally {
               _animationThread = null;
            }
         }
      };
      _animationThread.start();

      _view = new GLView(parentShell);
      //Canvas canvas = _view.getCanvas();
      _view.clearModels();
      try {
         _view.addModel(new Thing(Dice.d12, _view, 60/*sizeFactor*/, "Blue"));
      } catch (IOException e) {
         e.printStackTrace();
      }

      _display = parentShell.getDisplay();
   }

   public void redraw() {
      _view.drawScene(_display);
   }

   public void updateFromCharacter(Character chr) {
      if (_shell != null) {
         Point currentLoc = _centerLoc;
         if (currentLoc == null) {
            _centerLoc = _shell.getLocation();
         }
         else {
            _shell.setLocation(currentLoc);
         }
      }

      if (_shell != null) {
         _shell.redraw();
      }
   }

   public void open() {
      Shell parent = getParent();
      _shell = new Shell(parent, SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      ControlListener parentControlListener = new ControlListener() {
         @Override
         public void controlResized(ControlEvent arg0) {
         }
         @Override
         public void controlMoved(ControlEvent arg0) {
            if (_offsetFromParent != null) {
               Point parentLoc = getParent().getLocation();
               setLocation(new Point(parentLoc.x + _offsetFromParent.x,
                                     parentLoc.y + _offsetFromParent.y));
            }
         }
      };
      parent.addControlListener(parentControlListener);

      //new Snippet134(parent);

      _shell.setText("Status Chit");
      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.makeColumnsEqualWidth = true;
      layout.horizontalSpacing = SWT.FILL;
      _shell.setLayout(layout);

      _shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));

      _shell.addPaintListener(this);

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
                     Point p = _shell.getDisplay().map(_shell, null, e.x, e.y);
                     setLocation(new Point(p.x - origin.x, p.y - origin.y));

                     if (CombatServer._uses3dMap) {
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
      _shell.addListener(SWT.MouseDown, l);
      _shell.addListener(SWT.MouseUp, l);
      _shell.addListener(SWT.MouseMove, l);
      _shell.addListener(SWT.KeyDown, l);
      _shell.addListener(SWT.KeyUp, l);
      _shell.pack();

      // Define the overall shape
      Region region = new Region();
      ArrayList<Point> points = new ArrayList<>();
      // TODO: fill in the points
      int[] polygon = new int[points.size() * 2];
      for (int i=0 ; i< points.size() ; i++) {
         polygon[i*2] = points.get(i).x;
         polygon[(i*2)+1] = points.get(i).y;
         if (polygon[i*2] > _centerLoc.x) {
            polygon[i*2]++;
         }
         if (polygon[(i*2)+1] > _centerLoc.y) {
            polygon[(i*2)+1]++;
         }
      }
      region.add(polygon);

      Rectangle size = region.getBounds();
      _shell.setRegion(region);
      region.dispose();
      _shell.setSize(size.width, size.height);

      _shell.open();

      _shell.setLocation(_centerLoc);
   }

   public void close() {
      if (!_shell.isDisposed()) {
         _shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));
   }

   public void setLocation(Point location) {
      _shell.setLocation(location);
   }
}
