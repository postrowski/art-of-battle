/*
 * Created on May 10, 2006
 *
 */
package ostrowski.combat.client;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.common.Rules;

public class CombatClient
{

   /**
    * @param args
    */
   public static void main(String[] args) {

      Rules.setDiagComponentName("Client");

      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Character"); // the window's title

      String preferedCharName = (args.length > 0) ? args[0] : null;
      String aiArg = (args.length > 1) ? args[1] : null;
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
