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
   public final Shell        shell;
   private StatusChit2       rollingDie = null;
   private final SyncRequest req;
   private Point             currentLoc;

   public DieRoll(Shell parent, int style, SyncRequest req) {
      super(parent, checkStyle(style));

      this.req = req;

      shell = new Shell(parent, SWT.DIALOG_TRIM | checkStyle(style));
      GridLayout layout = new GridLayout(1/*numColumns*/, false/*makeColumnsEqualWidth*/);
      shell.setLayout(layout);
      shell.addFocusListener(this);
      if (rollingDie == null) {
         rollingDie = new StatusChit2(shell, 0);//SWT.MODELESS | SWT.NO_TRIM | SWT.NO_FOCUS);
      }

      Composite body = new Composite(shell, SWT.NONE);
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
      shell.setText(string);
   }

   public void open() {
      focusGained();
      shell.pack();
      shell.open();
      shell.layout();

      if (MessageDialog.topMessage != null) {
         if (MessageDialog.topMessage.shell.isDisposed()) {
            MessageDialog.topMessage = null;
         }
         else {
            MessageDialog.topMessage.shell.moveAbove(shell);
         }
      }

      Point statusChitLoc = null;
      if (currentLoc == null) {
         currentLoc = shell.getLocation();
         Point size = shell.getSize();
         statusChitLoc = new Point((currentLoc.x + size.x) - 5, currentLoc.y);
      }
      else {
         shell.setLocation(currentLoc);
      }

      if (rollingDie != null) {
         rollingDie.open();
         rollingDie.setLocation(statusChitLoc);
      }

      shell.addKeyListener(this);
      shell.addControlListener(this);


      while (!shell.isDisposed()) {
         if (!shell.getDisplay().readAndDispatch()) {
            shell.getDisplay().sleep();
         }
      }
      if (rollingDie != null) {
         if (CombatServer.isServer) {
            rollingDie.close();
            rollingDie = null;
         }
      }
   }

   public void focusGained() {
      // don't care
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
      if ((req != null) && (arg0.keyCode == SWT.ESC)) {
         req.setAnswerID(SyncRequest.OPT_CANCEL_ACTION);
      }
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      if ((req != null) && req.keyPressed(arg0)) {
         shell.close();
      }
   }
   @Override
   public void focusGained(FocusEvent arg0) {
      focusGained();
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
   @Override
   public void focusLost(FocusEvent arg0) {
   }
   @Override
   public void controlMoved(ControlEvent arg0) {
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
      //Point currentLoc = shell.getLocation();
   }
   @Override
   public void controlResized(ControlEvent arg0) {
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
}
