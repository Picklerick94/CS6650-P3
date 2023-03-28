package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;
import compute.ServerInterface;

/**
 * Client class using RMI for RPC communication
 */
public class Client {
	public static void main(String[] args) {
		ClientHelper clientHelper = new ClientHelper();
		clientHelper.ClientParseArgs();

		ServerInterface[] stubs = new ServerInterface[5];
		Registry[] registries = new Registry[5];

		try {
			for (int i = 0 ; i < clientHelper.serverPorts.length ; i++) {
				registries[i] = LocateRegistry.getRegistry("localhost", clientHelper.serverPorts[0]);
				stubs[i] = (ServerInterface) registries[i].lookup("compute.ServerInterface");
			}

			/**
			 * 5 PUT operations to replica servers
			 */
			for (int i = 0; i < 5; i++) {
				String key = "s" + (i + 1);
				String value = String.valueOf(i+1);
				clientHelper.log(stubs[i].KeyValue(UUID.randomUUID(), "PUT", key, value));
			}

			/**
			 * 5 GET operations to replica servers
			 */
			for (int i = 0; i < 5; i++) {
				String key = "s" + (i + 1);
				String value = "";
				clientHelper.log(stubs[i].KeyValue(UUID.randomUUID(), "GET", key, value));
			}

			/**
			 * 5 DELETE operations to replica servers
			 */
			for (int i = 0; i < 5; i++) {
				String key = "s" + (i + 1);
				String value = "";
				clientHelper.log(stubs[i].KeyValue(UUID.randomUUID(), "DEL", key, value));
			}

			for (int i = 0; i < 5; i++) {
				String key = "q" + (i + 1);
				String value = String.valueOf(i+1);
				clientHelper.log(stubs[i].KeyValue(UUID.randomUUID(), "PUT", key, value));
			}
		} catch (Exception e) {
			clientHelper.log(e.getMessage());
		}
	}
}
