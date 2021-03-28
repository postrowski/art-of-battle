/*
 * Created on May 19, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
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
   private Combo                  cmbIpAddresses = null;
   private String                 ipAddress     = "";
   private Button                 connectButton = null;
   private final CharacterDisplay display;

   public ConnectionBlock(CharacterDisplay display) {
      this.display = display;
   }
   @Override
   public void widgetSelected(SelectionEvent e)
   {
      // handle the 'connect' button
      if (e.widget == connectButton) {
         if (connectButton.getText().equals("  Connect  ")) {
            display.connectToServer(ipAddress, Configuration.serverPort());
         }
         else {
            display.disconnectFromServer();
         }
         // disable the button until we get a success or failure.
         connectButton.setEnabled(false);
      }
   }
   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }
   public void handleDisconnect()
   {
      connectButton.setText("  Connect  ");
      cmbIpAddresses.setEnabled(true);
      connectButton.setEnabled(true);
   }
   public void handleConnect()
   {
      connectButton.setText("Disconnect");
      cmbIpAddresses.setEnabled(false);
      connectButton.setEnabled(true);
   }

   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Server Connection", 2/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      controlList.remove(group);
      createLabel(group, "Server IP:", SWT.TRAIL, 1, null);
      cmbIpAddresses = new Combo(group, SWT.DROP_DOWN);
      GridData data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
      cmbIpAddresses.setLayoutData(data);
      cmbIpAddresses.setItems(new String[] {"127.0.0.1", "localhost"});
      ipAddress = cmbIpAddresses.getItem(0);
      cmbIpAddresses.setText(ipAddress);
      cmbIpAddresses.addModifyListener(e -> {
         if (!CharacterWidget.inModify) {
            CharacterWidget.inModify = true;
            ipAddress = ((Combo)e.widget).getText();
            CharacterWidget.inModify = false;
         }
      });

      connectButton = createButton(group, "  Connect  ", 2, null, this);
      controlList.remove(connectButton);
      group.setTabList(new Control[] {cmbIpAddresses, connectButton});
   }
   public void updateServerStatus(ServerStatus status)
   {
      connectButton.setEnabled(true);
   }
}
