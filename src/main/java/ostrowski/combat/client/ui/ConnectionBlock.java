/*
 * Created on May 19, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.protocol.BeginBattle;
import ostrowski.combat.protocol.ServerStatus;
import ostrowski.combat.server.Configuration;
import ostrowski.ui.Helper;

public class ConnectionBlock extends Helper implements SelectionListener, Enums
{
   private Combo            _cmbIpAddresses    = null;
   private String           _ipAddress         = "";
   private Button           _connectButton     = null;
   private Combo            _teamSelection     = null;
   private byte             _teamSelected      = -1;
   private final CharacterDisplay _display;
   private int      _combatantsInArena = 0;
   private Button   _enterArenaButton = null;

   public ConnectionBlock(CharacterDisplay display) {
      _display = display;
   }
   public byte getTeamSelection() {
      return _teamSelected;
   }
   @Override
   public void widgetSelected(SelectionEvent e)
   {
      // handle the 'connect' button
      if (e.widget == _connectButton) {
         if (_connectButton.getText().equals("  Connect  ")) {
            _display.connectToServer(_ipAddress, Configuration.serverPort());
         }
         else {
            _display.disconnectFromServer();
         }
         // disable the button until we get a success or failure.
         _connectButton.setEnabled(false);
      }
      if (e.widget == _enterArenaButton) {
         if (_enterArenaButton.getText().equals(" Enter Arena")) {
            _display.onEnterArena(true, _teamSelected);
            _enterArenaButton.setText("Begin Battle");
            _enterArenaButton.setEnabled(_combatantsInArena >= 2);
         }
         else if (_enterArenaButton.getText().equals("Begin Battle")) {
            _display.onBeginBattle();
            _enterArenaButton.setText("  Surrender ");
         }
         else if (_enterArenaButton.getText().equals("  Surrender ")) {
            _display.onEnterArena(false, _teamSelected);
            _enterArenaButton.setText(" Enter Arena");
         }
      }
   }
   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }
   public void handleDisconnect()
   {
      _connectButton.setText("  Connect  ");
      _cmbIpAddresses.setEnabled(true);
      _connectButton.setEnabled(true);

      _enterArenaButton.setText(" Enter Arena");
      _enterArenaButton.setEnabled(false);
      _combatantsInArena = 0;
   }
   public void handleConnect()
   {
      _connectButton.setText("Disconnect");
      _cmbIpAddresses.setEnabled(false);
      _connectButton.setEnabled(true);
      _enterArenaButton.setEnabled(true);
   }

   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Server Connection", 2/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      _controlList.remove(group);
      createLabel(group, "Server IP:", SWT.TRAIL, 1, null);
      _cmbIpAddresses = new Combo(group, SWT.DROP_DOWN);
      GridData data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
      _cmbIpAddresses.setLayoutData(data);
      _cmbIpAddresses.setItems(new String[] {"127.0.0.1", "localhost"});
      _ipAddress = _cmbIpAddresses.getItem(0);
      _cmbIpAddresses.setText(_ipAddress);
      _cmbIpAddresses.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e)
         {
            if (!CharacterWidget._inModify) {
               CharacterWidget._inModify = true;
               _ipAddress = ((Combo)e.widget).getText();
               CharacterWidget._inModify = false;
            }
         }
      });

      _connectButton = createButton(group, "  Connect  ", 2, null, this);
      _controlList.remove(_connectButton);
      group.setTabList(new Control[] {_cmbIpAddresses, _connectButton});

      createLabel(group, "Team:", SWT.TRAIL, 1, null);
      _teamSelection = new Combo(group, SWT.DROP_DOWN);
      data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
      _teamSelection.setLayoutData(data);
      _teamSelection.setItems(TEAM_NAMES);
      _teamSelection.setEnabled(true);
      _teamSelection.addModifyListener(new ModifyListener() {
          @Override
         public void modifyText(ModifyEvent e)
          {
             if (!CharacterWidget._inModify) {
                CharacterWidget._inModify = true;
                for (byte team=0 ; team<TEAM_NAMES.length ; team++) {
                   if (((Combo)e.widget).getText().equals(TEAM_NAMES[team])) {
                      _teamSelected = team;
                      break;
                   }
                }
                CharacterWidget._inModify = false;
             }
          }
       });

      _enterArenaButton = createButton(group, " Enter Arena", 2, null, this);
      _enterArenaButton.setEnabled(false);
   }
   public void updateServerStatus(ServerStatus status)
   {
      java.util.List<Character> combatants = status.getCombatants();
      _combatantsInArena = combatants.size();
      if (_enterArenaButton.getText().equals("Begin Battle")) {
         _enterArenaButton.setEnabled(_combatantsInArena >= 2);
      }
      _teamSelection.removeAll();
      _teamSelection.setEnabled(false);
      _connectButton.setEnabled(true);
      boolean selfInArena = false;
      for (Character combatant : status.getCombatants()) {
         if (combatant._uniqueID == _display._charWidget._character._uniqueID) {
            selfInArena = true;
         }
      }
      if (!selfInArena) {
//         _enterArenaButton.setText("Arena Full!");
         byte firstAvailableTeam = -1;
         for (byte team=0 ; team<TEAM_NAMES.length ; team++) {
            if (status.isRoomOnTeam(team)) {
               _teamSelection.add(TEAM_NAMES[team]);
               if (firstAvailableTeam == -1) {
                  firstAvailableTeam = team;
               }
            }
         }
         if ((_teamSelected == -1) || (!status.isRoomOnTeam(_teamSelected))) {
            _teamSelected = firstAvailableTeam;
         }
         if (_teamSelected != -1) {
            _teamSelection.setText(TEAM_NAMES[_teamSelected]);
            _teamSelection.setEnabled(true);
            _enterArenaButton.setText(" Enter Arena");
            _connectButton.setEnabled(false);
         }
      }
   }
   public void beginBattle(BeginBattle battleMsg) {
      _enterArenaButton.setText("  Surrender ");
   }
}
