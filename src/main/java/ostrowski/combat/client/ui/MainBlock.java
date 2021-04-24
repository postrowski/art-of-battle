/*
 * Created on May 19, 2006
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.client.OpenCharacter;
import ostrowski.combat.client.OpenCharacter.ExitButton;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class MainBlock extends Helper implements ModifyListener, SelectionListener, IUIBlock
{
   private       Button           openButton;
   private       Button           newButton;
   private       Button           saveButton;
   private       Button           genButton;
   private       Text             name;
   private       Combo            race;
   private       Combo            gender;
   private       Text             points;
   private final CharacterWidget  widget;
   private final CharacterDisplay display;
   public final  CharacterFile    charFile;
   private       String           originalCharacterName = null;

   public MainBlock(CharacterWidget widget, CharacterFile charFile, CharacterDisplay display) {
      this.widget = widget;
      this.charFile = charFile;
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {

      Group topGroup = createGroup(parent, "", 6/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);

      createLabel(topGroup, "Name:", SWT.LEFT, 2/*hSpan*/, null);
      name = createText(topGroup, "", true/*editable*/, 2/*hSpan*/);
      points = createText(topGroup, "0 points", false, 2/*hSpan*/);

      createLabel(topGroup, "Race:", SWT.LEFT, 2/*hSpan*/, null);
      race = createCombo(topGroup, SWT.READ_ONLY, 2/*hSpan*/, Race.getRaceNames(CombatServer.isServer/*includeNPCs*/));
      race.setText(Race.NAME_Human);
      List<Gender> genders = Race.getGendersForRace(race.getText());
      List<String> genderNames = new ArrayList<>();
      for (Gender gender : genders) {
         genderNames.add(gender.name);
      }
      gender = createCombo(topGroup, SWT.READ_ONLY, 2/*hSpan*/, genderNames);
      gender.setText(genderNames.get(0));

      createLabel(topGroup, "", SWT.LEFT, 1/*hSpan*/, null);
      openButton = createButton(topGroup, " Open ", 1/*hSpan*/, null, this);
      newButton = createButton(topGroup, "  New ", 1/*hSpan*/, null, this);
      genButton = createButton(topGroup, "Generate...", 1/*hSpan*/, null, this);
      saveButton = createButton(topGroup, " Save ", 1/*hSpan*/, null, this);
      createLabel(topGroup, "", SWT.LEFT, 1/*hSpan*/, null);

      topGroup.setTabList(new Control[] {name, race, openButton, newButton, saveButton, genButton});
      name.addModifyListener(this);
      race.addModifyListener(this);
      gender.addModifyListener(this);
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      // handle the 'save/add/delete' character button
      if (e.widget == genButton) {
         GenerateCharacterDialog genCharDialog = new GenerateCharacterDialog(genButton.getShell(), race.getText());
         int genPoints = genCharDialog.open();
         if (genCharDialog.cancelSelected) {
            return;
         }
         String race = genCharDialog.getRace();
         String equipment = genCharDialog.getEquipment();
         if (genPoints != -1) {
            Character generatedChar = CharacterGenerator.generateRandomCharacter(genPoints, race, equipment, true/*genNewPseudoRndNumber*/, true/*printCharacter*/);
            generatedChar.setName("? " + generatedChar.getRace().getName() + " " + genPoints);
            widget.setCharacter(generatedChar.clone());
            originalCharacterName = null;
            widget.updateDisplayFromCharacter();
         }
      }
      if (e.widget == saveButton) {
         if (!widget.character.getName().equals(originalCharacterName)) {
            if (originalCharacterName != null) {
               charFile.delCharacter(originalCharacterName);
               if (CombatServer.isServer) {
                  // remove this name from all the drop-down boxes in the arena map view:
                  CombatServer._this.removeCharacter(originalCharacterName);
               }
            }
            if (CombatServer.isServer) {
               // add this name to all the drop-down boxes in the arena map view:
               CombatServer._this.addNewCharacter(widget.character.getName());
            }
         }
         charFile.putCharacter(widget.character);
         charFile.writeNameToCharMapToFile(name.getText());
         updateSaveButton();
      }
      else if (e.widget == openButton) {
         OpenCharacter charDialog = new OpenCharacter(e.display.getShells()[0], charFile, 0);
         ExitButton exitButton = charDialog.open();
         if (exitButton != ExitButton.Cancel) {
            String characterName = charDialog.selectedName;
            Character selectedCharacter = charFile.getCharacter(characterName);
            Character copy = selectedCharacter.clone();
            Gender gender = copy.getGender();
            if (exitButton == ExitButton.Open) {
               originalCharacterName = characterName;
            }
            if (exitButton == ExitButton.Copy) {
               copy.setName("Copy of " + characterName);
               originalCharacterName = null;
            }
            widget.setCharacter(copy);
            name.setText(copy.getName());
            race.setText(copy.getRace().getName());
            // Setting the race text can set the gender attribute of the current character (which is 'copy')
            // so we need to preserver the initial intended gender, and use that here:
            this.gender.setText(gender.name);
            widget.updateDisplayFromCharacter();
         }
      }
      else if (e.widget == newButton) {
         Character newChar = new Character();
         newChar.setName("New Character");
         widget.setCharacter(newChar);
         name.setText(newChar.getName());
         race.setText(newChar.getRace().getName());
         originalCharacterName = null;
         widget.updateDisplayFromCharacter();
      }
   }


   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (CharacterWidget.inModify) {
         return;
      }
      try {
         CharacterWidget.inModify = true;
         if (e.widget == name) {
            String newName = name.getText();
            if (display != null) {
               display.shell.setText(newName);
            }
            boolean saveEnabled = true;
            if (!newName.equals(originalCharacterName)) {
               Character newChar = charFile.getCharacter(newName);

               if (newChar != null) {
                  // character name already in use!
                  saveEnabled = false;
               }
               else {
                  if (widget.character != null) {
                     widget.character.setName(name.getText());
                  }
               }
            }
            saveButton.setEnabled(saveEnabled);
         }
         else if ((e.widget == race) || (e.widget == gender)) {
            if (e.widget == race) {
               String oldGender = gender.getText();
               List<Gender> genders = Race.getGendersForRace(race.getText());
               boolean genderFound = false;
               gender.removeAll();
               for (Gender gender : genders) {
                  if (oldGender.equals(gender.name)) {
                     genderFound = true;
                  }
                  this.gender.add(gender.name);
               }
               if (genderFound) {
                  gender.setText(oldGender);
               }
               else {
                  gender.setText(genders.get(0).name);
               }
            }
            if (widget.character != null) {
               widget.character.setRace(race.getText(), Gender.getByName(gender.getText()));
            }
            widget.updateDisplayFromCharacter();
         }
         else {
            if (widget.character != null) {
               widget.updateCharacterFromDisplay();
               updateSaveButton();
            }
         }
      } finally {
         CharacterWidget.inModify = false;
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      if (character == null) {
         name.setText("");
         race.setText("");
         gender.setText(Gender.MALE.name);
         saveButton.setEnabled(false);
      }
      else {
         saveButton.setEnabled(true);
         race.setText(character.getRace().getName());
         gender.setText(character.getGender().name);
         name.setText(character.getName());
      }
      updateSaveButton();
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      points.setText(((character == null) ? "" : String.valueOf(character.getPointTotal())) + " points");
      race.setEnabled(character != null);
      updateSaveButton();
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character == null) {
         return;
      }
      character.setName(name.getText());
      character.setRace(race.getText(), Gender.getByName(gender.getText()));
      updateSaveButton();
   }

   private void updateSaveButton() {
      boolean enableSaveButton = false;
      if (originalCharacterName == null) {
         enableSaveButton = true;
      }
      else {
         Character origChar = charFile.getCharacter(originalCharacterName);
         if ((origChar == null) || (!origChar.equals(widget.character))) {
            enableSaveButton = true;
         }
      }
      saveButton.setEnabled(enableSaveButton);
   }
}
