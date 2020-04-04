package ostrowski.combat.common.spells.priest.elemental;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableHeader;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellSonicBlast extends ResistedPriestSpell implements IRangedSpell
{
   public static final String NAME = "Sonic Blast";

   public SpellSonicBlast() {
      this(null, 0);
   }

   public SpellSonicBlast(Class<PriestElementalSpell> group, int affinity) {
      super(NAME, Attribute.Health, (byte) 2, false/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return getTargetName() + " is blasted by the '" + getName() + "' spell.";
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      TableRow header1 = new TableRow(-1);
      header1.addHeader(new TableHeader("Effective Power").setRowSpan(3));
      header1.addHeader(new TableHeader("Effects (applies within 4 hexes of safety boundary)").setColSpan(7));
      table.addRow(header1);
      TableRow header2 = new TableRow(-1);
      header2.addHeader(new TableHeader("Description").setRowSpan(2));
      header2.addHeader(new TableHeader("HT saving throw TN vs.").setColSpan(6));
      table.addRow(header2);
      table.addRow(new TableRow(-1, "Disorientation","Knockback","Knockdown","Deafness","KnockOut","Death"));
      for (int power = 1; power <= 8; power++) {
         TableRow row = new TableRow(power-1);
         row.addHeader(power);
         row.addTD(getDescriptionForPower(power));
         row.addTD(getTN(getTNDisorient(power)));
         row.addTD(getTN(getTNKnockBack(power)));
         row.addTD(getTN(getTNKnockDown(power)));
         row.addTD(getTN(getTNDeafness(power)));
         row.addTD(getTN(getTNKnockOut(power)));
         row.addTD(getTN(getTNDeath(power)));
         table.addRow(row);
      }
      String sb = "The '" + getName() + "' spell causes a pressure waves of air, centered on (but not effecting) the caster." +
                  " At low power, this creates an audible boom." +
                  " At higher power, this pressure wave can disorient or even kill nearby characters." +
                  " High power pressure waves are also capable of pushing objects down and away from the caster." +
                  " Normally, the pressure wave affects anything outside the caster's personal hex, however, if the caster" +
                  " wishes to protect others nearby, he/she may use some of the power of the spell into protecting those nearby." +
                  " For each point of power diverted into protecting those nearby, the radius of safety (which is 1-hex initially) will be doubled." +
                  " Therefore, spending 1 point of the spell's power on protection causes the safety radius to be 2 hexes (anyone in any hex adjacent to the caster's own hex will be safe.)" +
                  " Spending 3 point of the spell's power on protection causes the safety radius to be 8 hexes (anyone within 7 hexes of the caster's hex will be safe.)" +
                  table +
                  " Possible damage done by the spell is reduced by the range, with the power dropping by 1 power point every time the range increases." +
                  " Everyone within point-blank range (that is not within the protected radius) suffers the full power of this spell, as detailed in the above table." +
                  " Anyone within the short range distance from the caster suffers damage as if the spell had 1 less power than its casting power." +
                  " Anyone in medium range suffers damage as if the spell had 2 less power than it casting power." +
                  " Anyone in long range suffers damage as if the spell had 3 less power than it casting power.";
      return sb;
   }

   public String getDescriptionForPower(int power) {
      switch (power) {
         case 1:
            return "Audible pop";
         case 2:
            return "Startling Boom"; // ---- possible (TN 5) -----   |  ---- likely (TN 10) -----   | --- very likely (TN 15) ---- |   ---- certain
         case 3:
            return "Small Blast"; // knockback, disorientation   |                              |                              |
         case 4:
            return "Disorienting Blast"; // deafness, knockdown          | knockback & disorientation  |                              |
         case 5:
            return "Concussive Blast"; // KO                           | deafness, knockdown          | knockback & disorientation  |
         case 6:
            return "Small Explosion"; // Death                        | KO                           | deafness, knockdown          | knockback & disorientation
         case 7:
            return "Explosion"; //                              | Death                        | KO                           | deafness, knockdown, knockback & disorientation
         case 8:
            return "Massive Explosion"; //                              |                              | Death                        | KO, deafness, knockdown, knockback & disorientation
      }
      return "";
   }

   public String getTN(int tn) {
      if (tn <= 0) {
         return "--";
      }
      if (tn > 16) {
         return "always";
      }
      return String.valueOf(tn);
   }

   public int getTNDeafness(int power) {
      return Math.max(0, 5 * (power - 3));
   }

   public int getTNDisorient(int power) {
      return Math.max(0, 5 * (power - 2));
   }

   public int getTNKnockBack(int power) {
      return Math.max(0, 5 * (power - 2));
   }

   public int getTNKnockDown(int power) {
      return Math.max(0, 5 * (power - 3));
   }

   public int getTNKnockOut(int power) {
      return Math.max(0, 5 * (power - 4));
   }

   public int getTNDeath(int power) {
      return Math.max(0, 5 * (power - 5));
   }

   @Override
   public short getRangeBase() {
      return 8;
   }

   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return 0;
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return 0;
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   @Override
   public boolean affectsMultipleTargets() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      if (_castingEffectiveness > 0) {
         //int tnDeafness = getTNDeafness(_effectivePower);
         int tnDisorient = getTNDisorient(_effectivePower);
         int tnKnockBack = getTNKnockBack(_effectivePower);
         int tnKnockDown = getTNKnockDown(_effectivePower);
         int tnKnockOut = getTNKnockOut(_effectivePower);
         int tnDeath = getTNDeath(_effectivePower);

         StringBuilder damageExplanationButtfer = new StringBuilder();
         byte damage = 0;
         int wounds = resolveTN(tnDisorient, "disorientation", damageExplanationButtfer) ? 0 : _effectivePower;
         int pain = wounds * 2;
         boolean knockedBack = !resolveTN(tnKnockBack, "knock back", damageExplanationButtfer);
         boolean knockedDown = !resolveTN(tnKnockDown, "knock down", damageExplanationButtfer);
         boolean knockedOut = !resolveTN(tnKnockOut, "knock out", damageExplanationButtfer);
         boolean killed = !resolveTN(tnDeath, "killed", damageExplanationButtfer);
         long effectsMask = 0;
         if (knockedDown) {
            effectsMask |= EFFECT_KNOCKDOWN;
         }
         if (knockedOut) {
            effectsMask |= EFFECT_KNOCKOUT;
         }
         if (killed) {
            effectsMask |= EFFECT_DEATH;
         }

         int knockedBackDist = (int) Math.pow(2, _effectivePower - (knockedBack ? 3 : 4));

         Wound wound = new Wound(damage, Location.BODY, getDescriptionForPower(_power), pain, wounds, 0/*bleedrate*/, 0/*armPenalty*/, 0/*movementPenalty*/,
                                 knockedBackDist, DamageType.BLUNT, effectsMask, getTarget());

         damageExplanationButtfer.append(getTargetName()).append(" takes damage from the ").append(getName()).append(" spell: ").append(wound.describeEffects());
         arena.sendMessageTextToAllClients(damageExplanationButtfer.toString(), false/*popUp*/);
         getTarget().applyWound(wound, arena);
      }
   }

   // Return true if the target successfully resists the effects
   private boolean resolveTN(int tn, String descriptionOfRoll, StringBuilder damageExplanationButtfer) {
      if (tn == 0) {
         damageExplanationButtfer.append(getTargetName()).append(" automatically passes the ").append(descriptionOfRoll).append(" roll.");
      }
      return false;
   }

}
