/*
 * Created on Jun 14, 2006
 *
 */
package ostrowski.combat.client.ui;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.TargetPrioritiesWidget;
import ostrowski.ui.Helper;

public class TargetPriorityBlock extends Helper implements IUIBlock
{
   public TargetPrioritiesWidget _priorityWidget;

   public TargetPriorityBlock(CharacterDisplay display) {
      _priorityWidget = new TargetPrioritiesWidget(null, display);
   }

   @Override
   public void buildBlock(Composite parent) {
      _priorityWidget.buildBlock(parent);
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      _priorityWidget.setSelf(character);
   }
   @Override
   public void refreshDisplay(Character character) {
      _priorityWidget.setSelf(character);
   }
   @Override
   public void updateCharacterFromDisplay(Character character) {
      _priorityWidget.setSelf(character);
   }
   public void updateCombatants(List<Character> combatants) {
      _priorityWidget.updateCombatants(combatants);
   }
   public void updateCombatant(Character character) {
      _priorityWidget.updateCombatant(character);
   }
   public void updateServerWithTargets() {
      _priorityWidget.updateServerWithTargets();
   }

   public void setTeam(byte teamId) {
      _priorityWidget.setTeam(teamId);
   }

   public List<Character> getOrderedEnemies() {
      return _priorityWidget.getOrderedEnemies();
   }
   public boolean ignoreEnemy(Character enemy) {
      return _priorityWidget.ignoreEnemy(enemy);
   }

}
