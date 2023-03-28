package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import compute.ServerInterface;

/**
 * Multi-threaded server class to handle multiple outstanding client requests at once.
 * This class is able to handle requests from multiple running instances of
 * client doing concurrent PUT, GET, and DELETE operations.
 * It is also able to sync the updates to all replica servers.
 */
public class Server extends Thread implements ServerInterface {
	/**
	 * This class store the request type and
	 * key value from Client
	 */
	class Value {
		String requestType;
		String key;
		String value;
	}

	/**
	 * This class identifies whether the message
	 * is two-phase committed
	 */
	class Ack {
		public boolean isAcked;
	}

	static ServerHelper serverHelper = new ServerHelper();
	private int[] otherServers = new int[4];
	private  int currPort;
	private  Map<UUID, Value> pendingChanges = Collections.synchronizedMap(new HashMap<>());
	private  Map<UUID,Map<Integer,Ack>> pendingPrepareAcks = Collections.synchronizedMap(new HashMap<>());
	private  Map<UUID,Map<Integer,Ack>> pendingGoAcks = Collections.synchronizedMap(new HashMap<>());
	ReadWriteLock rwl=new ReadWriteLock();

	/**
	 * Records the current server and other replica servers
	 * @param otherServersPorts replica servers
	 * @param currPort current server
	 * @throws RemoteException exceptions occur during the execution of a rpc
	 */
	public void setServersInfo(int[] otherServersPorts, int currPort) throws RemoteException {
		this.otherServers = otherServersPorts;
		this.currPort = currPort;
	}

	/**
	 * Use lock to update key value store for replica servers
	 * @param requestType request type from client
	 * @param key key from client
	 * @param value value from client
	 * @return status of the update operations
	 */
	public String KeyValue(String requestType, String key, String value) {
		String message= "";
		String fileName = "keyValueStore_" + currPort + ".txt";
		ReadWriteFile rwFile = new ReadWriteFile(fileName);

		try {
			if (requestType.equalsIgnoreCase("GET")) {
				serverHelper.log("GET key: " + key + " - from client: ");
				rwl.lockRead();
				message += key + " : " + rwFile.getKvStore(key);
				rwl.unlockRead();
			} else if (requestType.equalsIgnoreCase("PUT")) {
				serverHelper.log("Writing the key: " + key + " and value: " + value + " - from client: ");
				rwl.lockWrite();
				message += key + " : " + rwFile.putKVstore(key, value);
				rwl.unlockWrite();
			} else {
				serverHelper.log("Deleting "+key);
				rwl.lockWrite();
				message += key + " : " + rwFile.deleteKeyValue(key);
				rwl.unlockWrite();
			}
		} catch (Exception e) {
			serverHelper.log(e.getMessage());
		}

		return message;
	}

	/**
	 * Prepare the request sent with two-phase commit protocol
	 * @param msgId Unique message id from Client
	 * @param requestType request type from Client
	 * @param key key sent from Client class
	 * @param value value sent from Client class
	 * @return final status of the requests operations
	 * @throws RemoteException status of the update operations
	 */
	@Override
	public String KeyValue(UUID msgId, String requestType, String key, String value) throws RemoteException {
		if (requestType.equalsIgnoreCase("GET")) return KeyValue(requestType, key, value);

		addToTempStorage(msgId, requestType, key, value);

		tellToPrepare(msgId, requestType, key, value);
		boolean prepareSucceed = waitAckPrepare(msgId, requestType, key, value);
		if (!prepareSucceed) return "PREPARE FAILED";
		
		tellToGo(msgId);
		boolean goSucceed = waitToAckGo(msgId);
		if (!goSucceed) return "PREPARE FAILED";
		
		Value store = this.pendingChanges.get(msgId);
		
		if (store == null) throw new IllegalArgumentException("Store not found");
		
		String message = this.KeyValue(store.requestType, store.key, store.value);
		this.pendingChanges.remove(msgId);

		return message;
	}

	/**
	 * Prepare to commit message via go for two-phase commit protocol
	 * @param msgId Unique message id
	 * @return whether preparation succeeded
	 */
	private boolean waitToAckGo(UUID msgId) {
		int totalAck;
		int retry = 3;
		
		while (retry != 0) {
			try{
			  Thread.sleep(100);
			} catch(Exception ex) {
				serverHelper.log("Lock wait failed");
			}

			totalAck = 0;
			retry--;
			Map<Integer,Ack> map = this.pendingGoAcks.get(msgId);
			
			for (int server : this.otherServers) {
				if (map.get(server).isAcked) totalAck++;
				else callGo(msgId, server);
			}

			if (totalAck == 4) return true;
		}
		
		return false;
	}

	/**
	 * Prepare to attach acknowledgements to messages for two-phase commit protocol
	 * @param msgId Unique message id
	 * @param requestType request from Client
	 * @param key key from Client
	 * @param value value from Client
	 * @return whether preparation succeeded
	 */
	private boolean waitAckPrepare(UUID msgId, String requestType, String key, String value) {
		int totalAck;
		int retry = 3;
		
		while (retry != 0) {
			try {
			  Thread.sleep(100);
			} catch (Exception ex) {
				serverHelper.log("Lock wait failed");
			}

			totalAck = 0;
			retry--;
			Map<Integer,Ack> map = this.pendingPrepareAcks.get(msgId);

			for (int server : this.otherServers) {
				if (map.get(server).isAcked) totalAck++;
				else callPrepare(msgId, requestType, key, value, server);
			}
			
			if (totalAck == 4) return true;
		}
		
		return false;
	}

