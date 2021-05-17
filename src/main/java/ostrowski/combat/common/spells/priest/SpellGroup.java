package ostrowski.combat.common.spells.priest;

public enum SpellGroup {
   DEFENSIVE("Defensive"),
   HEALING("Healing"),
   INFORMATION("Information"),
   NATURE("Nature"),
   ELEMENTAL("Elemental"),
   EVIL("Evil"),
   GOOD("Good"),
   OFFENSIVE("Offensive"),
   DEMON("Demon");

   SpellGroup(String name) {
      this.name = name;
   }

   private String name;

   public String getName() {
      return this.name;
   }
}
