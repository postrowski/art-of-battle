package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.Condition;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.ui.Helper;

public class ConditionBlock extends Helper implements IUIBlock {
   final   CharacterDisplay _display;
   private Text             _initiative;
   private Text             _moveAvail;
   private Text             _position;
   private Text             _actionsAvail;
   private Text             _pain;
   private Text             _wounds;
   private Text             _bleedRate;
   private Text             _painLeft;
   private Text             _painRight;
   private Text             _painMove;
   private Text             _painRetreat;
   private Text             _weaponReady;

   public ConditionBlock(CharacterDisplay display) {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent)
   {
       Group group = createGroup(parent, "Condition", 2/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
       group.setTabList(new Control[] {});
       createLabel(group, "Initiative:", SWT.LEFT, 1, null);
       _initiative = createText(group, null, false, 1);

       createLabel(group, "Weapon:", SWT.LEFT, 1, null);
       _weaponReady = createText(group, null, false, 1);
       createLabel(group, "Movement:", SWT.LEFT, 1, null);
       _moveAvail = createText(group, null, false, 1);

       createLabel(group, "Position:", SWT.LEFT, 1, null);
       _position = createText(group, null, false, 1);
       createLabel(group, "Actions avail.:", SWT.LEFT, 1, null);
       _actionsAvail = createText(group, null, false, 1);
       createLabel(group, "Pain:", SWT.LEFT, 1, null);
       _pain = createText(group, null, false, 1);
       createLabel(group, "Wounds:", SWT.LEFT, 1, null);
       _wounds = createText(group, null, false, 1);
       createLabel(group, "Bleed rate:", SWT.LEFT, 1, null);
       _bleedRate = createText(group, null, false, 1);

       createLabel(group, "Penalties:", SWT.CENTER, 2, new FontData("Arial", 10, SWT.BOLD));
       createLabel(group, "Left arm:", SWT.LEFT, 1, null);
       _painLeft = createText(group, null, false, 1);
       createLabel(group, "Right arm:", SWT.LEFT, 1, null);
       _painRight = createText(group, null, false, 1);
       createLabel(group, "Movement:", SWT.LEFT, 1, null);
       _painMove = createText(group, null, false, 1);
       createLabel(group, "Retreat:", SWT.LEFT, 1, null);
       _painRetreat = createText(group, null, false, 1);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
       // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   }


   @Override
   public void refreshDisplay(Character character) {
       // refreshDisplay is used to update fields that don't have ModifyListeners:
      if (_initiative == null) {
         return;
      }
       Condition cond = character.getCondition();
       _initiative.setText(String.valueOf(cond.getInitiative()));
       _moveAvail.setText(String.valueOf(cond.getMovementAvailableThisRound(false)));
       if (!cond.isAlive()) {
           _position.setText("DEAD");
       }
       else if (!cond.isConscious()) {
           _position.setText("K.O.");
       }
       else if (cond.isCollapsed()) {
           _position.setText("Collapsed");
       }
       else {
           _position.setText(cond.getPositionName());
       }
       byte availActions = cond.getActionsAvailable(false/*usedForDefenseOnly*/);
       byte finalDefActions = cond.getActionsAvailable(true/*usedForDefenseOnly*/);
       if ((availActions == 0) && (finalDefActions > 0)) {
         _actionsAvail.setText(finalDefActions + " (def only)");
      }
      else {
         _actionsAvail.setText(String.valueOf(availActions));
      }

       _pain.setText(String.valueOf(cond.getPenaltyPain()));
       _wounds.setText(String.valueOf(cond.getWounds()));
       _bleedRate.setText(String.valueOf(cond.getBleedRate()));
       Limb leftHand = character.getLimb(LimbType.HAND_LEFT);
       Limb rightHand = character.getLimb(LimbType.HAND_RIGHT);
       _painLeft.setText(leftHand == null ? "" : String.valueOf(leftHand.getWoundPenalty()));
       _painRight.setText(rightHand == null ? "" : String.valueOf(rightHand.getWoundPenalty()));
       _painMove.setText(String.valueOf(cond.getPenaltyMove()));
       _painRetreat.setText(String.valueOf(cond.getPenaltyRetreat(false/*includePain*/)));
       byte weaponTime = rightHand == null ? 0 : rightHand.getActionsNeededToReady();
       if (weaponTime == 0) {
         _weaponReady.setText("ready");
      }
      else {
         _weaponReady.setText(String.valueOf(weaponTime));
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }
}
