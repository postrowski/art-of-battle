package ostrowski.combat.client.ui;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.ui.Helper;

public class AttributesBlock extends Helper implements ModifyListener, IUIBlock, FocusListener {
   private final HashMap<Attribute, Text> _attEdit        = new HashMap<>();
   private final HashMap<Attribute, Text> _attCost        = new HashMap<>();
   private Text                           _adjStr         = null;
   private Text                           _build          = null;
   private Text                           _racialBuildAdj = null;
   private final CharacterWidget          _display;

   public AttributesBlock(CharacterWidget display) {
      _display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "Attributes", 4/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "Racial size adj.:", SWT.TRAIL, 2, null);
      _racialBuildAdj = createText(group, null, false, 1);
      createLabel(group, "", SWT.TRAIL, 1, null);
      for (Attribute att : Attribute.values()) {
         createLabel(group, att.shortName +":", SWT.TRAIL, 1, null);
         if (att == Attribute.Strength) {
            _attEdit.put(att, createText(group, null, true, 1));
            _adjStr = createText(group, null, false, 1);
         }
         else if (att == Attribute.Health) {
            _attEdit.put(att, createText(group, null, true, 1));
            _build = createText(group, null, false, 1);
         }
         else {
            _attEdit.put(att, createText(group, null, true, 2));
         }
         _attCost.put(att, createText(group, null, false, 1));
         _attEdit.get(att).setText("0");
         _attCost.get(att).setText("(0)");
      }

      for (Attribute att : Attribute.values()) {
         _attEdit.get(att).addModifyListener(this);
         _attEdit.get(att).addFocusListener(this);
      }
      Control[] tabList = new Control[Attribute.COUNT];
      for (Attribute att : Attribute.values()) {
         tabList[att.value] = _attEdit.get(att);
      }
      group.setTabList(tabList);
   }
   @Override
   public void modifyText(ModifyEvent e) {
      _display.modifyText(e, this);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      for (Attribute att : Attribute.values()) {
         _attEdit.get(att).setText(String.valueOf(character.getAttributeLevel(att)));
      }
   }
   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that dont have ModifyListeners:
      for (Attribute att : Attribute.values()) {
         _attEdit.get(att).setEnabled(character != null);
         int cost = (character == null) ? 0 : character.getAttCostAtCurLevel(att);
         _attCost.get(att).setText("(" + cost + ")");
      }
      if (character == null) {
          _adjStr.setText("");
          _build.setText("");
          _racialBuildAdj.setText("");
      }
      else {
         _adjStr.setText("["+character.getAdjustedStrength()+"]");
         _build.setText("["+character.getBuildBase()+"]");
         _racialBuildAdj.setText(String.valueOf(character.getRace().getBuildModifier()));
      }
   }
   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character != null) {
         for (Attribute att : Attribute.values()) {
            character.setAttribute(att, getIntValue(_attEdit.get(att)), true/*containInLimits*/);
         }
      }
   }

   @Override
   public void focusGained(FocusEvent e)
   {
      Text editControl = (Text) e.widget;
      editControl.selectAll();
   }

   @Override
   public void focusLost(FocusEvent e)
   {
   }
}
