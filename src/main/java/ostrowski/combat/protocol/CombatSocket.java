/*
 * Created on Dec 15, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.Rules;
import ostrowski.util.sockets.SocketConnector;

public abstract class CombatSocket extends SocketConnector
{
   public CombatSocket(String threadName) {
      super(threadName);
   }

   @Override
   public void diag(String message)
   {
      Rules.diag(message);
   }

}
