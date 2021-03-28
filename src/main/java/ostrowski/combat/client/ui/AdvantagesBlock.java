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
   private final        CharacterWidget display;
   private              Group           advGroup;
   private static final int             ADV_COUNT               = 9;
   private final        Combo[]         advCombo                = new Combo[ADV_COUNT];
   private final        Combo[]         advLevel                = new Combo[ADV_COUNT];
   private final        Text[]          advCost                 = new Text[advCombo.length];
   // These are used to cache the current advantages set. If this set doesn't change
   // in updateDisplayFromCharacter, then there is nothing to do in this block:
   private final        List<Advantage> currentRacialAdvantages = new ArrayList<>();
   private final        List<Advantage> currentAdvantages       = new ArrayList<>();
   private              Race            currentRace             = null;


   public AdvantagesBlock(CharacterWidget display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      advGroup = createGroup(parent, "Dis/Advantages", 3/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(advGroup, "Name", SWT.LEFT, 1, null);
      createLabel(advGroup, "Level", SWT.CENTER, 1, null);
      createLabel(advGroup, "Cost", SWT.CENTER, 1, null);
      List<String> existingProperties = new ArrayList<>();
      List<String> advNames = Advantage.getAdvantagesNames(existingProperties, display.character.getRace());
      advNames.add(0, "---");
      for (int i = 0; i < advCombo.length ; i++) {
         advCombo[i] = createCombo(advGroup, SWT.READ_ONLY, 1, advNames);
         advLevel[i] = createCombo(advGroup, SWT.READ_ONLY, 1, new ArrayList<>());
         advLevel[i].setEnabled(false);
         advLevel[i].setSize(50, advLevel[i].getItemHeight());
         advLevel[i].add("---");
         advLevel[i].setText("---");
         advCost[i] = createText(advGroup, "0", false/*editable*/, 1/*hSpan*/);
         advCombo[i].addModifyListener(this);
         advLevel[i].addModifyListener(this);
      }
      advGroup.setTabList(advCombo);
   }

   @Override
   public void refreshDisplay(Character character)
   {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      boolean oldInModify = CharacterWidget.inModify;
      CharacterWidget.inModify = true;
      updateDisplayFromCharacter(character);
      CharacterWidget.inModify = oldInModify;
//       int i=0;
//       if (character != null) {
//          List<Advantage> racialAdvantages =  character.getRace().getAdvantagesList();
//          for (Advantage advantage : racialAdvantages) {
//             advCost[i].setText("(" + advantage.getCost(character.getRace()) + ")");
//             advLevel[i].setEnabled(advantage.hasLevels());
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
//                advCost[i].setText("(" + advantage.getCost(character.getRace()) + ")");
//                advLevel[i].setEnabled(advantage.hasLevels());
//                i++;
//             }
//          }
//       }
//       boolean enabled = true;
//       // clear out any remaining advantages combo boxes
//       for ( ; i<_advCombo.length ; i++) {
//          advCost[i].setText("(0)");
//          advLevel[i].setEnabled(false);
//          advCombo[i].setEnabled(enabled);
//          enabled = false;
//       }
   }

   @Override
   public void updateCharacterFromDisplay(Character character)
   {
      List<Advantage> newAdv = new ArrayList<>();
      List<Advantage> racialAdvantages = (character==null) ? new ArrayList<>() : character.getRace().getAdvantagesList();
      for (int i = 0; i < advCombo.length ; i++) {
         String advName = advCombo[i].getText();
         if (!advName.equals("---")) {
            Advantage adv = Advantage.getAdvantage(advName);
            if (adv != null) {
               if (adv.hasLevels()) {
                  String levelStr = advLevel[i].getText();
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

   @Override
   public void updateDisplayFromCharacter(Character character)
   {
      boolean old = CharacterWidget.inModify;
      CharacterWidget.inModify = true;
      // updateDisplayFromCharacter is used to update fields that do have ModifyListeners:
      long start = System.currentTimeMillis();

      List<Advantage> racialAdvantages = (character==null) ? new ArrayList<>() : character.getRace().getAdvantagesList();
      List<Advantage> advantages = (character==null) ? new ArrayList<>() : character.getAdvantagesList();
      Race race = (character == null) ? null : character.getRace();
      long end = System.currentTimeMillis();
      Rules.diag("getAdvantages took " + ((end-start) /1000.0) + " seconds.");

      if ((racialAdvantages.size() == currentRacialAdvantages.size()) &&
          racialAdvantages.containsAll(currentRacialAdvantages) &&
          (advantages.size() == currentAdvantages.size()) &&
          advantages.containsAll(currentAdvantages) &&
          (race == currentRace)) {
         // both list match, nothing to do
         return;
      }

      currentAdvantages.clear();
      currentRacialAdvantages.clear();
      // add clones of each advantage, so if a level of one changes,
      // we don't reflect that change in our list.
      for (Advantage adv : advantages) {
         currentAdvantages.add(adv.clone());
      }
      for (Advantage adv : racialAdvantages) {
         currentRacialAdvantages.add(adv.clone());
      }
      currentRace = race;

      int i=0;
      for (Advantage advantage : racialAdvantages) {
         if (i >= advCombo.length) {
            break;
         }
         advCombo[i].removeAll();
         advCombo[i].add(advantage.getName());
         advCombo[i].setText(advantage.getName());
         advCombo[i].setEnabled(false);
         advCost[i].setText("---");
         if (advantage.hasLevels()) {
            List<String> levelNames = advantage.getLevelNames();
            String levelName = advantage.getLevelName();
            setComboContents(advLevel[i], levelNames);
            advLevel[i].setEnabled(true);
            advLevel[i].setText(levelName);
         }
         else {
            advLevel[i].setEnabled(false);
            advLevel[i].add("---");
            advLevel[i].setText("---");
         }
         i++;
      }
      int racialAdvCount = i;
      for (Advantage advantage : advantages) {
         int currentI = i;
         boolean racialAdv = false;
         for (int j=0 ; j<racialAdvCount ; j++) {
            if (j >= advCombo.length) {
               break;
            }
            if (advCombo[j].getText().equals(advantage.getName())) {
               i = j;
               currentI--;
               racialAdv = true;
               break;
            }
         }
         if (i >= advCost.length) {
            break;
         }

         advCost[i].setText("(" + advantage.getCost(race) + ")");
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
            advCombo[i].add(advantage.getName());
            advCombo[i].setText(advantage.getName());
            setComboContents(advCombo[i], existingProperties);

            advCombo[i].setEnabled(character != null);
         }
         if (advantage.hasLevels()) {
            List<String> levelNames = advantage.getLevelNames();
            String levelName = advantage.getLevelName();
            setComboContents(advLevel[i], levelNames);
            advLevel[i].setText(levelName);
            advLevel[i].setEnabled(true);
         }
         else {
            advLevel[i].setEnabled(false);
            advLevel[i].add("---");
            advLevel[i].setText("---");
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
      for (; i < advCombo.length ; i++) {
         advCombo[i].removeAll();
         advCombo[i].add("---");
         for (String advName : advNames) {
            if (!existingProperties.contains(advName)) {
               advCombo[i].add(advName);
            }
         }
         advCombo[i].setText("---");
         advCombo[i].setEnabled(enabled);
         enabled = false;
         advCost[i].setText("(0)");
         advLevel[i].setEnabled(false);
         advLevel[i].add("---");
         advLevel[i].setText("---");
      }
      CharacterWidget.inModify = old;
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
      if (CharacterWidget.inModify) {
         return;
      }

      display.modifyText(e, this);
      updateTabList();
   }

   public void updateTabList() {
      List<Control> tabList = new ArrayList<>();
      for (int i = 0; i < advCombo.length ; i++) {
         tabList.add(advCombo[i]);
         if (advLevel[i].isEnabled()) {
            tabList.add(advLevel[i]);
         }
      }
      advGroup.setTabList(tabList.toArray(new Control[0]));
   }
}
