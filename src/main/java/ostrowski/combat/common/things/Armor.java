package ostrowski.combat.common.things;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;

public class Armor extends Thing {

   public HashMap<DamageType, Byte> _barrier = new HashMap<>();

   public Armor() {}
   Armor(String name, int pd, int blunt, int cut, int imp, int fire, int electric, double weight, int cost, Race racialBase) {
      super(name, racialBase, cost, weight, (byte)pd);
      _barrier.put(DamageType.NONE,     (byte) 0);
      _barrier.put(DamageType.BLUNT,    (byte) blunt);
      _barrier.put(DamageType.CUT,      (byte) cut);
      _barrier.put(DamageType.IMP,      (byte) imp);
      _barrier.put(DamageType.FIRE,     (byte) fire);
      _barrier.put(DamageType.ELECTRIC, (byte) electric);
   }
   @Override
   public Armor clone() {
      return new Armor(_name, _passiveDefense,
                       _barrier.get(DamageType.BLUNT), _barrier.get(DamageType.CUT), _barrier.get(DamageType.IMP),
                       _barrier.get(DamageType.FIRE), _barrier.get(DamageType.ELECTRIC), _weight, _cost, getRacialBase());
   }
   @Override
   public String toString() {
       return super.toString() + ", barriers (blunt/cut/imp/fire/electric):" + _barrier.get(DamageType.BLUNT) + "/" + _barrier.get(DamageType.CUT) + "/" +  _barrier.get(DamageType.IMP) + "/" + _barrier.get(DamageType.FIRE) + "/" +  _barrier.get(DamageType.ELECTRIC);
   }

