package ostrowski.combat.common.things;

import java.util.ArrayList;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.SkillType;

public class Tool extends Thing
{
   public Tool() {}
   public Tool(String name, Race racialBase) {
      super(name, racialBase, 0, 0, (byte)0);
   }
   @Override
   public Tool clone() {
      return new Tool(_name, getRacialBase());
   }

   @Override
   public String getActiveDefenseName() {
      return null;
   }

   @Override
   public byte getBestDefenseOption(Character wielder, LimbType useHand, boolean canUse2Hands, DamageType damType, boolean isGrappleAttack) {
      return 0;
   }

   @Override
   public String getDefenseName(boolean tensePast, Character defender) {
      return null;
   }

   @Override
   public ArrayList<SkillType> getDefenseSkillTypes() {
      return null;
   }

   @Override
   public boolean isReal() {
      return true;
   }
}
