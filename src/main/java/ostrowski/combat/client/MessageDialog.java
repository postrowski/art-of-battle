package ostrowski.combat.client;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ostrowski.combat.server.Arena;

public class MessageDialog extends Dialog implements FocusListener{
   String targetName;
   public Shell shell;

   public static final List<MessageDialog> ACTIVE_MESSAGES = new ArrayList<>();
   public static       MessageDialog       topMessage      = null;

   public MessageDialog (Shell parent, int style) {
      super (parent, style);
   }
   public MessageDialog (Shell parent) {
      this (parent, 0); // your default style bits go here (not the Shell's style bits)
   }
   public void setTargetName(String targetName) {
      this.targetName = targetName;
   }
   public void open (String message, boolean isPublic) {
      Shell parent = getParent();
      shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      if (isPublic) {
         shell.setText("Public message from server");
      }
      else {
         shell.setText("Private message from server" + ((targetName == null) ? "" : (" to " + targetName)));
      }
      shell.setLayout(new GridLayout(1, false));
      Label label = new Label(shell, SWT.LEFT);
      GridData data1 = new GridData();
      label.setSize(300, 40);
      label.setLayoutData(data1);

      // print all "\n" as a true new line:
      while (true) {
         int index = message.indexOf("\\n");
         if (index == -1) {
            break;
         }
         message = message.substring(0, index) + '\n' + message.substring(index + 2).trim();
      }
      label.setText(message);


      Composite footer = new Composite(shell, SWT.NONE);

      GridData data3 = new GridData();
      data3.horizontalAlignment = SWT.FILL;
      footer.setLayoutData(data3);

      RowLayout rowlayout = new RowLayout();
      rowlayout.justify = true;
      rowlayout.fill = true;
      footer.setLayout(rowlayout);

      Button ok = new Button(footer, SWT.PUSH);
      ok.setText(" OK ");
      ok.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              shell.dispose();
          }
      });
      shell.setDefaultButton(ok);
//      body.pack();
      shell.pack();

      shell.addFocusListener(this);

      topMessage = this;
      ACTIVE_MESSAGES.add(this);
      shell.open();
      Display display = parent.getDisplay();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }
      ACTIVE_MESSAGES.remove(this);
      if (topMessage == this) {
         while (!ACTIVE_MESSAGES.isEmpty()) {
            topMessage = ACTIVE_MESSAGES.get(0);
            if (MessageDialog.topMessage.shell.isDisposed()) {
               ACTIVE_MESSAGES.remove(topMessage);
            }
            else {
               topMessage.shell.forceActive();
               break;
            }
         }
      }
   }
   @Override
   public void focusLost(FocusEvent arg0) {
      if (topMessage == this) {
         if (shell.isDisposed()) {
            return;
         }

         shell.forceActive();
         shell.forceFocus();
         shell.setFocus();
         for (RequestUserInput userInput : Arena.activeRequestUserInputs) {
            if (!userInput.shell.isDisposed()) {
               shell.moveAbove(userInput.shell);
            }
         }
      }
   }

   @Override
   public void focusGained(FocusEvent arg0) {
      topMessage = this;
   }
}