   private static Armor NO_ARMOR = null;
   static public Armor getArmor(String name, Race racialBase) {
      if (name != null) {
         for (Armor element : _armorList) {
            if (name.equalsIgnoreCase(element._name)) {
               Armor armor = element.clone();
               armor.setRacialBase(racialBase);
               return armor;
            }
            if (NO_ARMOR == null) {
               if (element._weight == 0) {
                  NO_ARMOR = element;
               }
            }
         }
         // check natural armor list:
         for (Armor element : _naturalArmorList) {
            if (name.equalsIgnoreCase(element._name)) {
               Armor armor = element.clone();
               armor.setRacialBase(racialBase);
               return armor;
            }
         }
      }
      Armor armor = NO_ARMOR.clone();
      armor.setRacialBase(racialBase);
      return armor;
   }
   @Override
   public String getActiveDefenseName() { return null; }
   @Override
   public ArrayList<SkillType> getDefenseSkillTypes() { return null;}
   @Override
   public byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands, DamageType damType, boolean isGrappleAttack) { return 0;}

   public byte getBarrier(DamageType damType) { return _barrier.get(damType); }

   @Override
   public void copyData(Thing source) {
      super.copyData(source);
      if (source instanceof Armor) {
         Armor armor = (Armor) source;
         _barrier.clear();
         for(DamageType damageType : armor._barrier.keySet()) {
            _barrier.put(damageType, armor._barrier.get(damageType));
         }
      }
   }

   public static String NAME_NoArmor      = "No Armor";
   public static String NAME_Cloth        = "Cloth";
   public static String NAME_HeavyCloth   = "Cloth Armor";
   public static String NAME_Leather      = "Leather";
   public static String NAME_HeavyLeather = "Heavy Leather";
   public static String NAME_ChainMail    = "Chain Mail";
   public static String NAME_HeavyChain   = "Heavy Chain";
   public static String NAME_ElvenChain   = "Elven Chain";
   public static String NAME_ScaleMail    = "Scale Mail";
   public static String NAME_BandedMail   = "Banded Mail";
   public static String NAME_Samurai      = "Samurai";
   public static String NAME_LightPlate   = "Light Plate";
   public static String NAME_PlateMail    = "Plate Mail";
   public static String NAME_HeavyPlate   = "Heavy Plate";
   public static String NAME_DwarvenScale = "Dwarven Scale";
   public static String NAME_Mithril      = "Dwarven Mithril";

   public static Armor[] _armorList = new Armor[] {// pd blt cut imp fire electric, lbs,  cost
                      new Armor(NAME_NoArmor,           0,  0,  0,  0,   0,    0,      0,     0, null),
                      new Armor(NAME_Cloth,             0,  1,  1,  1,   1,    0,      5,    50, null),
                      new Armor(NAME_HeavyCloth,        0,  2,  3,  4,   2,    2,     15,   200, null),
                      new Armor(NAME_Leather,           1,  3,  4,  6,   4,    2,     20,   300, null),
                      new Armor(NAME_HeavyLeather,      2,  4,  5,  7,   4,    3,     30,   500, null),
                      new Armor(NAME_ChainMail,         3,  4,  7,  7,   4,    6,     45,   550, null),
                      new Armor(NAME_HeavyChain,        3,  5,  8,  8,   4,    7,     60,   800, null),
                      new Armor(NAME_ElvenChain,        3,  6,  9, 15,   4,    7,     40,  4000, null),
                      new Armor(NAME_ScaleMail,         4,  6,  9, 11,   6,    7,     45,  2000, null),
                      new Armor(NAME_BandedMail,        4,  7, 10, 12,   6,    8,     55,  2500, null),
                      new Armor(NAME_Samurai,           3,  6, 12, 10,   6,    7,     60,  3000, null),
                      new Armor(NAME_LightPlate,        5,  8, 10, 13,   7,   10,     60,  3000, null),
                      new Armor(NAME_PlateMail,         5,  8, 11, 14,   7,   10,     70,  3500, null),
                      new Armor(NAME_HeavyPlate,        5,  9, 13, 16,   7,   10,     90,  6000, null),
                      new Armor(NAME_DwarvenScale,      4,  7, 11, 13,   5,    7,     40, 15000, null),
                      new Armor(NAME_Mithril,           5, 10, 15, 18,   6,    8,     50, 25000, null),
   };
   public static Armor[] _naturalArmorList = new Armor[] {// pd blt cut imp fire electric, lbs.    $
                      new Armor(Race.PROPERTIES_FEATHERS,     0,  1,  1,  0,   0,    1,      0,    0, null),
                      new Armor(Race.PROPERTIES_THIN_FUR,     0,  1,  1,  0,   0,    1,      0,    0, null),
                      new Armor(Race.PROPERTIES_FUR,          0,  2,  3,  1,   1,    2,      0,    0, null),
                      new Armor(Race.PROPERTIES_THICK_FUR,    0,  3,  5,  2,   2,    3,      0,    0, null),
                      new Armor(Race.PROPERTIES_THIN_SCALES,  1,  3,  4,  6,   2,    2,      0,    0, null), // similar to Leather
                      new Armor(Race.PROPERTIES_SCALES,       2,  4,  6,  8,   3,    3,      0,    0, null), // similar to Heavy Leather
                      new Armor(Race.PROPERTIES_THICK_SCALES, 3,  5,  8, 10,   4,    4,      0,    0, null), // similar to ScaleMail
   };

   public static List<String> getArmorNames() {
      ArrayList<String> list = new ArrayList<>();
      for (Armor element : _armorList) {
         list.add(element._name);
      }
      return list;
   }
   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
       sb.append("<H4>Armor data:</H4>");
       sb.append("<table>");
       Table table = new Table();
       TableRow tr = new TableRow(-1);
       tr.addHeader(new TableHeader("Armor<br/>Name").setRowSpan(2));
       tr.addHeader(new TableHeader("Passive<br/>Defense").setRowSpan(2));
       tr.addTD(new TableHeader("Build&nbsp;vs.").setColSpan(5));
       tr.addTD(new TableHeader("Cost").setRowSpan(2));
       tr.addTD(new TableHeader("Weight").setRowSpan(2));
       table.addRow(tr);
       tr = new TableRow();
       tr.setClassName("header-row");
       tr.addHeader("Blunt");
       tr.addHeader("Cut");
       tr.addHeader("Imp.");
       tr.addHeader("Fire");
       tr.addHeader("Elect");
       table.addRow(tr);
       List<Armor> humanArmors = getArmorListForRace(Race.getRace(Race.NAME_Human, Gender.MALE));
       int i=0;
       for (Armor armor : humanArmors) {
          tr = new TableRow(i++);
          tr.addHeader(armor.getName());
          tr.addTD(armor.getPassiveDefense());
          tr.addTD(armor.getBarrier(DamageType.BLUNT));
          tr.addTD(armor.getBarrier(DamageType.CUT));
          tr.addTD(armor.getBarrier(DamageType.IMP));
          tr.addTD(armor.getBarrier(DamageType.FIRE));
          tr.addTD(armor.getBarrier(DamageType.ELECTRIC));
          tr.addTD(armor.getCost());
          tr.addTD(armor.getWeight());
          table.addRow(tr);
       }
       sb.append(table.toString());
       return sb.toString();
   }
   @Override
   public String getDefenseName(boolean tensePast, Character defender) { return ""; }
   public static ArrayList<Armor> getArmorListForRace(Race race) {
      ArrayList<Armor> list = new ArrayList<>();
      for (Armor armor : _armorList) {
         Armor copy = armor.clone();
         copy.setRacialBase(race);
         list.add(copy);
      }
      return list;
   }
}
