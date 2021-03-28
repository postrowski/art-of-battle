package ostrowski.combat.common.things;

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

import java.util.ArrayList;
import java.util.List;

public class Shield extends Thing {

   private             byte     damageDone;
   public static final String   NAME_None    = "No Shield";
   public static final String   NAME_Buckler = "Buckler";
   public static final String   NAME_Small   = "Small Shield";
   public static final String   NAME_Medium  = "Medium Shield";
   public static final String   NAME_Large   = "Large Shield";
   public static final String   NAME_Tower   = "Tower Shield";
   static private      Shield   noShield     = null;
   public static final Shield[] shieldList   = new Shield[]{     //  dam  pd lbs   $
                               new Shield(NAME_None,     0,  0,  0,   0, null),
                               new Shield(NAME_Buckler,  8,  0,  2,  30, null),
                               new Shield(NAME_Small,   12,  1,  4,  50, null),
                               new Shield(NAME_Medium,  14,  2,  8,  70, null),
                               new Shield(NAME_Large,   16,  3, 15,  90, null),
                               new Shield(NAME_Tower,   18,  4, 25, 120, null),
                            };

   public Shield() {}
   private Shield(String name, int damage, int pd, double weight, int cost, Race racialBase) {
      super(name, racialBase, cost, weight, (byte) pd);
      damageDone = (byte) damage;
   }

   @Override
   public Shield clone() {
      return new Shield(name, damageDone, passiveDefense, weight, cost, getRacialBase());
   }

   static public Shield getShield(String name, Race racialBase) {
      if (name != null) {
         for (Shield element : shieldList) {
            if (name.equalsIgnoreCase(element.name)) {
               Shield shield = element.clone();
               shield.setRacialBase(racialBase);
               return shield;
            }
            if (noShield == null) {
               if (element.weight == 0) {
                  noShield = element;
               }
            }
         }
         if (!name.contains(" Shield")) {
            return getShield(name + " Shield", racialBase);
         }
         if (name.startsWith("Magic ")) {
            Shield nonMagicShield = getShield(name.substring("Magic ".length()), racialBase);
            Shield magicShield = nonMagicShield.clone();
            magicShield.name = name;
            magicShield.weight = 0;
            magicShield.cost = 1;// don't set to zero, because that causes isReal() to return false.
            return magicShield;
         }
      }
      Shield shield = noShield.clone();
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
   public List<SkillType> getDefenseSkillTypes() {
      List<SkillType> results = new ArrayList<>();
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

   static public List<String> getShieldNames() {
      List<String> list = new ArrayList<>();
      for (Shield element : shieldList) {
         list.add(element.name);
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
      return damageDone;
   }
   @Override
   public DrawnObject drawThing(int size, RGB foreground, RGB background)
   {
      if (!isReal()) {
         return null;
      }
      double width;
      switch (name) {
         case NAME_Buckler: width = 0.6;  break;
         case NAME_Small:   width = 0.75; break;
         case NAME_Medium:  width = 1.0;  break;
         case NAME_Large:   width = 1.2;  break;
         case NAME_Tower:   width = 1.5;  break;
         default:           width = 1.0;
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

   public static List<Shield> getShieldListForRace(Race race) {
      List<Shield> list = new ArrayList<>();
      for (Shield shield : shieldList) {
         Shield copy = shield.clone();
         copy.setRacialBase(race);
         list.add(copy);
      }
      return list;
   }

}
