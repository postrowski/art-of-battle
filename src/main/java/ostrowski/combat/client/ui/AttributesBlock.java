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
   private final HashMap<Attribute, Text> attEdit        = new HashMap<>();
   private final HashMap<Attribute, Text> attCost        = new HashMap<>();
   private       Text                     adjStr         = null;
   private       Text                     build          = null;
   private       Text                     racialBuildAdj = null;
   private final CharacterWidget          display;

   public AttributesBlock(CharacterWidget display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group group = createGroup(parent, "Attributes", 4/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      createLabel(group, "Racial size adj.:", SWT.TRAIL, 2, null);
      racialBuildAdj = createText(group, null, false, 1);
      createLabel(group, "", SWT.TRAIL, 1, null);
      for (Attribute att : Attribute.values()) {
         createLabel(group, att.shortName +":", SWT.TRAIL, 1, null);
         if (att == Attribute.Strength) {
            attEdit.put(att, createText(group, null, true, 1));
            adjStr = createText(group, null, false, 1);
         }
         else if (att == Attribute.Health) {
            attEdit.put(att, createText(group, null, true, 1));
            build = createText(group, null, false, 1);
         }
         else {
            attEdit.put(att, createText(group, null, true, 2));
         }
         attCost.put(att, createText(group, null, false, 1));
         attEdit.get(att).setText("0");
         attCost.get(att).setText("(0)");
      }

      for (Attribute att : Attribute.values()) {
         attEdit.get(att).addModifyListener(this);
         attEdit.get(att).addFocusListener(this);
      }
      Control[] tabList = new Control[Attribute.COUNT];
      for (Attribute att : Attribute.values()) {
         tabList[att.value] = attEdit.get(att);
      }
      group.setTabList(tabList);
   }
   @Override
   public void modifyText(ModifyEvent e) {
      display.modifyText(e, this);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      for (Attribute att : Attribute.values()) {
         attEdit.get(att).setText(String.valueOf(character.getAttributeLevel(att)));
      }
   }
   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that dont have ModifyListeners:
      for (Attribute att : Attribute.values()) {
         attEdit.get(att).setEnabled(character != null);
         int cost = (character == null) ? 0 : character.getAttCostAtCurLevel(att);
         attCost.get(att).setText("(" + cost + ")");
      }
      if (character == null) {
          adjStr.setText("");
          build.setText("");
          racialBuildAdj.setText("");
      }
      else {
         adjStr.setText("[" + character.getAdjustedStrength() + "]");
         build.setText("[" + character.getBuildBase() + "]");
         racialBuildAdj.setText(String.valueOf(character.getRace().getBuildModifier()));
      }
   }
   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character != null) {
         for (Attribute att : Attribute.values()) {
            character.setAttribute(att, getIntValue(attEdit.get(att)), true/*containInLimits*/);
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
