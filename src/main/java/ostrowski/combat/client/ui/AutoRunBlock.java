package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.common.Character;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;

public class AutoRunBlock extends Helper implements IUIBlock, ModifyListener, SelectionListener
{
   private Spinner _playCount;
   private Button  _runButton;
   private final Text[]  _winCountPerTeam   = new Text[TEAM_NAMES.length];
   private final CombatServer _combatServer;

   public AutoRunBlock(CombatServer combatServer) {
      _combatServer = combatServer;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group playGroup = createGroup(parent, "AutoRun", 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(playGroup, "Count", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
      _playCount = createSpinner(playGroup, 0/*min*/, 99/*max*/, 1/*value*/, 1/*hSpan*/);
      _runButton = createButton(playGroup, "  Run ", 1/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);

      Group countGroup = new Group(playGroup, SWT.SHADOW_NONE);
      GridLayout layout = new GridLayout(TEAM_NAMES.length +1, false/*sameSize*/);
      layout.horizontalSpacing = 3;
      layout.verticalSpacing = 1;
      countGroup.setLayout(layout);

      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 3;
      countGroup.setLayoutData(data);

      createLabel(countGroup, "", SWT.CENTER, 1/*hSpan*/, null/*fontData*/);
      createLabel(countGroup, "Team " , SWT.CENTER, TEAM_NAMES.length/*hSpan*/, null/*fontData*/);
      createLabel(countGroup, "Win", SWT.CENTER, 1/*hSpan*/, null/*fontData*/);
      for (String element : TEAM_NAMES) {
         createLabel(countGroup, element, SWT.CENTER, 1/*hSpan*/, null/*fontData*/);
      }
      createLabel(countGroup, "count", SWT.CENTER, 1/*hSpan*/, null/*fontData*/);
      for (int t=0 ; t<TEAM_NAMES.length ; t++) {
         _winCountPerTeam[t] = createText(countGroup, "", false/*editable*/, 1/*hSpan*/, null/*fontData*/, 35/*minWdith*/);
      }
   }

   @Override
   public void refreshDisplay(Character character) {
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
   }

   @Override
   public void modifyText(ModifyEvent e) {
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _runButton) {
         for (int t=0 ; t<TEAM_NAMES.length ; t++) {
            _winCountPerTeam[t].setText("0");
         }
         _combatServer.onAutoRun(this);
      }
   }

   public void battleStarted() {
      _runButton.setEnabled(false);
   }

   public boolean battleEnded(final int winningTeamId) {
      Shell shell = _combatServer.getShell();
      final StringBuilder results = new StringBuilder();
      if (!shell.isDisposed()) {
         Display display = shell.getDisplay();
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               if (winningTeamId != -1) {
                  String winCountStr = _winCountPerTeam[winningTeamId].getText();
                  int winCount = Integer.parseInt(winCountStr);
                  winCount++;
                  _winCountPerTeam[winningTeamId].setText(String.valueOf(winCount));
               }
               int count = _playCount.getSelection();
               char result;
               count--;
               if (count == 0) {
                  result = 'f';
                  _runButton.setEnabled(true);
               }
               else {
                  _playCount.setSelection(count);
                  result = 't';
               }
               synchronized (results) {
                  results.append(result);
               }
            }
         });
      }
      do {
         synchronized(results) {
            if (results.length() > 0) {
               break;
            }
         }
         try {
            Thread.sleep(100 /*millis*/);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      } while (true);

      return results.charAt(0) == 't';
   }
}
