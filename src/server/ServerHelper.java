package server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class provides helper functions for Server class
 */
public class ServerHelper {
	private static Logger LOGGER = LogManager.getLogger(ServerHelper.class.getName());

	public int[] servers = new int[5];
	int[] ports = {1234, 2345, 3451, 1112, 1113};

	/**
	 * Read in ports
	 */
	public void ServerParseArgs() {
	 	 for (int i = 0 ; i < 5 ; i++) {
	 	    servers[i] = ports[i];
	 	 }
	 }

	/**
	 * Log server changes
	 * @param message server changes
	 */
	public void log(String message) {
	 	  LOGGER.info(message);
	 }
}
