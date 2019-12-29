/*
 * Created on Jun 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.ui.Helper;

public class EncumbranceBlock extends Helper implements IUIBlock
{
   private Text _totalLbs;
   private Text[] _strLbs;
   private Text[] _adjLbs;
   private Text _encLevel;
   private Text _actionsPerTurn;
   private Text _move;

   @SuppressWarnings("unused")
   public EncumbranceBlock(CharacterWidget display)
   {
   }

   @Override
   public void buildBlock(Composite parent) {
      buildBlock(parent, false);
   }
   public void buildBlock(Composite parent, boolean isTall) {
      if (isTall) {
         buildBlockTall(parent);
      }
      else {
         buildBlockWide(parent);
      }
   }
   public void buildBlockTall(Composite parent) {
      Group encGroup = createGroup(parent, "Encumbrance", 6/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      encGroup.setTabList(new Control[] {});

      _strLbs = new Text[6];
      _adjLbs = new Text[_strLbs.length];
      createLabel(encGroup, "Enc-level", SWT.CENTER, 2/*hSpan*/, null);
      createLabel(encGroup, "Max lbs.", SWT.CENTER, 2/*hSpan*/, null);
      createLabel(encGroup, "Nim Adj.", SWT.CENTER, 2/*hSpan*/, null);

      for (int enc=0 ; enc < _strLbs.length ; enc++) {
         createLabel(encGroup, String.valueOf(enc), SWT.CENTER, 2/*hSpan*/, null);
         _strLbs[enc] = createText(encGroup, "0", false, 2/*hSpan*/);
         _adjLbs[enc] = createText(encGroup, "0", false, 2/*hSpan*/);
      }

      createLabel(encGroup, "", SWT.RIGHT, 6/*hSpan*/, null);

      createLabel(encGroup, "Total weight carried:", SWT.RIGHT, 3/*hSpan*/, null);
      _totalLbs = createText(encGroup, "0", false, 3/*hSpan*/);

      createLabel(encGroup, "Actions per turn:", SWT.RIGHT, 3/*hSpan*/, null);
      _actionsPerTurn = createText(encGroup, "0", false, 3/*hSpan*/);

      createLabel(encGroup, "Current Enc:", SWT.RIGHT, 3/*hSpan*/, null);
      _encLevel = createText(encGroup, "0", false, 3/*hSpan*/);

      createLabel(encGroup, "Move per action:", SWT.RIGHT, 3/*hSpan*/, null);
      _move = createText(encGroup, "0", false, 3/*hSpan*/);
   }
   public void buildBlockWide(Composite parent) {
      Group encGroup = createGroup(parent, "Encumbrance", 7/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);
      encGroup.setTabList(new Control[] {});

      createLabel(encGroup, "Weight carried:", SWT.RIGHT, 2/*hSpan*/, null);
      _totalLbs = createText(encGroup, "0", false, 1/*hSpan*/);
      createLabel(encGroup, "", SWT.CENTER, 1/*hSpan*/, null);

      createLabel(encGroup, "Actions per turn:", SWT.RIGHT, 2/*hSpan*/, null);
      _actionsPerTurn = createText(encGroup, "0", false, 1/*hSpan*/);

      createLabel(encGroup, "Current Enc:", SWT.RIGHT, 2/*hSpan*/, null);
      _encLevel = createText(encGroup, "0", false, 1/*hSpan*/);
      createLabel(encGroup, "", SWT.CENTER, 1/*hSpan*/, null);

      createLabel(encGroup, "Move per action:", SWT.RIGHT, 2/*hSpan*/, null);
      _move = createText(encGroup, "0", false, 1/*hSpan*/);
      createLabel(encGroup, "", SWT.CENTER, 7/*hSpan*/, null);

      _strLbs = new Text[6];
      _adjLbs = new Text[_strLbs.length];
      createLabel(encGroup, "Enc-level:", SWT.RIGHT, 1/*hSpan*/, null);
      for (int enc=0 ; enc < _strLbs.length ; enc++) {
         createLabel(encGroup, String.valueOf(enc), SWT.CENTER, 1/*hSpan*/, null);
      }
      createLabel(encGroup, "Max lbs.:", SWT.RIGHT, 1/*hSpan*/, null);
      for (int enc=0 ; enc < _strLbs.length ; enc++) {
         _strLbs[enc] = createText(encGroup, "0", false, 1/*hSpan*/);
      }
      createLabel(encGroup, "Nim Adj.:", SWT.RIGHT, 1/*hSpan*/, null);
      for (int enc=0 ; enc < _strLbs.length ; enc++) {
         _adjLbs[enc] = createText(encGroup, "0", false, 1/*hSpan*/);
      }
   }

   // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   @Override
   public void updateDisplayFromCharacter(Character character)
   {
   }

   // refreshDisplay is used to update fields that dont have ModifyListeners:
   @Override
   public void refreshDisplay(Character character)
   {
      String lbsStr = "";
      if (character != null) {
         double lbs = character.getWeightCarried();
         lbsStr = String.valueOf(lbs);
      }
      _totalLbs.setText(lbsStr);

      byte strength = (character==null) ? 0 : character.getAdjustedStrength();
      byte nimbleness = (character==null) ? 0 : character.getAttributeLevel(Attribute.Nimbleness);
      for (byte enc=0 ; enc < _strLbs.length ; enc++) {
          String strLbs = String.valueOf(Rules.getMaxWeightForEncLevel(strength, (byte)0/*nimbleness*/, enc));
          String nimLbs = String.valueOf(Rules.getMaxWeightForEncLevel(strength, nimbleness, enc));
          if (strLbs.endsWith(".0")) {
            strLbs = strLbs.substring(0, strLbs.length() - 2);
         }
          if (nimLbs.endsWith(".0")) {
            nimLbs = nimLbs.substring(0, nimLbs.length() - 2);
         }
         _strLbs[enc].setText(strLbs);
         _adjLbs[enc].setText(nimLbs);
      }
      if (character==null) {
          _encLevel.setText("");
          _actionsPerTurn.setText("");
          _move.setText("");
      }
      else {
         byte enc = Rules.getEncumbranceLevel(character);
         _encLevel.setText(String.valueOf(enc));
         _actionsPerTurn.setText(String.valueOf(Rules.getStartingActions(character)));
         _move.setText(String.valueOf(character.getRace().getMovementRate(enc)));
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character)
   {
   }

}
