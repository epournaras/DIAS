package protopeer.network.delayloss;

import protopeer.network.Message;
import protopeer.network.NetworkAddress;

public class LossyUniformDelayModel extends UniformDelayModel {
	
	final double lossthreshold;

	public LossyUniformDelayModel(double minDelay, double maxDelay, double threshold) {
		super(minDelay, maxDelay);
		this.lossthreshold = threshold;
	}

	public boolean getLoss(NetworkAddress sourceAddress, NetworkAddress destinationAddress, Message message) {
		if(Math.random()<lossthreshold) return true;
		return false;
	}
}
