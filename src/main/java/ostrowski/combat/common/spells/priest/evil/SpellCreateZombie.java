package ostrowski.combat.common.spells.priest.evil;

import ostrowski.combat.common.Advantage;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.Skill;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableData;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellCreateZombie extends PriestSpell
{
   public static final String NAME = "Create Zombie";
   public SpellCreateZombie() {
   }
   public SpellCreateZombie(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power&nbsp;points<br/>spent&nbsp;on&nbsp;duration", "Duration"));
      for (int p=0 ; p<=7 ; p++) {
         table.addRow(new TableRow(p).addHeader("" + p)
                                     .addTD(new TableData(getDescriptionForpower(p)).setAlignLeft()));
      }
      return "The '" + getName() + "' spell reanimates one corpse."+
             " The zombie will obey the commands of its creator, even to the point of its own destruction." +
             " The attributes of the zombie will be equal to that of the creature when it was alive, except for IQ, which will be 5 points lower."+
             " However, no attribute level can be higher that the effective power of the spell." +
             " Similarly, the skills of the zombie will be equal to their living level, but will not exceed " +
             "the effective power of the spell times two. " +
             " Zombies do not feel pain, but are impeded by wounds the same as living creatures are." +
             " If the corpse used to create a zombie is missing any limbs, the zombie will also be missing the limb, "+
             "and will be hampered appropriately." +
             " A zombie is dispelled when its wounds level reaches 10, or the spell expires." +
             " Zombies normally stay around for 1 hour, however, spell points may be allocated away from skills and attributes " +
             "to prolong the zombies existence:" +
             table.toString() +
             "For example, given the corpse of a fallen warrior who had a STR of 4 and a DEX of 2, and a sword and skill level of 5:" +
             " A priest with 3 levels of divine power could raise this zombie with its STR at 3, and DEX at 2, and skill levels of 5 for 1 hour." +
             " The same caster could instead spend two of its effective power points on duration, raising the zombie with a STR and DEX of 1 and skill levels of 2 for 1 day." +
             " When animating large creatures, such as Ogres, Trolls, Giants, etc., the caster must spend 1 extra power point for every full 4 points of size" +
             " over the caster's size. For example, to raise an Ogre (racial size adjuster of +10), a priest must spend 2 points to allow for the size." +
             " So if a priest with 3 levels of divine power, raised an Ogre, the spell’s effective power would only be 1 point," +
             " giving it a maximum attribute level of 1, and a maximum skill level of 2, and could only stay around for 1 hour.<br/>" +
             " <b>Note</b>: While the maximum STR of this Ogre zombie is 1, its ASTR could still be as high as 11, due to its large size." +
             " Similarly, the Ogre zombie's BLD will still be 10 points higher than its HT attribute.";
   }
   private static String getDescriptionForpower(int p) {
      switch (p) {
         case 0: return "1 hour";
         case 1: return "5 hours";
         case 2: return "1 day";
         case 3: return "1 week";
         case 4: return "1 month";
         case 5: return "6 months";
         case 6: return "3 years";
         case 7: return "15 years";
      }
      return "";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_DEAD;
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }
   @Override
   public void applyEffects(Arena arena) {
      if ((_target != null) && (!_target.getCondition().isAlive())) {
         // lower IQ by 5.
         _target.setAttribute(Attribute.Intelligence, (byte) (_target.getAttributeLevel(Attribute.Intelligence)-5), false /*containInLimits*/);
         // max out all attributes at effective power.
         byte maxAtt = getEffectivePower();
         for (Attribute att : Attribute.values()) {
            if (_target.getAttributeLevel(att) > maxAtt) {
               _target.setAttribute(att, maxAtt, false /*containInLimits*/);
            }
         }
         // max out all skill at effective power * 2
         byte maxSkill = (byte) (getEffectivePower() * 2);
         for (Skill skill : _target.getSkillsList()) {
            if (skill.getLevel() > maxSkill) {
               skill.setLevel(maxSkill);
            }
         }
         // cure all the wounds on the character, except lost limbs.
         for (Wound wound : _target.getWoundsList()) {
            if (!wound.isSeveredArm() && !wound.isSeveredLeg()) {
               getTarget().cureWound(wound, (byte)10/*woundReduction*/, (byte)10/*bleedingReduction*/);
            }
         }
         _target.getCondition().raiseFromDead(true/*asZombie*/);
         _target._teamID = getCaster()._teamID;
         _target.addAdvantage(Advantage.getAdvantage(Advantage.NO_PAIN));
         _target.addAdvantage(Advantage.getAdvantage(Advantage.UNDEAD));
         // TODO: how do we attach this to a new AI engine, if it was a player character?
      }
   }
   @Override
   public void removeEffects(Arena arena) {
      // We don't need to do anything here, because the only way a zombie will be destroyed in
      // the simulator is if it's killed or knocked out. So we don't need to kill it again.
   }


}
