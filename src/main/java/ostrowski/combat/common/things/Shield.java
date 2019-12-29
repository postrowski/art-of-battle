package ostrowski.combat.common.things;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DrawnObject;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.Race.Gender;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;

public class Shield extends Thing {

   private byte _damageDone;
   public Shield() {}
   private Shield(String name, int damage, int pd, double weight, int cost, Race racialBase) {
      super(name, racialBase, cost, weight, (byte) pd);
      _damageDone = (byte) damage;
   }

   @Override
   public Shield clone() {
      return new Shield(_name, _damageDone, _passiveDefense, _weight, _cost, getRacialBase());
   }

   static private Shield _noShield = null;
   static public Shield getShield(String name, Race racialBase) {
      if (name != null) {
         for (Shield element : _shieldList) {
            if (name.equalsIgnoreCase(element._name)) {
               Shield shield = element.clone();
               shield.setRacialBase(racialBase);
               return shield;
            }
            if (_noShield == null) {
               if (element._weight == 0) {
                  _noShield = element;
               }
            }
         }
         if (name.indexOf(" Shield") == -1) {
            return getShield(name + " Shield", racialBase);
         }
         if (name.startsWith("Magic ")) {
            Shield nonMagicShield = getShield(name.substring("Magic ".length()), racialBase);
            Shield magicShield = nonMagicShield.clone();
            magicShield._name = name;
            magicShield._weight = 0;
            magicShield._cost   = 1;// don't set to zero, because that causes isReal() to return false.
            return magicShield;
         }
      }
      Shield shield = _noShield.clone();
      shield.setRacialBase(racialBase);
      return shield;
   }
   @Override
   public String getActiveDefenseName() {
      if (isReal()) {
         return "block";
      }
      return null;
   }
   @Override
   public ArrayList<SkillType> getDefenseSkillTypes() {
      ArrayList<SkillType> results = new ArrayList<>();
      results.add(SkillType.Shield);
      return results;
   }
   @Override
   public byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands, DamageType damType,
                                    boolean isGrappleAttack, short distance) {
      return Rules.getBlockLevel(wielder.getSkillLevel(getDefenseSkillTypes().get(0), useHand, false/*sizeAdjust*/,
                                                       true/*adjustForEncumbrance*/, true/*adjustForHolds*/));
   }
   @Override
   public void copyData(Thing source) {
      super.copyData(source);
   }

   public static String NAME_None      = "No Shield";
   public static String NAME_Buckler   = "Buckler";
   public static String NAME_Small     = "Small Shield";
   public static String NAME_Medium    = "Medium Shield";
   public static String NAME_Large     = "Large Shield";
   public static String NAME_Tower     = "Tower Shield";
   public static Shield[] _shieldList = new Shield[] {     //  dam  pd lbs   $
                                      new Shield(NAME_None,     0,  0,  0,   0, null),
                                      new Shield(NAME_Buckler,  8,  0,  2,  30, null),
                                      new Shield(NAME_Small,   12,  1,  4,  50, null),
                                      new Shield(NAME_Medium,  14,  2,  8,  70, null),
                                      new Shield(NAME_Large,   16,  3, 15,  90, null),
                                      new Shield(NAME_Tower,   18,  4, 25, 120, null),
   };
   static public List<String> getShieldNames() {
      ArrayList<String> list = new ArrayList<>();
      for (Shield element : _shieldList) {
         list.add(element._name);
      }
      return list;
   }
   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append("<H4>Shield data:</H4>");
      Table table = new Table();
      TableRow tr = new TableRow(-1);
      tr.addHeader("Shield<br/>Name");
      tr.addHeader("Passive<br/>Defense");
      tr.addHeader("Barrier<br/>Value");
      tr.addHeader("Cost");
      tr.addHeader("Weight");
      table.addRow(tr);
      List<Shield> humanShield = getShieldListForRace(Race.getRace(Race.NAME_Human, Gender.MALE));
      int row = 0;
      for (Shield shield : humanShield) {
         tr = new TableRow(row++);
         tr.addHeader(shield.getName());
         tr.addTD(shield.getPassiveDefense());
         tr.addTD(shield.getDamage());
         tr.addTD(shield.getCost());
         tr.addTD(shield.getWeight());
         table.addRow(tr);
      }
      sb.append(table);
      return sb.toString();
   }
   @Override
   public boolean canDefendAgainstRangedWeapons() { return true;}

   @Override
   public byte getHandUseagePenalties(LimbType limbType, Character weilder, SkillType skillType) {
      return 0;
   }
   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      if (tensePast) {
         return "blocks";
      }
      return "block";
   }
   public byte getDamage() {
      return _damageDone;
   }
   @Override
   public DrawnObject drawThing(int size, RGB foreground, RGB background)
   {
      if (!isReal()) {
         return null;
      }
      double width = 1.0;
           if (_name.equals(NAME_Buckler)) {
            width = 0.6;
         }
         else if (_name.equals(NAME_Small)) {
         width = 0.75;
      }
      else if (_name.equals(NAME_Medium)) {
         width = 1.0;
      }
      else if (_name.equals(NAME_Large)) {
         width = 1.2;
      }
      else if (_name.equals(NAME_Tower)) {
         width = 1.5;
      }

      DrawnObject obj = new DrawnObject(foreground, background);
      obj.addPoint(((size * width * -20)/32), ((size * 2)/32));
      obj.addPoint(((size * width * -20)/32), ((size * 0)/32));
      obj.addPoint(((size * width * -16)/32), ((size * -4)/32));
      obj.addPoint(((size * width *  -5)/32), ((size * -8)/32));
      obj.addPoint(((size * width *   6)/32), ((size * -8)/32));
      obj.addPoint(((size * width *  10)/32), ((size * -7)/32));
      obj.addPoint(((size * width *  10)/32), ((size * -5)/32));
      return obj;
   }

   public static ArrayList<Shield> getShieldListForRace(Race race) {
      ArrayList<Shield> list = new ArrayList<>();
      for (Shield shield : _shieldList) {
         Shield copy = shield.clone();
         copy.setRacialBase(race);
         list.add(copy);
      }
      return list;
   }

}
