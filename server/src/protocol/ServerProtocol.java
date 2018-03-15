package protocol;

/**
 * A protocol that describes the behabiour of the server.
 */
public interface ServerProtocol<T> {

    /**
     * processes a message
     * @param msg the message to process
     * @param callback an instance of ProtocolCallback unique to the connection from which
     * 			msg originated.
     */
    void processMessage(T msg, ProtocolCallback<T> callback);

    /**
     * detetmine whether the given message is the termination message
     * @param msg the message to examine
     * @return true if the message is the termination message, false otherwise
     */
    boolean isEnd(T msg);

}
