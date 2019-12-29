package ostrowski.combat.common.spells.priest.nature.plant;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;

public class SpellEntangle extends ResistedPriestSpell
{
   public static final String NAME = "Entangle";
   public SpellEntangle() {}

   public SpellEntangle(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Strength, (byte) 3/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }

   @Override
   public String describeSpell()
   {
      return "The '" + getName() + "' spell causes any plant matter nearby the subject to reach out and grab him or her." +
             " The plant matter will grow in size and strength to reach and hold the subject." +
             " The first turn that the spell is cast, the plant has a size equal to the casting effectiveness of the spell (the caster's success roll)." +
             " The subject rolls their resistance roll against this size. If they match or exceed the spell's size, they are able to move away." +
             " If the subject fails their resistance roll (or they do not move away for any reason), their legs become entangled, unable to move." +
             " Every turn that the subject is entangled, the plants continues to grow and grab the subject more completely." +
             " Each turn that the spell has a subject entangled, its size increases by the effective power of the spell." +
             " At the start of each turn, the subject may make a resistance roll to free themselves, however, "+
             "the TN to match is equal to the increasing size of the plant, making it harder and harder for the subject to escape." +
             " Chopping the plant with a cutting weapon will decrease its size by the amount of damage delivered." +
             " One the subject fails their resistance roll by 10 or more points, they are unable to move at all, and the " +
             "plant stops growing, but the subject is no longer able to make resistance attempts." +
             " The subject will either need to be cut out of the plant mass, or wait for the spell the end." +
             " Chopping at a spell to free someone from the spell can often harm the subject." +
             " Every time someone swings a blade at the plant, roll a d4. If the die roll is a 1, the subject is hit instead of the plant." +
             " Helpers may choose to 'pull their punches' by only swinging for reduced damage, but this needs to be declared before the attack is made.";
   }

}
