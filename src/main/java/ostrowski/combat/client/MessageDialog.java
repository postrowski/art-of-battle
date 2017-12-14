package ostrowski.combat.client;

import java.util.ArrayList;

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
   Object result;
   String _targetName;
   public Shell _shell;

   public MessageDialog (Shell parent, int style) {
      super (parent, style);
   }
   public MessageDialog (Shell parent) {
      this (parent, 0); // your default style bits go here (not the Shell's style bits)
   }
   public void setTargetName(String targetName) {
      _targetName = targetName;
   }
   public Object open (String message, boolean isPublic) {
      Shell parent = getParent();
      _shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      if (isPublic) {
         _shell.setText("Public message from server");
      }
      else {
         _shell.setText("Private message from server" + ((_targetName == null) ? "" : (" to " + _targetName)));
      }
      _shell.setLayout(new GridLayout(1, false));
      Label label = new Label(_shell, SWT.LEFT);
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


      Composite footer = new Composite(_shell, SWT.NONE);

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
              _shell.dispose();
          }
      });
      _shell.setDefaultButton(ok);
//      body.pack();
      _shell.pack();

      _shell.addFocusListener(this);

      _topMessage = this;
      _activeMessages.add(this);
      _shell.open();
      Display display = parent.getDisplay();
      while (!_shell.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
      }
      _activeMessages.remove(this);
      if (_topMessage == this) {
         while (!_activeMessages.isEmpty()) {
            _topMessage = _activeMessages.get(0);
            if (MessageDialog._topMessage._shell.isDisposed()) {
               _activeMessages.remove(_topMessage);
            }
            else {
               _topMessage._shell.forceActive();
               break;
            }
         }
      }
      return result;
   }
   @Override
   public void focusLost(FocusEvent arg0) {
      if (_topMessage == this) {
         if (_shell.isDisposed()) {
            return;
         }

         _shell.forceActive();
         _shell.forceFocus();
         _shell.setFocus();
         for( RequestUserInput userInput : Arena._activeRequestUserInputs) {
            if (!userInput._shell.isDisposed()) {
               _shell.moveAbove(userInput._shell);
            }
         }
      }
   }

   @Override
   public void focusGained(FocusEvent arg0) {
      _topMessage = this;
   }

   public static final ArrayList<MessageDialog> _activeMessages = new ArrayList<>();
   public static MessageDialog _topMessage = null;
}
