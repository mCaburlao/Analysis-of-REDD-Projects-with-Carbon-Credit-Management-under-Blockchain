package jabs.simulator.event;

import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.PeerBlockchainNode;
import jabs.simulator.Simulator;
import jabs.consensus.algorithm.Parlia;
import jabs.ledgerdata.ethereum.EthereumBlock;

public class ParliaBlockProposalEvent implements Event {
    private final Node validator;
    private final Simulator simulator;

    public ParliaBlockProposalEvent(Node validator, Simulator simulator) {
        this.validator = validator;
        this.simulator = simulator;
    }

    public void execute() {
        Parlia<EthereumBlock, ?> parlia =
            (Parlia<EthereumBlock, ?>) ((PeerBlockchainNode<?, ?>) validator).getConsensusAlgorithm();
        parlia.proposeBlock(validator, simulator);

        Node nextValidator = parlia.getNextValidator(validator);
        // double nextTime = simulator.getSimulationTime() + parlia.getTurnLength();
        simulator.putEvent(new ParliaBlockProposalEvent(nextValidator, simulator), parlia.getTurnLength());
    }
}