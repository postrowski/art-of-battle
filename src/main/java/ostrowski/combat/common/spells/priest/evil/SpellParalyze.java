package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellParalyze extends ResistedPriestSpell implements ICastInBattle
{
   public static final String NAME = "Paralyze";
   public SpellParalyze() {
   }
   public SpellParalyze(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Nimbleness, (byte)3/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime)
   {
      if (firstTime) {
         return getTargetName() + " is unable to move from "+getCasterName()+"'s paralyze spell.";
      }
      return getTargetName() + " is still paralyzed, unable to move from "+getCasterName()+"'s paralyze spell.";
   }

   @Override
   public String describeSpell()  {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective Power", "Effects"));
      for (int p=1 ; p<=5 ; p++) {
         table.addRow(new TableRow(p-1).addHeader("" + p + ((p==5) ? "+" : ""))
                                       .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell makes the subject unable to move." + table;
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 1: return "Subject's legs are paralyzed, but remains standing. Subject may not move or retreat, and dodges at 1/2 effectiveness.";
         case 2: return "As above, plus the subjects waist becomes paralyzed. Dodging is not possible. Melee attacks are at 1/2 skill.";
         case 3: return "As above, plus the subjects arms becomes paralyzed. Wrists and fingers are still mobile. Blocking and parrying are no longer possible. Melee attacks are no longer possible.";
         case 4: return "As above, plus the subjects hands becomes paralyzed. Subject may still talk and move their head.";
         case 5: return "Subject is completely paralyzed, and may do nothing. Breathing and heart-rate are unaffected.";
      }
      return "";
   }

   @Override
   public void applyEffects(Arena arena) {
   }
   @Override
   public void removeEffects(Arena arena) {
   }

   @Override
   public byte getModifiedMovementPerRound(byte previousMovementRate) {
      return 0;
   }
   // paralyze at 1 or higher prevents retreat defenses.
   // paralyze at 2 or higher prevents dodge defenses.
   public boolean allowsRetreat() {
      return getEffectivePower() <= 0;
   }
   public boolean allowsDodge() {
      return getEffectivePower() <= 1;
   }
   public boolean allowsBlockParry() {
      return getEffectivePower() <= 2;
   }
   public boolean allowsAttack() {
      return getEffectivePower() <= 2;
   }

}
