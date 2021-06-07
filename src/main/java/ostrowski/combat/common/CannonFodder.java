package ostrowski.combat.common;

import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.Span;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;

import java.util.*;
import java.util.stream.Collectors;

public class CannonFodder {
   private static class FodderColumn {
      List<String>        elements         = new ArrayList<>();
      List<FodderColumn>  childColumns     = null;
      String              name;
      Map<String, String> headerAttributes = new HashMap<>();
      Map<String, String> attributes       = new HashMap<>();
      boolean             writeSideways    = false;
      boolean             hasFooter        = false;

      public FodderColumn(String name, Number value) {
         this(name, String.valueOf(value));
      }

      public FodderColumn(String name, String rowValue) {
         this.name = name;
         elements.add(rowValue);
      }

      public FodderColumn(String name, List<FodderColumn> children) {
         this.name = name;
         this.childColumns = children;
      }

      public FodderColumn addRowValue(Object obj) {
         elements.add(String.valueOf(obj));
         return this;
      }

      public FodderColumn setChildColumns(List<FodderColumn> children) {
         this.childColumns = children;
         return this;
      }

      private int getRowCount() {
         if (childColumns == null) {
            return elements.size();
         }
         return childColumns.stream().map(FodderColumn::getRowCount).max(Integer::compare).get()+1;
      }

      public FodderColumn setHasFooter() {
         this.hasFooter = true;
         return this;
      }

      public List<TableData> getHTML(int row, int totalRowCount) {
         if (childColumns != null) {
            return childColumns.stream()
                               .map(o -> o.getHTML(row, totalRowCount))
                               .flatMap(Collection::stream)
                               .collect(Collectors.toList());
         }
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
         TableData td = new TableData(elements.get(row));
         for (Map.Entry<String, String> attr : attributes.entrySet()) {
            td.setAttribute(attr.getKey(), attr.getValue());
         }

         // Is this the last row with content? If so, extend it to cover all remaining rows
         boolean lastRow = elements.size() == (row + 1);
         if (lastRow) {
            td.setRowSpan(remainingRows);
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

      public int getColSpan() {
         return (childColumns == null) ? 1 : childColumns.size();
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
   }

   public static final String BR = "<br/>";
   List<FodderColumn> cols = new ArrayList<>();
   public CannonFodder(Character chr) {

      cols.add(new FodderColumn("Points", chr.getPointTotal()).setSideways().setClass("points"));
      cols.add(new FodderColumn("Race" + BR + "Gender",
                                chr.getRace().getName() + BR + chr.getRace().getGender().name).setClass("race_gender"));
      for (Attribute attr : Attribute.values()) {
         String value = String.valueOf(chr.getAttributeLevel(attr));
         if (attr == Attribute.Strength) value += BR + chr.getAdjustedStrength();
         if (attr == Attribute.Health) value += BR + chr.getBuildBase();

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
      for (Profession prof : chr.getProfessionsList()) {
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
      List<Weapon> thingsHeld = new ArrayList<>();
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
               thingsHeld.add((Weapon) heldThing);
            }
         }
      }
      Weapon primaryWeapon = null;
      Weapon secondaryWeapon = null;
      if (!thingsHeld.isEmpty()) {
         primaryWeapon = thingsHeld.get(0);
      }
      for (Thing thing : chr.getEquipment()) {
         if (thing instanceof  Weapon) {
            secondaryWeapon = (Weapon) thing;
            break;
         }
      }

      if (!value.isEmpty()) {
         value += BR;
      }
      value += thingsHeld.stream().map(w->w.getName()).collect(Collectors.joining(", "));
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

      for (Weapon weapon : Arrays.asList(new Weapon[]{primaryWeapon, secondaryWeapon})) {
         FodderColumn weaponColumnA = new FodderColumn("Style name", (weapon != null) ? weapon.getName() : "");
         FodderColumn weaponColumnB = new FodderColumn("actions", (weapon != null) ? "spd" : "");
         FodderColumn weaponColumnC = new FodderColumn("damage", (weapon != null) ? "damage" : "");
         if (weapon != null) {
            for (WeaponStyleAttack style : weapon.getAttackStyles()) {
               weaponColumnA.addRowValue(style.getName());
               weaponColumnB.addRowValue(style.getSpeed(chr.getAttributeLevel(Attribute.Strength)));
               weaponColumnC.addRowValue(style.getDamageString(chr.getAdjustedStrength()));
            }
         }
         String name = (weapon == primaryWeapon) ? "Primary Weapon" : "Secondary weapon";
         children = new ArrayList<>();
         children.add(weaponColumnA);
         children.add(weaponColumnB);
         children.add(weaponColumnC);
         cols.add(new FodderColumn(name, children).setClass("weapon"));
      }
   }

   private int getRowCount() {
      return cols.stream().map(FodderColumn::getRowCount).max(Integer::compareTo).get();
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
