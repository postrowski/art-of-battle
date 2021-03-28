package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;

import ostrowski.combat.client.ui.CharInfoBlock;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;

import java.util.stream.Collectors;

public class MouseOverCharacterInfoPopup
{
   ArenaLocation currentMouseLoc = null;
   ToolTip       popupMessage    = null;

   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      if (currentMouseLoc == loc) {
         return;
      }

      currentMouseLoc = loc;
      if ((popupMessage == null) || popupMessage.isDisposed()) {

         Shell shell = event.display.getActiveShell();
         if (shell == null) {
            return;
         }
         popupMessage = new ToolTip(shell, SWT.NONE);
      }
      else {
         popupMessage.setVisible(false);
      }
      if ((loc == null) || loc.isEmpty()) {
         return;
      }

      IMapWidget map = CombatServer._this.map;
      int selfId = map.getSelfId();
      ArenaCoordinates selfHeadLoc = (selfId == -1) ? loc : map.getCombatMap().getCombatantByUniqueID(selfId).getHeadCoordinates();
      short distance = ArenaCoordinates.getDistance(loc, selfHeadLoc);
      // Only show popup messages if we can see the location. But also, we can auto-see characters within 3 hexes of us
      if (loc.getVisible(selfId) || (!loc.getCharacters().isEmpty() && (distance <= Rules.AUTO_VISIBLE_DISTANCE))) {
         StringBuilder sb = new StringBuilder();
         sb.append(loc.getCharacters()
                      .stream()
                      .map( chr -> (String)CharInfoBlock.getToolTipSummary(chr))
                      .collect(Collectors.joining("\n-----------------\n")));
         boolean first = sb.length() == 0;
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
         popupMessage.setMessage(sb.toString());
         popupMessage.setLocation(Display.getCurrent().getCursorLocation().x,
                                  Display.getCurrent().getCursorLocation().y);
         popupMessage.setVisible(true);
         currentMouseLoc = loc;
      }
   }

}
