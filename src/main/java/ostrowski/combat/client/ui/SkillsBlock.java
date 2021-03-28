package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

public class SkillsBlock extends Helper implements Enums, ModifyListener, IUIBlock
{
   final         CharacterWidget display;
   private final Combo[]         skillType     = new Combo[6];
   private final Spinner[]       skillLevel    = new Spinner[skillType.length];
   private final Text[]          skillLevelAdj = new Text[skillType.length];
   private final Text[]          skillCost     = new Text[skillType.length];
   private       Text            racialSize    = null;

   public SkillsBlock(CharacterWidget display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group skillGroup = createGroup(parent, "Skills", 4/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);
      createLabel(skillGroup, "Racial adj. for size:", SWT.RIGHT, 2, null);
      racialSize = createText(skillGroup, "0", false, 1);
      createLabel(skillGroup, "",           SWT.CENTER, 1, null);
      createLabel(skillGroup, "Name",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Base Level", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Adj.",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Cost",       SWT.CENTER, 1, null);
      List<String> skillNames = new ArrayList<>();
      skillNames.add("---");
      Attribute priorAttribute = null;
      for (SkillType element : SkillType.values()) {
         if ((priorAttribute != element.getAttributeBase()) && (element.getAttributeBase() != Attribute.Dexterity)) {
            skillNames.add("---");
         }
         skillNames.add(element.getName());
         priorAttribute = element.getAttributeBase();
      }
      for (int i = 0; i < skillType.length; i++) {
         skillType[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, skillNames);
         skillType[i].select(0);
         skillLevel[i] = createSpinner(skillGroup, 0/*min*/, Rules.getMaxSkillLevel()/*max*/, 0/*value*/, 1);
         skillLevelAdj[i] = createText(skillGroup, "[0]", false/*editable*/, 1);
         skillCost[i] = createText(skillGroup, "(0)", false/*editable*/, 1);
         skillType[i].addModifyListener(this);
         skillLevel[i].addModifyListener(this);
      }
      Control[] tabList = new Control[skillType.length * 2];
      int index = 0;
      for (int i = 0; i < skillType.length; i++) {
         tabList[index++] = skillType[i];
         tabList[index++] = skillLevel[i];
      }
      skillGroup.setTabList(tabList);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      display.modifyText(e, this);
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      List<Skill> newSkills = new ArrayList<>();
      for (int i = 0; i < skillType.length; i++) {
         String skillName = skillType[i].getText();
         if (!skillName.equals("---")) {
            SkillType skillType = SkillType.getSkillTypeByName(skillName);
            if (skillType != null) {
               boolean dupFound = false;
               for (Skill dupSkill : newSkills) {
                  if (dupSkill.getType() == skillType) {
                     dupSkill.setLevel((byte)Math.max(dupSkill.getLevel(), skillLevel[i].getSelection()));
                     dupFound = true;
                  }
               }
               if (!dupFound) {
                  newSkills.add(new Skill(skillType, getValidSkillRange((byte) skillLevel[i].getSelection())));
               }
            }
         }
      }
      if (character != null) {
         character.setSkillsList(newSkills);
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      refreshDisplay(character);
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      byte bonusToHit = (character == null) ? 0 : character.getRace().getBonusToHit();
      List<Skill> skills = (character == null) ? new ArrayList<>() : character.getSkillsList();
      racialSize.setText((bonusToHit >= 0) ? ("+" + bonusToHit) : String.valueOf(bonusToHit));
      int nextInsertIndex = 0;
      for (int i = 0; i < skillType.length; i++) {
         if (skillType[i].getText().equals("---")) {
            continue;
         }
         Skill skillFound = null;
         for (Skill skill : skills) {
            if (skill.getName().equals(skillType[i].getText())) {
               skillLevel[i].setSelection(skill.getLevel());
               int adjustedLevel = Rules.getAdjustedSkillLevel(skill, character);

               skillLevelAdj[i].setText("[" + adjustedLevel + "]");
               skillCost[i].setText("(" + Rules.getSkillCost(skill.getLevel()) + ")");
               skillType[i].setEnabled(true);
               skillLevel[i].setEnabled(true);
               skillFound = skill;
               nextInsertIndex++;
               break;
            }
         }
         if (skillFound != null) {
            skills.remove(skillFound);
         }
         else {
            removeSkillRow(i);
            //i--;
         }
      }
      for (Skill skill : skills) {
         skillType[nextInsertIndex].setText(skill.getName());
         skillLevel[nextInsertIndex].setSelection(skill.getLevel());
         int adjustedLevel = Rules.getAdjustedSkillLevel(skill, character);

         skillLevelAdj[nextInsertIndex].setText("[" + adjustedLevel + "]");
         skillCost[nextInsertIndex].setText("(" + Rules.getSkillCost(skill.getLevel()) + ")");
         skillType[nextInsertIndex].setEnabled(true);
         skillLevel[nextInsertIndex].setEnabled(true);
         nextInsertIndex++;
      }
      for (int i = 0; i < (skillType.length - 1); i++) {
         if ((skillType[i].getText().equals("---")) && (!skillType[i + 1].getText().equals("---")))
         {
            if (removeSkillRow(i)) {
               --i;
            }
         }
      }
      for (int i = 0; i < skillType.length; i++) {
         skillLevel[i].setEnabled(!skillType[i].getText().equals("---"));
         skillType[i].setEnabled((i == 0) || (!skillType[i - 1].getText().equals("---")));
      }
   }

   private boolean removeSkillRow(int rowIndex) {
      boolean nextItemAvailable = (rowIndex+1) < skillType.length;
      skillLevel[rowIndex].setSelection(nextItemAvailable ? skillLevel[rowIndex + 1].getSelection() : 0);
      skillLevelAdj[rowIndex].setText(nextItemAvailable ? skillLevelAdj[rowIndex + 1].getText() : "[0]");
      skillCost[rowIndex].setText(nextItemAvailable ? skillCost[rowIndex + 1].getText() : "(0)");
      skillType[rowIndex].select(nextItemAvailable ? skillType[rowIndex + 1].getSelectionIndex() : 0);
      skillType[rowIndex].setEnabled(!nextItemAvailable || skillType[rowIndex + 1].getEnabled());
      skillLevel[rowIndex].setEnabled(!nextItemAvailable || skillLevel[rowIndex + 1].getEnabled());
      if (nextItemAvailable) {
         removeSkillRow(rowIndex+1);
      }
      return nextItemAvailable;
   }

   private static byte getValidSkillRange(byte skillVal) {
      if ((skillVal >= 0) && (skillVal <= Rules.getMaxSkillLevel())) {
         return skillVal;
      }
      return 0;
   }
}
