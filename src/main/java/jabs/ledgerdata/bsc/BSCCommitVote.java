package jabs.ledgerdata.bsc;

import jabs.ledgerdata.Block;
import jabs.network.node.nodes.Node;

public class BSCCommitVote<B extends Block<B>> extends BSCBlockVote<B> {
    public BSCCommitVote(Node voter, B block) {
        super(block.getHash().getSize(), voter, block);
    }
}
