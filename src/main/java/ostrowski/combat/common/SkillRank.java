package ostrowski.combat.common;

public enum SkillRank {
   PROFICIENT("Proficient", 10),
   FAMILIAR("Familiar", 4),
   UNKNOWN("Unknown", 0);

   String name;
   byte   cost;

   SkillRank(String name, int cost) {
      this.name = name;
      this.cost = (byte) cost;
   }

   public static SkillRank getRankByName(String name) {
      for (SkillRank rank : values()) {
         if (rank.name.equalsIgnoreCase(name)) {
            return rank;
         }
      }
      return null;
   }

   public String getName() {
      return this.name;
   }

   public byte getCost() {
      return this.cost;
   }
}
