package ostrowski.combat.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;


public class StatusChit2 extends Dialog implements PaintListener
{
   private static Point          LOCATION   = null;
   private final List<TextWheel> textWheels = new ArrayList<>();
   private Shell                 shell      = null;

   private TextWheel initiative;

   public StatusChit2(Shell parent, int style) {
      super (parent, SWT.MODELESS | SWT.NO_TRIM | style);
      TextWheel.fullWidth = 150;
      TextWheel.fullHeight = 150;

      //                                         percentages   angle         edge       angle  labelOffset    label       label  text  text
      //                                       radius cw block Open          edge       Width  Vert Horiz     label      Height  text  Height
      textWheels.add(new TextWheel(1.0, 1.0, .60, 20, TextWheel.Edge.TOP, 0, -.08, .00, "Initiative", (20.0 / 500.0), "2", (14.0 / 500.0)));
   }

   public void open() {
      shell = new Shell(getParent(), SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      shell.setText("Status Chit");

      shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));
      shell.addPaintListener(this);

      shell.pack();

      Region region = TextWheel.getRectRegion();
      for (TextWheel wheel : textWheels) {
         Region wheelRegion = wheel.getRegion();
         region.add(wheelRegion );
         wheelRegion.dispose();
      }

      Rectangle size = region.getBounds();
      shell.setRegion(region);
      region.dispose();
      shell.setSize(size.width, size.height);

      shell.open();
      //_shell.setSize(TextWheel._fullWidth + 10, TextWheel._fullHieght + 10);
      if (LOCATION != null) {
         shell.setLocation(LOCATION);
      }
   }

   public void close() {
      if (!shell.isDisposed()) {
         shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : textWheels) {
         tw.drawPhase1(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      TextWheel.drawPhase2(event.gc);
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : textWheels) {
         tw.drawPhase3(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : textWheels) {
         tw.drawPhase4(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : textWheels) {
         tw.drawPhase5(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));

   }

   public void setLocation(Point location) {
      shell.setLocation(location);
      LOCATION = location;
   }
}
