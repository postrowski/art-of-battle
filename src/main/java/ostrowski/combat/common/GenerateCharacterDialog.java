package ostrowski.combat.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ostrowski.ui.Helper;

public class GenerateCharacterDialog extends Dialog implements ModifyListener {
   private final Shell _shell;
   public boolean _cancelSelected = false;
   final  Combo   _generationPointsCombo;
   final  Combo   _racesCombo;
   static int     _generationPoints = 200;
   private String _race;
   private final Text _equipmentTextBox;
   private String _equipment = "";

   public GenerateCharacterDialog(Shell parent, String defaultRace)
   {
      super(parent);
      _shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS);
      _shell.setText(getText());
      _shell.setLayout(new GridLayout(2, false));

      Helper helper = new Helper();
      new Label(_shell, SWT.CENTER).setImage(_shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));

      List<String> pointsList = new ArrayList<>();
      for (int i=-100 ; i<=500 ; i+=50) {
         pointsList.add(String.valueOf(i));
      }
      List<String> races = Race.getRaceNames(true/*includeNPCs*/);

      Group body = helper.createGroup(_shell, "Random character generation", 2, false/*sameSize*/, 3, 3);

      helper.createLabel(body, "How many point for this character:", SWT.LEFT, 1, null);
      _generationPointsCombo = helper.createCombo(body, 0/*style*/, 1/*hSpan*/, pointsList );

      helper.createLabel(body, "Select a race for this character:", SWT.LEFT, 1, null);
      _racesCombo = helper.createCombo(body, 0/*style*/, 1/*hSpan*/, races );
      _racesCombo.setText(Objects.requireNonNullElse(defaultRace, Race.NAME_Human));
      _race = _racesCombo.getText();

      helper.createLabel(body, "Enter all equipment for this character:", SWT.LEFT, 2, null);
      _equipmentTextBox = helper.createText(body, "", true/*editable*/, 2/*hSpan*/);

      _generationPointsCombo.setText(String.valueOf(_generationPoints));
      _equipmentTextBox.setText(_equipment);

      _generationPointsCombo.addModifyListener(this);
      _racesCombo.addModifyListener(this);
      _equipmentTextBox.addModifyListener(this);

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
      ok.setText("Generate");
      ok.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              _shell.dispose();
          }
      });

      if (defaultRace != null) {
         Button cancelButton = new Button(footer, SWT.PUSH);
         cancelButton.setText(" Cancel ");
         cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               _cancelSelected = true;
               _shell.dispose();
            }
         });
      }
      _shell.setDefaultButton(ok);
   }

   public int open() {
      _shell.pack();
      _shell.open();
      _shell.layout();

      while (!_shell.isDisposed()) {
         if (!_shell.getDisplay().readAndDispatch()) {
            _shell.getDisplay().sleep();
         }
      }
      return _generationPoints;
   }
   @Override
   public void modifyText(ModifyEvent e)
   {
      try {
         _generationPoints = Integer.parseInt(_generationPointsCombo.getText());
         _race = _racesCombo.getText();
         _equipment = _equipmentTextBox.getText();
      }
      catch (NumberFormatException ex) {
         _generationPoints = 0;
      }
   }
   public String getRace() {
      return _race;
   }
   public String getEquipment() {
      return _equipment;
   }
}
