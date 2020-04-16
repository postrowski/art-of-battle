package ostrowski.combat.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.SyncRequest;

public class DieRoll extends Dialog implements KeyListener, FocusListener, ControlListener
{
   public final Shell        _shell;
   private StatusChit2       _rollingDie = null;
   private final SyncRequest _req;
   private Point             _currentLoc;

   public DieRoll(Shell parent, int style, SyncRequest req) {
      super(parent, checkStyle(style));

      _req = req;

      _shell = new Shell(parent, SWT.DIALOG_TRIM | checkStyle(style));
      GridLayout layout = new GridLayout(1/*numColumns*/, false/*makeColumnsEqualWidth*/);
      _shell.setLayout(layout);
      _shell.addFocusListener(this);
      if (_rollingDie == null) {
         _rollingDie = new StatusChit2(_shell, 0);//SWT.MODELESS | SWT.NO_TRIM | SWT.NO_FOCUS);
      }

      Composite body = new Composite(_shell, SWT.NONE);
      body.addFocusListener(this);
   }

   protected static int checkStyle(int style) {
      if ((style & SWT.SYSTEM_MODAL) == SWT.SYSTEM_MODAL) {
         return SWT.SYSTEM_MODAL;
      }
      if ((style & SWT.PRIMARY_MODAL) == SWT.PRIMARY_MODAL) {
         return SWT.PRIMARY_MODAL;
      }
      if ((style & SWT.APPLICATION_MODAL) == SWT.APPLICATION_MODAL) {
         return SWT.APPLICATION_MODAL;
      }

      return style;
   }

   public void setTitle(String string) {
      super.setText(string);
      _shell.setText(string);
   }

   public void open() {
      focusGained();
      _shell.pack();
      _shell.open();
      _shell.layout();

      if (MessageDialog._topMessage != null) {
         if (MessageDialog._topMessage._shell.isDisposed()) {
            MessageDialog._topMessage = null;
         }
         else {
            MessageDialog._topMessage._shell.moveAbove(_shell);
         }
      }

      Point statusChitLoc = null;
      if (_currentLoc == null) {
         _currentLoc = _shell.getLocation();
         Point size = _shell.getSize();
         statusChitLoc = new Point((_currentLoc.x + size.x) - 5, _currentLoc.y);
      }
      else {
         _shell.setLocation(_currentLoc);
      }

      if (_rollingDie != null) {
         _rollingDie.open();
         _rollingDie.setLocation(statusChitLoc);
      }

      _shell.addKeyListener(this);
      _shell.addControlListener(this);


      while (!_shell.isDisposed()) {
         if (!_shell.getDisplay().readAndDispatch()) {
            _shell.getDisplay().sleep();
         }
      }
      if (_rollingDie != null) {
         if (CombatServer._isServer) {
            _rollingDie.close();
            _rollingDie = null;
         }
      }
   }

   public void focusGained() {
      // don't care
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
      if ((_req!= null) && (arg0.keyCode == SWT.ESC)) {
         _req.setAnswerID(SyncRequest.OPT_CANCEL_ACTION);
      }
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      if ((_req!= null) && _req.keyPressed(arg0)) {
         _shell.close();
      }
   }
   @Override
   public void focusGained(FocusEvent arg0) {
      focusGained();
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
   @Override
   public void focusLost(FocusEvent arg0) {
   }
   @Override
   public void controlMoved(ControlEvent arg0) {
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
      //Point currentLoc = _shell.getLocation();
   }
   @Override
   public void controlResized(ControlEvent arg0) {
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
}
