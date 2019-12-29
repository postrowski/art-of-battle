package ostrowski.combat.common.spells.priest.nature.weather;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;

public class SpellIceStorm extends PriestSpell
{
   public static final String NAME = "Ice Storm";
   public SpellIceStorm() {}

   public SpellIceStorm(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' spell causes a torrent of icicles to fall from the sky.");
      sb.append(" The power of the spell determines the duration of the damage done by the falling icicles.");
      sb.append(" Any character not protected by shelter may be hit by a falling icicle.");
      sb.append(" Also, any character within short distance of the caster will also be unaffected.");
      sb.append(" The range of this protection is chosen by the caster, and may be up to a range in hexes equal to the divine affinity of the caster.");
      sb.append(" Each turn the spell is active, each character in the open and outside of the protected range of the caster, be they friend or foe, must roll a d20.");
      sb.append(" If the die rolls lower than, or equal to the divine affinity of the caster, they may be struck by an icicle.");
      sb.append(" The character may dodge or block the icicle.");
      sb.append(" Characters will be attacked with a d10± plus the Divine Affinity of the caster.");
      sb.append(" As with any other attack, if the character's TN adjusted for their size, armor, and defensive actions, is not greater than the attack roll, results in damage to the character.");
      sb.append(" Bonus damage of 1 point for every two full points over the TN rolled on the attack roll applies as normal.");
      sb.append(" Icicles that are not dodged or blocked result in 1d10 impaling damage per point of spell power, plus any bonus damage.");
      sb.append(" The casting priest may choose to stop the storm at any time earlier than the normal expiration time, if he/she chooses.");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Storm duration", "Damage"));
      for (int power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, ""+power,
                                            getDurationForPower(power) + " turns",
                                            getDamageForPower(power)));
      }
      sb.append(table);
      return sb.toString();
   }
   public static int getDurationForPower(int power) {
      return (int) Math.pow(2, (power-1));
   }
   public static DiceSet getDamageForPower(int power) {
      return new DiceSet(power+"d10");
   }
}
