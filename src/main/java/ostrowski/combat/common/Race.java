package ostrowski.combat.common;

import ostrowski.DebugBreak;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.*;
import ostrowski.combat.common.orientations.*;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.*;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.evil.SpellFear;
import ostrowski.combat.common.spells.priest.evil.SpellMassFear;
import ostrowski.combat.common.spells.priest.good.SpellCharmPerson;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.wounds.Wound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Race implements Cloneable, Enums
{
   public enum Gender {
      MALE("Male"),
      FEMALE("Female");
      Gender(String name) {
         this.name =name;
      }
      public final String name;
      public static Gender getByName(String genderStr) {
         for (Gender gender : values()) {
            if (gender.name.equals(genderStr)) {
               return gender;
            }
         }
         return null;
      }
   }

   private enum BodyType {
      HUMANIOD(        new OrientationSingleHumaniod()),
      QUADRAPED(       new OrientationSingleQuadraped()),
      DOUBLE_QUADRAPED(new OrientationDoubleQuadraped()),
      DOUBLE_CENTAUR(  new OrientationDoubleCentaur()),
      TRIPLE_TRIANGLE( null), //new OrientationTriple(null)),
      QUAD_BLOCK(      null), //new OrientationQuad(null)),
      SERPENTINE_2(    new OrientationReptilian(2)),
      SERPENTINE_3(    new OrientationReptilian(3)),
      SERPENTINE_4(    new OrientationReptilian(4)),
      SERPENTINE_5(    new OrientationReptilian(5)),
      SERPENTINE_6(    new OrientationReptilian(6)),
      SERPENTINE_7(    new OrientationReptilian(7)),
      BIRD(            new OrientationSingleWinged()),
      SPIDER(          new OrientationSingleSpider());

      BodyType(Orientation orientation) {
         this.orientation = orientation;
      }
      public final Orientation orientation;
   }

   public final  String                   name;
   public final  Gender                   gender;
   public final  short                    cost;
   public final  byte                     buildModifier;
   public final  byte                     bonusToBeHit;
   public final  double                   aveWeight;
   public final  float                    widthMod3d;
   public final  float                    lengthMod3d;
   public final  byte                     baseSpeed;
   public final  HashMap<Attribute, Byte> attributeMods    = new HashMap<>();
   private       byte                     headCount        = 1;
   private       byte                     eyeCount         = 2;
   private       byte                     legCount         = 2;
   private       byte                     armCount         = 2;
   private       byte                     wingCount        = 0;
   private       Orientation              baseOrientation  = null;
   private final List<String>             racialProperties = new ArrayList<>();
   private final List<String>             racialAdvsNames  = new ArrayList<>();
   private final List<Spell>              inateSpells      = new ArrayList<>();
   private       Armor                    naturalArmor;
   private       boolean                  isAquatic        = false;
   private       boolean                  isAnimal         = false;
   private       boolean                  canFly           = false;
   final List<LimbType> limbSet = new ArrayList<>();

   public String getName() {
      return name;
   }

   public Gender getGender() {
      return gender;
   }

   public int getCost() {
      return cost;
   }

   public int getAttCost(byte attLevel, Attribute att) {
      return Rules.getAttCost((byte) (attLevel - attributeMods.get(att)));
   }

   public List<Spell> getInateSpells() {
      return inateSpells;
   }


   private Race(String name, Gender gender, int cost, int buildModifier, int bonusToBeHit, double aveWeight, int baseSpeed, BodyType bodyType, double lengthMod3d,
                double widthMod3d, byte[] attributeMods, String[] racialProperties, String[] racialAdvantages, String[] inateSpellNames) {
      this(name, gender, cost, buildModifier, bonusToBeHit, aveWeight, baseSpeed, bodyType.orientation, lengthMod3d, widthMod3d, attributeMods,
           racialProperties, racialAdvantages, inateSpellNames);
   }

   private Race(String name, Gender gender, int cost, int buildModifier, int bonusToBeHit, double aveWeight, int baseSpeed, Orientation orientation,
                double lengthMod3d, double widthMod3d, byte[] attributeMods, String[] racialProperties, String[] racialAdvantages, String[] inateSpellNames) {
      this.name = name;
      this.gender = gender;
      this.cost = (short) cost;
      this.buildModifier = (byte) buildModifier;
      this.bonusToBeHit = (byte) bonusToBeHit;
      this.aveWeight = aveWeight;
      this.widthMod3d = (float) widthMod3d;
      this.lengthMod3d = (float) lengthMod3d;
      this.baseSpeed = (byte) baseSpeed;
      for (Attribute att : Attribute.values()) {
         this.attributeMods.put(att, attributeMods[att.value]);
      }
      if (orientation != null) {
         baseOrientation = orientation.clone();
      }

      for (String element : racialProperties) {
         int loc = element.indexOf(" arms");
         if (loc != -1) {
            armCount = getPropertyCount(element, loc);
         }
         loc = element.indexOf(" legs");
         if (loc != -1) {
            legCount = getPropertyCount(element, loc);
         }
         loc = element.indexOf(" eyes");
         if (loc != -1) {
            eyeCount = getPropertyCount(element, loc);
         }
         loc = element.indexOf(" heads");
         if (loc != -1) {
            headCount = getPropertyCount(element, loc);
         }
         loc = element.indexOf(" wings");
         if (loc != -1) {
            wingCount = getPropertyCount(element, loc);
         }
         this.racialProperties.add(element);
         Armor naturalArmor = Armor.getArmor(element, this);
         if (!naturalArmor.getName().equals(Armor.NAME_NoArmor)) {
            this.naturalArmor = naturalArmor;
         }
      }
      if (wingCount > 0) {
         canFly = true;
      }
      if (this.racialProperties.contains(PROPERTIES_AQUATIC)) {
         isAquatic = true;
      }
      if (this.racialProperties.contains(PROPERTIES_ANIMAL)) {
         isAnimal = true;
      }
      if (this.racialProperties.contains(PROPERTIES_HORNS) && this.racialProperties.contains(PROPERTIES_FANGS)) {
         // you can't have two head attacks, only one weapon gets returned.
         // TODO: make the head the weapon, and horns one style, bite another.
         DebugBreak.debugBreak();
      }
      racialAdvsNames.addAll(Arrays.asList(racialAdvantages));
      for (String element : inateSpellNames) {
         int index = element.indexOf(":");
         if (index != -1) {
            String spellName = element.substring(0, index);
            byte level = Byte.parseByte(element.substring(index + 1));
            Spell spell = MageSpells.getSpell(spellName);
            if (spell == null) {
               spell = PriestSpell.getSpell(spellName);
               if (spell == null) {
                  DebugBreak.debugBreak();
               }
            }
            if (spell != null) {
               spell = spell.clone();
               spell.setPower(level);
               spell.setLevel(level);
               spell.setIsInate(true);
               inateSpells.add(spell);
            }
         }
      }
   }

   public Orientation getBaseOrientation() {
      return baseOrientation.clone();
   }

   private static byte getPropertyCount(String racialProperty, int loc) {
      String count = racialProperty.substring(0, loc);
      if (count.equalsIgnoreCase("no")) {
         return 0;
      }
      return Byte.parseByte(count);
   }

   @Override
   protected Race clone() {
      List<String> spellNames = new ArrayList<>();
      for (Spell spell : inateSpells) {
         //         if (spell instanceof PriestSpell) {
         //            sb.append(((PriestSpell)spell).getDeity()).append(";");
         //         }
         spellNames.add(spell.getName() + ":" + spell.getPower());
      }
      byte[] attributeMods = new byte[Attribute.COUNT];
      for (Attribute att : Attribute.values()) {
         attributeMods[att.value] = this.attributeMods.get(att);
      }
      return new Race(name, gender, cost, buildModifier, bonusToBeHit, aveWeight, baseSpeed, baseOrientation, lengthMod3d, widthMod3d,
                      attributeMods, racialProperties.toArray(new String[0]), racialAdvsNames.toArray(new String[0]), spellNames.toArray(new String[0])) {};
   }

   public byte getBonusToBeHit() {
      return bonusToBeHit;
   }

   public byte getBonusToHit() {
      return (byte) (-bonusToBeHit);
   }

   private double getAveWeight() {
      return aveWeight;
   }

   public byte getBuildModifier() {
      return buildModifier;
   }

   public byte getMovementRate(byte encLevel) {
      return (byte) Math.floor(baseSpeed * (1.0 - (.1 * encLevel)));
   }

   public byte getArmCount() {
      return armCount;
   }

   public byte getTailCount() {
      return (byte) ((hasProperty(PROPERTIES_TAIL)) ? 1 : 0);
   }

   public List<LimbType> getLimbSet() {
      if (limbSet.isEmpty()) {
         for (LimbType limbType : LimbType.values()) {
            if (limbType.isHead() && (limbType.setId <= headCount)) {
               limbSet.add(limbType);
            }
            else if (limbType.isHand() && (limbType.setId <= (armCount / 2))) {
               limbSet.add(limbType);
            }
            else if (limbType.isLeg() && (limbType.setId <= (legCount / 2))) {
               limbSet.add(limbType);
            }
            else if (limbType.isWing() && (limbType.setId <= (wingCount / 2))) {
               limbSet.add(limbType);
            }
            else if (limbType.isTail() && (limbType.setId <= getTailCount())) {
               limbSet.add(limbType);
            }
         }
      }
      return limbSet;
   }

   public byte getAttributeMods(Attribute att) {
      return attributeMods.get(att);
   }

   // racial build adj = log(racial average weight/150) * 13.33
   // racial to-hit adj=(build adj)/4 - rounded toward 0
   // racial average weight = 150 * 10^(racial build/13.33)

   static public final String PROPERTIES_2_WINGS      = "2 wings";
   static public final String PROPERTIES_0_ARMS       = "0 arms";
   static public final String PROPERTIES_4_ARMS       = "4 arms";
   static public final String PROPERTIES_4_LEGS       = "4 legs";
   static public final String PROPERTIES_6_LEGS       = "6 legs";
   static public final String PROPERTIES_ANIMAL       = "Animal";
   static public final String PROPERTIES_HORNS        = "Horns";
   static public final String PROPERTIES_FANGS        = "Fangs";
   static public final String PROPERTIES_STURGEBREAK  = "Beak";
   static public final String PROPERTIES_TUSKS        = "Tusks";
   static public final String PROPERTIES_CLAWS        = "Claws";
   static public final String PROPERTIES_TAIL         = "Tail";
   static public final String PROPERTIES_FEATHERS     = "Feathers";
   static public final String PROPERTIES_THIN_FUR     = "Thin Fur";
   static public final String PROPERTIES_FUR          = "Fur";
   static public final String PROPERTIES_THICK_FUR    = "Thick Fur";
   static public final String PROPERTIES_THIN_SCALES  = "Thin Scales";
   static public final String PROPERTIES_SCALES       = "Scales";
   static public final String PROPERTIES_THICK_SCALES = "Thick Scales";
   static public final String PROPERTIES_REGENERATIVE = "Regenerative";
   static public final String PROPERTIES_NPC          = "NPC";
   static public final String PROPERTIES_NIGHT_VISION = "Night vision";
   static public final String PROPERTIES_INFRAVISION  = "Infravision";
   public static final String PROPERTIES_AQUATIC      = "Aquatic";         // aquatic creature move at half speed on land, but full speed in water

   static public final double HUMAN_AVE_WEIGHT        = 150;
   static public final String NAME_Kobold             = "Kobold";
   static public final String NAME_Fairy              = "Fairy";
   static public final String NAME_Hobbit             = "Hobbit";
   static public final String NAME_Goblin             = "Goblin";
   static public final String NAME_Fawn               = "Fawn";
   static public final String NAME_Ellyon             = "Ellyon";
   static public final String NAME_Human              = "Human";
   static public final String NAME_Gnome              = "Gnome";
   static public final String NAME_HalfOrc            = "Half-Orc";
   static public final String NAME_HalfElf            = "Half-Elf";
   static public final String NAME_Orc                = "Orc";
   static public final String NAME_Dwarf              = "Dwarf";
   static public final String NAME_Elf                = "Elf";
   static public final String NAME_LizardMan          = "LizardMan";
   static public final String NAME_Gargoyle           = "Gargoyle";
   static public final String NAME_InsectMan          = "InsectMan";
   static public final String NAME_Centaur            = "Centaur";
   static public final String NAME_HalfOgre           = "Half-Ogre";
   static public final String NAME_Ogre               = "Ogre";
   static public final String NAME_Troll              = "Troll";
   static public final String NAME_Minotaur           = "Minotaur";
   static public final String NAME_Cyclops            = "Cyclops";
   static public final String NAME_Giant              = "Giant";
   static public final String NAME_Skeleton           = "Skeleton";
   static public final String NAME_Zombie             = "Zombie";
   static public final String NAME_Sturge             = "Sturge";
   static public final String NAME_Eagle              = "Eagle";
   static public final String NAME_Fox                = "Fox";
   static public final String NAME_Wolf               = "Wolf";
   static public final String NAME_Warg               = "Warg";
   static public final String NAME_HellHound          = "Hell Hound";
   static public final String NAME_Puma               = "Puma";
   static public final String NAME_Tiger              = "Tiger";
   static public final String NAME_Lion               = "Lion";
   static public final String NAME_Horse_Riding       = "Horse, Riding";
   static public final String NAME_Horse_War          = "Horse, War";
   static public final String NAME_Horse_Draft        = "Horse, Draft";
   static public final String NAME_GiantSpider        = "Giant-Spider";
   static public final String NAME_Crocodile          = "Crocodile";
   static public final String NAME_Crocodile_Lg       = "Crocodile, Large";
   static public final String NAME_Bear               = "Bear";
   static public final String NAME_CaveBear           = "Cave-Bear";
   static public final String NAME_Baby_Dragon        = "Dragon, Baby";
   static public final String NAME_Small_Dragon       = "Dragon, Small";
   static public final String NAME_Dragon             = "Dragon";
   static public final String NAME_Large_Dragon       = "Dragon, Large";
   static public final String NAME_Huge_Dragon        = "Dragon, Huge";
   static public final String NAME_Ancient_Dragon     = "Dragon, Ancient";
   static public final String NAME_Legendary_Dragon   = "Dragon, Legendary";
   static public final String NAME_Minor_Demon        = "Demon, Minor";
   static public final String NAME_Demon              = "Demon";
   static public final String NAME_Major_Demon        = "Demon, Major";

   static public final String NAME_Elemental_Fire     = "Elemental, Fire";
   static public final String NAME_Elemental_Water    = "Elemental, Water";
   static public final String NAME_Elemental_Earth    = "Elemental, Earth";
   static public final String NAME_Elemental_Air      = "Elemental, Air";

   static public final String NAME_Golem_Iron         = "Golem, Iron";
   static public final String NAME_Golem_Straw        = "Golem, Straw";
   static public final String NAME_Golem_Clay         = "Golem, Clay";
   static public final String NAME_Golem_Rope         = "Golem, Rope";

   // Racial Build Adj.  = log (racial average weight/150) * 40/3
   // racial weight ave. = 150 * 10^(racial build adj. * 3 / 40);  (doubles every 4 levels)
   // hit adjustment = (buildAdjustment / 4)
   // Racial Build Adj.  = log (racial average weight/150) * 40/3
   // racial weight ave. = 150 * 10^(racial build adj. * 3 / 40);  (doubles every 4 levels)
   // hit adjustment = (buildAdjustment / 4)
   static final Race[] raceList = new Race[] {
                  // name               gender,       cost, bld hit  ave# move   bodyConfig               length width           STR  HT TOU  IQ NIM DEX SOC
       new Race(NAME_Kobold,          Gender.MALE,     -70,  -7, -2,   45,   4,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  2,  0, -3,  0,  0, -3}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_5, Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Kobold,          Gender.FEMALE,   -80,  -7, -2,   45,   4,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2,  2,  0, -3,  0,  0, -2}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_5, Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Hobbit,          Gender.MALE,     -50,  -3, -1,   90,   4,          BodyType.HUMANIOD,.85, 1.1, new byte[] {-1,  1, -1,  0, -2,  0,  0}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_Hobbit,          Gender.FEMALE,   -50,  -3, -1,   90,   4,          BodyType.HUMANIOD,.85, 1.1, new byte[] {-3,  1, -1,  0, -2,  0,  1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_Goblin,          Gender.MALE,     -35,  -3, -1,   90,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0, -1, -2,  1,  1, -2}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.NIGHT_VISION, Advantage.GREEDY}, new String [] {}),
       new Race(NAME_Goblin,          Gender.FEMALE,   -40,  -3, -1,   90,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2,  0, -1, -2,  1,  1, -1}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.NIGHT_VISION, Advantage.GREEDY}, new String [] {}),
       new Race(NAME_Fairy,           Gender.MALE,       5, -29, -7,  1.0,   7,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0, -2,  0,  3,  2,  2}, new String[] {PROPERTIES_2_WINGS}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.WINGED_FLIGHT, Advantage.MAGICAL_APTITUDE+":+2"}, new String [] {}),
       new Race(NAME_Fairy,           Gender.FEMALE,     5, -29, -7,  1.0,   7,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2,  0, -2,  0,  3,  2,  3}, new String[] {PROPERTIES_2_WINGS}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.WINGED_FLIGHT, Advantage.MAGICAL_APTITUDE+":+2"}, new String [] {}),
       new Race(NAME_Fawn,            Gender.MALE,     -10,  -3, -1,   90,   7,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-1,  0, -1,  0,  2,  1,  0}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_4}, new String [] {}),
       new Race(NAME_Fawn,            Gender.FEMALE,   -10,  -3, -1,   90,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-3,  0, -1,  0,  2,  1,  1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_4}, new String [] {}),
       new Race(NAME_Ellyon,          Gender.MALE,     -10,  -7, -2,   45,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0, -1, -2, -1, -1,  1,  1}, new String[] {PROPERTIES_2_WINGS}, new String[] {Advantage.WEALTH_MULTIPLIER_2, Advantage.WINGED_FLIGHT, Advantage.VISION+":Acute vision"}, new String [] {}),
       new Race(NAME_Ellyon,          Gender.FEMALE,   -15,  -7, -2,   45,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2, -1, -2, -1, -1,  1,  2}, new String[] {PROPERTIES_2_WINGS}, new String[] {Advantage.WEALTH_MULTIPLIER_2, Advantage.WINGED_FLIGHT, Advantage.VISION+":Acute vision"}, new String [] {}),
       new Race(NAME_Human,           Gender.MALE,       0,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0,  0,  0,  0,  0,  0}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_Human,           Gender.FEMALE,    -5,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2,  0,  0,  0,  0,  0,  1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_Gnome,           Gender.MALE,      30,  -1,  0,  125,   4,          BodyType.HUMANIOD,.85, 1.1, new byte[] { 0,  1,  2,  0, -1,  0,  0}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.NIGHT_VISION, Advantage.ABSOLUTE_DIRECTION}, new String [] {}),
       new Race(NAME_Gnome,           Gender.FEMALE,    25,  -1,  0,  125,   4,          BodyType.HUMANIOD,.85, 1.1, new byte[] {-2,  1,  2,  0, -1,  0,  1}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.NIGHT_VISION, Advantage.ABSOLUTE_DIRECTION}, new String [] {}),
       new Race(NAME_HalfOrc,         Gender.MALE,      10,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  1,  1, -2,  0,  0, -1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_HalfOrc,         Gender.FEMALE,     0,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-1,  1,  1, -2,  0,  0,  0}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_HalfElf,         Gender.MALE,      30,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0, -1,  1,  1,  1,  1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_HalfElf,         Gender.FEMALE,    25,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] {-2,  0, -1,  1,  1,  1,  2}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_x1}, new String [] {}),
       new Race(NAME_Orc,             Gender.MALE,      20,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 2,  2,  2, -3, -1,  0, -2}, new String[] {PROPERTIES_INFRAVISION}, new String[] {Advantage.WEALTH_MULTIPLIER_2, Advantage.BAD_TEMPER, Advantage.INFRA_VISION}, new String [] {}),
       new Race(NAME_Orc,             Gender.FEMALE,     5,   0,  0,  150,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  2,  2, -3, -1,  0, -1}, new String[] {PROPERTIES_INFRAVISION}, new String[] {Advantage.WEALTH_MULTIPLIER_2, Advantage.BAD_TEMPER, Advantage.INFRA_VISION}, new String [] {}),
       new Race(NAME_Dwarf,           Gender.MALE,      60,   0,  0,  150,   4,          BodyType.HUMANIOD,.80,1.15, new byte[] { 2,  2,  2,  0,  0, -1,  0}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.ABSOLUTE_DIRECTION, Advantage.NIGHT_VISION, Advantage.GREEDY}, new String [] {}),
       new Race(NAME_Dwarf,           Gender.FEMALE,    50,   0,  0,  150,   4,          BodyType.HUMANIOD,.80,1.15, new byte[] { 0,  2,  2,  0,  0, -1,  1}, new String[] {PROPERTIES_NIGHT_VISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.ABSOLUTE_DIRECTION, Advantage.NIGHT_VISION, Advantage.GREEDY}, new String [] {}),
       new Race(NAME_Elf,             Gender.MALE,     100,  -1,  0,  125,   5,          BodyType.HUMANIOD,1.05,.97, new byte[] {-1,  0, -1,  2,  2,  2,  2}, new String[] {PROPERTIES_INFRAVISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.MAGICAL_APTITUDE+":+1", Advantage.INFRA_VISION}, new String [] {}),
       new Race(NAME_Elf,             Gender.FEMALE,   100,  -1,  0,  125,   5,          BodyType.HUMANIOD,1.05,.97, new byte[] {-3,  0, -1,  2,  2,  2,  3}, new String[] {PROPERTIES_INFRAVISION}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.MAGICAL_APTITUDE+":+1", Advantage.INFRA_VISION}, new String [] {}),
                  // name               gender,       cost, bld hit  ave# move   bodyConfig               length width           STR  HT TOU  IQ NIM DEX SOC
       new Race(NAME_LizardMan,       Gender.MALE,     120,   2,  0,  210,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  2,  2, -2, -2,  0, -1}, new String[] {PROPERTIES_INFRAVISION, PROPERTIES_CLAWS, PROPERTIES_FANGS, PROPERTIES_THIN_SCALES, PROPERTIES_TAIL}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.INFRA_VISION, Advantage.INTOLERANT}, new String [] {}),
       new Race(NAME_LizardMan,       Gender.FEMALE,   120,   2,  0,  210,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  2,  2, -2, -2,  0,  0}, new String[] {PROPERTIES_INFRAVISION, PROPERTIES_CLAWS, PROPERTIES_FANGS, PROPERTIES_THIN_SCALES, PROPERTIES_TAIL}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.INFRA_VISION, Advantage.INTOLERANT}, new String [] {}),
       new Race(NAME_Gargoyle,        Gender.MALE,     120,  -1,  0,  125,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  2,  2, -3, -1, -1, -2}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_2_WINGS, PROPERTIES_CLAWS, PROPERTIES_FANGS, PROPERTIES_THICK_SCALES}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.NIGHT_VISION, Advantage.WINGED_FLIGHT, Advantage.MAGIC_RESISTANCE+":2"}, new String [] {}),
       new Race(NAME_Gargoyle,        Gender.FEMALE,   115,  -1,  0,  125,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  2,  2, -3, -1, -1, -1}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_2_WINGS, PROPERTIES_CLAWS, PROPERTIES_FANGS, PROPERTIES_THICK_SCALES}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.NIGHT_VISION, Advantage.WINGED_FLIGHT, Advantage.MAGIC_RESISTANCE+":2"}, new String [] {}),
       new Race(NAME_InsectMan,       Gender.MALE,     150,   1,  0,  180,   4,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0,  2, -4,  0,  0,  0}, new String[] {PROPERTIES_4_ARMS, PROPERTIES_SCALES}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.ARMS_4}, new String [] {}),
       new Race(NAME_Centaur,         Gender.MALE,     250,   8,  2,  600,   9,    BodyType.DOUBLE_CENTAUR,1.0, 1.0, new byte[] {-2, -1,  0,  0,  0,  0,  1}, new String[] {PROPERTIES_4_LEGS}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.LEGS_4}, new String [] {}),
       new Race(NAME_Centaur,         Gender.FEMALE,   250,   8,  2,  600,   9,    BodyType.DOUBLE_CENTAUR,1.0, 1.0, new byte[] {-4, -1,  0,  0,  0,  0,  2}, new String[] {PROPERTIES_4_LEGS}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.LEGS_4}, new String [] {}),
       new Race(NAME_HalfOgre,        Gender.MALE,      60,   5,  1,  350,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 2,  0,  0, -2, -2,  0, -2}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_2}, new String [] {}),
       new Race(NAME_HalfOgre,        Gender.FEMALE,    45,   5,  1,  350,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0,  0, -2, -2,  0, -1}, new String[] {""}, new String[] {Advantage.WEALTH_MULTIPLIER_2}, new String [] {}),
       new Race(NAME_Ogre,            Gender.MALE,     230,  10,  2,  850,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 3,  0,  0, -3, -3, -1, -3}, new String[] {PROPERTIES_NPC, ""}, new String[] {Advantage.WEALTH_MULTIPLIER_3}, new String [] {}),
       new Race(NAME_Ogre,            Gender.FEMALE,   210,  10,  2,  850,   5,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  0,  0, -3, -3, -1, -2}, new String[] {PROPERTIES_NPC, ""}, new String[] {Advantage.WEALTH_MULTIPLIER_3}, new String [] {}),
       new Race(NAME_Minotaur,        Gender.MALE,     160,   6,  1,  425,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 2,  1,  2, -3,  0,  0, -3}, new String[] {PROPERTIES_NPC, PROPERTIES_HORNS, PROPERTIES_THIN_FUR}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.ABSOLUTE_DIRECTION, Advantage.BERSERKER, Advantage.VISION+":Near sighted", Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Minotaur,        Gender.FEMALE,   150,   6,  1,  425,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  1,  2, -3,  0,  0, -2}, new String[] {PROPERTIES_NPC, PROPERTIES_HORNS, PROPERTIES_THIN_FUR}, new String[] {Advantage.WEALTH_MULTIPLIER_3, Advantage.ABSOLUTE_DIRECTION, Advantage.BERSERKER, Advantage.VISION+":Near sighted", Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Troll,           Gender.MALE,     400,  10,  2,  850,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 3,  5,  2, -2, -2,  0, -4}, new String[] {PROPERTIES_NPC, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_5, Advantage.REGENERATION, Advantage.BAD_TEMPER}, new String [] {}),
       new Race(NAME_Troll,           Gender.FEMALE,   375,  10,  2,  850,   6,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  5,  2, -2, -2,  0, -3}, new String[] {PROPERTIES_NPC, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_5, Advantage.REGENERATION, Advantage.BAD_TEMPER}, new String [] {}),
       new Race(NAME_Cyclops,         Gender.MALE,     500,  16,  4, 2400,   6,          BodyType.HUMANIOD,1.2, .80, new byte[] { 2,  2,  0, -1, -1,  0, -2}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.BAD_TEMPER, Advantage.VISION+":Poor sight"}, new String [] {}),
       new Race(NAME_Cyclops,         Gender.FEMALE,   475,  16,  4, 2400,   6,          BodyType.HUMANIOD,1.2, .80, new byte[] { 0,  2,  0, -1, -1,  0, -1}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.BAD_TEMPER, Advantage.VISION+":Poor sight"}, new String [] {}),
       new Race(NAME_Giant,           Gender.MALE,     625,  18,  4, 3500,   6,          BodyType.HUMANIOD,1.25,.75, new byte[] { 0,  2,  1,  0, -1,  0,  0}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Giant,           Gender.FEMALE,   625,  18,  4, 3500,   6,          BodyType.HUMANIOD,1.25,.75, new byte[] {-2,  2,  1,  0, -1,  0,  1}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Skeleton,        Gender.MALE,      90,   0,  0,   37,   6,          BodyType.HUMANIOD,1.15,.80, new byte[] {-2, -5,  0, -5,  3,  2, -5}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.UNDEAD, Advantage.NO_PAIN, Advantage.INFRA_VISION, Advantage.MUTE}, new String [] {}),
       new Race(NAME_Zombie,          Gender.MALE,      65,   0,  0,  150,   3,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 3,  1,  0, -5, -2, -1, -5}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_2,  Advantage.UNDEAD, Advantage.NO_PAIN, Advantage.INFRA_VISION, Advantage.MUTE}, new String [] {}),
       new Race(NAME_Zombie,          Gender.FEMALE,    65,   0,  0,  150,   3,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 3,  1,  0, -5, -2, -1, -5}, new String[] {PROPERTIES_NPC}, new String[] {Advantage.WEALTH_MULTIPLIER_2,  Advantage.UNDEAD, Advantage.NO_PAIN, Advantage.INFRA_VISION, Advantage.MUTE}, new String [] {}),
       new Race(NAME_Sturge,          Gender.MALE,    -100, -21, -5,  4.0,   7,              BodyType.BIRD,1.0, 1.0, new byte[] { 0,  0,  0, -8,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_0_ARMS, PROPERTIES_NIGHT_VISION, PROPERTIES_2_WINGS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_STURGEBREAK}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.WINGED_FLIGHT, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Sturge,          Gender.FEMALE,  -100, -21, -5,  4.0,   7,              BodyType.BIRD,1.0, 1.0, new byte[] { 0,  0,  0, -8,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_0_ARMS, PROPERTIES_NIGHT_VISION, PROPERTIES_2_WINGS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_STURGEBREAK}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.WINGED_FLIGHT, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Eagle,           Gender.MALE,     -60, -13, -3, 16.0,   8,              BodyType.BIRD,1.0, 1.0, new byte[] { 0,  0,  0, -7,  1,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_0_ARMS, PROPERTIES_2_WINGS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FEATHERS, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.VISION+":Acute vision", Advantage.WINGED_FLIGHT, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Eagle,           Gender.FEMALE,   -60, -13, -3, 16.0,   8,              BodyType.BIRD,1.0, 1.0, new byte[] { 0,  0,  0, -7,  1,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_0_ARMS, PROPERTIES_2_WINGS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FEATHERS, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.VISION+":Acute vision", Advantage.WINGED_FLIGHT, Advantage.HANDS_0}, new String [] {}),
                  // name               gender,       cost, bld hit  ave# move   bodyConfig               length width           STR  HT TOU  IQ NIM DEX SOC
       new Race(NAME_Fox,             Gender.MALE,     -45,  -9, -3,   31,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  0, -5,  2,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.PERIPHERAL_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Fox,             Gender.FEMALE,   -45,  -9, -3,   31,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  0, -5,  2,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.PERIPHERAL_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Wolf,            Gender.MALE,      50,  -1, -1,  125,   8,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  1,  0, -4,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Wolf,            Gender.FEMALE,    50,  -1, -1,  125,   8,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  1,  0, -4,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Warg,            Gender.MALE,      70,   0, -1,  150,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 2,  2,  1, -5,  1,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_TUSKS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_Warg,            Gender.FEMALE,    70,   0, -1,  150,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 2,  2,  1, -5,  1,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_TUSKS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.HANDS_0}, new String [] {}),
       new Race(NAME_HellHound,       Gender.MALE,     220,  -2, -1,  106,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  1, -5,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION, Advantage.HANDS_0, Advantage.UNDEAD, Advantage.NO_PAIN}, new String [] {SpellFlameJet.NAME + ":2"}),
       new Race(NAME_HellHound,       Gender.FEMALE,   220,  -2, -1,  106,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  1, -5,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_FUR, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION, Advantage.HANDS_0, Advantage.UNDEAD, Advantage.NO_PAIN}, new String [] {SpellFlameJet.NAME + ":2"}),
       new Race(NAME_Tiger,           Gender.MALE,     300,   8,  1,  600,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  1, -1, -5,  3,  4,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Tiger,           Gender.FEMALE,   300,   8,  1,  600,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  1, -1, -5,  3,  4,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Lion,            Gender.MALE,     250,   6,  1,  425,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 2,  1, +1, -5,  1,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Lion,            Gender.FEMALE,   225,   4,  0,  300,   8,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  0, -4,  3,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Puma,            Gender.MALE,      75,  -1, -1,  125,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  0, -5,  3,  4,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_Puma,            Gender.FEMALE,    75,  -1, -1,  125,   7,         BodyType.QUADRAPED,1.0, 1.0, new byte[] { 0,  0,  0, -5,  3,  4,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL, PROPERTIES_THIN_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {}),
       new Race(NAME_GiantSpider,     Gender.MALE,     325,   6,  1,  425,   6,            BodyType.SPIDER,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_4_ARMS, PROPERTIES_THIN_FUR, PROPERTIES_ANIMAL, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {SpellSpiderWeb.NAME + ":4"}),
       new Race(NAME_GiantSpider,     Gender.FEMALE,   325,   6,  1,  425,   6,            BodyType.SPIDER,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_4_ARMS, PROPERTIES_THIN_FUR, PROPERTIES_ANIMAL, PROPERTIES_FANGS}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION}, new String [] {SpellSpiderWeb.NAME + ":4"}),
       new Race(NAME_Crocodile,       Gender.MALE,     340,  10,  1,  850,   6,      BodyType.SERPENTINE_2,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_AQUATIC, PROPERTIES_SCALES, PROPERTIES_ANIMAL, PROPERTIES_FANGS, PROPERTIES_TAIL}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Crocodile,       Gender.FEMALE,   340,  10,  1,  850,   6,      BodyType.SERPENTINE_2,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_AQUATIC, PROPERTIES_SCALES, PROPERTIES_ANIMAL, PROPERTIES_FANGS, PROPERTIES_TAIL}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_Riding,    Gender.MALE,     250,  10,  2,  850,   15,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  2,  0, -6,  2,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_Riding,    Gender.FEMALE,   250,  10,  2,  850,   15,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  2,  0, -6,  2,  1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_War,       Gender.MALE,     350,  12,  2, 1200,   13,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 2,  2,  3, -5,  0,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_War,       Gender.FEMALE,   350,  12,  2, 1200,   13,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 2,  2,  3, -5,  0,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_Draft,     Gender.MALE,     400,  14,  3, 1700,   10,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 4,  2,  1, -6, -1, -1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
