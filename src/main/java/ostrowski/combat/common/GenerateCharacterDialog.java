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
   private final Shell   shell;
   public        boolean cancelSelected   = false;
   final         Combo   generationPointsCombo;
   final         Combo   racesCombo;
   static        int     generationPoints = 200;
   private       String  race;
   private final Text    equipmentTextBox;
   private       String  equipment        = "";

   public GenerateCharacterDialog(Shell parent, String defaultRace)
   {
      super(parent);
      shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MODELESS);
      shell.setText(getText());
      shell.setLayout(new GridLayout(2, false));

      Helper helper = new Helper();
      new Label(shell, SWT.CENTER).setImage(shell.getDisplay().getSystemImage(SWT.ICON_QUESTION));

      List<String> pointsList = new ArrayList<>();
      for (int i=-100 ; i<=500 ; i+=50) {
         pointsList.add(String.valueOf(i));
      }
      List<String> races = Race.getRaceNames(true/*includeNPCs*/);

      Group body = helper.createGroup(shell, "Random character generation", 2, false/*sameSize*/, 3, 3);

      helper.createLabel(body, "How many point for this character:", SWT.LEFT, 1, null);
      generationPointsCombo = helper.createCombo(body, 0/*style*/, 1/*hSpan*/, pointsList);

      helper.createLabel(body, "Select a race for this character:", SWT.LEFT, 1, null);
      racesCombo = helper.createCombo(body, 0/*style*/, 1/*hSpan*/, races);
      racesCombo.setText(Objects.requireNonNullElse(defaultRace, Race.NAME_Human));
      race = racesCombo.getText();

      helper.createLabel(body, "Enter all equipment for this character:", SWT.LEFT, 2, null);
      equipmentTextBox = helper.createText(body, "", true/*editable*/, 2/*hSpan*/);

      generationPointsCombo.setText(String.valueOf(generationPoints));
      equipmentTextBox.setText(equipment);

      generationPointsCombo.addModifyListener(this);
      racesCombo.addModifyListener(this);
      equipmentTextBox.addModifyListener(this);

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
              shell.dispose();
          }
      });

      if (defaultRace != null) {
         Button cancelButton = new Button(footer, SWT.PUSH);
         cancelButton.setText(" Cancel ");
         cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               cancelSelected = true;
               shell.dispose();
            }
         });
      }
      shell.setDefaultButton(ok);
   }

   public int open() {
      shell.pack();
      shell.open();
      shell.layout();

      while (!shell.isDisposed()) {
         if (!shell.getDisplay().readAndDispatch()) {
            shell.getDisplay().sleep();
         }
      }
      return generationPoints;
   }
   @Override
   public void modifyText(ModifyEvent e)
   {
      try {
         generationPoints = Integer.parseInt(generationPointsCombo.getText());
         race = racesCombo.getText();
         equipment = equipmentTextBox.getText();
      }
      catch (NumberFormatException ex) {
         generationPoints = 0;
      }
   }
   public String getRace() {
      return race;
   }
   public String getEquipment() {
      return equipment;
   }
}
