/*
 * Created on Oct 30, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.Enums;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class AdvantagesBlock extends Helper implements Enums, IUIBlock, ModifyListener
{
   private final CharacterWidget  _display;
   private Group            _advGroup;
   private static final int ADV_COUNT = 9;
   private final Combo[]            _advCombo      = new Combo[ADV_COUNT];
   private final Combo[]            _advLevel      = new Combo[ADV_COUNT];
   private final Text[]             _advCost       = new Text[_advCombo.length];

   public AdvantagesBlock(CharacterWidget display) {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      _advGroup = createGroup(parent, "Dis/Advantages", 3/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(_advGroup, "Name", SWT.LEFT, 1, null);
      createLabel(_advGroup, "Level", SWT.CENTER, 1, null);
      createLabel(_advGroup, "Cost", SWT.CENTER, 1, null);
      List<String> existingProperties = new ArrayList<>();
      List<String> advNames = Advantage.getAdvantagesNames(existingProperties, _display._character.getRace());
      advNames.add(0, "---");
      for (int i=0 ; i<_advCombo.length ; i++) {
         _advCombo[i] = createCombo(_advGroup, SWT.READ_ONLY, 1, advNames);
         _advLevel[i] = createCombo(_advGroup, SWT.READ_ONLY, 1, new ArrayList<>());
         _advLevel[i].setEnabled(false);
         _advLevel[i].setSize(50, _advLevel[i].getItemHeight());
         _advLevel[i].add("---");
         _advLevel[i].setText("---");
         _advCost[i] = createText(_advGroup, "0", false/*editable*/, 1/*hSpan*/);
         _advCombo[i].addModifyListener(this);
         _advLevel[i].addModifyListener(this);
      }
      _advGroup.setTabList(_advCombo);
   }

   @Override
   public void refreshDisplay(Character character)
   {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      boolean oldInModify = CharacterWidget._inModify;
      CharacterWidget._inModify = true;
      updateDisplayFromCharacter(character);
      CharacterWidget._inModify = oldInModify;
//       int i=0;
//       if (character != null) {
//          List<Advantage> racialAdvantages =  character.getRace().getAdvantagesList();
//          for (Advantage advantage : racialAdvantages) {
//             _advCost[i].setText("(" + advantage.getCost(character.getRace()) + ")");
//             _advLevel[i].setEnabled(advantage.hasLevels());
//             i++;
//          }
//          List<Advantage> advantages = character.getAdvantagesList();
//          for (Advantage advantage : advantages) {
//             boolean isRacial = false;
//             for (Advantage racialAdvantage : racialAdvantages) {
//                if (racialAdvantage.getName().equals(advantage.getName())) {
//                   isRacial = true;
//                   break;
//                }
//             }
//             if (!isRacial) {
//                _advCost[i].setText("(" + advantage.getCost(character.getRace()) + ")");
//                _advLevel[i].setEnabled(advantage.hasLevels());
//                i++;
//             }
//          }
//       }
//       boolean enabled = true;
//       // clear out any remaining advantages combo boxes
//       for ( ; i<_advCombo.length ; i++) {
//          _advCost[i].setText("(0)");
//          _advLevel[i].setEnabled(false);
//          _advCombo[i].setEnabled(enabled);
//          enabled = false;
//       }
   }

   @Override
   public void updateCharacterFromDisplay(Character character)
   {
      List<Advantage> newAdv = new ArrayList<>();
      List<Advantage> racialAdvantages = (character==null) ? new ArrayList<>() : character.getRace().getAdvantagesList();
      for (int i=0 ; i<_advCombo.length ; i++) {
         String advName = _advCombo[i].getText();
         if (!advName.equals("---")) {
            Advantage adv = Advantage.getAdvantage(advName);
            if (adv != null) {
               if (adv.hasLevels()) {
                  String levelStr = _advLevel[i].getText();
                  adv.setLevelByName(levelStr);
               }
               boolean isRacial = false;
               for (Advantage racialAdv : racialAdvantages) {
                  if (racialAdv.getName().equals(advName)) {
                     // If this racial advantage has levels, and the user has
                     // specifed a level that is not the racial default, then
                     // we need to add it to the list of advantages for the character.
                     isRacial = !racialAdv.hasLevels() || (racialAdv.getLevel() == adv.getLevel());
                  }
               }
               if (!isRacial) {
                  newAdv.add(adv);
               }
            }
         }
      }
      if (character != null) {
         character.setAdvantagesList(newAdv);
      }
   }

   // These are used to cache the current advantages set. If this set doesn't change
   // in updateDisplayFromCharacter, then there is nothing to do in this block:
   private final List<Advantage> _currentRacialAdvantages = new ArrayList<>();
   private final List<Advantage> _currentAdvantages = new ArrayList<>();
   private Race _currentRace = null;

   @Override
   public void updateDisplayFromCharacter(Character character)
   {
      boolean old = CharacterWidget._inModify;
      CharacterWidget._inModify = true;
      // updateDisplayFromCharacter is used to update fields that do have ModifyListeners:
      long start = System.currentTimeMillis();

      List<Advantage> racialAdvantages = (character==null) ? new ArrayList<>() : character.getRace().getAdvantagesList();
      List<Advantage> advantages = (character==null) ? new ArrayList<>() : character.getAdvantagesList();
      Race race = (character == null) ? null : character.getRace();
      long end = System.currentTimeMillis();
      Rules.diag("getAdvantages took " + ((end-start) /1000.0) + " seconds.");

      if ((racialAdvantages.size() == _currentRacialAdvantages.size()) &&
          racialAdvantages.containsAll(_currentRacialAdvantages) &&
          (advantages.size() == _currentAdvantages.size()) &&
          advantages.containsAll(_currentAdvantages) &&
          (race == _currentRace)) {
         // both list match, nothing to do
         return;
      }

      _currentAdvantages.clear();
      _currentRacialAdvantages.clear();
      // add clones of each advantage, so if a level of one changes,
      // we don't reflect that change in our list.
      for (Advantage adv : advantages) {
         _currentAdvantages.add(adv.clone());
      }
      for (Advantage adv : racialAdvantages) {
         _currentRacialAdvantages.add(adv.clone());
      }
      _currentRace = race;

      int i=0;
      for (Advantage advantage : racialAdvantages) {
         if (i >= _advCombo.length) {
            break;
         }
         _advCombo[i].removeAll();
         _advCombo[i].add(advantage.getName());
         _advCombo[i].setText(advantage.getName());
         _advCombo[i].setEnabled(false);
         _advCost[i].setText("---");
         if (advantage.hasLevels()) {
            List<String> levelNames = advantage.getLevelNames();
            String levelName = advantage.getLevelName();
            setComboContents(_advLevel[i], levelNames);
            _advLevel[i].setEnabled(true);
            _advLevel[i].setText(levelName);
         }
         else {
            _advLevel[i].setEnabled(false);
            _advLevel[i].add("---");
            _advLevel[i].setText("---");
         }
         i++;
      }
      int racialAdvCount = i;
      for (Advantage advantage : advantages) {
         int currentI = i;
         boolean racialAdv = false;
         for (int j=0 ; j<racialAdvCount ; j++) {
            if (j >= _advCombo.length) {
               break;
            }
            if (_advCombo[j].getText().equals(advantage.getName())) {
               i = j;
               currentI--;
               racialAdv = true;
               break;
            }
         }
         if (i >= _advCost.length) {
            break;
         }

         _advCost[i].setText("(" + advantage.getCost(race) + ")");
         if (!racialAdv) {
            List<String> existingProperties;
            if (character != null) {
               existingProperties = character.getPropertyNames();
            }
            else {
               existingProperties = new ArrayList<>();
            }
            existingProperties.remove(advantage.getName());
            existingProperties = Advantage.getAdvantagesNames(existingProperties, race);
            existingProperties.add(0, "---");
            _advCombo[i].add(advantage.getName());
            _advCombo[i].setText(advantage.getName());
            setComboContents(_advCombo[i], existingProperties);

            _advCombo[i].setEnabled(character != null);
         }
         if (advantage.hasLevels()) {
            List<String> levelNames = advantage.getLevelNames();
            String levelName = advantage.getLevelName();
            setComboContents(_advLevel[i], levelNames);
            _advLevel[i].setText(levelName);
            _advLevel[i].setEnabled(true);
         }
         else {
            _advLevel[i].setEnabled(false);
            _advLevel[i].add("---");
            _advLevel[i].setText("---");
         }
         i = currentI + 1;
      }

      // clear out any remaining advantages combo boxes
      List<String> existingProperties = new ArrayList<>();
      if (character != null) {
         existingProperties = character.getPropertyNames();
      }
      List<String> advNames = Advantage.getAdvantagesNames(existingProperties, race);
      boolean enabled = true;
      for ( ; i<_advCombo.length ; i++) {
         _advCombo[i].removeAll();
         _advCombo[i].add("---");
         for (String advName : advNames) {
            if (!existingProperties.contains(advName)) {
               _advCombo[i].add(advName);
            }
         }
         _advCombo[i].setText("---");
         _advCombo[i].setEnabled(enabled);
         enabled = false;
         _advCost[i].setText("(0)");
         _advLevel[i].setEnabled(false);
         _advLevel[i].add("---");
         _advLevel[i].setText("---");
      }
      CharacterWidget._inModify = old;
   }

   /**
    * @param combo
    * @param availableItems
    */
   private static void setComboContents(Combo combo, List<String> availableItems)
   {
      String curSelection = combo.getText();
      int index;
      for (index=0 ; index<availableItems.size() ; index++) {
         if (index<combo.getItemCount()) {
            combo.setItem(index, availableItems.get(index));
         }
         else {
            combo.add(availableItems.get(index));
         }
      }
      for ( ; index<combo.getItemCount() ; ) {
         combo.remove(index);
      }
      combo.setText(curSelection);
   }

   @Override
   public void modifyText(ModifyEvent e)
   {
      if (CharacterWidget._inModify) {
         return;
      }

      _display.modifyText(e, this);
      updateTabList();
   }

   public void updateTabList() {
      ArrayList<Control> tabList = new ArrayList<>();
      for (int i=0 ; i<_advCombo.length ; i++) {
         tabList.add(_advCombo[i]);
         if (_advLevel[i].isEnabled()) {
            tabList.add(_advLevel[i]);
         }
      }
      _advGroup.setTabList(tabList.toArray(new Control[0]));
   }
}
