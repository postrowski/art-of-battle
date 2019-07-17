package ostrowski.combat.common.spells.priest;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundChart;
import ostrowski.combat.server.Arena;

public abstract class SpellDestroy extends ResistedPriestSpell implements IRangedSpell, ICastInBattle
{
   public SpellDestroy() {
   }
   public SpellDestroy(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, Attribute.Health, (byte)3/*resistedActions*/, false/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime)
   {
      byte damage = getDamageAmount();
      if (damage > 0) {
         return getTargetName() + " is damaged by the '"+getName()+"' spell, resulting in " + damage + " points of damage.";
      }
      return "";
   }

   @Override
   public String describeSpell()
   {
      return "The '"+getName()+"' spell affects any creature that is of "+getTargetTypeName() +
             " If the subject of the spell fails to resist the spell, they suffer one point of energy damage" +
             " for every point that they missed their resistance by, multiplied by the effective power of the spell.";
   }
   abstract public String getTargetTypeName();

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_EVIL_FIGHTING;
   }
   @Override
   public boolean affectsMultipleTargets() {
      return true;
   }

   @Override
   public void applyEffects(Arena arena) {
      byte damage = getDamageAmount();
      if (damage > 0) {
         StringBuilder alterationExplanationBuffer = new StringBuilder();
         Wound wound = WoundChart.getWound(damage, DamageType.ELECTRIC, getTarget(), alterationExplanationBuffer);
         StringBuilder damageExplanationButtfer = new StringBuilder();
         damageExplanationButtfer.append(getTargetName()).append(" takes damage from the ").append(getName()).append(" spell: ").append(wound.describeEffects());
         if(alterationExplanationBuffer.length()>0) {
            damageExplanationButtfer.append(", which is modified by ").append(alterationExplanationBuffer.toString());
         }
         arena.sendMessageTextToAllClients(damageExplanationButtfer.toString(), false/*popUp*/);
         getTarget().applyWound(wound, arena);
      }
   }

   public byte getDamageAmount() {
      return (byte) (_castingEffectiveness * getPower());
   }

   @Override
   public short getRangeBase() {
      return 5;
   }
   @Override
   public byte getRangeDefenseAdjustmentToPD(short distanceInHexes) {
      return 0;
   }
   @Override
   public byte getRangeDefenseAdjustmentPerAction(short distanceInHexes) {
      return 0;
   }

}
