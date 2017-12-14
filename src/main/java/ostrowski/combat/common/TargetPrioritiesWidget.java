/*
 * Created on Feb 23, 2007
 *
 */
package ostrowski.combat.common;

import java.util.ArrayList;
import java.util.List;

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

public class TargetPrioritiesWidget extends Helper implements SelectionListener
{
   public org.eclipse.swt.widgets.List _priorityList = null;
   private ArrayList<Character>        _combatants   = null;
   private Button                      _upButton     = null;
   private Button                      _dnButton     = null;
   private CharacterDisplay            _display      = null;
   private int                         _selfUniqueID = -1;
   private byte                        _selfTeamID   = -1;

   public TargetPrioritiesWidget(Character self, CharacterDisplay display) {
      setSelf(self);
      _display = display;
   }
   public void setSelf(Character self) {
      if (self != null) {
         _selfUniqueID = self._uniqueID;
         _selfTeamID   = self._teamID;
      }
   }

   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Target Priority", 2/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      _priorityList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
      GridData data = new GridData();
      data.grabExcessVerticalSpace = true;
      data.verticalSpan = 2;
      data.minimumHeight = 60;
      _priorityList.setLayoutData(data);
      _combatants = new ArrayList<>();
      _upButton = createButton(group, "up", 1, null, this);
      _dnButton = createButton(group, "dn", 1, null, this);
      _priorityList.add("----- Friends below this -----");
      _priorityList.addSelectionListener(this);
      _combatants.add(null);
      enabelUpDnButtons();
   }

   public boolean updateCombatants(List<Character> combatants) {
      boolean changed = false;
      // all we care about are the names of the combatants
      for (Character newCombatant : combatants) {
         if (newCombatant._uniqueID != _selfUniqueID) {
            boolean combatantFound = false;
            for (Character knownCombatant : _combatants) {
               if (knownCombatant != null) { // null divides friend & enemies
                  if (knownCombatant._uniqueID == newCombatant._uniqueID) {
                     combatantFound = true;
                     // update our copy of the combatants information
                     knownCombatant.copyData(newCombatant);
                     break;
                  }
               }
            }
            if (!combatantFound) {
               if ((_selfTeamID != newCombatant._teamID) ||
                   (_selfTeamID == Enums.TEAM_INDEPENDENT)) {
                  // add new combatants at the top of the list, so they default to be an enemy
                  _priorityList.add(newCombatant.getName(), 0);
                  // keep the combatants list synchronized with the priority list
                  _combatants.add(0, newCombatant);
               }
               else {
                  // add new combatants at the bottom of the list, so they default to be a friend
                  _priorityList.add(newCombatant.getName());
                  // keep the combatants list synchronized with the priority list
                  _combatants.add(newCombatant);
               }
               changed = true;
            }
         }
      }
      // check if anyone has been removed from battle:
      for (int knownIndex = 0 ;  knownIndex < _combatants.size(); knownIndex++) {
         Character knownCombatant = _combatants.get(knownIndex);
         if (knownCombatant != null) { // null divides friend & enemies
            boolean combatantFound = false;
            for (Character newCombatant : combatants) {
               if (newCombatant._uniqueID != _selfUniqueID) {
                  if (knownCombatant._uniqueID == newCombatant._uniqueID) {
                     combatantFound = true;
                     break;
                  }
               }
            }
            if (!combatantFound) {
               _priorityList.remove(knownIndex);
               _combatants.remove(knownIndex);
               knownIndex--;
               changed = true;
            }
         }
      }

      // If nothing is selected yet, select one now
      String[] selected = _priorityList.getSelection();
      if ((selected == null) || (selected.length == 0)) {
         if (_combatants.size() > 0) {
            _priorityList.select(0);
         }
      }
      if (changed) {
         enabelUpDnButtons();
         updateServerWithTargets();
      }
      return changed;
   }

   public void updateServerWithTargets() {
      if (_display != null) {
         List<Character> orderedEnemies = new ArrayList<>();
         for (int i=0 ; i<_combatants.size() ; i++) {
            if (_combatants.get(i) == null) {
               // The 'null' entry corresponds to the "friend below this" entry.
               // When we reach it, stop adding enemies.
               break;
            }
            orderedEnemies.add(_combatants.get(i));
         }
         _display.updateTargetPriorities(orderedEnemies);
      }
   }

   public void updateCombatant(Character character)
   {
      int index = 0;
      for (Character knownCombatant : _combatants) {
         if (knownCombatant != null) { // null divides friend & enemies
            if (knownCombatant._uniqueID == character._uniqueID) {
               // update our copy of the combatants information
               String oldName = knownCombatant.getName();
               String newName = character.getName();
               knownCombatant.copyData(character);
               if (!oldName.equals(newName)) {
                  _priorityList.setItem(index, newName);
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
      int selection = _priorityList.getSelectionIndex();
      if (e.widget == _upButton) {
         if (selection > 0) {
            String[] selected = _priorityList.getSelection();
            String selectedItem = selected[0];
            _priorityList.remove(selection);
            _priorityList.add(selectedItem, selection-1);
            _priorityList.select(selection-1);
            Character character = _combatants.remove(selection);
            _combatants.add(selection-1, character);
            modified = true;
         }
      }
      else if (e.widget == _dnButton) {
         if (selection < (_priorityList.getItemCount()-1)) {
            String[] selected = _priorityList.getSelection();
            String selectedItem = selected[0];
            _priorityList.remove(selection);
            _priorityList.add(selectedItem, selection+1);
            _priorityList.select(selection+1);
            Character character = _combatants.remove(selection);
            _combatants.add(selection+1, character);
            modified = true;
         }
      }
      else if (e.widget == _priorityList) {
         enabelUpDnButtons();
      }

      if (modified) {
         enabelUpDnButtons();
         updateServerWithTargets();
      }
   }
   public void setTeam(byte teamId) {
      for (int i=0 ; i<_combatants.size() ; i++) {
         Character combatant = _combatants.get(i);
         if (combatant == null) {
            // null divides friend & enemies
            return;
         }
         if (combatant._teamID == teamId) {
            _combatants.remove(i);
            _priorityList.remove(i);
            _combatants.add(combatant);
            _priorityList.add(combatant.getName());
         }
      }
   }

   public void enabelUpDnButtons() {
      int selection = _priorityList.getSelectionIndex();
      if (selection == -1) {
         _upButton.setEnabled(false);
         _dnButton.setEnabled(false);
      }
      else {
         int itemCount = _priorityList.getItemCount();
         if (itemCount >= 2) {
            _upButton.setEnabled(selection != 0);
            _dnButton.setEnabled(selection != (itemCount-1));
         }
      }
   }
   public ArrayList<Character> getOrderedEnemies() {
      ArrayList<Character> orderedEnemies = new ArrayList<>();
      for (int i=0 ; i<_combatants.size() ; i++) {
         if (_combatants.get(i) == null) {
            // The 'null' entry corresponds to the "friend below this" entry.
            // When we reach it, stop adding enemies.
            break;
         }
         orderedEnemies.add(_combatants.get(i));
      }
      return orderedEnemies;
   }
   public boolean ignoreEnemy(Character enemy) {
      for (int i=0 ; i<_combatants.size() ; i++) {
         if (_combatants.get(i) == null) {
            // If we haven't found it before we found our friends, then we are already ignoring this enemy.
            return false;
         }
         if ((_combatants.get(i))._uniqueID == enemy._uniqueID) {
            Character myEnemy = (_combatants.get(i));
            _combatants.set(i, _combatants.get(i+1));
            _combatants.set(i+1, myEnemy);

            // TODO: which item is selected? can we preserve this?
            String enemyName = _priorityList.getItem(i);
            _priorityList.remove(i);
            _priorityList.add(enemyName, i+1);

            if (_combatants.get(i) == null) {
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
