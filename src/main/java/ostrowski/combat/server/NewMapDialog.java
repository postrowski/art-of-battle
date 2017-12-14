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
   private final Shell   _shell;
   private final Text    _nameTextBox;
   private final Text    _sizeXTextBox;
   private final Text    _sizeYTextBox;
   private short   _sizeX;
   private short   _sizeY;
   private String  _name;
   private boolean _cancelSelected;

  public NewMapDialog(Shell parent) {
     super(parent);
     _shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS);
     _shell.setText(getText());
     _shell.setLayout(new GridLayout(2, false));

     Helper helper = new Helper();
     new Label(_shell, SWT.CENTER).setImage(_shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));

     Group body = helper.createGroup(_shell, "New Map creation", 2, false/*sameSize*/, 3, 3);

     helper.createLabel(body, "Map Name:", SWT.LEFT, 1, null);
     _nameTextBox = helper.createText(body, "", true/*editable*/, 1/*hSpan*/);

     helper.createLabel(body, "Size X:", SWT.LEFT, 1, null);
     _sizeXTextBox = helper.createText(body, "43", true/*editable*/, 1/*hSpan*/);

     helper.createLabel(body, "Size Y:", SWT.LEFT, 1, null);
     _sizeYTextBox = helper.createText(body, "30", true/*editable*/, 1/*hSpan*/);

     _nameTextBox.addModifyListener(this);
     _sizeXTextBox.addModifyListener(this);
     _sizeYTextBox.addModifyListener(this);

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
             _shell.dispose();
         }
     });

     Button cancelButton = new Button(footer, SWT.PUSH);
     cancelButton.setText(" Cancel ");
     cancelButton.addSelectionListener(new SelectionAdapter() {

      @Override
        public void widgetSelected(SelectionEvent e) {
           _cancelSelected = true;
           _shell.dispose();
        }
     });
     _shell.setDefaultButton(ok);
  }

  public void open() {
     _shell.pack();
     _shell.open();
     _shell.layout();

     while (!_shell.isDisposed()) {
        if (!_shell.getDisplay().readAndDispatch()) {
           _shell.getDisplay().sleep();
        }
     }
  }
  @Override
public void modifyText(ModifyEvent e)
  {
     try {
        _name = _nameTextBox.getText();
        _sizeX = Short.parseShort(_sizeXTextBox.getText());
        _sizeY = Short.parseShort(_sizeYTextBox.getText());
     }
     catch (NumberFormatException ex) {
     }
  }
  public String getName() {
     return _name;
  }
  public short getSizeX() {
     return _sizeX;
  }
  public short getSizeY() {
     return _sizeY;
  }
  public boolean isCanceled() {
     return _cancelSelected;
  }
}
