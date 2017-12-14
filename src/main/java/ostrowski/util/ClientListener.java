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
   ServerSocket _serverSocket;
   CombatServer _server               = null;
   boolean      _keepRunning          = true;
   public ClientListener(CombatServer server) {
      _server = server;
   }

   @Override
   public void run()
   {
      Thread.currentThread().setName("ClientListener");
      ClientProxy clientThread;
      try {
         _serverSocket = new ServerSocket(Configuration.serverPort());
      } catch (IOException e1) {
         e1.printStackTrace();
         return;
      }
      while (_keepRunning) {
         try {
            clientThread = new ClientProxy(_server.getArena());
            clientThread.setSocket(_serverSocket.accept());
            Rules.diag("socket connection accepted.");
            clientThread.start();
         } catch (IOException e) {
         }
      }
   }
   public void closePort()
   {
      _keepRunning = false;
      try {
         if (_serverSocket != null) {
            _serverSocket.close();
         }
      } catch (IOException e) {
      }
   }
}
