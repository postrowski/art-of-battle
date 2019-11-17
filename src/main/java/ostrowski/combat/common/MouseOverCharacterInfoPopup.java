package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolTip;

import ostrowski.combat.client.ui.CharInfoBlock;
import ostrowski.combat.server.ArenaLocation;

public class MouseOverCharacterInfoPopup
{
   ArenaLocation _currentMouseLoc = null;
   ToolTip _popupMessage = null;

   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      if (_currentMouseLoc != loc) {
         Rules.diag("onMouseMove (" + event.x + "," + event.y + ")");
         _currentMouseLoc = loc;
         if (_popupMessage == null) {
            _popupMessage = new ToolTip(event.display.getActiveShell(), SWT.NONE);
         }
         else {
            _popupMessage.setVisible(false);
         }

         if ((loc != null) && !loc.isEmpty()) {
            _popupMessage.setLocation(Display.getCurrent().getCursorLocation().x,
                                      Display.getCurrent().getCursorLocation().y);
            _popupMessage.setVisible(true);
            _currentMouseLoc = loc;
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Character ch : loc.getCharacters()) {
               if (!first) {
                  sb.append("\n------------\n");
               }
               sb.append(CharInfoBlock.getToolTipSummary(ch));
            }
            _popupMessage.setMessage(sb.toString());
         }
      }
   }

}
