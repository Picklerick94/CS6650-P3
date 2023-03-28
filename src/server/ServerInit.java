package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import compute.ServerInterface;

/**
 * This class is initiate the servers
 */
public class ServerInit extends Thread {
	static ServerHelper serverHelper = new ServerHelper();
	static Server[] servers = new Server[5];

	/**
	 * Register current server and initiate
	 * synchronization of replica servers
	 * @param servers replica servers
	 * @param port current server
	 */
	private static void registerServers(int[] servers, int port) {
		try {
			Registry registry = LocateRegistry.getRegistry(port);
			ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");

			int curr = 0;
			int[] other = new int[servers.length -1];
			for (int i = 0 ; i < servers.length ; i++) {
				if (servers[i] != port) {
					other[curr] = servers[i];
					curr++;
				}
			}

			stub.setServersInfo(other, port);
		} catch(Exception ex) {
			serverHelper.log("Failed to connect to server: " + port);
			serverHelper.log(ex.getMessage());
		}
	}
	
	public static void main(String args[]) throws Exception {
		 serverHelper.ServerParseArgs();
		 for (int i = 0 ; i < serverHelper.servers.length ; i++) {
			 try {
				 servers[i] = new Server();
				 ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(servers[i], 0);
				 Registry registry = LocateRegistry.createRegistry(serverHelper.servers[i]);
				 registry.bind("compute.ServerInterface", stub);
				 registerServers(serverHelper.servers, serverHelper.servers[i]);
				 serverHelper.log(String.format("Server %s is running at port %s", new String[] {Integer.toString(i), Integer.toString(serverHelper.servers[i])}));
			 } catch (Exception e) {
				 serverHelper.log("Server exception: " + e.getMessage());
			 }
	    		
			 Thread serverThread = new Thread();
			 serverThread.start();
		 }
	 }
}
