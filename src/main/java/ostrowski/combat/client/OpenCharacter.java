package ostrowski.combat.client;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterFile;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.server.CombatServer;

public class OpenCharacter extends Dialog implements MouseListener
{
   private Shell        _shell;
   public CharacterFile _charFile;
   public String        _selectedName = null;

   public enum ExitButton {
      Open, Copy, Cancel
   }

   public ExitButton _exitButton  = ExitButton.Cancel;

   Button            openButton   = null;
   Button            copyButton   = null;
   Button            deleteButton = null;
   Button            cancelButton = null;

   public Table      _table       = null;

   public OpenCharacter(Shell parent, CharacterFile charFile, int style) {
      super(parent, style);

      _charFile = charFile;

      _shell = new Shell(parent.getDisplay());
      _shell.setText(getText());
      GridLayout gridLayout = new GridLayout(3/*columns*/, false/*sameWidth*/);
      _shell.setLayout(gridLayout);

      _shell.setText("Character selection");

      // top row
      Label label = new Label(_shell, SWT.NONE);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 3;
      data.grabExcessVerticalSpace = false;
      data.minimumHeight = 25;
      label.setLayoutData(data);

      // second row
      label = new Label(_shell, SWT.NONE);
      label.setText("     ");
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.minimumWidth = 25;
      data.grabExcessVerticalSpace = true;
      data.grabExcessHorizontalSpace = false;
      label.setLayoutData(data);

      buildTable(_shell);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.grabExcessVerticalSpace = true;
      data.grabExcessHorizontalSpace = true;
      _table.setLayoutData(data);

      label = new Label(_shell, SWT.NONE);
      label.setText("     ");
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.minimumWidth = 25;
      data.grabExcessVerticalSpace = true;
      data.grabExcessHorizontalSpace = false;
      label.setLayoutData(data);

      // third row
      label = new Label(_shell, SWT.NONE);
      data = new GridData(GridData.FILL_HORIZONTAL);
      label.setLayoutData(data);
      data.grabExcessVerticalSpace = false;

      Composite buttonComp = new Composite(_shell, SWT.NONE);
      buttonComp.setLayout(new FillLayout());
      openButton = new Button(buttonComp, SWT.FLAT);
      openButton.setText("Open");
      openButton.setSize(150, 30);
      copyButton = new Button(buttonComp, SWT.FLAT);
      copyButton.setText("Copy");
      copyButton.setSize(150, 30);
      deleteButton = new Button(buttonComp, SWT.FLAT);
      deleteButton.setText("Delete");
      deleteButton.setSize(150, 30);
      cancelButton = new Button(buttonComp, SWT.FLAT);
      cancelButton.setText("Cancel");
      cancelButton.setSize(150, 30);

      setButtonsEnabledState(false);

      _table.addMouseListener(this);

      _table.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            setButtonsEnabledState(true);
         }
      });

      data = new GridData(GridData.FILL_HORIZONTAL);
      buttonComp.setLayoutData(data);
      data.grabExcessVerticalSpace = true;
      data.horizontalAlignment = SWT.RIGHT;

      openButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            // open button clicked:
            _exitButton = ExitButton.Open;
            _selectedName = _table.getSelection()[0].getText();
            _shell.dispose();
         }
      });
      copyButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            // copy button clicked:
            _exitButton = ExitButton.Copy;
            _selectedName = _table.getSelection()[0].getText();
            _shell.dispose();
         }
      });
      deleteButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            // delete button clicked:
            String selectedName = _table.getSelection()[0].getText();
            _charFile.delCharacter(selectedName);
            if (CombatServer._isServer) {
               CombatServer._this.removeCharacter(selectedName);
            }
            TableItem[] tableItem = _table.getSelection();
            for (int i = 0; i < _table.getItemCount(); i++) {
               TableItem row = _table.getItem(i);
               if (row == tableItem[0]) {
                  _table.clear(i);
                  _table.remove(i);
                  setButtonsEnabledState(false);
                  return;
               }
            }
         }

      });
      cancelButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            // cancel button clicked:
            _exitButton = ExitButton.Cancel;
            if (_table.getSelection().length > 0) {
               _selectedName = _table.getSelection()[0].getText();
            }
            _shell.dispose();
         }
      });
      _table.addKeyListener(new KeyListener() {
         @Override
         public void keyReleased(KeyEvent arg0) {
            if (arg0.keyCode == SWT.ESC) {
               _shell.dispose();
            }
            if (arg0.keyCode == 13) {
               // open button clicked:
               _exitButton = ExitButton.Open;
               _selectedName = _table.getSelection()[0].getText();
               _shell.dispose();
            }
         }
         @Override
         public void keyPressed(KeyEvent arg0) {
         }
      });


   }

   private void setButtonsEnabledState(boolean enabled) {
      deleteButton.setEnabled(enabled);
      copyButton.setEnabled(enabled);
      openButton.setEnabled(enabled);
   }

   public void buildTable(Composite parent) {
      _table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER);
      _table.setHeaderVisible(true);
      _table.setLinesVisible(true);
      _table.setBounds(new org.eclipse.swt.graphics.Rectangle(47, 67, 190, 70));

      String[] columnNames = new String[] { "Name", "Cost", "Race", "Gender", "Str (adj)", "Ht (adj)", "Tou", "IQ", "Nim", "Dex", "Soc",
                                            "Skills", "Right hand", "Left hand", "Armor", "Wealth", "M.A.", "Priest", "Advantages"};
      for (String columnName : columnNames) {
         TableColumn tableColumn = new TableColumn(_table, SWT.NONE);
         tableColumn.setWidth(100);
         tableColumn.setText(columnName);
      }

      for (String name : _charFile.getCharacterNames()) {
         Character character = _charFile.getCharacter(name);
         String[] rowStrs = convertCharacterToRow(character);
         TableItem row = new TableItem(_table, 0);
         row.setText(rowStrs);
      }
      for (TableColumn col : _table.getColumns()) {
         col.pack();
         col.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               sortTableOnColumn((TableColumn) e.widget);
            }
         });
      }
   }

   public static String[] convertCharacterToRow(Character character) {
      Advantage wealth = null;
      Advantage MA = null;
      Advantage priest = null;
      String deity = null;
      StringBuilder skills = new StringBuilder();
      StringBuilder advantages = new StringBuilder();
      for (Advantage adv : character.getAdvantagesList()) {
         if (adv.getName().equals(Advantage.MAGICAL_APTITUDE)) {
            MA = adv;
            continue;
         }
         if (adv.getName().startsWith(Advantage.DIVINE_AFFINITY_)) {
            priest = adv;
            deity = adv.getName().substring(Advantage.DIVINE_AFFINITY_.length());
            continue;
         }
         if (adv.getName().equals(Advantage.WEALTH)) {
            wealth = adv;
            continue;
         }
         if (advantages.length() > 0) {
            advantages.append(", ");
         }
         advantages.append(adv.getName());
         if (adv.hasLevels()) {
            advantages.append(":").append(adv.getLevelName());
         }
      }
      List<Skill> skillsList = character.getSkillsList();
      for (int i = 0; i < skillsList.size(); i++) {
         Skill skillI = skillsList.get(i);
         for (int j = i + 1; j < skillsList.size(); j++) {
            Skill skillJ = skillsList.get(j);
            if (skillI.getLevel() < skillJ.getLevel()) {
               skillsList.remove(j);
               skillsList.add(i, skillJ);
               i--;
               break;
            }
         }
      }
      for (Skill skill : skillsList) {
         if (skills.length() > 0) {
            skills.append(", ");
         }
         skills.append(skill.getName()).append(":").append(skill.getLevel());
      }
      // StringBuilder equipment = new StringBuilder();
      // for (Limb limb : character.getLimbs()) {
      //    Thing thing = limb.getHeldThing();
      //    if ((thing != null) && thing.isReal()) {
      //       if (equipment.length() > 0)
      //          equipment.append(", ");
      //
      //       equipment.append(thing.getName());
      //    }
      // }
      // for (Thing thing : character.getEquipment()) {
      //    if (equipment.length() > 0)
      //       equipment.append(", ");
      //    equipment.append(thing.getName());
      // }
      Armor armor = character.getArmor();
      Limb rightHand = character.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = character.getLimb(LimbType.HAND_LEFT);
      Thing rightHandThing = (rightHand != null) ? rightHand.getHeldThing() : null;
      Thing leftHandThing = (leftHand != null) ? leftHand.getHeldThing() : null;
      String strStr = String.valueOf(character.getAttributeLevel(Attribute.Strength));
      String htStr = String.valueOf(character.getAttributeLevel(Attribute.Health));
      if (character.getRace().getBuildModifier() != 0) {
         strStr += " (" + character.getAdjustedStrength() + ")";
         htStr += " (" + character.getBuildBase() + ")";
      }
      return new String[] {character.getName(),
                           String.valueOf(character.getPointTotal()),
                           character.getRace().getName(),
                           character.getRace().getGender()._name,
                           strStr,
                           htStr,
                           String.valueOf(character.getAttributeLevel(Attribute.Toughness)),
                           String.valueOf(character.getAttributeLevel(Attribute.Intelligence)),
                           String.valueOf(character.getAttributeLevel(Attribute.Nimbleness)),
                           String.valueOf(character.getAttributeLevel(Attribute.Dexterity)),
                           String.valueOf(character.getAttributeLevel(Attribute.Social)),
                           skills.toString(),
                           (rightHandThing == null) ? "" : rightHandThing.getName(),
                           (leftHandThing == null) ? "" : leftHandThing.getName(),
                           (armor == null) ? Armor.NAME_NoArmor : armor.getName(),
                           (wealth == null) ? "$" : String.valueOf(wealth.getLevelName()),
                           (MA == null) ? "" : String.valueOf(MA.getLevel()),
                           (deity == null) ? "" : (deity + ":" + priest.getLevelName()),
                           advantages.toString()};
   }

   int     _currentSortColumn = -1;
   boolean _ascending         = true;

   public void sortTableOnColumn(TableColumn column) {
      int columnIndex = -1;
      for (int n = 0; n < _table.getColumnCount(); n++) {
         TableColumn col = _table.getColumn(n);
         if (col == column) {
            columnIndex = n;
            break;
         }
      }
      System.out.println("sorting on column " + columnIndex);
      if (columnIndex == -1) {
         return;
      }

      boolean treatAsInteger = (columnIndex == 1);
      if (_currentSortColumn == columnIndex) {
         _ascending = !_ascending;
      }

      _currentSortColumn = columnIndex;

      for (int i = 0; i < _table.getItemCount(); i++) {
         TableItem rowI = _table.getItem(i);
         String textI = rowI.getText(columnIndex);
         int intI = 0;
         if (treatAsInteger) {
            intI = Integer.parseInt(textI);
         }

         for (int j = i + 1; j < _table.getItemCount(); j++) {
            TableItem rowJ = _table.getItem(j);
            String textJ = rowJ.getText(columnIndex);
            boolean swap;
            int intJ = 0;
            if (treatAsInteger) {
               intJ = Integer.parseInt(textJ);
               swap = (_ascending) ? (intI < intJ) : (intI > intJ);
            }
            else {
               swap = (_ascending) ? (textI.compareTo(textJ) < 0) : (textI.compareTo(textJ) > 0);
            }
            if (swap) {
               // swap rows i & j:
               for (int c = 0; c < _table.getColumnCount(); c++) {
                  String iText = rowI.getText(c);

                  rowI.setText(c, rowJ.getText(c));
                  rowJ.setText(c, iText);
               }
               textI = textJ;
               intI = intJ;
            }
         }
      }
      _table.redraw();
   }

   public ExitButton open() {
      _shell.pack();
      _shell.open();
      _shell.layout();
      _shell.setSize(1500, 750);

      while (!_shell.isDisposed()) {
         if (!_shell.getDisplay().readAndDispatch()) {
            _shell.getDisplay().sleep();
         }
      }
      return _exitButton;
   }

   @Override
   public void mouseDoubleClick(MouseEvent arg0) {
      _exitButton = ExitButton.Open;
      _selectedName = _table.getSelection()[0].getText();
      _shell.dispose();
   }

   @Override
   public void mouseDown(MouseEvent arg0) {
   }

   @Override
   public void mouseUp(MouseEvent arg0) {
   }
}
