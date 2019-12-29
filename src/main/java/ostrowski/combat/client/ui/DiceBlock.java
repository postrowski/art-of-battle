/*
 * Created on May 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.server.Configuration;
import ostrowski.ui.Helper;

import java.util.HashMap;

public class DiceBlock extends Helper implements IUIBlock
{
   final         CharacterWidget            _display;
   private final HashMap<Attribute, Text[]> _dice             = new HashMap<>();
   private       Text                       _painReductioDice = null;
   private       Text                       _maxPainWounds    = null;

   public DiceBlock(CharacterWidget display)
   {
      _display = display;
      for (Attribute att : Attribute.values()) {
         _dice.put(att, new Text[4]);
      }
   }

   @Override
   public void buildBlock(Composite parent) {
      if (Configuration.useExtendedDice()){
         buildBlockComplexDice(parent);
      }
      else {
         Group group = createGroup(parent, "Pain recovery", 2/*columns*/, false/*sameSize*/, 2/*hSpacing*/, 2/*vSpacing*/);
         group.setTabList(new Control[] {});
         createLabel(group, "Pain reduction dice:", SWT.CENTER, 1, new FontData("Arial", 7, SWT.BOLD));
         _painReductioDice = createText(group, " ", false, 1);
         createLabel(group, "Max Pain / Wounds:", SWT.CENTER, 1, new FontData("Arial", 7, SWT.BOLD));
         _maxPainWounds = createText(group, " ", false, 1);
      }
   }

   public void buildBlockComplexDice(Composite parent)
   {
      Group group = createGroup(parent, "Dice", 9/*columns*/, false/*sameSize*/, 2/*hSpacing*/, 2/*vSpacing*/);
      group.setTabList(new Control[] {});
      createLabel(group, "actions", SWT.CENTER, 1, null);
      createLabel(group, "DEX", SWT.CENTER, 2, new FontData("Arial", 7, SWT.BOLD));
      createLabel(group, "IQ",  SWT.CENTER, 2, new FontData("Arial", 7, SWT.BOLD));
      createLabel(group, "SOC", SWT.CENTER, 2, new FontData("Arial", 7, SWT.BOLD));
      createLabel(group, "TOU", SWT.CENTER, 2, new FontData("Arial", 7, SWT.BOLD));

      for (int actions=1 ; actions<=3 ; actions++) {
         createLabel(group, "" + actions, SWT.CENTER, 1, new FontData("Arial", 7, SWT.BOLD));
         _dice.get(Attribute.Dexterity)[actions] = createText(group, " ", false, 2);
         _dice.get(Attribute.Intelligence)[actions]  = createText(group, " ", false, 2);
         _dice.get(Attribute.Social)[actions] = createText(group, " ", false, 2);
         _dice.get(Attribute.Toughness)[actions] = createText(group, " ", false, 2);
      }

      createLabel(group, "Max Pain / Wounds:", SWT.CENTER, 5, new FontData("Arial", 7, SWT.BOLD));
      _maxPainWounds = createText(group, " ", false, 4);
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      for (Attribute att : Attribute.values()) {
         byte attributeLevel = character.getAttributeLevel(att);
         // set the value of all attributes
         for (byte actions=1; actions<=3; actions++) {
            // If we don't display the attribute, this array will be null, so do nothing.
            // Also, if the configuration was not set to complex dice when we started this application,
            // this array will also be empty.
            if (_dice.get(att)[actions] != null) {
               _dice.get(att)[actions].setText(Rules.getDice(attributeLevel, actions, att/*attribute*/).toString().replaceAll(" ", ""));
            }
         }
      }
      byte toughness = character.getAttributeLevel(Attribute.Toughness);
      if (_painReductioDice != null) {
         _painReductioDice.setText(Rules.getDice(toughness, (byte)1, Attribute.Toughness/*attribute*/).toString().replaceAll(" ", ""));
      }
      if (_maxPainWounds != null) {
         _maxPainWounds.setText("" + Rules.getUnconsciousWoundLevel(toughness));
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }
}
