package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;

import ostrowski.combat.client.ui.CharInfoBlock;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.server.ArenaLocation;

public class MouseOverCharacterInfoPopup
{
   ArenaLocation _currentMouseLoc = null;
   ToolTip _popupMessage = null;

   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      if (_currentMouseLoc != loc) {
         //Rules.diag("onMouseMove (" + event.x + "," + event.y + ")");
         _currentMouseLoc = loc;
         if ((_popupMessage == null) || _popupMessage.isDisposed()) {

            Shell shell = event.display.getActiveShell();
            if (shell == null) {
               return;
            }
            _popupMessage = new ToolTip(shell, SWT.NONE);
         }
         else {
            _popupMessage.setVisible(false);
         }

         if ((loc != null) && !loc.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Character ch : loc.getCharacters()) {
               if (!first) {
                  sb.append("\n-----------------\n");
               }
               sb.append(CharInfoBlock.getToolTipSummary(ch));
               first = false;
            }
            boolean firstThing = true;
            for (Object thing : loc.getThings()) {
               if (!(thing instanceof Character)) {
                  if (!first) {
                     if (firstThing) {
                        sb.append("\n=================\n");
                        firstThing = false;
                     }
                     else {
                        sb.append("\n-----------------\n");
                     }
                  }
                  if (thing instanceof Thing) {
                     Thing thg = (Thing) thing;
                     sb.append(thg.getName());
                  }
                  else {
                     sb.append(thing);
                  }
               }
            }
            _popupMessage.setMessage(sb.toString());
            _popupMessage.setLocation(Display.getCurrent().getCursorLocation().x,
                                      Display.getCurrent().getCursorLocation().y);
            _popupMessage.setVisible(true);
            _currentMouseLoc = loc;
         }
      }
   }

}
