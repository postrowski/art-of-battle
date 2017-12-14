package ostrowski.combat.server;

import ostrowski.combat.common.enums.Enums;


public class ArenaEventTrap implements Enums
{
   static final int TRAP_FLAG_DODGABLE        = 1;
   static final int TRAP_FLAG_RETREATABLE     = 2;
   static final int TRAP_FLAG_BLOCKABLE       = 4;
   static final int TRAP_FLAG_PARRYABLE       = 8;
   static final int TRAP_FLAG_SPELL_BLOCKABLE = 16;
   
//   private byte _damageType   = DAM_GENERAL;
//   private int  _damageAmount = 0;
//   private int  _trapFlags    = TRAP_FLAG_DODGABLE | TRAP_FLAG_RETREATABLE | TRAP_FLAG_BLOCKABLE | TRAP_FLAG_PARRYABLE;
//
//   private String _trapDescription = "";

   public boolean equals(ArenaEventTrap other) {
      return true;
   }
}
