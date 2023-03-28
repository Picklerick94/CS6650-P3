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
     * Process client requests and return GET/PUT/DELETE values accordingly
     * @param msgID Unique id of the message from Client class
     * @param requestType request type from Client class
     * @param key key sent from Client class
     * @param value value sent from Client class
     * @return GET/PUT/DELETE values accordingly
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    String KeyValue(UUID msgID, String requestType, String key, String value) throws RemoteException;

    /**
     *
     * @param msgID Unique id of the message from Client class
     * @param requestType request type from Client class
     * @param key key sent from Client class
     * @param value value sent from Client class
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void prepareKeyValue(UUID msgID, String requestType, String key, String value, int currServer) throws RemoteException;

    /**
     *
     * @param OtherServersPorts replica servers
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void setServersInfo(int[] OtherServersPorts, int currServer) throws RemoteException;

    /**
     *
     * @param msgId Unique id of the message from Client class
     * @param currServer current port running
     * @param type acknowledgement type
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void ackMe(UUID msgId, int currServer, AckType type) throws RemoteException;

    /**
     *
     * @param msgId Unique id of the message from Client class
     * @param currServer current port running
     * @throws RemoteException exceptions occur during the execution of a rpc
     */
    void go(UUID msgId, int currServer) throws RemoteException;
}
