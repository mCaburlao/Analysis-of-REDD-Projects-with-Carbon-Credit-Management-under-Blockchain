package jabs.simulator.event;

import jabs.network.node.nodes.Node;
import jabs.ledgerdata.bsc.BSCCommitVote;
import jabs.ledgerdata.SingleParentBlock;
import jabs.consensus.algorithm.Parlia;

public class VoteDeliveryEvent<B extends SingleParentBlock<B>> implements Event {
    private final Node validator;
    private final B block;
    private final Parlia<B, ?> consensus;

    public VoteDeliveryEvent(Node validator, B block, Parlia<B, ?> consensus) {
        this.validator = validator;
        this.block = block;
        this.consensus = consensus;
    }

    @Override
    public void execute() {
        consensus.newIncomingVote(new BSCCommitVote<>(validator, block));
    }
}