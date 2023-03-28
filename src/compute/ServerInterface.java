package compute;

import java.rmi.Remote;
import java.rmi.RemoteException;
import server.AckType;
import java.util.*;

/**
 * Interface for Server class
 */
public interface ServerInterface extends Remote {
    /**
     * Prepare the request sent with two-phase commit protocol
     * @param msgId Unique message id from Client
     * @param requestType request type from Client
     * @param key key sent from Client class
     * @param value value sent from Client class
     * @return final status of the requests operations
     * @throws RemoteException status of the update operations
     */
    String KeyValue(UUID msgId, String requestType, String key, String value) throws RemoteException;

    /**
     * Add messages to be updated to update queue
     * @param msgID Unique id of the message from Client class
     * @param requestType request type from Client class
     * @param key key sent from Client class
     * @param value value sent from Client class
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void prepareKeyValue(UUID msgID, String requestType, String key, String value, int currServer) throws RemoteException;

    /**
     * Records the current server and other replica servers
     * @param OtherServersPorts replica servers
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void setServersInfo(int[] OtherServersPorts, int currServer) throws RemoteException;

    /**
     * Process messages in the queue through two-phase commit protocol
     * @param msgId Unique id of the message from Client class
     * @param currPort current port running
     * @param type acknowledgement type
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void ackMe(UUID msgId, int currPort, AckType type) throws RemoteException;

    /**
     * Send message for commit via go
     * @param msgId Unique id of the message from Client class
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void go(UUID msgId, int currServer) throws RemoteException;
}
