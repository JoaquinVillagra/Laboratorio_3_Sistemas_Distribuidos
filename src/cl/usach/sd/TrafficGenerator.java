package cl.usach.sd;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class TrafficGenerator implements Control {
	private final static String PAR_PROT = "protocol";
	private final int layerId;

	public TrafficGenerator(String prefix) {
		layerId = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	@Override
	public boolean execute() {
		Node initNode = Network.get(CommonState.r.nextInt(Network.size())); 
		int sendNode = CommonState.r.nextInt(Network.size());
		Message message = new Message(0); 
		EDSimulator.add(0, message, initNode, layerId);
		return false;
	}

}
