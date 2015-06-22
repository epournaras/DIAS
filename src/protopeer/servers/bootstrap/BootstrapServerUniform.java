package protopeer.servers.bootstrap;

import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.Peer;
import protopeer.PeerIdentifier;
import protopeer.network.Message;

public class BootstrapServerUniform extends BasePeerlet {

	private static final Logger logger = Logger.getLogger(BootstrapServer.class);

	private enum BootstrapServerState {
		WAIT_FOR_CORE_PEERS, COMPLETED
	}

	private BootstrapServerState state;

	private TopologyGenerator topologyGenerator;

	private int nextArrivalSequenceNum = 0;

	private LinkedList<BootstrapServerListener> listeners = new LinkedList<BootstrapServerListener>();

	private LinkedList<Finger> knownFingers = new LinkedList<Finger>();

	public void addListener(BootstrapServerListener listener) {
		listeners.add(listener);
	}

	private synchronized int getNextArrivalSequenceNum() {
		logger.debug("nextArrivalSequenceNum: " + nextArrivalSequenceNum);
		return nextArrivalSequenceNum++;
	}

	@Override
	public void init(Peer peer) {
		super.init(peer);
		this.topologyGenerator = new RingTopologyGeneratorUniform(getPeer());
		state = BootstrapServerState.WAIT_FOR_CORE_PEERS;
	}

	private synchronized void deliverBootstrapHello(BootstrapHello message) {
		if (logger.isDebugEnabled()) {
			logger.debug("BootstrapHello received: " + message);
		}

		Finger finger = new Finger(message.getNetworkAddress(), message.getPeerIdentifier());
		knownFingers.add(finger);

		// defer initialization until the minimum number of peers (core peers)
		// is available
		if (logger.isDebugEnabled()) {
			logger.debug("waiting for core peers to join, current: " + knownFingers.size() + ", expecting "
					+ topologyGenerator.getMinimalNumberOfPeers() + " peers ");
		}

		// check if there are enough nodes to begin the initialization
		if (state == BootstrapServerState.WAIT_FOR_CORE_PEERS) {
			if (knownFingers.size() >= topologyGenerator.getMinimalNumberOfPeers()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Have the required minimum of " + knownFingers.size() + " peers, initializing them");
				}

				// send init messages to core peers
				initializeCorePeers();
				state = BootstrapServerState.COMPLETED;
			}
		} else if (state == BootstrapServerState.COMPLETED) {
			initializeNonCorePeer(finger);
		}
	}

	private void initializeCorePeers() {
		// send the peers their neighbors and the IDs
		for (Finger finger : knownFingers) {
			PeerIdentifier peerIdentifier = finger.getIdentifier();
			Set<Finger> neighbors = topologyGenerator.getInitialNeighbors(knownFingers, peerIdentifier, true);
			int arrivalSeqNum = getNextArrivalSequenceNum();
			if (logger.isDebugEnabled()) {
				logger.debug("initializing core peer: " + finger.getNetworkAddress() + " seqnum: " + arrivalSeqNum);
			}
			getPeer().sendMessage(finger.getNetworkAddress(), new InitializePeerMessage(neighbors, arrivalSeqNum));
		}
	}

	private void initializeNonCorePeer(Finger finger) {
		PeerIdentifier peerIdentifier = finger.getIdentifier();
		Set<Finger> neighbors = topologyGenerator.getInitialNeighbors(knownFingers, peerIdentifier, false);
		int arrivalSeqNum = getNextArrivalSequenceNum();
		if (logger.isDebugEnabled()) {
			logger.debug("initializing non-core peer: " + finger.getNetworkAddress() + " seqnum: " + arrivalSeqNum);
		}
		getPeer().sendMessage(finger.getNetworkAddress(), new InitializePeerMessage(neighbors, arrivalSeqNum));
	}

	private synchronized void deliverInitializationAck(InitializationAck message) {
		// TODO: nothing here for now
		if (logger.isDebugEnabled()) {
			logger.debug("InitializationAck received: " + message);
		}
	}

	@Override
	public void handleIncomingMessage(Message message) {
		if (message instanceof BootstrapHello) {
			deliverBootstrapHello((BootstrapHello) message);
		} else if (message instanceof InitializationAck) {
			deliverInitializationAck((InitializationAck) message);
		}

	}

	@Override
	public void start() {
		super.start();
		if (logger.isDebugEnabled()) {
			logger.debug("BootstrapServer started");
		}
	}

}
