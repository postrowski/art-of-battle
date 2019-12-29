/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.things.LimbType;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class TargetBlock extends Helper implements IUIBlock, ModifyListener
{
   private Combo _targetName;
   private Text _distance;
   private Text _weapon;
   private Text _shield;
   private Text _armor;
   private Text _buildImp;
   private Text _buildCut;
   private Text _buildBlunt;
   private Text _position;
   private Text _readyTime;

   private Character _self;
   private List<Character> _combatants;
   private final CharacterDisplay _display;
   public TargetBlock(CharacterDisplay display)
   {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Target", 2/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "name:", SWT.RIGHT, 1, null);
      _targetName = createCombo(group, SWT.READ_ONLY, 1, new ArrayList<>());
      createLabel(group, "distance:", SWT.RIGHT, 1, null);
      _distance = createText(group, "0", false, 1);
      createLabel(group, "weapon:", SWT.RIGHT, 1, null);
      _weapon = createText(group, "", false, 1);
      createLabel(group, "shield:", SWT.RIGHT, 1, null);
      _shield = createText(group, "", false, 1);
      createLabel(group, "armor:", SWT.RIGHT, 1, null);
      _armor = createText(group, "", false, 1);
      createLabel(group, "build-vs-imp:", SWT.RIGHT, 1, null);
      _buildImp = createText(group, "", false, 1);
      createLabel(group, "build-vs-cut:", SWT.RIGHT, 1, null);
      _buildCut = createText(group, "", false, 1);
      createLabel(group, "build-vs-blunt:", SWT.RIGHT, 1, null);
      _buildBlunt = createText(group, "", false, 1);
      createLabel(group, "position:", SWT.RIGHT, 1, null);
      _position = createText(group, Position.STANDING.name, false, 1);
      createLabel(group, "weapon ready in:", SWT.RIGHT, 1, null);
      _readyTime = createText(group, "now", false, 1);

      _targetName.addModifyListener(this);
   }

   public void updateTargetFromCharacter(Character target)    {
      _targetName.setText(target.getName());
      _distance.setText("");
      _weapon.setText(target.getLimb(LimbType.HAND_RIGHT).getHeldThingName());
      _shield.setText(target.getLimb(LimbType.HAND_LEFT).getHeldThingName());
      _armor.setText(target.getArmor().getName());
      _buildImp.setText(String.valueOf(target.getBuild(DamageType.IMP)));
      _buildCut.setText(String.valueOf(target.getBuild(DamageType.CUT)));
      _buildBlunt.setText(String.valueOf(target.getBuild(DamageType.BLUNT)));
      _position.setText(target.getPositionName());
      byte actionsNeeded = target.getLimb(LimbType.HAND_RIGHT).getActionsNeededToReady();
      if (actionsNeeded == 0) {
         _readyTime.setText("now");
      }
      else {
         _readyTime.setText(actionsNeeded + " actions");
      }
   }
   @Override
   public void updateDisplayFromCharacter(Character character)    {
      _self = character;
   }
   @Override
   public void refreshDisplay(Character character)    {
      _self = character;
   }
   @Override
   public void updateCharacterFromDisplay(Character character)    {
      _self = character;
   }
   public void updateCombatants(List<Character> combatants) {
      _combatants = combatants;
      String previousSelection = _targetName.getText();
      _targetName.removeAll();
      for (Character combatant : _combatants) {
         if (!combatant.getName().equals(_self.getName())) {
            _targetName.add(combatant.getName());
         }
      }
      if ((previousSelection == null) || (previousSelection.length() == 0)) {
         if ((_targetName != null) && (_targetName.getItemCount() > 0)) {
            previousSelection = _targetName.getItem(0);
         }
      }
      if ((previousSelection != null) && (previousSelection.length() > 0)) {
         _targetName.setText(previousSelection);
      }
   }

   static boolean inModText = false;
   @Override
   public void modifyText(ModifyEvent e)
   {
      if (!CharacterWidget._inModify) {
         CharacterWidget._inModify = true;
         if (e.widget == _targetName) {
            setTargetName(_targetName.getText());
            _display.refreshDisplay();
         }
         CharacterWidget._inModify = false;
      }
   }

   public int getTargetUniqueID() {
      for (Character combatant : _combatants) {
         if (combatant.getName().equals(_targetName.getText())) {
            return combatant._uniqueID;
         }
      }
      return -1;
   }

   public void setTargetName(String targetName)
   {
      for (Character combatant : _combatants) {
         if (combatant.getName().equals(targetName)) {
            updateTargetFromCharacter(combatant);
            break;
         }
      }
   }
}
