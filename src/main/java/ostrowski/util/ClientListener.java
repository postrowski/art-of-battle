/*
 * Created on May 15, 2006
 *
 */
package ostrowski.util;

import java.io.IOException;
import java.net.ServerSocket;

import ostrowski.combat.common.Rules;
import ostrowski.combat.server.ClientProxy;
import ostrowski.combat.server.CombatServer;
import ostrowski.combat.server.Configuration;

public class ClientListener extends Thread
{
   ServerSocket serverSocket;
   final CombatServer server;
   boolean keepRunning = true;
   public ClientListener(CombatServer server) {
      this.server = server;
   }

   @Override
   public void run()
   {
      Thread.currentThread().setName("ClientListener");
      ClientProxy clientThread;
      try {
         serverSocket = new ServerSocket(Configuration.serverPort());
      } catch (IOException e1) {
         e1.printStackTrace();
         return;
      }
      while (keepRunning) {
         try {
            clientThread = new ClientProxy(server.getArena());
            clientThread.setSocket(serverSocket.accept());
            Rules.diag("socket connection accepted.");
            clientThread.start();
         } catch (IOException e) {
         }
      }
   }
   public void closePort()
   {
      keepRunning = false;
      try {
         if (serverSocket != null) {
            serverSocket.close();
         }
      } catch (IOException e) {
      }
   }
}
