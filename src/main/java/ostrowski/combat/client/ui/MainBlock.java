/*
 * Created on May 19, 2006
 */
package ostrowski.combat.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.client.OpenCharacter;
import ostrowski.combat.client.OpenCharacter.ExitButton;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterFile;
import ostrowski.combat.common.CharacterGenerator;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.GenerateCharacterDialog;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;

public class MainBlock extends Helper implements ModifyListener, SelectionListener, IUIBlock
{
   private Button           _openButton;
   private Button           _newButton;
   private Button           _saveButton;
   private Button           _genButton;
   private Text             _name;
   private Combo            _race;
   private Combo            _gender;
   private Text             _points;
   private final CharacterWidget  _widget;
   private final CharacterDisplay _display;
   public CharacterFile     _charFile;

   private String           _originalCharacterName = null;

   public MainBlock(CharacterWidget widget, CharacterFile charFile, CharacterDisplay display) {
      _widget = widget;
      _charFile = charFile;
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent) {

      Group topGroup = createGroup(parent, "", 6/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);

      createLabel(topGroup, "Name:", SWT.LEFT, 2/*hSpan*/, null);
      ArrayList<String> namesList = new ArrayList<>();
      namesList.addAll(_charFile.getCharacterNames());
      _name = createText(topGroup, "", true/*editable*/, 2/*hSpan*/);
      _points = createText(topGroup, "0 points", false, 2/*hSpan*/);

      createLabel(topGroup, "Race:", SWT.LEFT, 2/*hSpan*/, null);
      _race = createCombo(topGroup, SWT.READ_ONLY, 2/*hSpan*/, Race.getRaceNames(CombatServer._isServer/*includeNPCs*/));
      _race.setText(Race.NAME_Human);
      List<Gender> genders = Race.getGendersForRace(_race.getText());
      List<String> genderNames = new ArrayList<>();
      for (Gender gender : genders) {
         genderNames.add(gender._name);
      }
      _gender = createCombo(topGroup, SWT.READ_ONLY, 2/*hSpan*/, genderNames);
      _gender.setText(genderNames.get(0));

      createLabel(topGroup, "", SWT.LEFT, 1/*hSpan*/, null);
      _openButton = createButton(topGroup, " Open ", 1/*hSpan*/, null, this);
      _newButton = createButton(topGroup, "  New ", 1/*hSpan*/, null, this);
      _genButton = createButton(topGroup, "Generate...", 1/*hSpan*/, null, this);
      _saveButton = createButton(topGroup, " Save ", 1/*hSpan*/, null, this);
      createLabel(topGroup, "", SWT.LEFT, 1/*hSpan*/, null);

      topGroup.setTabList(new Control[] { _name, _race, _openButton, _newButton, _saveButton, _genButton});
      _name.addModifyListener(this);
      _race.addModifyListener(this);
      _gender.addModifyListener(this);
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      // handle the 'save/add/delete' character button
      if (e.widget == _genButton) {
         GenerateCharacterDialog genCharDialog = new GenerateCharacterDialog(_genButton.getShell(), _race.getText());
         int genPoints = genCharDialog.open();
         if (genCharDialog._cancelSelected) {
            return;
         }
         String race = genCharDialog.getRace();
         String equipment = genCharDialog.getEquipment();
         if (genPoints != -1) {
            Character generatedChar = CharacterGenerator.generateRandomCharacter(genPoints, race, equipment, true/*genNewPseudoRndNumber*/, true/*printCharacter*/);
            generatedChar.setName("? " + generatedChar.getRace().getName() + " " + genPoints);
            _widget.setCharacter(generatedChar.clone());
            _originalCharacterName = null;
            _widget.updateDisplayFromCharacter();
         }
      }
      if (e.widget == _saveButton) {
         if (!_widget._character.getName().equals(_originalCharacterName)) {
            if (_originalCharacterName != null) {
               _charFile.delCharacter(_originalCharacterName);
               if (CombatServer._isServer) {
                  // remove this name from all the drop-down boxes in the arena map view:
                  CombatServer._this.removeCharacter(_originalCharacterName);
               }
            }
            if (CombatServer._isServer) {
               // add this name to all the drop-down boxes in the arena map view:
               CombatServer._this.addNewCharacter(_widget._character.getName());
            }
         }
         _charFile.putCharacter(_widget._character);
         _charFile.writeNameToCharMapToFile(_name.getText());
      }
      else if (e.widget == _openButton) {
         OpenCharacter charDialog = new OpenCharacter(e.display.getShells()[0], _charFile, 0);
         ExitButton exitButton = charDialog.open();
         if (exitButton != ExitButton.Cancel) {
            String characterName = charDialog._selectedName;
            Character selectedCharacter = _charFile.getCharacter(characterName);
            Character copy = selectedCharacter.clone();
            Gender gender = copy.getGender();
            if (exitButton == ExitButton.Open) {
               _originalCharacterName = characterName;
            }
            if (exitButton == ExitButton.Copy) {
               copy.setName("Copy of " + characterName);
               _originalCharacterName = null;
            }
            _widget.setCharacter(copy);
            _name.setText(copy.getName());
            _race.setText(copy.getRace().getName());
            // Setting the race text can set the gender attribute of the current character (which is 'copy')
            // so we need to preserver the initial intended gender, and use that here:
            _gender.setText(gender._name);
            _widget.updateDisplayFromCharacter();
         }
      }
      else if (e.widget == _newButton) {
         Character newChar = new Character();
         newChar.setName("New Character");
         _widget.setCharacter(newChar);
         _name.setText(newChar.getName());
         _race.setText(newChar.getRace().getName());
         _originalCharacterName = null;
         _widget.updateDisplayFromCharacter();
      }
   }


   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (CharacterWidget._inModify) {
         return;
      }
      try {
         CharacterWidget._inModify = true;
         if (e.widget == _name) {
            String newName = _name.getText();
            if (_display != null) {
               _display._shell.setText(newName);
            }
            boolean saveEnabled = true;
            if (!newName.equals(_originalCharacterName)) {
               Character newChar = _charFile.getCharacter(newName);

               if (newChar != null) {
                  // character name already in use!
                  saveEnabled = false;
               }
               else {
                  if (_widget._character != null) {
                     _widget._character.setName(_name.getText());
                  }
               }
            }
            _saveButton.setEnabled(saveEnabled);
         }
         else if ((e.widget == _race) || (e.widget == _gender)) {
            if (e.widget == _race) {
               String oldGender = _gender.getText();
               List<Gender> genders = Race.getGendersForRace(_race.getText());
               boolean genderFound = false;
               _gender.removeAll();
               for (Gender gender : genders) {
                  if (oldGender.equals(gender._name)) {
                     genderFound = true;
                  }
                  _gender.add(gender._name);
               }
               if (genderFound) {
                  _gender.setText(oldGender);
               }
               else {
                  _gender.setText(genders.get(0)._name);
               }
            }
            if (_widget._character != null) {
               _widget._character.setRace(_race.getText(), Gender.getByName(_gender.getText()));
            }
            _widget.updateDisplayFromCharacter();
         }
         else {
            if (_widget._character != null) {
               _widget.updateCharacterFromDisplay();
               updateSaveButton();
            }
         }
      } finally {
         CharacterWidget._inModify = false;
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      if (character == null) {
         _name.setText("");
         _race.setText("");
         _gender.setText(Gender.MALE._name);
         _saveButton.setEnabled(false);
      }
      else {
         _saveButton.setEnabled(true);
         _race.setText(character.getRace().getName());
         _gender.setText(character.getGender()._name);
         _name.setText(character.getName());
      }
      updateSaveButton();
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      _points.setText(((character == null) ? "" : String.valueOf(character.getPointTotal())) + " points");
      _race.setEnabled(character != null);
      updateSaveButton();
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character == null) {
         return;
      }
      character.setName(_name.getText());
      character.setRace(_race.getText(), Gender.getByName(_gender.getText()));
      updateSaveButton();
   }

   private void updateSaveButton() {
      boolean enableSaveButton = false;
      if (_originalCharacterName == null) {
         enableSaveButton = true;
      }
      else {
         Character origChar = _charFile.getCharacter(_originalCharacterName);
         if ((origChar == null) || (!origChar.equals(_widget._character))) {
            enableSaveButton = true;
         }
      }
      _saveButton.setEnabled(enableSaveButton);
   }
}
