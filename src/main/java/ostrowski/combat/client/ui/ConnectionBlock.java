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
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.protocol.ServerStatus;
import ostrowski.combat.server.Configuration;
import ostrowski.ui.Helper;

public class ConnectionBlock extends Helper implements SelectionListener, Enums
{
   private Combo            _cmbIpAddresses      = null;
   private String           _ipAddress           = "";
   private Button           _connectButton       = null;
   private final CharacterDisplay _display;

   public ConnectionBlock(CharacterDisplay display) {
      _display = display;
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
   }
   public void handleConnect()
   {
      _connectButton.setText("Disconnect");
      _cmbIpAddresses.setEnabled(false);
      _connectButton.setEnabled(true);
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
   }
   public void updateServerStatus(ServerStatus status)
   {
      _connectButton.setEnabled(true);
   }
}