	/**
	 * Add new messages to attach acknowledgement
	 * @param msgId Unique message id
	 * @param requestType request from Client
	 * @param key key from Client
	 * @param value value from Client
	 */
	private void tellToPrepare(UUID msgId, String requestType, String key, String value) {
		this.pendingPrepareAcks.put(msgId, Collections.synchronizedMap(new HashMap<>()));
		
		for (int server : this.otherServers) {
			callPrepare(msgId, requestType, key, value, server);
		}
		
	}

	/**
	 * Add new messages to go
	 * @param msgId Unique message id
	 */
	private void tellToGo(UUID msgId) {
		this.pendingGoAcks.put(msgId, Collections.synchronizedMap(new HashMap<>()));
		
		for (int server : this.otherServers){
			callGo(msgId, server);
		}
	}

	/**
	 * Add messages to update queue for go message
	 * @param msgId unique message if
	 * @param server replica server
	 */
	private void callGo(UUID msgId, int server) {
		try {
			Ack a = new Ack();
			a.isAcked = false;
			this.pendingGoAcks.get(msgId).put(server, a);

			Registry registry = LocateRegistry.getRegistry(server);
			ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
		    stub.go(msgId, currPort);
		} catch (Exception ex) {
			serverHelper.log("Commit via go failed, remove data from temporary storage");
			this.pendingGoAcks.remove(msgId);
		}

		serverHelper.log("Commit via go succeeded. Target: " + server);
	}

	/**
	 * Add messages to update queue for attaching ack
	 * @param msgId unique message id
	 * @param requestType request type from client
	 * @param key key from client
	 * @param value value from client
	 * @param server replica server
	 */
	private void callPrepare(UUID msgId, String requestType, String key, String value, int server) {
		try {
			Ack a = new Ack();
			a.isAcked = false;
			this.pendingPrepareAcks.get(msgId).put(server, a);

			Registry registry = LocateRegistry.getRegistry(server);
			ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
		    stub.prepareKV(msgId, requestType, key, value, currPort);
		} catch (Exception ex) {
			serverHelper.log("Attach ack failed, remove data from temporary storage");
			this.pendingPrepareAcks.remove(msgId);
		}

		serverHelper.log("Call prepare succeeded. Target: " + server);
	}

	/**
	 * Process messages in the queue through two-phase commit protocol
	 * @param msgId Unique id of the message from Client class
	 * @param currPort current port running
	 * @param type acknowledgement type
	 * @throws RemoteException exceptions occur during the execution of a rpc
	 */
	public void ackMe(UUID msgId, int currPort, AckType type) throws RemoteException{
		if (type == AckType.ackGo) {
        	 this.pendingGoAcks.get(msgId).get(currPort).isAcked = true;
         } else if (type == AckType.AkcPrepare) {
        	 this.pendingPrepareAcks.get(msgId).get(currPort).isAcked = true;
         }

		serverHelper.log("Ack received from: " + currPort);
	}

	/**
	 * Send message for commit via go
	 * @param msgId Unique id of the message from Client class
	 * @param currServer current port running
	 * @throws RemoteException exceptions occur during the execution of a rpc
	 */
	public void go(UUID msgId, int currServer) throws RemoteException {
		Value v = this.pendingChanges.get(msgId);
		
		if (v == null) throw new IllegalArgumentException("Message not found");
		
		this.KeyValue(v.requestType, v.key, v.value);
		this.pendingChanges.remove(msgId);
		this.sendAck(msgId, currServer, AckType.ackGo);
	}

	/**
	 * Add messages to be updated to update queue
	 * @param msgId Unique id of the message from Client class
	 * @param requestType request type from Client class
	 * @param key key sent from Client class
	 * @param value value sent from Client class
	 * @param currServer current port running
	 * @throws RemoteException exceptions occur during the execution of a rpc
	 */
	public void prepareKV(UUID msgId, String requestType, String key, String value, int currServer) throws RemoteException{
		if (this.pendingChanges.containsKey(msgId)) sendAck(msgId, currServer, AckType.AkcPrepare);
		
		this.addToTempStorage(msgId, requestType, key, value);
		sendAck(msgId, currServer, AckType.AkcPrepare);
	}

	/**
	 * Commit current port and sync with replica servers
	 * @param msgId Unique id of the message from Client class
	 * @param server replica server
	 * @param type AckGo or AckPrepare
	 */
	private void sendAck(UUID msgId, int server, AckType type) {
		try {
			Registry registry = LocateRegistry.getRegistry(server);
		    ServerInterface stub = (ServerInterface) registry.lookup("compute.ServerInterface");
		    
		    stub.ackMe(msgId, currPort, type);
		} catch(Exception ex) {
			serverHelper.log("Attach ack failed, remove data from temporary storage");
			this.pendingChanges.remove(msgId);
		}
	}

	/**
	 * Add temporary storage to the queue for udpates
	 * @param msgId Unique id of the message from Client class
	 * @param requestType request type from Client class
	 * @param key key sent from Client class
	 * @param value value sent from Client class
	 */
	private void addToTempStorage(UUID msgId, String requestType, String key, String value) {
		Value v = new Value();
		v.requestType = requestType;
		v.key = key;
		v.value = value;

		this.pendingChanges.put(msgId, v);
	}
}


