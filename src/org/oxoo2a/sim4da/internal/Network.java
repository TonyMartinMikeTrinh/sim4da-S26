package org.oxoo2a.sim4da.internal;

import org.oxoo2a.sim4da.Message;
import org.oxoo2a.sim4da.NetworkConnection;
import org.oxoo2a.sim4da.UnknownNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Network {

    private Network() {
    }

    private record Node (NetworkConnection nc, NodeProxy np ) {}
    private final Map<String,Node> nodes = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(Network.class);
    private static Network instance = null;
    public static Network getInstance() {
        if (instance == null) {
            synchronized (Network.class) {
                if (instance == null) {
                    instance = new Network();
                }
            }
        }
        return instance;
    }

    public void registerConnection(NetworkConnection networkConnection, NodeProxy nodeProxy) {
        logger.debug("Registering connection for " + networkConnection.NodeName());
        Node n = new Node(networkConnection, nodeProxy);
        nodes.put(networkConnection.NodeName(), n);
    }

    public List<NetworkConnection> getAllNetworkConnections () {
        List<NetworkConnection> ncs = new ArrayList<>(numberOfNodes());
        for (Node n : nodes.values()) {
            ncs.add(n.nc);
        }
        return ncs;
    }

    public int numberOfNodes() {
        return nodes.size();
    }

    public void send (Message message, NetworkConnection sender, String receiver_name ) throws UnknownNodeException {
        if (!nodes.containsKey(receiver_name)) {
            logger.error("Attempt to send message to non-existent node " + receiver_name);
            throw new UnknownNodeException(receiver_name);
        }
        MessageInTransit mit = new MessageInTransit(message, sender.NodeName());
        NodeProxy receiver = nodes.get(receiver_name).np;
        receiver.deliver(mit, sender);
    }

    public void send ( Message message, NetworkConnection sender ) {
        for (Node n : nodes.values()) {
            if (n.nc != sender) {
                MessageInTransit mit = new MessageInTransit(message, sender.NodeName());
                n.np.deliver(mit, sender);
            }
        }
    }

    public MessageInTransit receive(NetworkConnection receiver) {
        Node n = nodes.get(receiver.NodeName());
        MessageInTransit mit = n.np.receive();
        return mit;
    }

    public void shutdown() {
    }
}
