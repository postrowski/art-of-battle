/*
 * Created on Feb 23, 2007
 *
 */
package ostrowski.combat.common;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.enums.Enums;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class TargetPrioritiesWidget extends Helper implements SelectionListener
{
   public        org.eclipse.swt.widgets.List priorityList = null;
   private       List<Character>              combatants   = null;
   private       Button                       upButton     = null;
   private       Button                       dnButton     = null;
   private final CharacterDisplay             display;
   private       int                          selfUniqueID = -1;
   private       byte                         selfTeamID   = -1;

   public TargetPrioritiesWidget(Character self, CharacterDisplay display) {
      setSelf(self);
      this.display = display;
   }
   public void setSelf(Character self) {
      if (self != null) {
         selfUniqueID = self.uniqueID;
         selfTeamID = self.teamID;
      }
   }

   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Target Priority", 2/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      priorityList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
      GridData data = new GridData();
      data.grabExcessVerticalSpace = true;
      data.verticalSpan = 2;
      data.minimumHeight = 60;
      priorityList.setLayoutData(data);
      combatants = new ArrayList<>();
      upButton = createButton(group, "up", 1, null, this);
      dnButton = createButton(group, "dn", 1, null, this);
      priorityList.add("----- Friends below this -----");
      priorityList.addSelectionListener(this);
      combatants.add(null);
      enabelUpDnButtons();
   }

   public boolean updateCombatants(List<Character> combatants) {
      boolean changed = false;
      // all we care about are the names of the combatants
      for (Character newCombatant : combatants) {
         if (newCombatant.uniqueID != selfUniqueID) {
            boolean combatantFound = false;
            for (Character knownCombatant : this.combatants) {
               if (knownCombatant != null) { // null divides friend & enemies
                  if (knownCombatant.uniqueID == newCombatant.uniqueID) {
                     combatantFound = true;
                     // update our copy of the combatants information
                     knownCombatant.copyData(newCombatant);
                     break;
                  }
               }
            }
            if (!combatantFound) {
               if ((selfTeamID != newCombatant.teamID) ||
                   (selfTeamID == Enums.TEAM_INDEPENDENT)) {
                  // add new combatants at the top of the list, so they default to be an enemy
                  priorityList.add(newCombatant.getName(), 0);
                  // keep the combatants list synchronized with the priority list
                  this.combatants.add(0, newCombatant);
               }
               else {
                  // add new combatants at the bottom of the list, so they default to be a friend
                  priorityList.add(newCombatant.getName());
                  // keep the combatants list synchronized with the priority list
                  this.combatants.add(newCombatant);
               }
               changed = true;
            }
         }
      }
      // check if anyone has been removed from battle:
      for (int knownIndex = 0; knownIndex < this.combatants.size(); knownIndex++) {
         Character knownCombatant = this.combatants.get(knownIndex);
         if (knownCombatant != null) { // null divides friend & enemies
            boolean combatantFound = false;
            for (Character newCombatant : combatants) {
               if (newCombatant.uniqueID != selfUniqueID) {
                  if (knownCombatant.uniqueID == newCombatant.uniqueID) {
                     combatantFound = true;
                     break;
                  }
               }
            }
            if (!combatantFound) {
               priorityList.remove(knownIndex);
               this.combatants.remove(knownIndex);
               knownIndex--;
               changed = true;
            }
         }
      }

      // If nothing is selected yet, select one now
      String[] selected = priorityList.getSelection();
      if ((selected == null) || (selected.length == 0)) {
         if (this.combatants.size() > 0) {
            priorityList.select(0);
         }
      }
      if (changed) {
         enabelUpDnButtons();
         updateServerWithTargets();
      }
      return changed;
   }

   public void updateServerWithTargets() {
      if (display != null) {
         List<Character> orderedEnemies = new ArrayList<>();
         for (Character combatant : combatants) {
            if (combatant == null) {
               // The 'null' entry corresponds to the "friend below this" entry.
               // When we reach it, stop adding enemies.
               break;
            }
            orderedEnemies.add(combatant);
         }
         display.updateTargetPriorities(orderedEnemies);
      }
   }

   public void updateCombatant(Character character)
   {
      int index = 0;
      for (Character knownCombatant : combatants) {
         if (knownCombatant != null) { // null divides friend & enemies
            if (knownCombatant.uniqueID == character.uniqueID) {
               // update our copy of the combatants information
               String oldName = knownCombatant.getName();
               String newName = character.getName();
               knownCombatant.copyData(character);
               if (!oldName.equals(newName)) {
                  priorityList.setItem(index, newName);
               }
               return;
            }
         }
         index++;
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e)
   {
      boolean modified = false;
      int selection = priorityList.getSelectionIndex();
      if (e.widget == upButton) {
         if (selection > 0) {
            String[] selected = priorityList.getSelection();
            String selectedItem = selected[0];
            priorityList.remove(selection);
            priorityList.add(selectedItem, selection - 1);
            priorityList.select(selection - 1);
            Character character = combatants.remove(selection);
            combatants.add(selection - 1, character);
            modified = true;
         }
      }
      else if (e.widget == dnButton) {
         if (selection < (priorityList.getItemCount() - 1)) {
            String[] selected = priorityList.getSelection();
            String selectedItem = selected[0];
            priorityList.remove(selection);
            priorityList.add(selectedItem, selection + 1);
            priorityList.select(selection + 1);
            Character character = combatants.remove(selection);
            combatants.add(selection + 1, character);
            modified = true;
         }
      }
      else if (e.widget == priorityList) {
         enabelUpDnButtons();
      }

      if (modified) {
         enabelUpDnButtons();
         updateServerWithTargets();
      }
   }
   public void setTeam(byte teamId) {
      for (int i = 0; i < combatants.size() ; i++) {
         Character combatant = combatants.get(i);
         if (combatant == null) {
            // null divides friend & enemies
            return;
         }
         if (combatant.teamID == teamId) {
            combatants.remove(i);
            priorityList.remove(i);
            i--;
            combatants.add(combatant);
            priorityList.add(combatant.getName());
         }
      }
   }

   public void enabelUpDnButtons() {
      int selection = priorityList.getSelectionIndex();
      if (selection == -1) {
         upButton.setEnabled(false);
         dnButton.setEnabled(false);
      }
      else {
         int itemCount = priorityList.getItemCount();
         if (itemCount >= 2) {
            upButton.setEnabled(selection != 0);
            dnButton.setEnabled(selection != (itemCount - 1));
         }
      }
   }
   public List<Character> getOrderedEnemies() {
      List<Character> orderedEnemies = new ArrayList<>();
      for (Character combatant : combatants) {
         if (combatant == null) {
            // The 'null' entry corresponds to the "friend below this" entry.
            // When we reach it, stop adding enemies.
            break;
         }
         orderedEnemies.add(combatant);
      }
      return orderedEnemies;
   }
   public boolean ignoreEnemy(Character enemy) {
      for (int i = 0; i < combatants.size() ; i++) {
         if (combatants.get(i) == null) {
            // If we haven't found it before we found our friends, then we are already ignoring this enemy.
            return false;
         }
         if ((combatants.get(i)).uniqueID == enemy.uniqueID) {
            Character myEnemy = (combatants.get(i));
            combatants.set(i, combatants.get(i + 1));
            combatants.set(i + 1, myEnemy);

            // TODO: which item is selected? can we preserve this?
            String enemyName = priorityList.getItem(i);
            priorityList.remove(i);
            priorityList.add(enemyName, i + 1);

            if (combatants.get(i) == null) {
               // Once we push this past our friends list, stop pushing it down.
               updateServerWithTargets();
               return true;
            }
         }
      }
      return false;
   }
   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }

}
