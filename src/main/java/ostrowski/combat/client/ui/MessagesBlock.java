/*
 * Created on May 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.ui.Helper;

public class MessagesBlock extends Helper implements SelectionListener
{
   final   CharacterDisplay _display;
   private Browser          _messagesIn;
   private StringBuilder    _messageBuffer;
   private Text             _messagesOut;
   private Button           _sendMessageButton = null;
   public MessagesBlock(CharacterDisplay display)
   {
      _display = display;
      resetMessageBuffer();
   }

   public void resetMessageBuffer() {
      _messageBuffer = new StringBuilder("<body onload='window.scrollTo(0,500000);'>");
   }

   public void buildBlock(Composite parent)
   {
      Group messagesGroup = createGroup(parent, "Messages", 2/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      GridData data = new GridData(GridData.FILL_BOTH);
      data.horizontalSpan = 3;
      messagesGroup.setLayoutData(data);
      _messagesIn = new Browser(messagesGroup, SWT.NONE);
      _controlList.add(_messagesIn);

      data = new GridData(GridData.FILL_BOTH | SWT.BORDER);
      data.grabExcessVerticalSpace = true;
      data.horizontalSpan = 2;
      _messagesIn.setLayoutData(data);
      //_messagesIn.setBackground(new Color(parent.getDisplay(), 255, 255, 255));

      _messagesOut = createText(messagesGroup, "", true, 1);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.minimumHeight = 20;
      _messagesOut.setLayoutData(data);
      _sendMessageButton = createButton(messagesGroup, "Send Message", 1, null, this);
      data = new GridData();
      data.minimumHeight = 20;
      _sendMessageButton.setLayoutData(data);
      _messagesIn.setText("<br>");
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      // handle the 'send message' button
      if (e.widget == _sendMessageButton) {
         _display.sendMessageText(clearText());
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   public String clearText()
   {
      String text = _messagesOut.getText();
      _messagesOut.setText("");
      return text;
   }

   public void appendMesage(String message)
   {
      String fullMsg = message;
      if (!message.endsWith(">")) {
         fullMsg += "<br>";
      }
      // remove all CR-LF because they terminate the javascript execution for the insert
      fullMsg = fullMsg.replace("\n", "<br/>");
      // escape any single quote character, since we are putting this inside a single quote
      fullMsg = fullMsg.replace("'", "\\'");
      _messageBuffer.append(fullMsg);
      if (!_messagesIn.isDisposed()) {
         _messagesIn.execute("document.body.insertAdjacentHTML('beforeEnd', '"+fullMsg+"');window.scrollTo(0,document.body.scrollHeight);");
//         _messagesIn.setText(_messageBuffer.toString() + "</body>");
         try {
            Thread.sleep(10);
         } catch (InterruptedException e) {
         }
         _messagesIn.redraw();
      }
   }

}
