/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.offensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.SpecialDamage;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.wounds.Wound;

public class SpellIncreaseDamage extends ExpiringPriestSpell implements ICastInBattle
{
   public static final String NAME = "Increase Damage";
   public SpellIncreaseDamage() {
      this(null, 0);
   }
   public SpellIncreaseDamage(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short) 10/*baseEcpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      StringBuilder sb = new StringBuilder();
      sb.append(getTargetName());
      sb.append(" will do additional damage: ");
      int wounds = getWounds(getPower());
      int pain   = getPain(getPower());
      if (wounds>0) {
         if (wounds>0) {
            sb.append("+");
         }
         sb.append(wounds).append(" wounds, and ");
      }
      if (pain>0) {
         sb.append("+");
      }
      sb.append(pain).append(" pain.)");
      return sb.toString();
   }
   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The 'Increase Damage' spell causes the subject to do more damage, if they hit.");
      sb.append("  Any time the subject of the spell hits an opponent, and penetrates the armor (does at least 1 point of damage),");
      sb.append(" then attacker does additional pain and wounds, based upon the power of the spell:");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Extra pain", "Extra wounds", "Spell duration"));
      for (byte power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power,
                                            ""+getPain(power),
                                            ""+getWounds(power),
                                            ""+super.getDuration(power)));
      }
      sb.append(table);
      return sb.toString();
   }

   @Override
   public Wound modifyDamageDealt(Wound wound, Character attacker, Character defender, String attackingWeaponName, StringBuilder modificationsExplanation) {
      // this spell only does damage if the original wound does some damage.
      if ((wound == null) || (wound.getLevel() == 0)) {
         return wound;
      }
      SpecialDamage specialDamageModifier = new SpecialDamage(SpecialDamage.MOD_NONE);
      byte painMod = getPain(getPower());
      byte woundMod = getWounds(getPower());
      specialDamageModifier.setPainModifier(painMod);
      specialDamageModifier.setWoundModifier(woundMod);
      wound.setSpecialDamageModifier(specialDamageModifier);
      modificationsExplanation.append("Wound increased by the '").append(getName()).append("' spell,");
      if (painMod != 0) {
         if (painMod > 0) {
            modificationsExplanation.append(" pain increased by ");
         }
         else {
            modificationsExplanation.append(" pain decreased by ");
         }
         modificationsExplanation.append(painMod).append(" points");
         if (woundMod != 0) {
            modificationsExplanation.append(" and");
         }
         else {
            modificationsExplanation.append(".");
         }
      }
      if (woundMod != 0) {
         if (woundMod > 0) {
            modificationsExplanation.append(" wounds increased by ");
         }
         else {
            modificationsExplanation.append(" wounds decreased by ");
         }
         modificationsExplanation.append(woundMod).append(" points.");
      }
      return wound;
   }

   private static byte getWounds(byte power)
   {
      switch (power) {
         case 1: return 0;
         case 2: return 1;
         case 3: return 1;
         case 4: return 2;
         case 5: return 2;
         case 6: return 3;
         case 7: return 3;
         case 8: return 4;
      }
      return 0;
   }

   private static byte getPain(byte power)
   {
      switch (power) {
         case 1: return 3;
         case 2: return 3;
         case 3: return 4;
         case 4: return 4;
         case 5: return 5;
         case 6: return 5;
         case 7: return 6;
         case 8: return 6;
      }
      return 0;
   }


   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE_FIGHTING;
   }
}