//       new Race(NAME_Horse_Draft,     Gender.FEMALE,   400,  14,  3, 1700,   10,     BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 4,  2,  1, -6, -1, -1,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_ANIMAL}, new String[] {"Wealth:$0", Advantage.VISION+":Poor sight", Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Crocodile_Lg,    Gender.MALE,     550,  16,  2, 2400,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_AQUATIC, PROPERTIES_SCALES, PROPERTIES_ANIMAL, PROPERTIES_FANGS, PROPERTIES_TAIL}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Crocodile_Lg,    Gender.FEMALE,   550,  16,  2, 2400,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  2,  4, -7, -2,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_AQUATIC, PROPERTIES_SCALES, PROPERTIES_ANIMAL, PROPERTIES_FANGS, PROPERTIES_TAIL}, new String[] {"Wealth:$0", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Bear,            Gender.MALE,     325,  12,  3, 1200,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  0,  2, -6,  1,  2, -4}, new String[] {PROPERTIES_NPC, PROPERTIES_ANIMAL, PROPERTIES_THICK_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0"}, new String [] {}),
       new Race(NAME_Bear,            Gender.FEMALE,   325,  12,  3, 1200,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  0,  2, -6,  1,  2, -4}, new String[] {PROPERTIES_NPC, PROPERTIES_ANIMAL, PROPERTIES_THICK_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0"}, new String [] {}),
       new Race(NAME_CaveBear,        Gender.MALE,     465,  16,  4, 2400,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  0,  2, -6,  1,  2, -5}, new String[] {PROPERTIES_NPC, PROPERTIES_ANIMAL, PROPERTIES_THICK_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0",  Advantage.BERSERKER}, new String [] {}),
       new Race(NAME_CaveBear,        Gender.FEMALE,   465,  16,  4, 2400,   7,  BodyType.DOUBLE_QUADRAPED,1.0, 1.0, new byte[] { 1,  0,  2, -6,  1,  2, -5}, new String[] {PROPERTIES_NPC, PROPERTIES_ANIMAL, PROPERTIES_THICK_FUR, PROPERTIES_FANGS, PROPERTIES_CLAWS}, new String[] {"Wealth:$0",  Advantage.BERSERKER}, new String [] {}),
                    // name             gender,       cost, bld hit  ave# move   bodyConfig               length width           STR  HT TOU  IQ NIM DEX SOC
       new Race(NAME_Baby_Dragon,     Gender.MALE,     185,   6,  0,  425,   4,      BodyType.SERPENTINE_2,1.0, 1.0, new byte[] { 0,  0,  0, -1,  0,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THIN_SCALES,  PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0",                                   Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Baby_Dragon,     Gender.FEMALE,   185,   6,  0,  425,   4,      BodyType.SERPENTINE_2,1.0, 1.0, new byte[] { 0,  0,  0, -1,  0,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THIN_SCALES,  PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0",                                   Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {}),
       new Race(NAME_Small_Dragon,    Gender.MALE,     525,  12,  2, 1200,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  0,  1,  0,  1,  1,  1}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_SCALES,       PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+1", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Small_Dragon,    Gender.FEMALE,   525,  12,  2, 1200,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  0,  1,  0,  1,  1,  1}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_SCALES,       PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+1", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Dragon,          Gender.MALE,     850,  18,  3, 3500,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  1,  2,  1,  1,  1,  2}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+2", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Dragon,          Gender.FEMALE,   850,  18,  3, 3500,   6,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  1,  2,  1,  1,  1,  2}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+2", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Large_Dragon,    Gender.MALE,    1200,  24,  5, 9500,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  1,  3,  2,  0,  2,  3}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+3", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Large_Dragon,    Gender.FEMALE,  1200,  24,  5, 9500,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  1,  3,  2,  0,  2,  3}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+3", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Huge_Dragon,     Gender.MALE,    1500,  30,  6,27500,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  0,  3,  3,  0,  2,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+4", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Huge_Dragon,     Gender.FEMALE,  1500,  30,  6,27500,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] { 0,  0,  3,  3,  0,  2,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+4", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Ancient_Dragon,  Gender.MALE,    1900,  36,  8,75000,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] {-1, -1,  2,  4, -1,  2,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+5", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Ancient_Dragon,  Gender.FEMALE,  1900,  36,  8,75000,   7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] {-1, -1,  2,  4, -1,  2,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+5", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Legendary_Dragon,Gender.MALE,    2300,  42,  9,220000,  7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] {-1, -2,  2,  5, -1,  1,  5}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+5", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),
       new Race(NAME_Legendary_Dragon,Gender.FEMALE,  2300,  42,  9,220000,  7,      BodyType.SERPENTINE_3,1.0, 1.0, new byte[] {-1, -2,  2,  5, -1,  1,  5}, new String[] {PROPERTIES_NPC, PROPERTIES_NIGHT_VISION, PROPERTIES_THICK_SCALES, PROPERTIES_ANIMAL, PROPERTIES_4_LEGS, PROPERTIES_0_ARMS, PROPERTIES_FANGS, PROPERTIES_CLAWS, PROPERTIES_TAIL, PROPERTIES_2_WINGS}, new String[] {"Wealth:$0", Advantage.MAGICAL_APTITUDE+":+5", Advantage.NIGHT_VISION, Advantage.PERIPHERAL_VISION}, new String [] {SpellFlameJet.NAME + ":1"}),

       new Race(NAME_Minor_Demon,     Gender.MALE,     450,   6,  1,  425,   7,          BodyType.HUMANIOD,1.05,.95, new byte[] { 1,  0,  2,  0,  2,  2,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_THIN_SCALES,  PROPERTIES_NIGHT_VISION, PROPERTIES_FANGS, PROPERTIES_CLAWS                         }, new String[] {Advantage.WEALTH_MULTIPLIER_2,  Advantage.MAGIC_RESISTANCE+":1", Advantage.DIVINE_POWER+":3", Advantage.DIVINE_AFFINITY_+"Demonic:3",                         Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellFear.NAME + ":2"}),
       new Race(NAME_Minor_Demon,     Gender.FEMALE,   450,   6,  1,  425,   7,          BodyType.HUMANIOD,1.05,.95, new byte[] {-1, -2,  2,  1,  2,  2,  2}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_THIN_SCALES,  PROPERTIES_NIGHT_VISION, PROPERTIES_FANGS, PROPERTIES_CLAWS                         }, new String[] {Advantage.WEALTH_MULTIPLIER_2,  Advantage.MAGIC_RESISTANCE+":1", Advantage.DIVINE_POWER+":3", Advantage.DIVINE_AFFINITY_+"Demonic:3",                         Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellCharmPerson.NAME + ":1"}),
       new Race(NAME_Demon,           Gender.MALE,     800,  10,  2,  850,   7,          BodyType.HUMANIOD,1.10,.90, new byte[] { 2,  0,  3,  2,  1,  3,  2}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_SCALES,       PROPERTIES_NIGHT_VISION, PROPERTIES_HORNS, PROPERTIES_CLAWS, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.MAGIC_RESISTANCE+":3", Advantage.DIVINE_POWER+":4", Advantage.DIVINE_AFFINITY_+"Demonic:6", Advantage.REGENERATION, Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellMassFear.NAME + ":3"}),
       new Race(NAME_Demon,           Gender.FEMALE,   800,  10,  2,  850,   7,          BodyType.HUMANIOD,1.10,.90, new byte[] { 0, -2,  2,  3,  1,  3,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_SCALES,       PROPERTIES_NIGHT_VISION, PROPERTIES_HORNS, PROPERTIES_CLAWS, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_x1, Advantage.MAGIC_RESISTANCE+":3", Advantage.DIVINE_POWER+":4", Advantage.DIVINE_AFFINITY_+"Demonic:6", Advantage.REGENERATION, Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellFear.NAME + ":2", SpellCharmPerson.NAME + ":3"}),
       new Race(NAME_Major_Demon,     Gender.MALE,    1200,  16,  4, 2400,   7,          BodyType.HUMANIOD,1.15,.85, new byte[] { 2,  0,  4,  4,  0,  3,  4}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_THICK_SCALES, PROPERTIES_NIGHT_VISION, PROPERTIES_HORNS, PROPERTIES_CLAWS, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.MAGIC_RESISTANCE+":5", Advantage.DIVINE_POWER+":5", Advantage.DIVINE_AFFINITY_+"Demonic:9", Advantage.REGENERATION, Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellMassFear.NAME + ":5"}),
       new Race(NAME_Major_Demon,     Gender.FEMALE,  1200,  16,  4, 2400,   7,          BodyType.HUMANIOD,1.15,.85, new byte[] { 0, -2,  3,  5,  0,  3,  5}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_TAIL, PROPERTIES_THICK_SCALES, PROPERTIES_NIGHT_VISION, PROPERTIES_HORNS, PROPERTIES_CLAWS, PROPERTIES_REGENERATIVE}, new String[] {Advantage.WEALTH_MULTIPLIER_x3, Advantage.MAGIC_RESISTANCE+":5", Advantage.DIVINE_POWER+":5", Advantage.DIVINE_AFFINITY_+"Demonic:9", Advantage.REGENERATION, Advantage.SADISTIC, Advantage.PERIPHERAL_VISION,  Advantage.WINGED_FLIGHT}, new String [] {SpellMassFear.NAME + ":3", SpellCharmPerson.NAME + ":4"}),

       new Race(NAME_Elemental_Earth, Gender.MALE,     325,  12,  3, 1200,   3,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 2,  0,  0, -4, -2,  0,  0}, new String[] {PROPERTIES_NPC,                     PROPERTIES_ANIMAL},                          new String[] {"Wealth:$0", Advantage.MAGIC_RESISTANCE+":4",                          Advantage.PERIPHERAL_VISION,  Advantage.MUTE, Advantage.NO_PAIN}, new String [] {}),
       new Race(NAME_Elemental_Water, Gender.MALE,     350,   8,  2,  600,   4,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  2,  0, -4,  0,  0,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_AQUATIC, PROPERTIES_ANIMAL, PROPERTIES_REGENERATIVE}, new String[] {"Wealth:$0", Advantage.MAGIC_RESISTANCE+":2", Advantage.REGENERATION,  Advantage.PERIPHERAL_VISION,  Advantage.MUTE, Advantage.NO_PAIN}, new String [] {SpellWaterJet.NAME + ":6"}),
       new Race(NAME_Elemental_Fire,  Gender.MALE,     375,   8,  2,  600,   4,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 1,  1,  0, -4,  0,  2,  0}, new String[] {PROPERTIES_NPC,                     PROPERTIES_ANIMAL, PROPERTIES_REGENERATIVE}, new String[] {"Wealth:$0",                                  Advantage.REGENERATION,  Advantage.PERIPHERAL_VISION,  Advantage.MUTE, Advantage.NO_PAIN}, new String [] {SpellFlameJet.NAME + ":6"}),
       new Race(NAME_Elemental_Air,   Gender.MALE,     400,   6,  1,  425,   8,          BodyType.HUMANIOD,1.0, 1.0, new byte[] { 0,  0,  0, -4,  4,  3,  0}, new String[] {PROPERTIES_NPC, PROPERTIES_2_WINGS, PROPERTIES_ANIMAL, PROPERTIES_REGENERATIVE}, new String[] {"Wealth:$0",                                  Advantage.WINGED_FLIGHT, Advantage.PERIPHERAL_VISION,  Advantage.MUTE, Advantage.NO_PAIN}, new String [] {SpellPush.NAME + ":4"}),
                    // name             gender,       cost, bld hit  ave# move   bodyConfig               length width           STR  HT TOU  IQ NIM DEX SOC
   };

   public static Race getRace(String name, Gender gender) {
      if (name != null) {
         for (Race race : raceList) {
            if ((name.equalsIgnoreCase(race.name)) && (gender == race.gender)) {
               return race.clone();
            }
         }
      }
      return null;
   }

   public static List<String> getRaceNames(boolean includeNPCs) {
      List<String> list = new ArrayList<>();
      for (Race element : raceList) {
         if (includeNPCs || !element.isNpc()) {
            // don't repeat race names for male & female
            if (!list.contains(element.name)) {
               list.add(element.name);
            }
         }
      }
      return list;
   }

   public static List<Gender> getGendersForRace(String raceName) {
      List<Gender> list = new ArrayList<>();
      for (Race race : raceList) {
         if (raceName.equals(race.getName())) {
            list.add(race.gender);
         }
      }
      return list;
   }

   public static String generateHtmlTable() {
      StringBuilder sb = new StringBuilder();
      sb.append(HtmlBuilder.getHTMLHeader("TblRaces", 500, 65));
      sb.append("<body>");
      sb.append("<H4>Race data:</H4>");
      sb.append("<div style=\"overflow: hidden;\" id=\"DivHeaderRow\">\n");
      sb.append("</div>\n");
      sb.append("<div style=\"overflow:scroll;overflow-x:hidden; border-width:0px; border-bottom:1px; border-style:solid;\" onscroll=\"OnScrollDiv(this)\" id=\"DivMainContent\" >\n");
      Table table = new Table();
      table.setID("TblRaces").setAttribute("width", "100%");
      TableRow tr = new TableRow(-1);
      tr.addHeader(new TableHeader("Race<br/>Name").setRowSpan(2));
      tr.addHeader(new TableHeader("Gender").setRowSpan(2));
      tr.addHeader(new TableHeader("Point<br/>Cost").setRowSpan(2));
      tr.addHeader(new TableHeader("Attribute modifiers").setColSpan(Attribute.COUNT));
      tr.addHeader(new TableHeader("Move at<br/>enc. level").setColSpan(6));
      tr.addHeader(new TableHeader("Average<br/>Weight").setRowSpan(2));
      tr.addHeader(new TableHeader("Racial Size<br/>Adjuster").setRowSpan(2));
      tr.addHeader(new TableHeader("Bonus<br/>to&nbsp;be&nbsp;hit&nbsp;/<br/>to&nbsp;attack").setRowSpan(2));
      tr.addHeader(new TableHeader("Advantages<br/> &amp; properties").setRowSpan(2));
      table.addRow(tr);
      tr = new TableRow();
      tr.setClassName("header-row");
      for (Attribute att : Attribute.values()) {
         tr.addHeader(att.shortName.charAt(0) + att.shortName.substring(1).toLowerCase() + ".");
      }
      for (int i = 0; i <= 5; i++) {
         tr.addHeader(i);
      }
      table.addRow(tr);
      int i = 0;
      for (String raceName : getRaceNames(true /*includeNPCs*/)) {
         String className = "row" + ((i++) % HtmlBuilder.MAX_HTML_ROWS);
         List<Gender> genders = getGendersForRace(raceName);
         boolean firstGender = true;
         boolean sameBuild = true;
         int racialBuild = 0;
         boolean diffSpeedPerGender = false;
         List<Attribute> diffAttributePerGender = new ArrayList<>();
         HashMap<Attribute, Byte> baseAttrLevel = new HashMap<>();
         Byte moveRate = null;

         boolean allowAttrToCombineIntoSingleRow = false;
         for (Gender gender : genders) {
            Race race = getRace(raceName, gender);
            if (firstGender) {
               racialBuild = race.getBuildModifier();
            }
            else {
               if (racialBuild != race.getBuildModifier()) {
                  sameBuild = false;
               }
            }
            if (moveRate == null) {
               moveRate = race.getMovementRate((byte)0);
            }
            else if (moveRate != race.getMovementRate((byte)0)) {
               diffSpeedPerGender = true;
            }
            for (Attribute attr : Attribute.values()) {
               if (!baseAttrLevel.containsKey(attr)) {
                  baseAttrLevel.put(attr, race.getAttributeMods(attr));
               }
               else if (baseAttrLevel.get(attr) != race.getAttributeMods(attr)) {
                  diffAttributePerGender.add(attr);
               }
            }
         }
         for (Gender gender : genders) {
            tr = new TableRow();
            tr.setClassName(className);
            if (firstGender) {
               TableHeader th = new TableHeader(raceName);
               th.setRowSpan(genders.size());
               tr.addTD(th);
            }
            Race race = getRace(raceName, gender);
            tr.addTD(new TableData(race.getGender().name));
            tr.addTD(new TableData(race.getCost()));
            for (Attribute att : Attribute.values()) {
               if (diffAttributePerGender.contains(att) || !allowAttrToCombineIntoSingleRow) {
                  tr.addTD(new TableData(race.getAttributeMods(att)));
               }
               else if (firstGender) {
                  tr.addTD(new TableData(race.getAttributeMods(att)).setRowSpan(genders.size()));
               }
            }

            if (firstGender || diffSpeedPerGender) {
               for (byte enc = 0; enc <= 5; enc++) {
                  TableData td = new TableData(race.getMovementRate(enc));
                  td.setBold(enc == 0);
                  if (firstGender && !diffSpeedPerGender) {
                     td.setRowSpan(genders.size());
                  }
                  tr.addTD(td);
               }
            }
            if (sameBuild) {
               if (firstGender) {
                  TableData td;
                  double weight = race.getAveWeight();
                  if (weight == Math.floor(weight)) {
                     td = new TableData((int) race.getAveWeight());
                  }
                  else {
                     td = new TableData(race.getAveWeight());
                  }
                  td.setRowSpan(genders.size());
                  tr.addTD(td);
                  td = new TableData(signedString(race.getBuildModifier()));
                  td.setRowSpan(genders.size());
                  tr.addTD(td);

                  if (race.getBonusToHit() == race.getBonusToBeHit()) {
                     td = new TableData(String.valueOf(race.getBonusToHit()));
                  }
                  else {
                     td = new TableData(signedString(race.getBonusToBeHit()) + " / " + signedString(race.getBonusToHit()));
                  }
                  td.setRowSpan(genders.size());
                  tr.addTD(td);
               }
            }
            else {
               TableData td;
               double weight = race.getAveWeight();
               if (weight == Math.floor(weight)) {
                  td = new TableData((int) race.getAveWeight());
               }
               else {
                  td = new TableData(race.getAveWeight());
               }
               tr.addTD(td);
               tr.addTD(new TableData(signedString(race.getBuildModifier())));
               tr.addTD(new TableData(signedString(race.getBonusToBeHit()) + " / " + signedString(race.getBonusToHit())));
            }
            if (firstGender) {
               StringBuilder advantages = new StringBuilder();
               for (Advantage adv : race.getAdvantagesList()) {
                  if (adv.getName().equals(Advantage.WEALTH_MULTIPLIER_x1)) {
                     continue;
                  }
                  if (advantages.length() > 0) {
                     advantages.append(", ");
                  }
                  advantages.append(adv.getName());
                  if (adv.hasLevels()) {
                     advantages.append(":").append(adv.getLevelName());
                  }
               }
               for (String prop : race.getRacialPropertiesList()) {
                  if (advantages.length() > 0) {
                     advantages.append(", ");
                  }
                  advantages.append(prop);
               }
               for (Spell spell : race.getInateSpells()) {
                  if (advantages.length() > 0) {
                     advantages.append(", ");
                  }
                  advantages.append(spell.getName()).append(":").append(spell.getPower());
               }
               Armor armor = race.getNaturalArmor();
               if (armor != null) {
                  if (advantages.length() > 0) {
                     advantages.append("<br/>");
                  }
                  advantages.append(armor.getName()).append(" (");
                  advantages.append("PD: ").append(armor.getPassiveDefense());
                  advantages.append(", ");
                  boolean addCommas = false;
                  for (DamageType damType : new DamageType[] {ostrowski.combat.common.enums.DamageType.BLUNT, ostrowski.combat.common.enums.DamageType.CUT, ostrowski.combat.common.enums.DamageType.IMP, ostrowski.combat.common.enums.DamageType.FIRE, ostrowski.combat.common.enums.DamageType.ELECTRIC}) {
                     byte barrier = armor.getBarrier(damType);
                     if (barrier != 0) {
                        if (addCommas) {
                           advantages.append(", ");
                        }
                        addCommas = true;
                        advantages.append(damType.shortname).append(": +").append(barrier);
                     }
                  }
                  advantages.append(")");
               }
               if (advantages.length() == 0) {
                  advantages.append("&nbsp;");
               }
               TableData td = new TableData(advantages.toString());
               td.setClassName("alignLeft");
               td.setRowSpan(genders.size());
               tr.addTD(td);
            }
            table.addRow(tr);
            firstGender = false;
         }
      }
      table.addRow(new TableRow().addTD("&nbsp;"));
      sb.append(table);
      sb.append("</div>");
      sb.append("</body>");
      return sb.toString();
   }

   private static String signedString(int value) {
      if (value > 0) {
         return "+" + value;
      }
      return String.valueOf(value);
   }

   public int getLegCount() {
      return legCount;
   }

   public int getEyeCount() {
      return eyeCount;
   }

   public int getHeadCount() {
      return headCount;
   }

   public int getWingCount() {
      return wingCount;
   }

   public List<String> getRacialPropertiesList() {
      return new ArrayList<>(racialProperties);
   }

   public List<Advantage> getAdvantagesList() {
      List<Advantage> advList = new ArrayList<>();
      for (String advName : racialAdvsNames) {
         Advantage adv = Advantage.getAdvantage(advName);
         if (adv != null) {
            advList.add(adv);
         }
      }
      return advList;
   }

   public float getWealthMultiplier() {
      if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_x1)) {
         return 1.0f;
      }
      if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_x2)) {
         return 2.0f;
      }
      else if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_x3)) {
         return 3.0f;
      }
      else if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_2)) {
         return 1.0f /  2.0f;
      }
      else  if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_3)) {
         return 1.0f /  3.0f;
      }
      else if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_4)) {
         return 1.0f /  4.0f;
      }
      else if (racialAdvsNames.contains(Advantage.WEALTH_MULTIPLIER_5)) {
         return 1.0f /  5.0f;
      }
      return 1.0f;
   }
   public Advantage getAdvantage(String advName) {
      for (String racialAdvName : racialAdvsNames) {
         if (racialAdvName.equalsIgnoreCase(advName)) {
            return Advantage.getAdvantage(advName);
         }
         if (racialAdvName.startsWith(advName)) {
            Advantage adv = (Advantage.getAdvantage(advName));
            String selectedLevelName = racialAdvName.substring(advName.length() + 1);
            if (adv.setLevelByName(selectedLevelName)) {
               return adv;
            }
         }
      }
      return null;
   }

   public boolean hasProperty(String propName) {
      for (String racialPropName : racialProperties) {
         if (racialPropName.equals(propName)) {
            return true;
         }
      }
      return false;
   }

   public boolean isAquatic() {
      return isAquatic;
   }

   public boolean isAnimal() {
      return isAnimal;
   }

   public Wound alterWound(Wound wound, StringBuilder alterationExplanationBuffer) {
      if ((wound.getLocation() == Location.LEG) && (getLegCount() > 2) && (wound.getPenaltyMove() > 0)) {
         Wound altWound = new Wound(wound.getLevel(), wound.getLocation(), wound.getDescription(), wound.getPain(), wound.getWounds(), wound.getBleedRate(),
                                    wound.getPenaltyArm(), wound.getPenaltyMove() / 2, wound.getKnockedBackDist(), wound.getDamageType(),
                                    wound.getEffectsMask(), null /*target, used for wound placement*/);
         alterationExplanationBuffer.append(getName()).append("s have ").append(getLegCount()).append(" legs, so movement and retreat penalties are cut in half.");
         return altWound;
      }
      return wound;
   }

   @Override
   public String toString() {
      return getName();
   }

   public boolean isNpc() {
      return hasProperty(PROPERTIES_NPC);
   }

   public Limb createLimb(LimbType id) {
      if (id.isHead() && (id.setId <= headCount)) {
         return new Head(id, this);
      }
      if (id.isLeg() && (id.setId <= (legCount / 2))) {
         return new Leg(id, this);
      }
      if (id.isHand() && (id.setId <= (armCount / 2))) {
         return new Hand(id, this);
      }
      if (id.isTail() && (id.setId <= getTailCount())) {
         return new Tail(id, this);
      }
      if (id.isWing() && (id.setId <= (wingCount / 2))) {
         Wing.Type wingType = Wing.Type.Feathered;
         if (name.equals(NAME_Fairy)) {
            wingType = Wing.Type.Fairy;
         }
         if (name.contains("Dragon") ||
             name.equals(NAME_Sturge) ||
             name.equals(NAME_Gargoyle)) {
            wingType = Wing.Type.Bat;
         }
         return new Wing(id, wingType, this);
      }
      return null;
   }

   public Thing createSeveredLimb(LimbType id) {
      Limb thing = createLimb(id);
      String simpleName = thing.getSimpleName();
      if (simpleName.equals("hand")) {
         simpleName = "arm";
      }

      thing.name = "Severed " + simpleName;
      return thing;
   }

   public Armor getNaturalArmor() {
      return naturalArmor;
   }

   public byte getNaturalArmorBarrier(DamageType damType) {
      if (naturalArmor == null) {
         return 0;
      }
      return naturalArmor.getBarrier(damType);
   }

   public void getPointEstimate() {
      Character testCharacter = new Character();
      for (Attribute att : Attribute.values()) {
         testCharacter.setAttribute(att, attributeMods.get(att), false/*containInLimits*/);
      }
      int attributeCost = testCharacter.getPointTotal();

      List<Advantage> advs = getAdvantagesList();
      int advCost = 0;
      for (Advantage adv : advs) {
         advCost += adv.getCost(getRace(NAME_Human, Gender.MALE));
      }

      int propCost = 0;
      boolean hasFangs = false;
      boolean isAnimal = false;
      for (String prop : racialProperties) {
         switch (prop) {
            case PROPERTIES_2_WINGS:
               propCost += 40;
               break;
            case PROPERTIES_4_ARMS:
               propCost += 100;
               break;
            case PROPERTIES_4_LEGS:
               propCost += 40;
               break;
            case PROPERTIES_6_LEGS:
               propCost += 60;
               break;
            case PROPERTIES_ANIMAL:
               propCost += -100;
               isAnimal = true;
               break;
            case PROPERTIES_HORNS:
               propCost += 15;
               break;
            case PROPERTIES_STURGEBREAK:
               propCost += 25;
               break;
            case PROPERTIES_FANGS:
               propCost += 10;
               hasFangs = true;
               break;
            case PROPERTIES_TUSKS:
               propCost += 25;
               break;
            case PROPERTIES_CLAWS:
               propCost += 10;
               break;
            case PROPERTIES_TAIL:
               propCost += 5;
               break;
            case PROPERTIES_FEATHERS:
               propCost += 10;
               break;
            case PROPERTIES_THIN_FUR:
               propCost += 10;
               break;
            case PROPERTIES_FUR:
               propCost += 20;
               break;
            case PROPERTIES_THICK_FUR:
               propCost += 30;
               break;
            case PROPERTIES_THIN_SCALES:
               propCost += 40;
               break;
            case PROPERTIES_SCALES:
               propCost += 50;
               break;
            case PROPERTIES_THICK_SCALES:
               propCost += 60;
               break;
            case PROPERTIES_REGENERATIVE:
               propCost += 50;
               break;
            case PROPERTIES_NPC:
               propCost += 0;
               break;
            case PROPERTIES_NIGHT_VISION:
               propCost += 10;
               break;
            case PROPERTIES_INFRAVISION:
               propCost += 15;
               break;
            case PROPERTIES_AQUATIC:
               propCost += -25;
               break;
         }
      }
      if (hasFangs && isAnimal) {
         // animals with fangs get a grapple attack worth 20 points over just fangs alone
         propCost += 20;
      }
      int adjustmentForBuild;
      int adjustmentForMove = (baseSpeed - 5) * 10;
      if (buildModifier > 0) {
         // +20 for level above 0, up to 5:
         adjustmentForBuild = 20 * buildModifier;
         if (buildModifier > 5) {
            // +40 for level above 5 (plus 100 for the first 5 levels):
            adjustmentForBuild += (buildModifier - 5) * 20;
         }
      }
      else {
         // -10 for level below 0:
         adjustmentForBuild = 10 * buildModifier;
         if (buildModifier < -3) {
            // -7.5 for levels below -3:
            adjustmentForBuild = (int) (-30 + (7.5 * (buildModifier + 3)));
         }
      }
      adjustmentForBuild += getBonusToHit() * 5;
      adjustmentForBuild += getBonusToBeHit() * -5;

      int adjustmentForInateSpells = 0;
      for (Spell spell : inateSpells) {
         adjustmentForInateSpells += 20 * spell.getPower();
      }

      int total = attributeCost + adjustmentForBuild + adjustmentForMove + advCost + propCost + adjustmentForInateSpells;
      int diff = Math.abs(cost - total);
      // If it's within 5 points, ignore the difference
      if (diff < 5) {
         return;
      }
      // if it's within 5%, ignore the difference
      if ((diff * 20) < total) {
         return;
      }
      Rules.diag("cost analysis of " + getName() + "(" + getGender().name.charAt(0) + ") = " + total + "(" + attributeCost + " + " + advCost + " + " + propCost
                 + " + " + adjustmentForBuild + " + " + adjustmentForMove + "), listed at " + cost);
   }

   public void testRacialSize() {
      // Racial Build Adj.  = log (racial average weight/150) * 40/3
      // racial weight ave. = 150 * 10^(racial build adj. * 3 / 40);
      // hit adjustment = (buildAdjustment / 4)
      double expectedWeight = 150 * Math.pow(10, ((buildModifier * 3.0) / 40.0));
      // allow a 5% margin of error:
      StringBuilder sb = new StringBuilder();
      sb.append("analysis of ").append(getName()).append("(").append(getGender().name.charAt(0)).append(") : ");
      if ((Math.abs(expectedWeight - aveWeight) / aveWeight) > .05) {
         sb.append("weight exp weight=").append(expectedWeight).append(", listed weight=").append(aveWeight);
      }
      byte expectedBonusToBeHit = (byte) Math.floor(.499d + (buildModifier / 4.0));
      if ((legCount > 2) && (armCount == 0)) {
         expectedBonusToBeHit--;
      }
      if (bonusToBeHit != expectedBonusToBeHit ) {
         sb.append(" exp bonusToBeHit=").append(expectedBonusToBeHit).append(", listed bonusToBeHit=").append(bonusToBeHit);
      }
      Rules.diag(sb.toString());
   }
}
