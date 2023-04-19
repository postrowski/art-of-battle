package ostrowski.combat.common;

import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.html.Span;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CannonFodder {
   private static class FodderColumn {
      List<String>        elements         = new ArrayList<>();
      List<FodderColumn>  childColumns     = null;
      String              name;
      Map<String, String> headerAttributes = new HashMap<>();
      Map<String, String> attributesFirstRow = new HashMap<>();
      Map<String, String> attributes       = new HashMap<>();
      boolean             writeSideways    = false;
      boolean             hasFooter        = false;
      boolean             extendLastRow    = true;

      public FodderColumn(String name, Number value) {
         this(name, ((value == null) ? "-" : String.valueOf(value)));
      }

      public FodderColumn(String name, String rowValue) {
         this.name = name;
         elements.add(rowValue);
      }

      public FodderColumn(String name, List<FodderColumn> children) {
         this.name = name;
         this.childColumns = new ArrayList<>(children);
      }

      public FodderColumn addRowValue(Object obj) {
         elements.add(String.valueOf(obj));
         return this;
      }

      public FodderColumn setChildColumns(List<FodderColumn> children) {
         this.childColumns = new ArrayList<>(children);
         return this;
      }

      private int getRowCount() {
         if (childColumns == null) {
            return elements.size();
         }
         if (childColumns.isEmpty()) {
            return 0;
         }
         return childColumns.stream().map(FodderColumn::getRowCount).max(Integer::compare).get()+1;
      }

      public FodderColumn setHasFooter() {
         this.hasFooter = true;
         return this;
      }

      public List<TableData> getHTML(int row, int totalRowCount) {
         if (hasFooter) {
            return getHTMLWithFooter(row, totalRowCount);
         }
         return getHTML(this.elements, row, totalRowCount);
      }

      private List<TableData> getHTMLWithFooter(int row, int totalRowCount) {
         if (row == (totalRowCount-2)) {
            List<TableData> results = new ArrayList<>();
            results.add(new TableData(elements.get(elements.size() - 1)));
            return results;
         }
         List<String> elementsWithoutFooter = this.elements.subList(0, this.elements.size()-1);
         return getHTML(elementsWithoutFooter, row, totalRowCount-1);
      }

      private List<TableData> getHTML(List<String> elements, int row, int totalRowCount) {
         List<TableData> results = new ArrayList<>();
         if (row >= elements.size()) {
            if (childColumns != null) {
               return childColumns.stream()
                                  .map(o -> o.getHTML(row, totalRowCount))
                                  .flatMap(Collection::stream)
                                  .collect(Collectors.toList());
            }
            if (!extendLastRow && (row < (totalRowCount-1))) {
               TableData td = new TableData("&nbsp;");
               results.add(td);
            }
            return results;
         }
         ///////////////////////////////////////////////////////
         //     Col 1      |      Col 2      |       Col 3     |
         //                |      Col 2      |       Col 3     |
         //                |                 |       Col 3     |
         //    footer 1    |     footer 2    |     footer 3    |
         ///////////////////////////////////////////////////////
         // when row=1, <td colspan=3></td>          <td></td><td></td>
         // when row=2,                    <td colspan=2></td><td></td>
         // when row=3,                                       <td></td>
         // when row=4, <td>footer 1</td>  <td>footer 2</td>  <td>footer 3</td>

         int remainingRows = totalRowCount - row - 1;
         Map<String, String> combinedAttributes = new HashMap<>();
         combinedAttributes.putAll(attributes);
         if (row == 0) {
            for (Map.Entry<String, String> attr : attributesFirstRow.entrySet()) {
               String value = attr.getValue();
               if (combinedAttributes.containsKey(attr.getKey())) {
                  value += " " + combinedAttributes.get(attr.getKey());
               }
               combinedAttributes.put(attr.getKey(), value);
            }
         }
         TableData td = new TableData(elements.get(row));
         for (Map.Entry<String, String> attr : combinedAttributes.entrySet()) {
            td.setAttribute(attr.getKey(), attr.getValue());
         }

         // Is this the last row with content? If so, extend it to cover all remaining rows
         boolean lastRow = elements.size() == (row + 1);
         if (lastRow && extendLastRow && ((childColumns == null) || childColumns.get(0).elements.get(0).isEmpty())) {
            td.setRowSpan(remainingRows);
         }
         if (childColumns != null) {
            td.setColSpan(childColumns.size());
         }
         results.add(td);
         return results;
      }
      public FodderColumn setSideways() {
         writeSideways = true;
         return this;
      }
      public FodderColumn setHeaderAttribute(String key, String value) {
         this.headerAttributes.put(key, value);
         return this;
      }
      public FodderColumn setAttribute(String key, String value) {
         this.attributes.put(key, value);
         return this;
      }
      public FodderColumn setExtendLastRow(boolean extendLastRow) {
         this.extendLastRow = extendLastRow;
         return this;
      }

      public int getColSpan() {
         return (childColumns == null) ? 1 : childColumns.stream().map(FodderColumn::getColSpan).reduce(0, Integer::sum);
      }

      public TableHeader getHeader() {
         Object value = this.name;
         if (writeSideways) {
            Span span = new Span(this.name);
            span.setAttribute("class", "vertical-text");
            value = span;
         }
         TableHeader th = new TableHeader(value);
         for (Map.Entry<String, String> attr : headerAttributes.entrySet()) {
            th.setAttribute(attr.getKey(), attr.getValue());
         }
         return th;
      }

      public FodderColumn setClass(String cellClass) {
         setHeaderAttribute("class", cellClass);
         return setAttribute("class", cellClass);
      }
      public FodderColumn setFirstRowClass(String cellClass) {
         this.attributesFirstRow.put("class", cellClass);
         return this;
      }
   }

   public static final String BR = "<br/>";
   List<FodderColumn> cols = new ArrayList<>();
   public CannonFodder(Character chr) {

      cols.add(new FodderColumn("Points", chr.getPointTotal()).setSideways().setClass("points"));
//      cols.add(new FodderColumn("Race" + BR + "Gender",
//                                chr.getRace().getName() + BR + chr.getRace().getGender().name).setClass("race_gender"));
      byte vision = chr.getAlertness(true, false);
      byte hearing = chr.getAlertness(false, true);
      for (Attribute attr : Attribute.values()) {
         byte attributeLevel = chr.getAttributeLevel(attr);
         String value = String.valueOf(attributeLevel);
         if (attr == Attribute.Strength) value += BR + chr.getAdjustedStrength();
         if (attr == Attribute.Health) value += BR + chr.getBuildBase();
         if (attr == Attribute.Intelligence) {
            if (attributeLevel != vision) {
               value += BR + vision;
            }
            if ((attributeLevel != hearing) && (vision != hearing)) {
               value += BR + hearing;
            }
         }

         cols.add(new FodderColumn(attr.name(), (value)).setSideways().setClass("attr_" + attr.shortName));
      }
      cols.add(new FodderColumn("Encumbrance", Rules.getEncumbranceLevel(chr)).setSideways().setClass("encumbrance"));
      cols.add(new FodderColumn("Moves/Action", chr.getMovementRate()).setSideways().setClass("movement"));
      cols.add(new FodderColumn("Actions/Turn", chr.getActionsPerTurn()).setSideways().setClass("actions"));
      cols.add(new FodderColumn("Die for Pain Recovery" + BR + "max pain/wounds",
                                BR + Rules.getPainReductionDice(chr.getAttributeLevel(Attribute.Toughness)).toString() + BR)
                       .addRowValue(Rules.getCollapsePainLevel(chr.getAttributeLevel(Attribute.Toughness)))
                       .setHasFooter()
                       .setClass("pain_recovery")
              );
      List<FodderColumn> children = new ArrayList<>();
      int profIndex=1;
      Map<String, List<SkillType>> profProficient = new HashMap<>();
      Map<String, List<SkillType>> profFamiliar = new HashMap<>();
      for (Profession prof : chr.getProfessionsList()) {
         profProficient.put(prof.getType().getName(), new ArrayList<>(prof.getProficientSkills()));
         profFamiliar.put(prof.getType().getName(), new ArrayList<>(prof.getFamiliarSkills()));

         String name = prof.getType().getName() + ": " + prof.getLevel();
         String profProf = "prof: " + prof.getProficientSkillsAsString();
         String profFam = "";
         if (!prof.getFamiliarSkills().isEmpty()) {
            profFam = "fam: " + prof.getFamiliarSkillsAsString();
         }
         FodderColumn fodderColumn;
         if (false) {
            fodderColumn = new FodderColumn(prof.getType().getName(), name);
            fodderColumn.addRowValue(profProf);
            if (!profFam.isEmpty()) {
               fodderColumn.addRowValue(profFam);
            }
         }
         else {
            fodderColumn = new FodderColumn(prof.getType().getName(), name + BR + profProf + BR + profFam);
         }
         children.add(fodderColumn.setClass("profession" + profIndex++));
      }

      while (children.size() < 3) {
         children.add(new FodderColumn("", ""));
      }
      cols.add(new FodderColumn("Professions" + BR + "Name:level [adj]" + BR + "Advantages", children).setClass("professions"));

      String value = "";
      if (chr.getArmor() != null && chr.getArmor().isReal()) {
         value = chr.getArmor().getName();
      }
      List<Thing> thingsHeld = new ArrayList<>();
      Weapon primaryWeapon = null;
      Weapon secondaryWeapon = null;
      int primaryWeaponCount = 0;
      int secondaryWeaponCount = 0;
      for (Limb limb : chr.getLimbs()) {
         if (limb instanceof Hand) {
            Thing heldThing = limb.getHeldThing();
            if (heldThing instanceof Shield) {
               if (!value.isEmpty()) {
                  value += BR;
               }
               value += heldThing.getName();
            }
            else if (heldThing instanceof Weapon) {
               thingsHeld.add(heldThing);
               if (primaryWeapon == null) {
                  primaryWeapon = (Weapon) heldThing;
                  primaryWeaponCount = 1;
               }
               else if (primaryWeapon.getName().equals(heldThing.getName())) {
                  primaryWeaponCount++;
               }
            }
         }
      }
      for (Thing thing : chr.getEquipment()) {
         if (thing instanceof  Weapon) {
            secondaryWeapon = (Weapon) thing;
         }
         thingsHeld.add(thing);
      }
      boolean dualWield = primaryWeaponCount == 2;
      if (secondaryWeapon != null) {
         removeSkills(profProficient, profFamiliar, chr.getBestSkillType(secondaryWeapon));
         for (Thing thing : chr.getEquipment()) {
            if (thing.getName().equals(secondaryWeapon.getName())) {
               secondaryWeaponCount++;
            }
            if (thing.getName().equals(primaryWeapon.getName())) {
               primaryWeaponCount++;
            }
         }
      }

      SkillType bestSecondaryWeaponSkillType = null;
      SkillType bestUnarmedSkillType = null;
      byte bestSecondaryWeaponSkillLevel = -1;
      byte bestUnarmedSkillLevel = -1;
      if (secondaryWeapon != null) {
         bestSecondaryWeaponSkillType = chr.getBestSkillType(secondaryWeapon);
         bestSecondaryWeaponSkillLevel = chr.getAdjustedSkillLevel(bestSecondaryWeaponSkillType, LimbType.HAND_RIGHT, true, true, false);
      }
      List<SkillType> unarmedSkills = Arrays.asList(SkillType.Karate, SkillType.Boxing, SkillType.Brawling, SkillType.Wrestling);
      Race race = chr.getRace();
      Weapon weaponHeadButt = Weapons.getWeapon(Weapon.NAME_HeadButt, race);
      Weapon weaponKarate   = Weapons.getWeapon(Weapon.NAME_KarateKick, race);
      Weapon weaponPunch    = Weapons.getWeapon(Weapon.NAME_Punch, race);
      Weapon unarmedWeapon = null;
      if (chr.getLimb(LimbType.HAND_RIGHT) != null) {
         for (SkillType skill : unarmedSkills) {
            byte level = chr.getAdjustedSkillLevel(skill, LimbType.HAND_RIGHT, true, true, false);
            if (level > 0 && level > bestUnarmedSkillLevel) {
               bestUnarmedSkillLevel = level;
               bestUnarmedSkillType = skill;
               switch (skill) {
                  case Brawling: unarmedWeapon = weaponHeadButt; break;
                  case Karate:   unarmedWeapon = weaponKarate;   break;
                  case Boxing:   unarmedWeapon = weaponPunch;    break;
               }
            }
         }
      }
      if (primaryWeapon == null) {
         primaryWeapon = unarmedWeapon;
      }
      else if (secondaryWeapon == null) {
         secondaryWeapon = unarmedWeapon;
         bestSecondaryWeaponSkillType = bestUnarmedSkillType;
      }
      else {
         if (bestUnarmedSkillLevel > bestSecondaryWeaponSkillLevel) {
            secondaryWeapon = unarmedWeapon;
            bestSecondaryWeaponSkillType = bestUnarmedSkillType;
         }
      }

      if (primaryWeapon != null) {
         removeSkills(profProficient, profFamiliar, chr.getBestSkillType(primaryWeapon));
      }
      if (bestSecondaryWeaponSkillType != null) {
         removeSkills(profProficient, profFamiliar, bestSecondaryWeaponSkillType);
      }
      if (!value.isEmpty()) {
         value += BR;
      }

      String finalPrimaryWeaponName = primaryWeapon == null ? "" : primaryWeapon.getName();
      String finalSecondaryWeaponName = secondaryWeapon == null ? "" : secondaryWeapon.getName();
      thingsHeld.removeIf(thing -> thing.getName().equals(finalPrimaryWeaponName));
      thingsHeld.removeIf(thing -> thing.getName().equals(finalSecondaryWeaponName));
      value += thingsHeld.stream().map(Thing::getName).collect(Collectors.joining(", "));
      value += BR;
      List<String> advantageNames = chr.getAdvantagesList()
                                       .stream()
                                       .filter(adv -> !adv.getName().equals(Advantage.WEALTH))
                                       .map(adv -> {
                                          if (adv.hasLevels()
                                              && !adv.getName().equals(Advantage.MAGIC_RESISTANCE)
                                              && !adv.getName().equals(Advantage.PHOBIA)) {
                                             return adv.getLevelName();
                                          }
                                          return adv.toString();
                                       })
                                       .collect(Collectors.toList());
      if (race.getGender() != Race.Gender.MALE) {
         advantageNames.add(race.getGender().name);
      }
      value += "<B>" + String.join(", ", advantageNames) + "</B>";
      cols.add(new FodderColumn("Armor" + BR + "Shield" + BR + "Weapon(s)", value).setClass("equipment"));
      HashMap<Enums.RANGE, HashMap<DefenseOption, Byte>> defenses;
      defenses = chr.getDefenseOptionsBase(DamageType.BLUNT,false, false, false,false, (short) 1);
      HashMap<DefenseOption, Byte> defs = defenses.get(Enums.RANGE.OUT_OF_RANGE);

      cols.add(new FodderColumn("PD", defs.get(DefenseOption.DEF_PD)).setSideways().setClass("def_pd"));
      cols.add(new FodderColumn("Retreat", defs.get(DefenseOption.DEF_RETREAT)).setSideways().setClass("def_Retreat"));
      cols.add(new FodderColumn("Dodge", defs.get(DefenseOption.DEF_DODGE)).setSideways().setClass("def_Dodge"));
      cols.add(new FodderColumn("Block", defs.get(DefenseOption.DEF_LEFT)).setSideways().setClass("def_Block"));
      cols.add(new FodderColumn("Parry", defs.get(DefenseOption.DEF_RIGHT)).setSideways().setClass("def_Parry"));
      cols.add(new FodderColumn("Blunt", chr.getBuild(DamageType.BLUNT)).setSideways().setClass("def_Blunt"));
      cols.add(new FodderColumn("Cut", chr.getBuild(DamageType.CUT)).setSideways().setClass("def_Cut"));
      cols.add(new FodderColumn("Impale", chr.getBuild(DamageType.IMP)).setSideways().setClass("def_Impale"));

      if (primaryWeapon == null) {
         primaryWeapon = secondaryWeapon;
         secondaryWeapon = null;
      }
      Map<String, String> naturalAttack = new HashMap<>() {{
         put(Race.PROPERTIES_STURGEBREAK, Weapon.NAME_SturgeBeak);
         put(Race.PROPERTIES_FANGS, Weapon.NAME_Fangs);
         put(Race.PROPERTIES_TUSKS, Weapon.NAME_Tusks);
         put(Race.PROPERTIES_CLAWS, Weapon.NAME_Claws);
         put(Race.PROPERTIES_HORNS, Weapon.NAME_HornGore);
         put(Race.PROPERTIES_TAIL, Weapon.NAME_TailStrike);
      }};
      if (primaryWeapon == null) {
         for (String prop : naturalAttack.keySet()) {
            if (race.hasProperty(prop)) {
               primaryWeapon = Weapons.getWeapon(naturalAttack.remove(prop), race);
               break;
            }
         }
      }
      if (secondaryWeapon == null) {
         for (String prop : naturalAttack.keySet()) {
            if (race.hasProperty(prop)) {
               secondaryWeapon = Weapons.getWeapon(naturalAttack.remove(prop), race);
               break;
            }
         }
      }
      SkillType skillUsed = null;
      for (Weapon weapon : new Weapon[]{primaryWeapon, secondaryWeapon}) {
         String weaponName = (weapon != null) ? weapon.getName() : "";
         SkillType bestSkill = null;
         if (weapon != null) {
            bestSkill = chr.getBestSkillType(weapon);
            if (!weapon.isReal()) {
               weaponName = bestSkill.getName();
            }
         }
         if (skillUsed == bestSkill) {
            // print blank sections, rather than skipping
            weaponName = "";
            weapon = null;
         }
         skillUsed = bestSkill;

         if (weapon == primaryWeapon && primaryWeaponCount > 1) {
            weaponName += "&nbsp;x"+primaryWeaponCount;
            if (dualWield) {
               weaponName += "<br/>(Dual Wield)";
            }
         }
         else if (weapon == secondaryWeapon && secondaryWeaponCount > 1) {
            weaponName += " x"+secondaryWeaponCount;
         }
         FodderColumn weaponColumnA = new FodderColumn("Style name", weaponName).setFirstRowClass("weapon_name");
         FodderColumn weaponColumnB = new FodderColumn("actions", (weapon != null) ? "spd" : "");
         FodderColumn weaponColumnC = new FodderColumn("damage", (weapon != null) ? "damage" : "");
         if (weapon != null) {
            if (bestSkill == SkillType.Brawling || bestSkill == SkillType.Karate) {
               for (Map.Entry<String, String> attack : naturalAttack.entrySet()) {
                  if (race.hasProperty(attack.getKey())) {
                     Weapon naturalWeapon = Weapons.getWeapon(attack.getValue(), race);
                     SkillType skill = chr.getBestSkillType(naturalWeapon);
                     addWeaponStyles(chr, naturalWeapon, weaponColumnA, weaponColumnB, weaponColumnC, skill);
                  }
               }
               // Brawling can head butt, unless we've already added it as a natural attack
               if (bestSkill == SkillType.Brawling && !race.hasProperty(Race.PROPERTIES_HORNS) && !race.hasProperty(Race.PROPERTIES_FANGS)) {
                  addWeaponStyles(chr, weaponHeadButt, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
               // Brawling and karate can punch, unless we've already added a claws attack:
               if (!race.hasProperty(Race.PROPERTIES_CLAWS) && !race.hasProperty(Race.PROPERTIES_0_ARMS)) {
                  // This becomes Punch and Elbow Strike
                  addWeaponStyles(chr, weaponPunch, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
               else if (race.hasProperty(Race.PROPERTIES_CLAWS) && bestSkill == SkillType.Karate) {
                  Weapon claws = Weapons.getWeapon(Weapon.NAME_Claws, race);
                  addWeaponStyles(chr, claws, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }

               if (race.hasProperty(Race.PROPERTIES_HORNS) && bestSkill == SkillType.Karate) {
                  Weapon hornGore = Weapons.getWeapon(Weapon.NAME_HornGore, race);
                  addWeaponStyles(chr, hornGore, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
               if (bestSkill == SkillType.Karate) {
                  // This becomes Spin Kick and Kick
                  addWeaponStyles(chr, weaponKarate, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
               if (weapon.getName().equals(Weapon.NAME_Fangs)) {
                  addWeaponStyles(chr, weapon, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
            }
            else {
               addWeaponStyles(chr, weapon, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
            }
         }
         children = new ArrayList<>();
         children.add(weaponColumnB.setExtendLastRow(false));
         children.add(weaponColumnC.setExtendLastRow(false));
         String skillDesc = "";
         if (weapon != null) {
            byte adjSkillLevel;
            if (chr.getLimb(LimbType.HAND_RIGHT) == null) {
               adjSkillLevel = chr.getAdjustedSkillLevel(bestSkill, LimbType.HEAD, true, true, false);
            }
            else {
               adjSkillLevel = chr.getAdjustedSkillLevel(bestSkill, LimbType.HAND_RIGHT, true, true, false);
            }
            skillDesc = "Adj. skill: " + adjSkillLevel;
            if (bestSkill != SkillType.Throwing) {
               byte throwingSkillLevel = chr.getAdjustedSkillLevel(SkillType.Throwing, LimbType.HAND_RIGHT, true, false, false);
               if (throwingSkillLevel > 0) {
                  if (Arrays.stream(weapon.attackStyles).anyMatch(s -> s.isThrown())) {
                     skillDesc += "<br/> Thrown: " + throwingSkillLevel;
                  }
               }
            }
         }
         FodderColumn weaponColumnBC = new FodderColumn("skill", skillDesc);
         weaponColumnBC.setChildColumns(children);

         String name = (weapon == primaryWeapon) ? "Primary Weapon" : "Secondary weapon";
         children = new ArrayList<>();
         children.add(weaponColumnA.setExtendLastRow(false));
         children.add(weaponColumnBC.setExtendLastRow(false));
         cols.add(new FodderColumn(name, children).setClass("weapon").setExtendLastRow(false));
      }
   }

   private void addWeaponStyles(Character chr, Weapon weapon, FodderColumn weaponColumnA,
                                FodderColumn weaponColumnB, FodderColumn weaponColumnC,
                                SkillType weaponSkillType) {
      byte strengthAttr = chr.getAttributeLevel(Attribute.Strength);
      List<Profession> professions = chr.getProfessionsList();
      for (WeaponStyleAttack style : weapon.getAttackStyles()) {
         boolean minRankMet = (style.getMinRank() == SkillRank.UNKNOWN);
         if (style.getSkillType() != weaponSkillType && weaponSkillType != null) {
            continue;
         }
         if (style.getMinRank() == SkillRank.PROFICIENT) {
            for (Profession prof : professions) {
               if (prof.getProficientSkills().contains(style.getSkillType())) {
                  minRankMet = true;
                  break;
               }
            }
         }
         else { // if (style.getMinRank() == SkillRank.FAMILIAR) {
            for (Profession prof : professions) {
               if (prof.getProficientSkills().contains(style.getSkillType()) ||
                   prof.getFamiliarSkills().contains(style.getSkillType())) {
                  minRankMet = true;
                  break;
               }
            }
         }
         if (!minRankMet) {
            continue;
         }
         if (style.getName().equals("Elbow strike")) {
            continue;
         }
         byte penalty = style.getSkillPenalty();
         weaponColumnA.addRowValue(style.getName().replace(" ", "&nbsp;") +
                                   ((penalty == 0) ? "" : "&nbsp;(-" + penalty + ")"));
         weaponColumnB.addRowValue(style.getSpeed(strengthAttr));
         String damage = style.getDamageString(chr.getAdjustedStrength())
                              .replace(" + ", "+")
                              .replace(" ", "&nbsp;");
         Span span = new Span(damage);
         weaponColumnC.addRowValue(span);
      }
      if ((weaponSkillType == SkillType.Throwing) ||
          (chr.getAdjustedSkillLevel(SkillType.Throwing, LimbType.HAND_RIGHT, true, false, false) <= 0)) {
         return;
      }
      for (WeaponStyleAttack style : weapon.getAttackStyles()) {
         if (style.isThrown()) {
            addWeaponStyles(chr, weapon, weaponColumnA, weaponColumnB, weaponColumnC, SkillType.Throwing);
            return;
         }
      }
   }

   private void removeSkills(Map<String, List<SkillType>> profProficient, Map<String, List<SkillType>> profFamiliar, SkillType bestSkill) {
      if (bestSkill == null) {
         return;
      }
      removeSkill(profProficient, profFamiliar, bestSkill);
      removeSkill(profFamiliar, profProficient, bestSkill);
   }

   private void removeSkill(Map<String, List<SkillType>> primaryMap, Map<String, List<SkillType>> secondaryMap, SkillType bestSkill) {
      for (Map.Entry<String, List<SkillType>> entry : primaryMap.entrySet()) {
         if (entry.getValue().remove(bestSkill)) {
            String professionName = entry.getKey();
            if (entry.getValue().isEmpty() && secondaryMap.get(professionName).isEmpty()) {
               primaryMap.remove(professionName);
               secondaryMap.remove(professionName);
            }
            return;
         }
      }
   }

   private int getRowCount() {
      return cols.stream().map(FodderColumn::getRowCount).max(Integer::compareTo).get() - 1;
   }

   public String getRowHTML(int rowIndex) {
      int totalRowCount = getRowCount();
      StringBuilder sb = new StringBuilder();
      for (int row=0 ; row < totalRowCount ; row++) {
         TableRow tr = new TableRow();
         for (FodderColumn col : cols) {
            for (TableData td : col.getHTML(row, totalRowCount)) {
               tr.addTD(td);
            }
         }
         tr.setClassName("row" + rowIndex %4);
         sb.append(tr.toString());
      }
      return sb.toString();
   }
   public TableRow getHeaderRowHTML() {
      TableRow tr = new TableRow();
      for (FodderColumn col : cols) {
         TableHeader th = col.getHeader();
         th.setColSpan(col.getColSpan());
         tr.addTD(th);
      }
      return tr;
   }

   public static class HtmlCharWriter {
      private static final Map<String, String> advantageNamesMap = new HashMap<>() {{
         put(Advantage.ALERTNESS, "");
         put(Advantage.APPEARANCE, "");
         put(Advantage.DISFIGURED_ARMS, "");
         put(Advantage.DISFIGURED_FACE, "Face");
         put(Advantage.DISFIGURED_HANDS, "");
         put(Advantage.DISFIGURED_LEGS, "");
         put(Advantage.HEARING, "");
         put(Advantage.VISION, "");
         put(Advantage.WEALTH, "");
         put(Advantage.RANK_MILITARY_ENLISTED, "Rank");
         put(Advantage.RANK_MILITARY_OFFICER, "Officer");
         put(Advantage.RANK_SOCIAL_MALE, "Title");
         put(Advantage.RANK_SOCIAL_FEMALE, "Title");
      }};

      public static String convertCharacterToRow(Character character, boolean returnHeaderNames, int rowsToUse) {
         List<String> advantages = character.getAdvantagesList()
                                            .stream()
                                            .filter(o -> !o.getName().equals(Advantage.WEALTH))
                                            .map(adv -> {
                                               String mappedName = advantageNamesMap.get(adv.getName());
                                               if (mappedName == null) {
                                                  mappedName = adv.getName();
                                               }
                                               if (adv.hasLevels()) {
                                                  if (!mappedName.isBlank()) {
                                                     mappedName += ":";
                                                  }
                                                  mappedName += adv.getLevelName();
                                               }
                                               return mappedName;
                                            })
                                            .sorted(Comparator.comparingInt(String::length))
                                            .collect(Collectors.toList());
         Optional<Integer> maxLength = advantages.stream().map(String::length).max(Integer::compareTo);
         if (maxLength.isPresent()) {
            Integer maxLen = Math.max(maxLength.get(), 25);
            for (int i = 0; i < advantages.size() - 1; i++) {
               if (advantages.get(i).length() + advantages.get(i + 1).length() < maxLen) {
                  String adv1 = advantages.remove(i);
                  String adv2 = advantages.remove(i);
                  if (adv2.compareTo(adv1) < 1) {
                     advantages.add(i, adv2 + ", " + adv1);
                  } else {
                     advantages.add(i, adv1 + ", " + adv2);
                  }
                  i--;
               }
            }
         }
         advantages.sort(String::compareTo);
         WeaponDesc weaponDescPrime = null;
         WeaponDesc weaponDescAlt = null;
         @SuppressWarnings("unused")
         StringBuilder equipment = new StringBuilder();
         Shield shield = null;
         Weapon weaponPrime = null;
         Weapon weaponAlt = null;
         for (Limb limb : character.getLimbs()) {
            Thing thing = limb.getHeldThing();
            if (thing != null) {
               if (thing instanceof Shield) {
                  shield = (Shield) thing;
               }
               else if (thing instanceof Weapon) {
                  weaponPrime = (Weapon) thing;
                  weaponDescPrime = new WeaponDesc(character, weaponPrime, limb.limbType);
               }
               else if (thing.isReal()) {
                  if (equipment.length() > 0) {
                     equipment.append(", ");
                  }
                  equipment.append(thing.getName());
               }
            }
         }
         if ((weaponPrime == null) || (weaponAlt == null)) {
            for (LimbType limbType : Arrays.asList(LimbType.HAND_RIGHT, LimbType.LEG_RIGHT, LimbType.HEAD, LimbType.TAIL)) {
               Limb limb = character.getLimb(limbType);
               if (limb == null) {
                  continue;
               }
               Weapon weapon = limb.getWeapon(character);
               if (weapon != null) {
                  if (weaponPrime == null) {
                     weaponPrime = weapon;
                     weaponDescPrime = new WeaponDesc(character, weapon, limb.limbType);
                     break;
                  }
                  else if ((weaponAlt == null) && (weapon != weaponPrime)) {
                     if (weaponPrime.isReal()) {
                        // Show the 'punch' option for the alt weapon.
                        limb = character.getLimb(LimbType.HAND_RIGHT);
                        Thing oldWeap = limb.dropThing();
                        weapon = limb.getWeapon(character);
                        limb.setHeldThing(oldWeap, character);
                     }
                     weaponDescAlt = new WeaponDesc(character, weapon, limb.limbType);
//                  if ((weaponStrAlt != null) && (!weaponStrAlt.isEmpty()) && (!weaponStrPrime.equals(weaponStrAlt))) {
//                     weaponAlt = weapon;
//                  }
//                  else {
//                     weaponStrAlt = null;
//                  }
                     break;
                  }
               }
            }
         }

         for (Thing thing : character.getEquipment()) {
            if ((thing == weaponPrime) || (thing == shield)) {
               continue;
            }
            if (thing instanceof Weapon) {
               if (weaponPrime == null) {
                  weaponPrime = (Weapon) thing;
                  weaponDescPrime = new WeaponDesc(character, weaponPrime, LimbType.HAND_RIGHT);
                  continue;
               }
               else if ((weaponAlt == null) && (thing != weaponPrime)) {
                  weaponAlt = (Weapon) thing;
                  weaponDescAlt = new WeaponDesc(character, (Weapon) thing, LimbType.HAND_RIGHT);
                  continue;
               }
            }
            if (thing instanceof Potion) {
               continue;
            }
            if (equipment.length() > 0) {
               equipment.append(", ");
            }
            equipment.append(thing.getName());
         }

         List<Profession> professionsList = character.getProfessionsList();
         // Sort the professions by highest level.
         professionsList.sort((o1, o2) -> Byte.compare(o2.getLevel(), o1.getLevel()));

         List<List<String>> profs = new ArrayList<>();

         for (Profession profession : professionsList) {
            List<String> prof = new ArrayList<>();

            prof.add("<span class='professions'>" + profession.getType().getName() +
                     ": " + profession.getLevel() + "</span>");

            ArrayList<SkillType> proficientSkills = new ArrayList<>(profession.getProficientSkills());
            ArrayList<SkillType> familiarSkills = new ArrayList<>(profession.getFamiliarSkills());
            for (List<SkillType> skillTypeList : new ArrayList[]{proficientSkills, familiarSkills}) {
               if (skillTypeList.isEmpty()) {
                  continue;
               }
               skillTypeList.sort(Comparator.comparingInt((SkillType a) -> character.getAdjustedSkillLevel(a, null, true, true, false))
                                            .reversed()
                                            .thenComparing(a -> a.name));
               boolean firstFamiliarSkill  = (skillTypeList == familiarSkills);

               for (SkillType skill : skillTypeList) {
                  String thisSkillDesc = skill.getName();
                  thisSkillDesc = thisSkillDesc.replace("2-Handed ", "2-Hand ");
                  byte skillLevel = character.getAdjustedSkillLevel(skill, null/*useLimb*/, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
                  if (skillLevel != profession.getLevel(skill)) {
                     thisSkillDesc += " [" + skillLevel + "]";
                  }

                  if (skillTypeList == proficientSkills) {
                     prof.add("<span class='proficient_skill'>&nbsp; " + thisSkillDesc + "</span>");
                  } else {
//                  if (firstFamiliarSkill) {
//                     prof.add("<span class='familiar_skill'>familiar: " + thisSkillDesc + "</span>");
//                     firstFamiliarSkill = false;
//                  } else {
                     prof.add("<span class='familiar_skill'>&nbsp; &nbsp; " + thisSkillDesc + "</span>");
//                  }
                  }
               }
            }
            profs.add(prof);
         }
         Armor armor = character.getArmor();
         String strStr = String.valueOf(character.getAttributeLevel(Attribute.Strength));
         String htStr = String.valueOf(character.getAttributeLevel(Attribute.Health));
         if (character.getRace().getBuildModifier() != 0) {
            strStr += "(" + character.getAdjustedStrength() + ")";
            htStr += "(" + character.getBuildBase() + ")";
         }
         short distance = 1;
         HashMap<Enums.RANGE, HashMap<DefenseOption, Byte>> defMap =
                 character.getDefenseOptionsBase(DamageType.GENERAL,
                                                 false/*isGrappleAttack*/,
                                                 false /*includeWoundPenalty*/,
                                                 false /*includePosition*/,
                                                 false /*computePdOnly*/, distance);

         DiceSet painDice = Rules.getPainReductionDice(character.getAttributeLevel(Attribute.Toughness));
         byte collapsePainLevel = Rules.getCollapsePainLevel(character.getAttributeLevel(Attribute.Toughness));

         @SuppressWarnings("unused")
         StringBuilder attributes = new StringBuilder();
         for (Attribute attr : Attribute.values()) {
            StringBuilder attrStr = new StringBuilder(String.valueOf(character.getAttributeLevel(attr)));
            int maxLen = 4;
            if (attr == Attribute.Strength) {
               attrStr = new StringBuilder(strStr);
               maxLen = 6;
            }
            else if (attr == Attribute.Health) {
               attrStr = new StringBuilder(htStr);
               maxLen = 6;
            }
            while (attrStr.length() < maxLen) {
               attrStr.insert(0, " ");
            }
            attributes.append(attrStr);
         }
         byte enc = Rules.getEncumbranceLevel(character);
         byte move = character.getMovementRate();
         byte actions = character.getActionsPerTurn();
         weaponDescPrime.describeWeapon();
         if (weaponDescAlt != null) {
            weaponDescAlt.describeWeapon();
         }

         if (returnHeaderNames) {
            rowsToUse = 3;
         }

         TableRow[] rows = new TableRow[rowsToUse];
         for (int r = 0 ; r < rowsToUse ; r++) {
            rows[r] = new TableRow();
         }
         if (returnHeaderNames) {
            //rows[0].addTD(new TableData("gen").setRowSpan(rowsUsed));
            rows[0].addTD(new TableData("Points").setRowSpan(3).setClassName("points header"));
            rows[0].addTD(new TableData("Race<br/><br/>Gender").setRowSpan(3).setClassName("race_gender header"));
            rows[0].addTD(new TableData("Attributes").setColSpan(Attribute.values().length)
                                                     .setClassName("attr_HEADER"));
            for (Attribute attr : Attribute.values()) {
               rows[1].addTD(new TableData(attr.name()).setRowSpan(2)
                                                       .setClassName("header attr attr_" + attr.shortName.toUpperCase()));
            }
            rows[0].addTD(new TableData("Encumbrance").setRowSpan(3).setClassName("header encumbrance"));
            rows[0].addTD(new TableData("Move / Action").setRowSpan(3).setClassName("header movement"));
            rows[0].addTD(new TableData("Actions / Turn").setRowSpan(3).setClassName("header actions"));
            rows[0].addTD(new TableData("Dice for Pain Recovery").setRowSpan(2).setClassName("header pain_recovery_top"));
            rows[2].addTD(new TableData("max pain<br/>/ wounds").setClassName("pain_recovery_bottom"));
            String profHeader = "<span class='professions'>Professions: Level</span><br/>" +
                                "<span class='proficient_skill'>Prof. Skill [adj lvl]</span><br/>" +
                                "<span class='familiar_skill'>Fam. Skill [adj lvl]</span><br/><br/>" +
                                "<span class='advantages'>Advantages</span>";
            rows[0].addTD(new TableData(profHeader).setRowSpan(3).setColSpan(12));
            rows[0].addTD(new TableData("Armor<br/>Shield<br/>Weapons").setRowSpan(3));
            rows[0].addTD(new TableData("PD").setRowSpan(3).setClassName("header def_pd"));
            rows[0].addTD(new TableData("Base<br/>Defenses").setColSpan(4).setClassName("def_HEADER"));
            rows[1].addTD(new TableData("Retreat").setRowSpan(2).setClassName("header def_Retreat"));
            rows[1].addTD(new TableData("Dodge").setRowSpan(2).setClassName("header def_Dodge"));
            rows[1].addTD(new TableData("Block").setRowSpan(2).setClassName("header def_Block"));
            rows[1].addTD(new TableData("Parry").setRowSpan(2).setClassName("header def_Parry"));
            rows[0].addTD(new TableData("Build<br/>vs.").setColSpan(3).setClassName("deff_HEADER"));
            rows[1].addTD(new TableData("Blunt").setRowSpan(2).setClassName("header deff_Blunt"));
            rows[1].addTD(new TableData("Cut").setRowSpan(2).setClassName("header deff_Cut"));
            rows[1].addTD(new TableData("Impale").setRowSpan(2).setClassName("header deff_Impale"));
            rows[0].addTD(new TableData("Primary Weapon").setColSpan(4).setClassName("weapon_name header"));
            rows[1].addTD(new TableData("Style Name").setRowSpan(2));
            rows[1].addTD(new TableData("Re-ready Actions").setRowSpan(2).setColSpan(2).setClassName("reready_actions_HEADER"));
            rows[1].addTD(new TableData("Damage").setRowSpan(2));
            rows[0].addTD(new TableData("Alternate Weapon").setColSpan(4).setClassName("weapon_name header"));
            rows[1].addTD(new TableData("Style Name").setRowSpan(2));
            rows[1].addTD(new TableData("Re-ready Actions").setRowSpan(2).setColSpan(2).setClassName("reready_actions_HEADER"));
            rows[1].addTD(new TableData("Damage").setRowSpan(2));
         } else {
            //rows[0].addTD(new TableData(character.getName()).setRowSpan(rowsUsed));
            rows[0].addTD(new TableData(String.valueOf(character.getPointTotal())).setRowSpan(rowsToUse));
            String gender = character.getRace().getGender().toString();
            gender = gender.charAt(0) + gender.substring(1).toLowerCase();
            rows[0].addTD(new TableData(character.getRace().getName() + "<br/>" + gender).setRowSpan(rowsToUse)
                                                                                         .setClassName("race_gender"));
            for (Attribute attr : Attribute.values()) {
               String attrVal = String.valueOf(character.getAttributeLevel(attr));
               if (attr == Attribute.Strength) {
                  if (character.getAttributeLevel(attr) != character.getAdjustedStrength()) {
                     attrVal += "<br/><b>" + character.getAdjustedStrength() + "</b>";
                  }
                  else {
                     attrVal = "<b>" + attrVal + "</b>";
                  }
               }
               else if (attr == Attribute.Health) {
                  if (character.getAttributeLevel(attr) != character.getBuildBase()) {
                     attrVal += "<br/><b>" + character.getBuildBase() + "</b>";
                  }
                  else {
                     attrVal = "<b>" + attrVal + "</b>";
                  }
               }
               rows[0].addTD(new TableData(attrVal).setRowSpan(rowsToUse)
                                                   .setClassName("attr_" + attr.shortName.toUpperCase()));
            }
            rows[0].addTD(new TableData(enc).setRowSpan(rowsToUse).setClassName("encumbrance"));
            rows[0].addTD(new TableData(move).setRowSpan(rowsToUse).setClassName("movement"));
            rows[0].addTD(new TableData(actions).setRowSpan(rowsToUse).setClassName("actions"));
            rows[0].addTD(new TableData(painDice.toString()).setRowSpan(rowsToUse-1));
            rows[rowsToUse-1].addTD(new TableData(collapsePainLevel));
            int profRows = rowsToUse;
            String profsClass = "profession";
            if (!advantages.isEmpty()) {
               profRows--;
               profsClass += " noBottomBorder";
            }

            // can we combine professions into a single column?
            for (int p=0 ; p<profs.size() ; p++) {
               List<String> prof = profs.get(p);
               if ((p < (profs.size() - 1)) && (prof.size() + profs.get(p + 1).size()) <= 5) {
                  // combine this prof with the next one:
                  List<String> nextProfs = profs.remove(p + 1);
                  prof.addAll(nextProfs);
               }
            }
            int spansUsed = 0;
            int columnsPerProfession = 12 / profs.size();
            int profIndex = 0;
            for (List<String> prof : profs) {
               String profClass = profsClass;
               if (++profIndex == profs.size()) {
                  // This is the list profession block. Used all the remaining columns
                  columnsPerProfession = 12 - spansUsed;
               } else {
                  profClass += " noRightBorder";
               }
               String content = String.join("<br/>", prof);
               rows[0].addTD(new TableData(content).setRowSpan(profRows)
                                                   .setColSpan(columnsPerProfession)
                                                   .setClassName(profClass.trim()));
               spansUsed += columnsPerProfession;
            }
            if (!advantages.isEmpty()) {
               String advantagesStr = String.join("<br/>", advantages);
               rows[rowsToUse - 1].addTD(new TableData(advantagesStr).setColSpan(12)
                                                                     .setClassName("advantages"));
            }
            StringBuilder equip = new StringBuilder(armor.getName() + ((shield == null) ? "" : "<br/>" + shield.getName()) + "<br/>");
            List<Thing> things = new ArrayList<>();
            Limb rightHand = character.getLimb(LimbType.HAND_RIGHT);
            Limb leftHand = character.getLimb(LimbType.HAND_LEFT);
            Thing thingR = (rightHand == null) ? null : rightHand.getHeldThing();
            Thing thingL = ( leftHand == null) ? null :  leftHand.getHeldThing();
            if ((thingR != null) && (thingR.isReal())) {
               things.add(thingR);
            }
            if ((thingL != null) && (thingL.isReal())) {
               if (!(thingL instanceof Shield)) {
                  // We've already listed our shield above, don't print it twice.
                  things.add(thingL);
               }
            }
            things.addAll(character.getEquipment());
            boolean first = true;
            for (Thing thing : things) {
               if (thing instanceof Potion) {
                  // Don't list potions in the cannon fodder list.
                  // When created, they did not impact the characters enc. level or money spent.
                  continue;
               }
               if (!first) {
                  equip.append(", ");
               }
               first = false;
               equip.append(thing.name);
            }
            HashMap<DefenseOption, Byte> defOptMap = defMap.get(Enums.RANGE.OUT_OF_RANGE);
            rows[0].addTD(new TableData(equip.toString()).setRowSpan(rowsToUse));
            rows[0].addTD(new TableData(String.valueOf(character.getPassiveDefense(Enums.RANGE.OUT_OF_RANGE, false, distance))).setRowSpan(rowsToUse).setClassName("def_pd"));
            rows[0].addTD(new TableData(String.valueOf(defOptMap.get(DefenseOption.DEF_RETREAT))).setRowSpan(rowsToUse).setClassName("def_Retreat"));
            rows[0].addTD(new TableData(String.valueOf(defOptMap.get(DefenseOption.DEF_DODGE))).setRowSpan(rowsToUse).setClassName("def_Dodge"));
            Byte block = defOptMap.get(DefenseOption.DEF_LEFT);
            if ((shield == null) || (block == null)) {
               block = 0;
            }
            rows[0].addTD(new TableData(block < 1 ? "-" : String.valueOf(block)).setRowSpan(rowsToUse).setClassName("def_Block"));
            rows[0].addTD(new TableData(String.valueOf(defMap.get(Enums.RANGE.OUT_OF_RANGE).get(DefenseOption.DEF_RIGHT))).setRowSpan(rowsToUse).setClassName("def_Parry"));
            rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.BLUNT))).setRowSpan(rowsToUse).setClassName("deff_Blunt"));
            rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.CUT))).setRowSpan(rowsToUse).setClassName("deff_Cut"));
            rows[0].addTD(new TableData(String.valueOf(character.getBuild(DamageType.IMP))).setRowSpan(rowsToUse).setClassName("deff_Impale"));
            if (weaponDescPrime.weapon.isUnarmedStyle()) {
               SkillType primeSkill = character.getBestSkillType(weaponDescPrime.weapon);
               rows[0].addTD(new TableData(primeSkill.getName()).setColSpan(2).setClassName("weapon_name"));
            } else {
               rows[0].addTD(new TableData(weaponDescPrime.weapon.getName()).setColSpan(2).setClassName("weapon_name"));
            }
            rows[0].addTD(new TableData("Adjusted skill: " + weaponDescPrime.adjustedSkill).setColSpan(2).setClassName("weapon_skill"));
            int s = 1;
            for (WeaponStyleDesc styleDesc : weaponDescPrime.styleList) {
               rows[s++].addTD(new TableData(styleDesc.getAlteredName(weaponDescPrime)).setColSpan(2))
                        .addTD(new TableData(styleDesc.actionsRequired).setClassName("reready_actions"))
                        .addTD(new TableData(styleDesc.damageStr));
            }
            while (s < rowsToUse) {
               rows[s++].addTD(new TableData("&nbsp;").setColSpan(2))
                        .addTD(new TableData("&nbsp;").setClassName("reready_actions"))
                        .addTD(new TableData("&nbsp;"));
            }
            s = 1;
            boolean altWeaponAvailable = false;
            if ((weaponDescAlt != null) && (!weaponDescAlt.weapon.getName().equals(weaponDescPrime.weapon.getName()))) {
               altWeaponAvailable = true;
               if (weaponDescAlt.weapon.isUnarmedStyle()) {
                  SkillType altSkill = character.getBestSkillType(weaponDescAlt.weapon);
                  if (altSkill == null) {
                     altWeaponAvailable = false;
                  }
                  else {
                     rows[0].addTD(new TableData(altSkill.getName()).setColSpan(2).setClassName("weapon_name"));
                  }
               } else {
                  rows[0].addTD(new TableData(String.valueOf(weaponDescAlt.weapon.getName())).setColSpan(2)
                                                                                             .setClassName("weapon_name"));
               }
               if (altWeaponAvailable) {
                  rows[0].addTD(new TableData("Adjusted skill: " + weaponDescAlt.adjustedSkill).setColSpan(2)
                                                                                               .setClassName("weapon_skill"));
                  for (WeaponStyleDesc styleDesc : weaponDescAlt.styleList) {
                     rows[s++].addTD(new TableData(styleDesc.getAlteredName(weaponDescAlt)).setColSpan(2))
                              .addTD(new TableData(styleDesc.actionsRequired).setClassName("reready_actions"))
                              .addTD(new TableData(styleDesc.damageStr));
                  }
               }
            }
            if (!altWeaponAvailable) {
               rows[0].addTD(new TableData("&nbsp;").setColSpan(2));
               rows[0].addTD(new TableData("&nbsp;").setClassName("reready_actions"));
               rows[0].addTD(new TableData("&nbsp;"));
            }
            while (s < rowsToUse) {
               rows[s++].addTD(new TableData("&nbsp;").setColSpan(2))
                        .addTD(new TableData("&nbsp;").setClassName("reready_actions"))
                        .addTD(new TableData("&nbsp;"));
            }
         }
         int maxStyles = 1;
         if (weaponDescPrime != null) {
            maxStyles = Math.max(maxStyles, weaponDescPrime.styleList.size());
         }
         if (weaponDescAlt != null) {
            maxStyles = Math.max(maxStyles, weaponDescAlt.styleList.size());
         }
         maxStyles++;
         character.setName("" + maxStyles);
         StringBuilder sb = new StringBuilder();
         for (TableRow row : rows) {
            sb.append(row);
         }
         return sb.toString().replace("<br/></", "</");//.replaceAll("<br/>", "%");
      }

   }

   static class WeaponStyleDesc {
      private final WeaponStyleAttack style;
      private final SkillType skillType;
      public byte baseSkill;
      public       byte    adjustedSkill = (byte)0;
      public final byte    actionsRequired;
      public final String  damageStr;
      public final String  styleName;
      public       boolean isShowable = false;
      public WeaponStyleDesc(WeaponStyleAttack style, Character character) {
         this.style           = style;
         if (this.style.isRanged() && (this.style instanceof WeaponStyleAttackMissile)) {
            this.actionsRequired = ((WeaponStyleAttackMissile)this.style).getNumberOfPreparationSteps();
         }
         else {
            this.actionsRequired = style.getSpeed(character.getAttributeLevel(Attribute.Strength));
         }
         if (style.canCharge(false, character.getLegCount() > 3)) {
            this.styleName    = style.getName() + " (may charge)";
         } else {
            this.styleName    = style.getName();
         }
         this.damageStr       = style.getDamageString(character.getPhysicalDamageBase());
         this.skillType       = style.getSkillType();
         this.baseSkill       = character.getSkillLevel(style.getSkillType(), null, false, false, false);
         if (this.baseSkill > 0 ) {
            this.adjustedSkill = character.getSkillLevel(this.skillType, null,
                                                         true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
            this.adjustedSkill += character.getAttributeLevel(Attribute.Dexterity);
            this.adjustedSkill -= this.style.getSkillPenalty();
            byte baseSkillLevel = character.getSkillLevel(this.skillType, null, false, true, false);
            this.isShowable = (baseSkillLevel > this.style.getSkillPenalty());
         }
      }

      public String getAlteredName(WeaponDesc weaponDesc) {
         String alteredName = this.styleName;
         boolean diffSkill = weaponDesc.singleSkillType != skillType;
         if (weaponDesc.weapon.getName().equals(Weapon.NAME_BastardSword) ||
             weaponDesc.weapon.getName().equals(Weapon.NAME_BastardSword_Fine)) {
            diffSkill = false;
         }
         if (diffSkill) {
            String skillName = skillType.name;
            if (skillType == SkillType.TwoHanded_Sword) {
               skillName = "2h Sword";
            } else if (skillType == SkillType.TwoHanded_AxeMace) {
               skillName = "2h Axe";
            }
            if ((!skillName.equals(skillType.name) && (alteredName.endsWith(" (2h)")))) {
               alteredName = alteredName.substring(0, alteredName.length() - 5);
            }

            alteredName += " (" + skillName;
         }
         if (weaponDesc.adjustedSkill != adjustedSkill) {
            alteredName += " [" + adjustedSkill + "]";
         }
         if (diffSkill) {
            alteredName += ")";
         }
         if (this.style.isRanged()) {
            short maxDistance = ((WeaponStyleAttackRanged)this.style).getMaxDistance(weaponDesc.character.getAdjustedStrength());
            if (this.style.isMissile()) {
               alteredName = "Ranges: " + getRanges(maxDistance, this.style.isThrown());
            } else {
               alteredName += "<br/>Ranges: " + getRanges(maxDistance, this.style.isThrown());
            }
         }
         return alteredName;
      }
      private String getRanges(float maxDistance, boolean thrown) {
         List<Float> ranges = new ArrayList<>();
         if (thrown) {
            // Thrown weapons have no PB range
            ranges.add(0f);
         } else {
            ranges.add(maxDistance / 8.0f);
         }
         ranges.add(maxDistance / 4.0f);
         ranges.add(maxDistance / 2.0f);
         ranges.add(maxDistance);
         return ranges.stream()
                      .map(Math::round)
                      .map(Object::toString)
                      .collect(Collectors.joining(", "));
      }

      private void describeStyle(boolean isOnlySkill, Byte singleSkillBaseLevel, StringBuilder description) {
         if (description.length() > 0) {
            description.append("\n");
            if (isOnlySkill) {
               description.append("    ");
            }
         }
         description.append(this.styleName).append(", ");
         if (isOnlySkill) {
            description.append(" skill ").append(this.skillType.getName()).append(": ").append(this.baseSkill);
         }
         if (((singleSkillBaseLevel != null) ? singleSkillBaseLevel : this.baseSkill) != this.adjustedSkill) {
            description.append(" (adj. ").append(this.adjustedSkill).append(")");
         }
         description.append(" ").append(this.actionsRequired).append(" actions: ");
         description.append(this.damageStr);
      }
   }

   static class WeaponDesc {
      private final Character character;
      private final Weapon weapon;
      private final LimbType limbType;
      private      SkillType             singleSkillType = null;
      public final List<WeaponStyleDesc> styleList       = new ArrayList<>();
      public       int                   baseSkill;
      public int adjustedSkill;
      public Byte singleSkillBaseLevel = null;

      public WeaponDesc(Character character, Weapon weapon, LimbType limbType) {
         this.character = character;
         this.weapon = weapon;
         this.limbType = limbType;
      }

      public String describeWeapon() {
         // See if there is a single skill that governs all the possible attack styles:
         for (WeaponStyleAttack style : this.weapon.attackStyles) {
            byte skillLevel = this.character.getSkillLevel(style.getSkillType(), null, false, true, true);
            if (skillLevel < 1 ) {
               continue;
            }

            if (this.singleSkillType == null) {
               this.singleSkillType = style.getSkillType();
            }
            else if (this.singleSkillType != style.getSkillType()) {
               if (this.singleSkillType.getName().equals(SkillType.Knife.getName()) && style.getSkillType().getName().equals(SkillType.Throwing.getName())) {
                  continue;
               }
               if (this.singleSkillType.getName().equals(SkillType.Throwing.getName()) && style.getSkillType().getName().equals(SkillType.Knife.getName())) {
                  this.singleSkillType = style.getSkillType();
                  continue;
               }
               this.singleSkillType = null;
               break;
            }
         }
         StringBuilder description = new StringBuilder();
         if (this.weapon.isReal()) {
            description.append(this.weapon.getName()).append(": ");
         }
         if (this.singleSkillType != null) {
            this.singleSkillBaseLevel = this.character.getSkillLevel(this.singleSkillType, null, false, true, true);
            byte skillLevel = this.character.getSkillLevel(this.singleSkillType, null, true/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
            skillLevel += this.character.getAttributeLevel(Attribute.Dexterity);
            if (this.weapon.isReal()) {
               description.append(" skill ");
            }
            this.baseSkill = this.singleSkillBaseLevel;
            this.adjustedSkill = skillLevel;
            description.append(this.singleSkillType.getName()).append(": ").append(this.singleSkillBaseLevel);
            if (this.singleSkillBaseLevel != skillLevel) {
               description.append(" (adj. ").append(skillLevel).append(")");
            }
            this.singleSkillBaseLevel = skillLevel;
         }

         for (WeaponStyleAttack style : this.weapon.attackStyles) {
            WeaponStyleDesc styleDesc = new WeaponStyleDesc(style, this.character);
            if (styleDesc.isShowable) {
               styleDesc.describeStyle((this.singleSkillType != null), this.singleSkillBaseLevel, description);
               this.styleList.add(styleDesc);
            }
         }
         // list the attack types corresponding to the higher skill level first:
         this.styleList.sort((o1, o2) -> {
            if (o1.style.skillType.getName().equals(o2.style.skillType.getName())) {
               return 0;
            }
            byte l1 = character.getSkillLevel(o1.style.skillType, null, false, false, false);
            byte l2 = character.getSkillLevel(o2.style.skillType, null, false, false, false);
            if (l1 == l2) {
               return 0;
            }
            return l1 > l2 ? -1 : 1;
         });
         // unarmed skills need to also list the damage from kicks:
         if (!(this.weapon.isReal() || (this.limbType != LimbType.HAND_RIGHT))) {
            Limb leg = this.character.getLimb(LimbType.LEG_RIGHT);
            if (leg != null) {
               Weapon legWeapon = leg.getWeapon(this.character);
               for (WeaponStyleAttack style : legWeapon.attackStyles) {
                  WeaponStyleDesc styleDesc = new WeaponStyleDesc(style, this.character);
                  if (styleDesc.isShowable) {
                     styleDesc.describeStyle((this.singleSkillType != null), this.singleSkillBaseLevel, description);
                     this.styleList.add(styleDesc);
                  }
               }
            }
            Limb head = this.character.getLimb(LimbType.HEAD);
            if (head != null) {
               Weapon headWeapon = head.getWeapon(this.character);
               for (WeaponStyleAttack style : headWeapon.attackStyles) {
                  WeaponStyleDesc styleDesc = new WeaponStyleDesc(style, this.character);
                  if (styleDesc.isShowable) {
                     styleDesc.describeStyle((this.singleSkillType != null), this.singleSkillBaseLevel, description);
                     this.styleList.add(styleDesc);
                  }
               }
            }
         }
         return description.toString();
      }
   }

}
