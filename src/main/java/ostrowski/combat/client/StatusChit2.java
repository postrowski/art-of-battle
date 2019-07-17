package ostrowski.combat.client;

import java.util.ArrayList;

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
   private static Point LOCATION = null;
   private final ArrayList<TextWheel> _textWheels = new ArrayList<>();
   private Shell _shell = null;

   private TextWheel _initiative;

   public StatusChit2(Shell parent, int style) {
      super (parent, SWT.MODELESS | SWT.NO_TRIM | style);
      TextWheel._fullWidth = 150;
      TextWheel._fullHieght = 150;

      //                                         percentages   angle         edge       angle  labelOffset    label       label  text  text
      //                                       radius cw block Open          edge       Width  Vert Horiz     label      Height  text  Height
      _textWheels.add(new TextWheel(1.0, 1.0, .60, 20,    TextWheel.Edge.TOP,  0, -.08,  .00, "Initiative",    (20.0/500.0),        "2", (14.0/500.0)));
   }

   public void open() {
      _shell = new Shell(getParent(), SWT.MODELESS | SWT.NO_BACKGROUND | SWT.TRANSPARENT | SWT.NO_TRIM | SWT.NO_FOCUS);

      _shell.setText("Status Chit");

      _shell.setLayout(new GridLayout(1/*numcolumns*/, false/*makeColumnsEqualWidth*/));
      _shell.addPaintListener(this);

      _shell.pack();

      Region region = TextWheel.getRectRegion();
      for (TextWheel wheel : _textWheels) {
         Region wheelRegion = wheel.getRegion();
         region.add(wheelRegion );
         wheelRegion.dispose();
      }

      Rectangle size = region.getBounds();
      _shell.setRegion(region);
      region.dispose();
      _shell.setSize(size.width, size.height);

      _shell.open();
      //_shell.setSize(TextWheel._fullWidth + 10, TextWheel._fullHieght + 10);
      if (LOCATION != null) {
         _shell.setLocation(LOCATION);
      }
   }

   public void close() {
      if (!_shell.isDisposed()) {
         _shell.close();
      }
   }

   @Override
   public void paintControl(PaintEvent event) {
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase1(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      TextWheel.drawPhase2(event.gc);
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_WHITE));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase3(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase4(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      for (TextWheel tw : _textWheels) {
         tw.drawPhase5(event.gc);
      }
      event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
      event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));

   }

   public void setLocation(Point location) {
      _shell.setLocation(location);
      LOCATION = location;
   }
}
