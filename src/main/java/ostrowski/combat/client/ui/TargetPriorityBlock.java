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
   public final TargetPrioritiesWidget priorityWidget;

   public TargetPriorityBlock(CharacterDisplay display) {
      priorityWidget = new TargetPrioritiesWidget(null, display);
   }

   @Override
   public void buildBlock(Composite parent) {
      priorityWidget.buildBlock(parent);
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      priorityWidget.setSelf(character);
   }
   @Override
   public void refreshDisplay(Character character) {
      priorityWidget.setSelf(character);
   }
   @Override
   public void updateCharacterFromDisplay(Character character) {
      priorityWidget.setSelf(character);
   }
   public void updateCombatants(List<Character> combatants) {
      priorityWidget.updateCombatants(combatants);
   }
   public void updateCombatant(Character character) {
      priorityWidget.updateCombatant(character);
   }
   public void updateServerWithTargets() {
      priorityWidget.updateServerWithTargets();
   }

   public void setTeam(byte teamId) {
      priorityWidget.setTeam(teamId);
   }

   public List<Character> getOrderedEnemies() {
      return priorityWidget.getOrderedEnemies();
   }
   public boolean ignoreEnemy(Character enemy) {
      return priorityWidget.ignoreEnemy(enemy);
   }

}
