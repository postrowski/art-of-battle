package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ostrowski.ui.Helper;

public class NewMapDialog extends Dialog implements ModifyListener
{
   private final Shell   shell;
   private final Text    nameTextBox;
   private final Text    sizeXTextBox;
   private final Text    sizeYTextBox;
   private       short   sizeX;
   private       short   sizeY;
   private       String  name;
   private       boolean cancelSelected;

  public NewMapDialog(Shell parent) {
     super(parent);
     shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS);
     shell.setText(getText());
     shell.setLayout(new GridLayout(2, false));

     Helper helper = new Helper();
     new Label(shell, SWT.CENTER).setImage(shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));

     Group body = helper.createGroup(shell, "New Map creation", 2, false/*sameSize*/, 3, 3);

     helper.createLabel(body, "Map Name:", SWT.LEFT, 1, null);
     nameTextBox = helper.createText(body, "", true/*editable*/, 1/*hSpan*/);

     helper.createLabel(body, "Size X:", SWT.LEFT, 1, null);
     sizeXTextBox = helper.createText(body, "43", true/*editable*/, 1/*hSpan*/);

     helper.createLabel(body, "Size Y:", SWT.LEFT, 1, null);
     sizeYTextBox = helper.createText(body, "30", true/*editable*/, 1/*hSpan*/);

     nameTextBox.addModifyListener(this);
     sizeXTextBox.addModifyListener(this);
     sizeYTextBox.addModifyListener(this);

     Composite footer = new Composite(body, SWT.NONE);

     GridData data3 = new GridData();
     data3.grabExcessHorizontalSpace = true;
     data3.horizontalAlignment = SWT.FILL;
     data3.horizontalSpan = 2;
     footer.setLayoutData(data3);

     RowLayout rowlayout = new RowLayout();
     rowlayout.justify = true;
     rowlayout.fill = true;
     footer.setLayout(rowlayout);

     Button ok = new Button(footer, SWT.PUSH);
     ok.setText("Create");
     ok.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
             shell.dispose();
         }
     });

     Button cancelButton = new Button(footer, SWT.PUSH);
     cancelButton.setText(" Cancel ");
     cancelButton.addSelectionListener(new SelectionAdapter() {

      @Override
        public void widgetSelected(SelectionEvent e) {
           cancelSelected = true;
           shell.dispose();
        }
     });
     shell.setDefaultButton(ok);
  }

  public void open() {
     shell.pack();
     shell.open();
     shell.layout();

     while (!shell.isDisposed()) {
        if (!shell.getDisplay().readAndDispatch()) {
           shell.getDisplay().sleep();
        }
     }
  }
  @Override
public void modifyText(ModifyEvent e)
  {
     try {
        name = nameTextBox.getText();
        sizeX = Short.parseShort(sizeXTextBox.getText());
        sizeY = Short.parseShort(sizeYTextBox.getText());
     }
     catch (NumberFormatException ex) {
     }
  }
  public String getName() {
     return name;
  }
  public short getSizeX() {
     return sizeX;
  }
  public short getSizeY() {
     return sizeY;
  }
  public boolean isCanceled() {
     return cancelSelected;
  }
}
