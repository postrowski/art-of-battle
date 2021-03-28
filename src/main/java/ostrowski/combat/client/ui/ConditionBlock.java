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
   private Text initiative;
   private Text moveAvail;
   private Text position;
   private Text actionsAvail;
   private Text pain;
   private Text wounds;
   private Text bleedRate;
   private Text painLeft;
   private Text painRight;
   private Text painMove;
   private Text painRetreat;
   private Text weaponReady;

   public ConditionBlock(CharacterDisplay display) {
   }

   @Override
   public void buildBlock(Composite parent)
   {
       Group group = createGroup(parent, "Condition", 2/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
       group.setTabList(new Control[] {});
       createLabel(group, "Initiative:", SWT.LEFT, 1, null);
       initiative = createText(group, null, false, 1);

       createLabel(group, "Weapon:", SWT.LEFT, 1, null);
       weaponReady = createText(group, null, false, 1);
       createLabel(group, "Movement:", SWT.LEFT, 1, null);
       moveAvail = createText(group, null, false, 1);

       createLabel(group, "Position:", SWT.LEFT, 1, null);
       position = createText(group, null, false, 1);
       createLabel(group, "Actions avail.:", SWT.LEFT, 1, null);
       actionsAvail = createText(group, null, false, 1);
       createLabel(group, "Pain:", SWT.LEFT, 1, null);
       pain = createText(group, null, false, 1);
       createLabel(group, "Wounds:", SWT.LEFT, 1, null);
       wounds = createText(group, null, false, 1);
       createLabel(group, "Bleed rate:", SWT.LEFT, 1, null);
       bleedRate = createText(group, null, false, 1);

       createLabel(group, "Penalties:", SWT.CENTER, 2, new FontData("Arial", 10, SWT.BOLD));
       createLabel(group, "Left arm:", SWT.LEFT, 1, null);
       painLeft = createText(group, null, false, 1);
       createLabel(group, "Right arm:", SWT.LEFT, 1, null);
       painRight = createText(group, null, false, 1);
       createLabel(group, "Movement:", SWT.LEFT, 1, null);
       painMove = createText(group, null, false, 1);
       createLabel(group, "Retreat:", SWT.LEFT, 1, null);
       painRetreat = createText(group, null, false, 1);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
       // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   }


   @Override
   public void refreshDisplay(Character character) {
       // refreshDisplay is used to update fields that don't have ModifyListeners:
      if (initiative == null) {
         return;
      }
       Condition cond = character.getCondition();
       initiative.setText(String.valueOf(cond.getInitiative()));
       moveAvail.setText(String.valueOf(cond.getMovementAvailableThisRound(false)));
       if (!cond.isAlive()) {
           position.setText("DEAD");
       }
       else if (!cond.isConscious()) {
           position.setText("K.O.");
       }
       else if (cond.isCollapsed()) {
           position.setText("Collapsed");
       }
       else {
           position.setText(cond.getPositionName());
       }
       byte availActions = cond.getActionsAvailable(false/*usedForDefenseOnly*/);
       byte finalDefActions = cond.getActionsAvailable(true/*usedForDefenseOnly*/);
       if ((availActions == 0) && (finalDefActions > 0)) {
         actionsAvail.setText(finalDefActions + " (def only)");
      }
      else {
         actionsAvail.setText(String.valueOf(availActions));
      }

       pain.setText(String.valueOf(cond.getPenaltyPain()));
       wounds.setText(String.valueOf(cond.getWounds()));
       bleedRate.setText(String.valueOf(cond.getBleedRate()));
       Limb leftHand = character.getLimb(LimbType.HAND_LEFT);
       Limb rightHand = character.getLimb(LimbType.HAND_RIGHT);
       painLeft.setText(leftHand == null ? "" : String.valueOf(leftHand.getWoundPenalty()));
       painRight.setText(rightHand == null ? "" : String.valueOf(rightHand.getWoundPenalty()));
       painMove.setText(String.valueOf(cond.getPenaltyMove()));
       painRetreat.setText(String.valueOf(cond.getPenaltyRetreat(false/*includePain*/)));
       byte weaponTime = rightHand == null ? 0 : rightHand.getActionsNeededToReady();
       if (weaponTime == 0) {
         weaponReady.setText("ready");
      }
      else {
         weaponReady.setText(String.valueOf(weaponTime));
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }
}
