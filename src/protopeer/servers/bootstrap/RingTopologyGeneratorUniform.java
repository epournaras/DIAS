package protopeer.servers.bootstrap;

import java.util.Collection;
import java.util.Set;

import protopeer.Finger;
import protopeer.Peer;
import protopeer.PeerIdentifier;

public class RingTopologyGeneratorUniform extends RingTopologyGenerator{

	public RingTopologyGeneratorUniform(Peer peer) {
		super(peer);
	}
	
	@Override
	public Set<Finger> getInitialNeighbors(Collection<Finger> knownFingers, PeerIdentifier peerIdentifier, boolean coreNode) {

			return getBootstrapNodesUniform(knownFingers, peerIdentifier);
	}

}
