/*
 * Created on May 31, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.ui.Helper;

public class AttackBlock extends Helper implements IUIBlock, SelectionListener
{
   final   Button[] button        = new Button[4];
   private byte     selectedStyle = -1;
   private Text     weaponName;

   public AttackBlock(CharacterDisplay display) {
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "Attack Style", 1 /*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      weaponName = createText(group, "really long weapon name:", false, 1);
//    Composite styles = new Composite(group, SWT.NONE);
//    styles.setLayout(new GridLayout(1, false));
      for (int i = 0; i < button.length ; i++) {
         button[i] = createRadioButton(group, "really long style name " + i, null, this);
         button[i].setVisible(false);
         GridData data = new GridData();
         data.minimumWidth = 200;
         data.grabExcessHorizontalSpace = true;
         button[i].setLayoutData(data);
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      if (weaponName == null) {
         return;
      }

      Hand rightHand = (Hand) character.getLimb(LimbType.HAND_RIGHT);
      Hand leftHand = (Hand) character.getLimb(LimbType.HAND_LEFT);
      boolean leftHandEmpty = (leftHand == null) || leftHand.isEmpty();
      Weapon myWeapon = (rightHand == null) ? null : rightHand.getWeapon(character);
      weaponName.setText(myWeapon == null ? "" : myWeapon.getName() + ":");
      WeaponStyleAttack[] styles = null;
      if (myWeapon != null) {
         styles = myWeapon.attackStyles;
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < button.length ; i++) {
         sb.setLength(0);
         if ((styles != null ) && (i<styles.length)) {
            sb.append(styles[i].getName());
            sb.append(" (").append(styles[i].getDamageString(character.getPhysicalDamageBase())).append(")");
            button[i].setEnabled(!styles[i].isTwoHanded() || !leftHandEmpty);
            button[i].setVisible(true);
         }
         else {
            button[i].setVisible(false);
         }
         button[i].setText(sb.toString());
         button[i].setSelection(false);
      }
      selectedStyle = (rightHand == null) ? -1 : rightHand.getAttackStyle();
      if ((selectedStyle == -1) || ((styles != null) && (selectedStyle >= styles.length))) {
         selectedStyle = 0;
         if (rightHand != null) {
            rightHand.setAttackStyle(selectedStyle);
         }
      }
      button[selectedStyle].setSelection(true);
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      character.getLimb(LimbType.HAND_RIGHT).setAttackStyle(selectedStyle);
   }

   @Override
   public void widgetSelected(SelectionEvent e)    {
      for (byte i = 0; i < button.length ; i++) {
         if (e.widget == button[i]) {
            selectedStyle = i;
            break;
         }
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e)    {
   }

   public byte getAttackStyle(RequestAttackStyle styleRequest) {
      //if (weaponName.equals(styleRequest.get))
      return selectedStyle;
   }
   public void setAttackStyle(byte newStyle) {
      button[selectedStyle].setSelection(false);
      selectedStyle = newStyle;
      button[selectedStyle].setSelection(true);
   }
}
