package ostrowski.combat.common;

import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.html.Span;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyle;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackMelee;

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
      Weapon weaponHeadButt = Weapons.getWeapon(Weapon.NAME_HeadButt, chr.getRace());
      Weapon weaponKarate   = Weapons.getWeapon(Weapon.NAME_KarateKick, chr.getRace());
      Weapon weaponPunch    = Weapons.getWeapon(Weapon.NAME_Punch, chr.getRace());
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
      if (chr.getRace().getGender() != Race.Gender.MALE) {
         advantageNames.add(chr.getRace().getGender().name);
      }
      value += "<B>" + String.join(", ", advantageNames) + "</B>";
      cols.add(new FodderColumn("Armor" + BR + "Shield" + BR + "Weapon(s)", value).setClass("equipment"));
      HashMap<Enums.RANGE, HashMap<DefenseOption, Byte>> defenses;
      defenses = chr.getDefenseOptionsBase(DamageType.CUT,false, false, false,false, (short) 1);
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
         put(Race.PROPERTIES_FANGS, Weapon.NAME_Fangs);
         put(Race.PROPERTIES_CLAWS, Weapon.NAME_Claws);
         put(Race.PROPERTIES_HORNS, Weapon.NAME_HornGore);
      }};
      if (primaryWeapon == null) {
         for (String prop : naturalAttack.keySet()) {
            if (chr.getRace().hasProperty(prop)) {
               primaryWeapon = Weapons.getWeapon(naturalAttack.remove(prop), chr.getRace());
               break;
            }
         }
      }
      if (secondaryWeapon == null) {
         for (String prop : naturalAttack.keySet()) {
            if (chr.getRace().hasProperty(prop)) {
               secondaryWeapon = Weapons.getWeapon(naturalAttack.remove(prop), chr.getRace());
               break;
            }
         }
      }
      for (Weapon weapon : new Weapon[]{primaryWeapon, secondaryWeapon}) {
         String weaponName = (weapon != null) ? weapon.getName() : "";
         if (weapon == weaponPunch) {
            weaponName = "Boxing";
         } else if (weapon == weaponHeadButt) {
            weaponName = "Brawling";
         } else if (weapon == weaponKarate) {
            weaponName = "Karate";
         }

         if (weapon == primaryWeapon && primaryWeaponCount > 1) {
            weaponName += "&nbsp;x"+primaryWeaponCount;
            if (dualWield) {
               weaponName += "<br/>(Dual Wield)";
            }
         }
         else if (weapon == secondaryWeapon && secondaryWeaponCount > 1) {
            weaponName += " x"+secondaryWeaponCount;
         }
         SkillType bestSkill = null;
         if (weapon != null) {
            bestSkill = chr.getBestSkillType(weapon);
         }
         FodderColumn weaponColumnA = new FodderColumn("Style name", weaponName).setFirstRowClass("weapon_name");
         FodderColumn weaponColumnB = new FodderColumn("actions", (weapon != null) ? "spd" : "");
         FodderColumn weaponColumnC = new FodderColumn("damage", (weapon != null) ? "damage" : "");
         if (weapon != null) {
            if (weapon == weaponHeadButt && chr.getRace().hasProperty(Race.PROPERTIES_HORNS)) {
               Weapon weaponHornGore = Weapons.getWeapon(Weapon.NAME_HornGore, chr.getRace());
               bestSkill = chr.getBestSkillType(weaponHornGore);
               addWeaponStyles(chr, weaponHornGore, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               addWeaponStyles(chr, weaponPunch, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
            }
            else {
               addWeaponStyles(chr, weapon, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               if ((weapon == weaponHeadButt) || (weapon == weaponKarate)) {
                  addWeaponStyles(chr, weaponPunch, weaponColumnA, weaponColumnB, weaponColumnC, bestSkill);
               }
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
}
