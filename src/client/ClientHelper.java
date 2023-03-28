package client;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class provides helper function
 * for Client class
 */
public class ClientHelper {
	private static Logger LOGGER = LogManager.getLogger(ClientHelper.class.getName());

	public int[] serverPorts = new int[5];
	private int[] ports = {1234, 2345, 3451, 1112, 1113};

	/**
	 * Read in ports
	 */
	public void ClientParseArgs() {
		 for (int i = 0 ; i < 5 ; i++) {
			 serverPorts[i] = ports[i];
		 }
	 }

	/**
	 * Log all client requests using log4j
	 * @param message client requests
	 */
	public void log(String message) {
		 LOGGER.info(message);
	 }
}
