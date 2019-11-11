/*
 * Created on May 10, 2006
 *
 */
package ostrowski.combat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.common.Rules;
import ostrowski.combat.server.CombatServer;

public class CombatClient
{

   /**
    * @param args
    */
   public static void main(String[] args) {

      List<String> newArgs = new ArrayList<>(Arrays.asList(args));
      if (!newArgs.isEmpty()) {
         if (newArgs.get(0).equalsIgnoreCase("server")) {
            newArgs.remove(0);
            CombatServer.main(newArgs);
            return;
         }
      }
      main(newArgs);
   }
   public static void main(List<String> args) {
      Rules.setDiagComponentName("Client");

      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Character"); // the window's title

      String preferedCharName = !args.isEmpty() ? args.get(0) : null;
      String aiArg = (args.size() > 1) ? args.get(1) : null;
      boolean aiOn = ((aiArg != null) && (aiArg.equalsIgnoreCase("ai")));

      CharacterDisplay combat = new CharacterDisplay(preferedCharName);
      boolean startOnArenaPage = combat._charWidget._character.getName().equals(preferedCharName);
      combat.buildCharSheet(shell, startOnArenaPage, aiOn);

      shell.pack();
      shell.open ();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep ();
         }
      }
      display.dispose ();

      combat.disconnectFromServer();
   }

}
