package ostrowski.combat.common.enums;

import ostrowski.combat.common.Skill.ArmsUsed;

import java.util.HashMap;

public enum SkillType {
   // Combat physical skills:
   TwoHanded_AxeMace ("2-Handed Axe/Mace", true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   TwoHanded_Sword   ("2-Handed Sword",    true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Aikido            ("Aikido",            true,  true, Attribute.Dexterity, ArmsUsed.Both, true),
   AxeMace           ("Axe/Mace",          true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   BlowGun           ("Blow Gun",          true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Bow               ("Bow",               true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Brawling          ("Brawling",          true,  true, Attribute.Dexterity, ArmsUsed.None, true),
   Boxing            ("Boxing",            true,  true, Attribute.Dexterity, ArmsUsed.None, true),
   Crossbow          ("Crossbow",          true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Fencing           ("Fencing",           true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Flail             ("Flail",             true, false, Attribute.Dexterity, ArmsUsed.OneOrTwo, false),
   Jousting          ("Jousting",          true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Karate            ("Karate",            true,  true, Attribute.Dexterity, ArmsUsed.None, true),
   Knife             ("Knife",             true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Net               ("Net",               true, false, Attribute.Dexterity, ArmsUsed.OneOrTwo, false),
   NunChucks         ("Nun chucks",        true, false, Attribute.Dexterity, ArmsUsed.OneOrTwo, false),
   Polearm           ("Polearm",           true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Quarterstaff      ("Quarterstaff",      true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Shield            ("Shield",           false, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Sling             ("Sling",             true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Spear             ("Spear",             true, false, Attribute.Dexterity, ArmsUsed.OneOrTwo, false),
   Sword             ("Sword",             true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Throwing          ("Throwing",          true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Wrestling         ("Wrestling",         true,  true, Attribute.Dexterity, ArmsUsed.Both, true),
   // Non-combat physical skills:
   Acrobatics        ("Acrobatics",       false,  true, Attribute.Nimbleness, ArmsUsed.Both, false),
   Backstab          ("Backstab",         false,  true, Attribute.Dexterity,  ArmsUsed.Both, false),
   Climbing          ("Climbing",         false,  true, Attribute.Dexterity,  ArmsUsed.Both, false),
   LockPicking       ("Lock Picking",     false,  true, Attribute.Dexterity,  ArmsUsed.Both, false),
   PickPocket        ("Pick Pocket",      false,  true, Attribute.Dexterity,  ArmsUsed.One, false),
   Riding            ("Riding",           false, false, Attribute.Dexterity,  ArmsUsed.Both, false),
   Running           ("Running",          false,  true, Attribute.Health,     ArmsUsed.None, false),
   Sport             ("Sport",            false, false, Attribute.Dexterity,  ArmsUsed.None, false),
   Stealth           ("Stealth",          false,  true, Attribute.Dexterity,  ArmsUsed.None, false),
   Swimming          ("Swimming",         false,  true, Attribute.Dexterity,  ArmsUsed.Both, false),
   // Mental Skills:
   AnimalHusbandry   ("Animal Husbandry",  false, false, Attribute.Intelligence, ArmsUsed.None, false),
   AnimalKnowledge   ("Animal Knowledge",  false, false, Attribute.Intelligence, ArmsUsed.None, false),
   AnimalTraining    ("Animal Training",   false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Appraising        ("Appraising",        false, false, Attribute.Intelligence, ArmsUsed.None, false),
   AreaKnowledge     ("Area Knowledge",    false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Blacksmith        ("Blacksmith",        false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Carpentry         ("Carpentry",         false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   CommonKnowledge   ("Common Knowledge",  false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Cooking           ("Cooking",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Dentistry         ("Dentistry",         false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Disguise          ("Disguise",          false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Farming           ("Farming",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   FirstAid          ("First Aid",         false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Geology           ("Geology",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Heraldry          ("Heraldry",          false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Herbalist         ("Herbalist",         false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Investigation     ("Investigation",     false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Language2         ("2nd Language",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Language3         ("3rd Language",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Language4         ("4th Language",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Language5         ("5th Language",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Language6         ("6th Language",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   LeatherWorking    ("Leather Working",   false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Masonry           ("Masonry",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Meteorology       ("Meteorology",       false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Painting          ("Painting",          false, false, Attribute.Intelligence, ArmsUsed.One, false),
   PlantKnowledge    ("Plant Knowledge",   false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Poisons           ("Poisons",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Repair            ("Repair",            false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Sailing           ("Sailing",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   StoneWorking      ("Stone Working",     false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Surgery           ("Surgery",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Survival          ("Survival",          false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Tactics           ("Tactics",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Tracking          ("Tracking",          false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Trapping          ("Trapping",          false, false, Attribute.Intelligence, ArmsUsed.One, false),
   Veterinarian      ("Veterinarian",      false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   // Social Skills:
   Bartering         ("Bartering",         false, false, Attribute.Social, ArmsUsed.None, false),
   Dancing           ("Dancing",           false, false, Attribute.Social, ArmsUsed.Both, false),
   DetectLies        ("DetectLies",        false, false, Attribute.Social, ArmsUsed.None, false),
   Etiquette         ("Etiquette",         false, false, Attribute.Social, ArmsUsed.None, false),
   Flute             ("Flute",             false, false, Attribute.Social, ArmsUsed.Both, false),
   GuitarPlaying     ("GuitarPlaying",     false, false, Attribute.Social, ArmsUsed.Both, false),
   Leadership        ("Leadership",        false, false, Attribute.Social, ArmsUsed.None, false),
   Lying             ("Lying",             false, false, Attribute.Social, ArmsUsed.None, false),
   Persuasion        ("Persuasion",        false, false, Attribute.Social, ArmsUsed.None, false),
   Piano             ("Piano",             false, false, Attribute.Social, ArmsUsed.Both, false),
   SexAppeal         ("SexAppeal",         false, false, Attribute.Social, ArmsUsed.None, false),
   Singing           ("Singing",           false, false, Attribute.Social, ArmsUsed.None, false),
   Violin            ("Violin",            false, false, Attribute.Social, ArmsUsed.Both, false),

   // Spellcasting Skills:
   Ancient_Language         ("Ancient_Language",false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Arcane_History           ("Arcane_History",  false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Fire        ("Fire",            false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Water       ("Water",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Earth       ("Earth",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Air         ("Air",             false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Energy      ("Energy",          false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Evocation   ("Evocation",       false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Conjuration ("Conjuration",     false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Illusion    ("Illusion",        false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Divination  ("Divination",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Necromancy  ("Necromancy",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Protection  ("Protection",      false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Spellcasting_Enchantment ("Enchantment",     false, false, Attribute.Intelligence, ArmsUsed.None, false);

   public               String                     name;
   public               Attribute                  attributeBase;
   public               ArmsUsed                   armUseCount;
   public               boolean                    isAdjustedForSize;
   public               boolean                    isAdjustedForEncumbrance;
   public               boolean                    isUnarmed;
   static private final HashMap<String, SkillType> MAP_BY_NAME = new HashMap<>();

   public String getName()                   { return name;}

   public Attribute getAttributeBase()       { return attributeBase; }

   public boolean isUnarmed()                { return isUnarmed; }

   SkillType(String name, boolean isAdjForSize, boolean isAdjustedForEncumbrance, Attribute attributeBase, ArmsUsed armCount, boolean isUnarmed) {
      this.name = name;
      isAdjustedForSize = isAdjForSize;
      this.isAdjustedForEncumbrance = isAdjustedForEncumbrance;
      this.attributeBase = attributeBase;
      armUseCount = armCount;
      this.isUnarmed = isUnarmed;
   }
   static {
      for (SkillType type : values()) {
         MAP_BY_NAME.put(type.name.toLowerCase(), type);
      }
   }
   static public SkillType getSkillTypeByName(String name) {
      return MAP_BY_NAME.get(name.toLowerCase());
   }
}