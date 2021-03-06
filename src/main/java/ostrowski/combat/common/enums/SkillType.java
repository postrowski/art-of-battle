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
   StaffSling        ("Staff Sling",       true, false, Attribute.Dexterity, ArmsUsed.Both, false),
   Sword             ("Sword",             true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Throwing          ("Throwing",          true, false, Attribute.Dexterity, ArmsUsed.One,  false),
   Wrestling         ("Wrestling",         true,  true, Attribute.Dexterity, ArmsUsed.None, true),
   // Non-combat physical skills:
   Acrobatics        ("Acrobatics",       false,  true, Attribute.Nimbleness, ArmsUsed.None, false),
   Climbing          ("Climbing",         false,  true, Attribute.Dexterity, ArmsUsed.Both, false),
   LockPicking       ("Lock Picking",     false,  true, Attribute.Dexterity, ArmsUsed.Both, false),
   PickPocket        ("Pick Pocket",      false,  true, Attribute.Dexterity, ArmsUsed.One, false),
   Riding            ("Riding",           false, false, Attribute.Dexterity, ArmsUsed.None, false),
   Stealth           ("Stealth",          false,  true, Attribute.Dexterity, ArmsUsed.None, false),
   Swimming          ("Swimming",         false,  true, Attribute.Dexterity, ArmsUsed.Both, false),
   // Mental Skills:
   AreaKnowledge     ("Area Knowledge",    false, false, Attribute.Intelligence, ArmsUsed.None, false),
   AnimalTraining    ("Animal Training",   false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Blacksmith        ("Blacksmith",        false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Carpentry         ("Carpentry",         false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Farming           ("Farming",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   FirstAid          ("First Aid",         false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Heraldry          ("Heraldry",          false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Languages         ("Languages",         false, false, Attribute.Intelligence, ArmsUsed.None, false),
   LeatherWorking    ("Leather Working",   false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Masonry           ("Masonry",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Poisons           ("Poisons",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Sailing           ("Sailing",           false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Survival          ("Survival",          false, false, Attribute.Intelligence, ArmsUsed.Both, false),
   Tactics           ("Tactics",           false, false, Attribute.Intelligence, ArmsUsed.None, false),
   Tracking          ("Tracking",          false, false, Attribute.Intelligence, ArmsUsed.None, false);

   public String           _name;
   public Attribute        _attributeBase;
   public ArmsUsed         _armUseCount;
   public boolean          _isAdjustedForSize;
   public boolean          _isAdjustedForEncumbrance;
   public boolean          _isUnarmed;

   public String getName()                   { return _name;}

   public Attribute getAttributeBase()       { return _attributeBase; }

   public boolean isUnarmed()                { return _isUnarmed; }

   SkillType(String name, boolean isAdjForSize, boolean isAdjustedForEncumbrance, Attribute attributeBase, ArmsUsed armCount, boolean isUnarmed) {
      _name                      = name;
      _isAdjustedForSize         = isAdjForSize;
      _isAdjustedForEncumbrance  = isAdjustedForEncumbrance;
      _attributeBase             = attributeBase;
      _armUseCount               = armCount;
      _isUnarmed                 = isUnarmed;
   }
   static private final HashMap<String, SkillType> _mapByName = new HashMap<>();
   static {
      for (SkillType type : values()) {
         _mapByName.put(type._name.toLowerCase(), type);
      }
   }
   static public SkillType getSkillTypeByName(String name) {
      return _mapByName.get(name.toLowerCase());
   }
}