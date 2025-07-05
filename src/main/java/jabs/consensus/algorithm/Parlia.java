package jabs.consensus.algorithm;

import jabs.consensus.blockchain.LocalBlockTree;
import jabs.consensus.config.ConsensusAlgorithmConfig;
import jabs.ledgerdata.SingleParentBlock;
import jabs.ledgerdata.*;
import jabs.ledgerdata.bsc.*;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.PeerBlockchainNode;
import jabs.simulator.Simulator;
import jabs.simulator.event.BlockFinalizationEvent;
import jabs.simulator.event.VoteDeliveryEvent;
import jabs.ledgerdata.BlockFactory;
import jabs.network.node.nodes.ethereum.EthereumMinerNode;
import jabs.ledgerdata.ethereum.EthereumBlock;
import java.util.HashSet;

import java.util.*;

/**
 * Parlia consensus algorithm for JABS simulator (BSC-style PoA with fast
 * finality voting).
 * This is a simplified simulation version, not a full protocol implementation.
 *
 * @param <B> Block type
 * @param <T> Transaction type
 */
public class Parlia<B extends SingleParentBlock<B>, T extends Tx<T>>
        extends AbstractChainBasedConsensus<B, T>
        implements VotingBasedConsensus<B, T>, DeterministicFinalityConsensus<B, T> {

    private final List<Node> validators;
    private final int turnLength;
    private final int epochLength;
    private final Set<B> finalizedBlocks = new HashSet<>();
    private final Set<T> finalizedTxs = new HashSet<>();
    private int currentTurn = 0;

    // For fast finality voting (simplified)
    private final Map<B, Set<Node>> votes = new HashMap<>();
    // Track traffic for each block (in bytes)
    private final Map<B, Long> blockTraffic = new HashMap<>();

    public Parlia(LocalBlockTree<B> localBlockTree, int turnLength, int epochLength) {
        super(localBlockTree);
        this.validators = new ArrayList<>();
        this.turnLength = turnLength;
        this.epochLength = epochLength;
        this.currentMainChainHead = localBlockTree.getGenesisBlock();
        this.finalizedBlocks.add(localBlockTree.getGenesisBlock());
    }

    public int getNumOfStakeholders() {
        return this.validators.size();
    }

    public int getTurnLength() {
        return this.turnLength;
    }

    public void addValidator(Node validator) {
        if (!validators.contains(validator)) {
            validators.add(validator);
        }
    }

    // Add this method to Parlia
    public void addBlockTraffic(B block, long bytes) {
        blockTraffic.put(block, blockTraffic.getOrDefault(block, 0L) + bytes);
    }

    /**
     * Called when a new block is received.
     */
    @Override
    public void newIncomingBlock(B block) {
        // Accept block if valid and from correct proposer
        if (isBlockValid(block)) {
            // System.out.println("[Parlia] Block received and accepted: height=" + block.getHeight() +
            //                    ", proposer=" + block.getCreator().nodeID);
            confirmedBlocks.add(block);
            this.currentMainChainHead = block;
            if (block instanceof BlockWithTx) {
                confirmedTxs.addAll(((BlockWithTx<T>) block).getTxs());
            }
            // Optionally, trigger voting for finality
            startFinalityVote(block);
        } else {
            // System.out.println("[Parlia] Block rejected: height=" + block.getHeight() +
            //                    ", proposer=" + block.getCreator().nodeID);
        }
    }

    /**
     * Called when a new vote is received (for fast finality).
     */
    public void newIncomingVote(Vote vote) {
        if (vote instanceof BSCBlockVote) {
            BSCBlockVote<B> bscVote = (BSCBlockVote<B>) vote;
            // For simulation, assume vote contains block reference and voter
            if (bscVote.getBlock() instanceof SingleParentBlock) {
                B block = (B) bscVote.getBlock();
                Node voter = bscVote.getVoter();
                votes.computeIfAbsent(block, k -> new HashSet<>()).add(voter);
                // System.out.println("[Parlia] Vote received: blockHeight=" + block.getHeight() +
                //                    ", voter=" + voter.nodeID +
                //                    ", totalVotes=" + votes.get(block).size());
                // Finalize if 2/3+ votes
                if (votes.get(block).size() > ((getNumOfStakeholders() * 2) / 3)) {
                    // System.out.println("[Parlia] Block eligible for finalization: height=" + block.getHeight());
                    finalizeBlock(block);
                }
            }
        }
    }

    /**
     * Determines if a block is valid (proposed by correct validator in turn).
     */
    @Override
    public boolean isBlockValid(B block) {
        int blockHeight = block.getHeight();
        int proposerIndex = (blockHeight / turnLength) % validators.size();
        Node expectedProposer = validators.get(proposerIndex);
        return block.getCreator().equals(expectedProposer);
    }

    /**
     * Returns true if the block is confirmed (accepted by this node).
     */
    @Override
    public boolean isBlockConfirmed(B block) {
        return confirmedBlocks.contains(block);
    }

    /**
     * Returns true if the transaction is confirmed (in a confirmed block).
     */
    @Override
    public boolean isTxConfirmed(T tx) {
        return confirmedTxs.contains(tx);
    }

    /**
     * Returns true if the block is finalized (2/3+ votes).
     */
    @Override
    public boolean isBlockFinalized(B block) {
        return finalizedBlocks.contains(block);
    }

    /**
     * Returns true if the transaction is finalized (in a finalized block).
     */
    @Override
    public boolean isTxFinalized(T tx) {
        return finalizedTxs.contains(tx);
    }

    @Override
    public int getNumOfFinalizedBlocks() {
        return finalizedBlocks.size();
    }

    @Override
    public int getNumOfFinalizedTxs() {
        return finalizedTxs.size();
    }

    @Override
    protected void updateChain() {
        // Called after new block is added to the chain
        // For simulation, finalize latest block if enough votes
        B head = this.currentMainChainHead;
        if (votes.containsKey(head) && votes.get(head).size() > ((getNumOfStakeholders() * 2) / 3)) {
            finalizeBlock(head);
        }
    }

    private void startFinalityVote(B block) {
        // In real Parlia, validators sign and broadcast votes for fast finality
        // // For simulation, auto-vote by this node if validator
        // Node self = this.peerBlockchainNode;
        // if (validators.contains(self)) {
        // newIncomingVote(new BSCCommitVote(self, block));
        // }

        // Simulate each validator voting for the block, with a small random delay
        for (Node validator : validators) {
            double voteDelay = 0.1 + Math.random() * 0.5; // 0.1-0.6s delay for realism
            if (this.peerBlockchainNode != null && this.peerBlockchainNode.getSimulator() != null) {
                // System.out.println("[Parlia] Scheduling vote: blockHeight=" + block.getHeight() +
                //                    ", validator=" + validator.nodeID +
                //                    ", delay=" + voteDelay);
                this.peerBlockchainNode.getSimulator().putEvent(
                        new VoteDeliveryEvent<B>(validator, block, this), // <-- add <B>
                        voteDelay);
            }
        }
    }

    private void finalizeBlock(B block) {
        if (!finalizedBlocks.contains(block)) {
            finalizedBlocks.add(block);
            if (block instanceof BlockWithTx) {
                finalizedTxs.addAll(((BlockWithTx<T>) block).getTxs());
            }
            // System.out.println("[Parlia] Block finalized: height=" + block.getHeight() +
            //                    ", proposer=" + block.getCreator().nodeID);
           // Log traffic until finalization
            if (this.peerBlockchainNode != null && this.peerBlockchainNode.getSimulator() != null) {
                long traffic = blockTraffic.getOrDefault(block, 0L);
                //  System.out.println("[Parlia] Block finalized traffic =" + traffic);
                this.peerBlockchainNode.getSimulator().putEvent(
                        new BlockFinalizationEvent(
                                this.peerBlockchainNode.getSimulator().getSimulationTime(),
                                this.peerBlockchainNode,
                                block,
                                traffic),
                        0);
            }
        }
    }

    /**
     * Returns the next validator in round-robin order after the given node.
     */
    public Node getNextValidator(Node currentValidator) {
        if (validators.isEmpty()) {
            return null;
        }
        int idx = validators.indexOf(currentValidator);
        if (idx == -1) {
            // System.out.println("[Parlia] Not found " + currentValidator.nodeID + " in validators list");
            return validators.get(0);
        }
        // System.out.println("[Parlia] Next validator after " + currentValidator.nodeID + " is "
        //         + validators.get((idx + 1) % validators.size()).nodeID + " from " + validators.size() + " validators");
        return validators.get((idx + 1) % validators.size());
    }

    /**
     * Called by ParliaBlockProposalEvent to propose a block.
     * This should create a new block, add it to the local chain, and broadcast it.
     */
    public void proposeBlock(Node validator, Simulator simulator) {
        // Only allow if it's this validator's turn
        int height = currentMainChainHead.getHeight() + 1;
        int proposerIndex = (height / turnLength) % validators.size();
        if (!validators.get(proposerIndex).equals(validator)) {
            // System.out.println("[Parlia] Not " + validator.nodeID + "'s turn to propose at height " + height);
            return; // Not this validator's turn
        }

        // Replace createNextBlock with BlockFactory.sampleEthereumBlock
        EthereumMinerNode creator = (EthereumMinerNode) validator;
        creator.generateNewBlock();

        // EthereumBlock parent = (EthereumBlock) currentMainChainHead;
        // double weight = creator.getNetwork().getRandom().sampleExponentialDistribution(1);
        // // You may want to pass actual uncles, here we use an empty set for simplicity
        // EthereumBlock newBlock = BlockFactory.sampleEthereumBlock(
        //         simulator,
        //         creator.getNetwork().getRandom(),
        //         creator,
        //         parent,
        //         new HashSet<>(),
        //         weight);
        // System.out.println("[Parlia] Block proposed: height=" + (parent.getHeight() + 1) +
        //                    ", proposer=" + creator.nodeID);
        // Accept the block locally
        // this.newIncomingBlock((B) newBlock);

        // Optionally, broadcast the block to other validators (simulate network
        // propagation)
        // for (Node node : validators) {
        // if (!node.equals(validator)) {
        // node.receiveBlock(newBlock); // Implement receiveBlock in your node class if
        // needed
        // }
        // }
    }
}