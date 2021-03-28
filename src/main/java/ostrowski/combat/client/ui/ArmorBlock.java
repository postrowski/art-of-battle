/*
 * Created on May 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.things.Armor;
import ostrowski.ui.Helper;

import java.util.HashMap;

public class ArmorBlock extends Helper implements IUIBlock, ModifyListener
{
   final         CharacterWidget           display;
   private       Combo                     armorName;
   private       Text                      armorCost;
   private       Text                      armorLbs;
   private       Text                      armorPD;
   private       Text                      buildBase;
   private final HashMap<DamageType, Text> build = new HashMap<>();
   public ArmorBlock(CharacterWidget display)
   {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      buildBlockWide(parent);
   }
   public void buildBlockTall(Composite parent)
   {
      Group armorGroup = createGroup(parent, "Armor", 3/*columns*/, false, 3/*hSpacing*/, 3/*vSpacing*/);

      createLabel(armorGroup, "Armor:", SWT.LEFT, 1, null);
      armorName = createCombo(armorGroup, SWT.READ_ONLY, 2, Armor.getArmorNames());
      armorCost = createText(armorGroup, null, false, 1);
      armorLbs = createText(armorGroup, null, false, 1);
      armorPD = createText(armorGroup, null, false, 1);

      createLabel(armorGroup, "Build Base:", SWT.LEFT, 1, null);
      buildBase = createText(armorGroup, null, false, 1);
      createLabel(armorGroup, null, SWT.LEFT, 1, null);

      for (DamageType damType : DamageType.values()) {
         if ((damType != DamageType.NONE) && (damType != DamageType.GENERAL)) {
            createLabel(armorGroup, "Build-"+damType.shortname.toLowerCase()+":", SWT.LEFT, 1, null);
            build.put(damType, createText(armorGroup, null, false, 1));
            createLabel(armorGroup, null, SWT.LEFT, 1, null);
         }
      }
      // add a ModifyListener to each of the important edit boxes.
      armorName.addModifyListener(this);

      armorGroup.setTabList(new Control[] {armorName});
   }
   public void buildBlockWide(Composite parent)
   {
      int columns = 6;
      Group armorGroup = createGroup(parent, "Armor", columns, false, 3/*hSpacing*/, 3/*vSpacing*/);

      createLabel(armorGroup, "Armor:", SWT.LEFT, 1, null);
      armorName = createCombo(armorGroup, SWT.READ_ONLY, (columns - 1), Armor.getArmorNames());

      createLabel(armorGroup, "cost:", SWT.RIGHT, 1, null);
      armorCost = createText(armorGroup, null, false, 2);
      armorCost.setText("$25000");
      //createLabel(armorGroup, "lbs.:", SWT.RIGHT, 1, null);
      armorLbs = createText(armorGroup, null, false, 1);
      createLabel(armorGroup, "PD:", SWT.RIGHT, 1, null);
      armorPD = createText(armorGroup, null, false, 1);

      createLabel(armorGroup, "Build", SWT.CENTER, 1, new FontData("Arial", 8/*height*/, SWT.BOLD));
      createLabel(armorGroup, "Build vs.:", SWT.CENTER, columns-1, null);

      createLabel(armorGroup, "Base", SWT.CENTER, 1, new FontData("Arial", 8/*height*/, SWT.BOLD));
      for (DamageType damType : DamageType.values()) {
         if ((damType != DamageType.NONE) && (damType != DamageType.GENERAL)) {
            createLabel(armorGroup, damType.shortname.toLowerCase(), SWT.CENTER, 1, null);
         }
      }

      buildBase = createText(armorGroup, null, false, 1, new FontData("Arial", 8/*height*/, SWT.BOLD));
      for (DamageType damType : DamageType.values()) {
         if ((damType != DamageType.NONE) && (damType != DamageType.GENERAL)) {
            build.put(damType, createText(armorGroup, null, false, 1));
         }
      }
      // add a ModifyListener to each of the important edit boxes.
      armorName.addModifyListener(this);

      armorGroup.setTabList(new Control[] {armorName});
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      armorName.setText(character.getArmor().getName());
   }

   @Override
   public void refreshDisplay(Character character) {
      armorName.setEnabled(character != null);

      if (character != null) {
         Armor naturalArmor = character.getRace().getNaturalArmor();
         armorName.remove(0);
         if (naturalArmor != null) {
            armorName.add(naturalArmor.getName(), 0);
         }
         else {
            armorName.add(Armor.NAME_NoArmor, 0);
         }
         armorName.setText(character.getArmor().getName());
         armorCost.setText("$" + character.getArmor().getCost());
         double lbs = character.getArmor().getAdjustedWeight();
         String lbsStr = String.valueOf(lbs);
         if (lbsStr.endsWith(".0") && (lbs > 100)) {
            lbsStr = lbsStr.substring(0, lbsStr.length() - 2);
         }
         armorLbs.setText(lbsStr + " Lbs.");
         armorPD.setText(String.valueOf(character.getArmor().getPassiveDefense()));
         // refreshDisplay is used to update fields that don't have ModifyListeners:
         buildBase.setText(String.valueOf(character.getBuildBase()));
         build.get(DamageType.BLUNT).setText(String.valueOf(character.getBuild(DamageType.BLUNT)));
         build.get(DamageType.CUT).setText(String.valueOf(character.getBuild(DamageType.CUT)));
         build.get(DamageType.IMP).setText(String.valueOf(character.getBuild(DamageType.IMP)));
         build.get(DamageType.FIRE).setText(String.valueOf(character.getBuild(DamageType.FIRE)));
         build.get(DamageType.ELECTRIC).setText(String.valueOf(character.getBuild(DamageType.ELECTRIC)));
      }
      else {
         armorCost.setText("");
         armorLbs.setText("");
         armorPD.setText("");
         buildBase.setText("");
         build.get(DamageType.BLUNT).setText("");
         build.get(DamageType.CUT).setText("");
         build.get(DamageType.IMP).setText("");
         build.get(DamageType.FIRE).setText("");
         build.get(DamageType.ELECTRIC).setText("");
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      character.setArmor(armorName.getText());
   }

   @Override
   public void modifyText(ModifyEvent e)
   {
      display.modifyText(e, this);
   }
}
