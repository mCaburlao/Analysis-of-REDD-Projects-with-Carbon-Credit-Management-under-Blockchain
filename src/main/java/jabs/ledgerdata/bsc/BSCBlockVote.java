package jabs.ledgerdata.bsc;

import jabs.ledgerdata.Block;
import jabs.ledgerdata.Vote;
import jabs.network.node.nodes.Node;

public abstract class BSCBlockVote<B extends Block<B>> extends Vote {
    private final B block;
    // private final VoteType voteType;

    // public static final int PBFT_VOTE_SIZE_OVERHEAD = 10;

    // public enum VoteType {
    //     PRE_PREPARE,
    //     PREPARE,
    //     COMMIT
    // }

    protected BSCBlockVote(int size, Node voter, B block) {
        super(size, voter);
        this.block = block;
        // this.voteType = voteType;
    }

    // public VoteType getVoteType() {
    //     return this.voteType;
    // }
    public B getBlock() {
        return this.block;
    }
}
