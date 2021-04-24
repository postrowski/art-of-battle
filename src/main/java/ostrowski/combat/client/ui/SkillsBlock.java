package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.ui.Helper;

import java.util.*;
import java.util.List;

public class SkillsBlock extends Helper implements Enums, ModifyListener, IUIBlock, SelectionListener {
   final         CharacterWidget display;
   static final  int             SKILL_COUNT       = 12;
   private final Combo[]         professionType    = new Combo[SKILL_COUNT];
   private final Combo[]         professionLevel   = new Combo[SKILL_COUNT];
   private final Combo[]         skillType         = new Combo[SKILL_COUNT];
   private final Combo[]         skillRank         = new Combo[SKILL_COUNT];
   private final Text[]          skillLevelAdj     = new Text[SKILL_COUNT];
   private final Text[]          skillCost         = new Text[SKILL_COUNT];
   private       Text            racialSize        = null;
   private static final String   NO_PROF_SELECTED  = "--";
   private static final String   NO_SKILL_SELECTED = "---";
   private static final String   NO_RANK_SELECTED  = "----";

   public SkillsBlock(CharacterWidget display) {
      this.display = display;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group skillGroup = createGroup(parent, "Professions / Skills", 6/*columns*/, false, 4/*hSpacing*/, 1/*vSpacing*/);
      createLabel(skillGroup, "Racial adj. for size:", SWT.RIGHT, 4, null);
      racialSize = createText(skillGroup, "0", false, 1);
      createLabel(skillGroup, "", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Profession", SWT.CENTER, 1, null);
      createLabel(skillGroup, "Level",      SWT.CENTER, 1, null);
      createLabel(skillGroup, "Skill",      SWT.CENTER, 1, null);
      createLabel(skillGroup, "Rank",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Adj.",       SWT.CENTER, 1, null);
      createLabel(skillGroup, "Cost",       SWT.CENTER, 1, null);

      List<String> professionNames = new ArrayList<>();
      professionNames.add(NO_PROF_SELECTED);
      for (ProfessionType element : ProfessionType.values()) {
         professionNames.add(element.getName());
      }

      List<String> profLevels = new ArrayList<>();
      for (int i=0 ; i<=Rules.getMaxSkillLevel() ; i++) {
         profLevels.add(String.valueOf(i));
      }

      List<String> skillNames = new ArrayList<>();
      skillNames.add(NO_SKILL_SELECTED);

      List<String> ranks = new ArrayList<>();
      ranks.add(NO_RANK_SELECTED);
      ranks.add(SkillRank.PROFICIENT.getName());
      ranks.add(SkillRank.FAMILIAR.getName());
      ranks.add(SkillRank.UNKNOWN.getName());

      for (int i = 0; i < SKILL_COUNT; i++) {
         professionType[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, professionNames);
         professionType[i].select(0);
         professionLevel[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, profLevels);
         skillType[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, skillNames);
         skillType[i].select(0);
         skillRank[i] = createCombo(skillGroup, SWT.READ_ONLY, 1, ranks);
         skillRank[i].select(0);
         skillLevelAdj[i] = createText(skillGroup, "[0]", false/*editable*/, 1);
         skillCost[i] = createText(skillGroup, "(0)", false/*editable*/, 1);
         // register watchers on all the editable elements we've created
         professionType[i].addSelectionListener(this);
         professionLevel[i].addSelectionListener(this);
         skillType[i].addSelectionListener(this);
         skillRank[i].addSelectionListener(this);
      }

      // setup a tab ordering on the editable controls, row by row:
      Control[] tabList = new Control[SKILL_COUNT * 4];
      int index = 0;
      for (int i = 0; i < SKILL_COUNT; i++) {
         tabList[index++] = professionType[i];
         tabList[index++] = professionLevel[i];
         tabList[index++] = skillType[i];
         tabList[index++] = skillRank[i];
      }
      skillGroup.setTabList(tabList);
   }
   public static boolean inModify = false;
   @Override
   public void modifyText(ModifyEvent e) {
      if (!inModify) {
         inModify = true;
         try {
            display.modifyText(e, this);
         }
         finally {
            inModify = false;
         }
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (!inModify) {
         inModify = true;
         Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == professionType[i]) {
               String oldSelectionText = skillType[i].getText();
               if (oldSelectionText.isEmpty()) {
                  professionLevel[i].select(0);
               }
               onSetProfession(existingProfessions, i, oldSelectionText);
            }
            existingProfessions.computeIfAbsent(professionType[i].getText(), o -> new ArrayList<>()).add(i);
         }
         // If they change a profession level, make sure all the levels of the same profession is set to match
         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == professionLevel[i]) {
               int newLevel = professionLevel[i].getSelectionIndex();
               ArrayList<Integer> existingProfs = existingProfessions.get(professionType[i].getText());
               for (Integer existingProf : existingProfs) {
                  if (i != existingProf) {
                     professionLevel[existingProf].select(newLevel);
                  }
               }
               break;
            }
         }
         for (int i = 0; i < SKILL_COUNT; i++) {
            if (e.widget == skillType[i]) {
               if (skillType[i].getText().equals(NO_SKILL_SELECTED)){
                  if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
                     skillRank[i].setText(NO_RANK_SELECTED);
                     skillRank[i].setEnabled(false);
                  }
               }
               else {
                  skillRank[i].setEnabled(true);
                  if (skillRank[i].getText().equals(NO_RANK_SELECTED)) {
                      skillRank[i].setText(SkillRank.FAMILIAR.getName());
                  }
               }
               break;
            }
         }
         inModify = false;
      }
      display.modifyText(e, this);
   }

   private void onSetProfession(Map<String, ArrayList<Integer>> existingProfessions, int rowIndex, String preferredSkillName) {
      if (professionType[rowIndex].getText().equals(NO_PROF_SELECTED)) {
         skillRank[rowIndex].setText(NO_RANK_SELECTED);
         skillRank[rowIndex].setEnabled(false);
         skillType[rowIndex].setText(NO_SKILL_SELECTED);
         skillType[rowIndex].setEnabled(false);
         professionLevel[rowIndex].select(0);
         professionLevel[rowIndex].setEnabled(false);
      }
      else {
         ArrayList<Integer> existingProfs = existingProfessions.get(professionType[rowIndex].getText());
         if (existingProfs != null && !existingProfs.isEmpty()) {
            professionLevel[rowIndex].setEnabled(false);
            professionLevel[rowIndex].select(professionLevel[existingProfs.get(0)].getSelectionIndex());
            skillRank[rowIndex].setEnabled(true);
         } else {
            professionLevel[rowIndex].setEnabled(true);
            skillRank[rowIndex].setEnabled(false);
            skillRank[rowIndex].setText(SkillRank.PROFICIENT.getName());
         }
         skillType[rowIndex].setEnabled(true);
         for (ProfessionType type : ProfessionType.values()) {
            if (professionType[rowIndex].getText().equals(type.getName())) {
               skillType[rowIndex].removeAll();
               skillType[rowIndex].add(NO_SKILL_SELECTED);
               boolean skillFound = false;
               for (SkillType skType : type.skillList) {
                  skillType[rowIndex].add(skType.getName());
                  if (skType.name().equals(preferredSkillName)) {
                     skillType[rowIndex].select(skillType[rowIndex].getItemCount() - 1);
                     // leave skillRank[i] alone
                     skillFound = true;
                  }
               }
               if (!skillFound) {
                  skillType[rowIndex].select(0);
               }
               break;
            }
         }
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
      widgetSelected(e);
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      Map<ProfessionType, Profession> charProfs = new HashMap<>();//character.getProfessionsList();
      for (int i = 0; i < SKILL_COUNT; i++) {
         String profName = professionType[i].getText();
         if (!profName.equals(NO_PROF_SELECTED)) {
            ProfessionType profType = ProfessionType.getByName(profName);
            String levelStr = professionLevel[i].getText();
            if (!levelStr.isEmpty()) {
               byte profLevel = Byte.parseByte(levelStr);
               Profession charProfession = charProfs.computeIfAbsent(profType, o -> new Profession(profType, profLevel));
               charProfession.setLevel(profLevel);
               String skillName = skillType[i].getText();
               String skillRnk = skillRank[i].getText();
               if (!skillName.equals(NO_SKILL_SELECTED) && !skillRnk.equals(NO_RANK_SELECTED)) {
                  SkillType skillType = SkillType.getSkillTypeByName(skillName);
                  SkillRank skillRank = SkillRank.getRankByName(skillRnk);
                  if (skillType != null && skillRank != null && skillRank != SkillRank.UNKNOWN) {
                     charProfession.setRank(skillType, skillRank);
                  }
               }
            }
         }
      }
      if (character != null) {
         character.setProfessionsList(new ArrayList<>(charProfs.values()));
      }
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
      // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      refreshDisplay(character);
   }

   private static class ProfessionDisplay {
      public ProfessionType profType;
      public SkillType skillType;
      public SkillRank rank;
      public byte      level;
   }

   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      byte bonusToHit = (character == null) ? 0 : character.getRace().getBonusToHit();
      List<Profession> professions = (character == null) ? new ArrayList<>() : character.getProfessionsList();
      List<ProfessionDisplay> charProfs = new ArrayList<>();
      for (Profession profession : professions) {
         for (SkillType skill : profession.getProficientSkills()) {
            ProfessionDisplay charProf = new ProfessionDisplay();
            charProf.profType = profession.getType();
            charProf.skillType = SkillType.getSkillTypeByName(skill.name);
            charProf.rank = SkillRank.PROFICIENT;
            charProf.level = profession.getLevel();
            charProfs.add(charProf);
         }
         for (SkillType skill : profession.getFamiliarSkills()) {
            ProfessionDisplay charProf = new ProfessionDisplay();
            charProf.profType = profession.getType();
            charProf.skillType = SkillType.getSkillTypeByName(skill.name);
            charProf.rank = SkillRank.FAMILIAR;
            charProf.level = profession.getLevel();
            charProfs.add(charProf);
         }
      }
      charProfs.sort((o1, o2) -> {
         if (o1.profType != o2.profType) {
            int compare = Byte.compare(o1.level, o2.level);
            if (compare != 0) {
               // different professions, of different levels
               return compare * -1; // descending
            }
            // different professions, same level, order by profession name
            return o1.profType.getName().compareTo(o2.profType.getName());
         }
         // same profession, order by rank first
         int compare = Byte.compare(o1.rank.getCost(), o2.rank.getCost());
         if (compare != 0) {
            // same profession, different ranks
            return compare * -1; // descending
         }
         if (o1.skillType == o2.skillType) {
            return 0;
         }
         // same profession, same rank, order by skill name - but always put shield below other fighting skills
         if (o1.skillType == SkillType.Shield) {
            return 1;
         }
         if (o2.skillType == SkillType.Shield) {
            return -1;
         }
         return o1.skillType.getName().compareTo(o2.skillType.getName());
      });

      racialSize.setText((bonusToHit >= 0) ? ("+" + bonusToHit) : String.valueOf(bonusToHit));
      int nextInsertIndex = 0;
      Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
      for (int i = 0; i < SKILL_COUNT; i++) {
         if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
            continue;
         }
         ArrayList<Integer> existingProfRows = existingProfessions.computeIfAbsent(professionType[i].getText(), o -> new ArrayList<>());
         existingProfRows.add(i);
         boolean isFirstProf = existingProfRows.size() == 1;
         ProfessionDisplay professionFound = null;
         ProfessionType profType = ProfessionType.getByName(professionType[i].getText());
         SkillType skillType = SkillType.getSkillTypeByName(this.skillType[i].getText());
         for (ProfessionDisplay charProf : charProfs) {
            if (charProf.profType == profType &&
                ((charProf.skillType == skillType) || this.skillType[i].getText().equals(NO_SKILL_SELECTED))) {
               //professionType[i] already matches
               professionLevel[i].select(charProf.level);
               onSetProfession(existingProfessions, i, charProf.skillType.getName());
               this.skillType[i].setText(charProf.skillType.getName());
               if (isFirstProf) {
                  charProf.rank = SkillRank.PROFICIENT;
               }
               skillRank[i].setText(charProf.rank.getName());
               int adjustedLevel = Rules.getAdjustedSkillLevel(profType, charProf.skillType, character);
               skillLevelAdj[i].setText("[" + adjustedLevel + "]");
               if (isFirstProf) {
                  skillCost[i].setText("(" + Rules.getProfessionCost(charProf.level) + ")");
               }
               else {
                  skillCost[i].setText("(" + charProf.rank.getCost() + ")");
               }
               this.skillType[i].setEnabled(true);
               professionLevel[i].setEnabled(isFirstProf);
               skillRank[i].setEnabled(!isFirstProf);
               professionFound = charProf;
               nextInsertIndex++;
               break;
            }
         }
         if (professionFound != null) {
            charProfs.remove(professionFound);
         }
         else {
            if (this.skillType[i].getText().equals(NO_SKILL_SELECTED) && isFirstProf) {
               skillLevelAdj[i].setText("[0]");
               skillCost[i].setText("(" + Rules.getProfessionCost((byte) professionLevel[i].getSelectionIndex()) + ")");
            }
            else if (this.skillType[i].getText().equals(NO_SKILL_SELECTED) ||
                     this.skillRank[i].getText().equals(NO_RANK_SELECTED)) {
               skillLevelAdj[i].setText("[0]");
               skillCost[i].setText("(0)");
            }
            else {
               removeSkillRow(i);
            }
            //i--;
         }
      }
      // Add any profession from the character that we didn't already find.
      for (ProfessionDisplay charProf : charProfs) {
         ArrayList<Integer> existingProfRows = existingProfessions.computeIfAbsent(professionType[nextInsertIndex].getText(), o -> new ArrayList<>());
         existingProfRows.add(nextInsertIndex);
         boolean isFirstProf = existingProfRows.size() == 1;
         professionType[nextInsertIndex].setText(charProf.profType.getName());
         onSetProfession(existingProfessions, nextInsertIndex, charProf.skillType.getName());
         skillType[nextInsertIndex].setText(charProf.skillType.getName());
         professionLevel[nextInsertIndex].select(charProf.level);
         int adjustedLevel = Rules.getAdjustedSkillLevel(charProf.profType, charProf.skillType, character);

         skillLevelAdj[nextInsertIndex].setText("[" + adjustedLevel + "]");
         if (isFirstProf) {
            skillCost[nextInsertIndex].setText("(" + Rules.getProfessionCost(charProf.level) + ")");
         }
         else {
            skillCost[nextInsertIndex].setText("(" + charProf.rank.getCost() + ")");
         }
         professionLevel[nextInsertIndex].setEnabled(isFirstProf);
         skillRank[nextInsertIndex].setEnabled(!isFirstProf);
         skillRank[nextInsertIndex].setText(charProf.rank.getName());
         nextInsertIndex++;
      }
      // Now remove any remaining rows
      for (int i = 0; i < (SKILL_COUNT - 1); i++) {
         if ((professionType[i].getText().equals(NO_PROF_SELECTED)) && (!professionType[i + 1].getText().equals(NO_PROF_SELECTED))) {
            if (removeSkillRow(i)) {
               --i;
            }
         }
      }
      existingProfessions.clear();
      for (int i = 0; i < SKILL_COUNT; i++) {
         boolean isFirstProf = (null == existingProfessions.put(professionType[i].getText(), new ArrayList<>()));
         if (professionType[i].getText().equals(NO_PROF_SELECTED)) {
            professionLevel[i].select(0);
            professionLevel[i].setEnabled(false);
            skillType[i].setText(NO_SKILL_SELECTED);
            skillType[i].setEnabled(false);
         }
         if (skillType[i].getText().equals(NO_SKILL_SELECTED) && !isFirstProf) {
            skillRank[i].setText(NO_RANK_SELECTED);
            skillRank[i].setEnabled(false);
         }
         if (skillRank[i].getText().equals(NO_RANK_SELECTED) && !isFirstProf) {
            skillLevelAdj[i].setText("[0]");
            skillCost[i].setText("(0)");
         }
      }
   }

   private boolean removeSkillRow(int rowIndex) {
      boolean nextItemAvailable = (rowIndex+1) < SKILL_COUNT;
      professionType[rowIndex].select(nextItemAvailable ? professionType[rowIndex + 1].getSelectionIndex() : 0);
      professionType[rowIndex].setEnabled(!nextItemAvailable || professionType[rowIndex + 1].getEnabled());
      Map<String, ArrayList<Integer>> existingProfessions = new HashMap<>();
      for (int row=0 ; row<rowIndex ; row++) {
         existingProfessions.computeIfAbsent(professionType[row].getText(), o -> new ArrayList<>()).add(row);
      }
      onSetProfession(existingProfessions, rowIndex, nextItemAvailable ? skillType[rowIndex + 1].getText() : "");
      professionLevel[rowIndex].select(nextItemAvailable ? professionLevel[rowIndex + 1].getSelectionIndex() : 0);
      skillLevelAdj[rowIndex].setText(nextItemAvailable ? skillLevelAdj[rowIndex + 1].getText() : "[0]");
      skillCost[rowIndex].setText(nextItemAvailable ? skillCost[rowIndex + 1].getText() : "(0)");
      skillType[rowIndex].select(nextItemAvailable ? skillType[rowIndex + 1].getSelectionIndex() : 0);
      skillType[rowIndex].setEnabled(!nextItemAvailable || skillType[rowIndex + 1].getEnabled());
      skillRank[rowIndex].select(nextItemAvailable ? skillRank[rowIndex + 1].getSelectionIndex() : 0);
      skillRank[rowIndex].setEnabled(!nextItemAvailable || skillRank[rowIndex + 1].getEnabled());
      professionLevel[rowIndex].setEnabled(!nextItemAvailable || professionLevel[rowIndex + 1].getEnabled());
      if (nextItemAvailable) {
         removeSkillRow(rowIndex+1);
      }
      return nextItemAvailable;
   }

}
