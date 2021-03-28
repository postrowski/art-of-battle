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
   private Combo targetName;
   private Text distance;
   private Text weapon;
   private Text shield;
   private Text armor;
   private Text buildImp;
   private Text buildCut;
   private Text buildBlunt;
   private Text position;
   private Text readyTime;

   private Character              self;
   private List<Character>        combatants;
   private final CharacterDisplay display;
   public TargetBlock(CharacterDisplay display)
   {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent)
   {
      Group group = createGroup(parent, "Target", 2/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "name:", SWT.RIGHT, 1, null);
      targetName = createCombo(group, SWT.READ_ONLY, 1, new ArrayList<>());
      createLabel(group, "distance:", SWT.RIGHT, 1, null);
      distance = createText(group, "0", false, 1);
      createLabel(group, "weapon:", SWT.RIGHT, 1, null);
      weapon = createText(group, "", false, 1);
      createLabel(group, "shield:", SWT.RIGHT, 1, null);
      shield = createText(group, "", false, 1);
      createLabel(group, "armor:", SWT.RIGHT, 1, null);
      armor = createText(group, "", false, 1);
      createLabel(group, "build-vs-imp:", SWT.RIGHT, 1, null);
      buildImp = createText(group, "", false, 1);
      createLabel(group, "build-vs-cut:", SWT.RIGHT, 1, null);
      buildCut = createText(group, "", false, 1);
      createLabel(group, "build-vs-blunt:", SWT.RIGHT, 1, null);
      buildBlunt = createText(group, "", false, 1);
      createLabel(group, "position:", SWT.RIGHT, 1, null);
      position = createText(group, Position.STANDING.name, false, 1);
      createLabel(group, "weapon ready in:", SWT.RIGHT, 1, null);
      readyTime = createText(group, "now", false, 1);

      targetName.addModifyListener(this);
   }

   public void updateTargetFromCharacter(Character target)    {
      targetName.setText(target.getName());
      distance.setText("");
      weapon.setText(target.getLimb(LimbType.HAND_RIGHT).getHeldThingName());
      shield.setText(target.getLimb(LimbType.HAND_LEFT).getHeldThingName());
      armor.setText(target.getArmor().getName());
      buildImp.setText(String.valueOf(target.getBuild(DamageType.IMP)));
      buildCut.setText(String.valueOf(target.getBuild(DamageType.CUT)));
      buildBlunt.setText(String.valueOf(target.getBuild(DamageType.BLUNT)));
      position.setText(target.getPositionName());
      byte actionsNeeded = target.getLimb(LimbType.HAND_RIGHT).getActionsNeededToReady();
      if (actionsNeeded == 0) {
         readyTime.setText("now");
      }
      else {
         readyTime.setText(actionsNeeded + " actions");
      }
   }
   @Override
   public void updateDisplayFromCharacter(Character character)    {
      self = character;
   }
   @Override
   public void refreshDisplay(Character character)    {
      self = character;
   }
   @Override
   public void updateCharacterFromDisplay(Character character)    {
      self = character;
   }
   public void updateCombatants(List<Character> combatants) {
      this.combatants = combatants;
      String previousSelection = targetName.getText();
      targetName.removeAll();
      for (Character combatant : this.combatants) {
         if (!combatant.getName().equals(self.getName())) {
            targetName.add(combatant.getName());
         }
      }
      if ((previousSelection == null) || (previousSelection.length() == 0)) {
         if ((targetName != null) && (targetName.getItemCount() > 0)) {
            previousSelection = targetName.getItem(0);
         }
      }
      if ((previousSelection != null) && (previousSelection.length() > 0)) {
         targetName.setText(previousSelection);
      }
   }

   static boolean inModText = false;
   @Override
   public void modifyText(ModifyEvent e)
   {
      if (!CharacterWidget.inModify) {
         CharacterWidget.inModify = true;
         if (e.widget == targetName) {
            setTargetName(targetName.getText());
            display.refreshDisplay();
         }
         CharacterWidget.inModify = false;
      }
   }

   public int getTargetUniqueID() {
      for (Character combatant : combatants) {
         if (combatant.getName().equals(targetName.getText())) {
            return combatant.uniqueID;
         }
      }
      return -1;
   }

   public void setTargetName(String targetName)
   {
      for (Character combatant : combatants) {
         if (combatant.getName().equals(targetName)) {
            updateTargetFromCharacter(combatant);
            break;
         }
      }
   }
}
