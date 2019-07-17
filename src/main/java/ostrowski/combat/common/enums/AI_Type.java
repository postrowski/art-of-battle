package ostrowski.combat.common.enums;

public enum AI_Type {
   SIMPLE ("Simple"),
   NORM ("Norm"),
   GOD ("God"),
   STATIONARY("Stationary");

   // "God" -      can see through walls,
   //              knows all map details,
   // "Norm" -     normal character,
   //              will walk around, looking for characters
   //              will attack closest enemy that it knows about,
   //              will advance toward closest enemy, unless a ranged attack can be made

   private AI_Type(String name) {
      this.name = name;
   }
   public final String name;
   public static AI_Type getByString(String name) {
      name = name.strip();
      if (name.startsWith("AI -")) {
         name = name.replaceAll("AI -", "").strip();
      }
      for (AI_Type type : AI_Type.values()) {
         if (type.name.equalsIgnoreCase(name)) {
            return type;
         }
      }
      return null;
   }
}