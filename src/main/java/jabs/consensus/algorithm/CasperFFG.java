package jabs.consensus.algorithm;

import jabs.consensus.blockchain.LocalBlockTree;
import jabs.consensus.config.CasperFFGConfig;
import jabs.ledgerdata.*;
import jabs.ledgerdata.casper.CasperFFGLink;
import jabs.ledgerdata.casper.CasperFFGVote;
import jabs.network.message.Packet;
import jabs.network.message.VoteMessage;
import jabs.network.node.nodes.Node;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import jabs.simulator.event.BlockFinalizationEvent;

import java.util.*;

/**
 * @param <B>
 * @param <T>
 */
public class CasperFFG<B extends SingleParentBlock<B>, T extends Tx<T>> extends GhostProtocol<B, T>
        implements VotingBasedConsensus<B, T>, DeterministicFinalityConsensus<B,T> {
    private final HashMap<CasperFFGLink<B>, HashMap<Node, CasperFFGVote<B>>> votes = new HashMap<>();
    private final SortedSet<B> justifiedBlocks = new TreeSet<>();
    private final SortedSet<B> finalizedBlocks = new TreeSet<>();

    private final Set<B> indirectlyFinalizedBlocks = new HashSet<>();
    private final Set<T> finalizedTxs = new HashSet<>();
    private final Map<B, Long> blockTraffic = new HashMap<>();

    private final int checkpointSpace;
    private final int numOfStakeholders;
    private int latestCheckpoint = 0;

    private DescriptiveStatistics blockFinalizationTimes = null;

    /**
     * @param localBlockTree
     * @param casperFFGConfig
     */
    public CasperFFG(LocalBlockTree<B> localBlockTree, CasperFFGConfig casperFFGConfig) {
        super(localBlockTree, casperFFGConfig);
        this.checkpointSpace = casperFFGConfig.checkpointSpace();
        this.numOfStakeholders = casperFFGConfig.numOfStakeholders();
        this.justifiedBlocks.add(localBlockTree.getGenesisBlock());
        this.finalizedBlocks.add(localBlockTree.getGenesisBlock());
        this.indirectlyFinalizedBlocks.add(localBlockTree.getGenesisBlock());
        // System.out.println("[CasperFFG] Initialized with checkpointSpace=" + checkpointSpace +
        //         ", numOfStakeholders=" + numOfStakeholders);
    }

    /**
     * @param blockFinalizationTimes
     */
    public void enableFinalizationTimeRecords(DescriptiveStatistics blockFinalizationTimes) {
        this.blockFinalizationTimes = blockFinalizationTimes;
    }

    // Add this method to allow traffic tracking from network code
    public void addBlockTraffic(B block, long bytes) {
        blockTraffic.put(block, blockTraffic.getOrDefault(block, 0L) + bytes);
    }

    public void newIncomingVote(Vote vote) {
        if (vote instanceof CasperFFGVote) {
            CasperFFGVote<B> casperFFGVote = (CasperFFGVote<B>) vote;
            CasperFFGLink<B> casperFFGLink = casperFFGVote.getLink();
            //  System.out.println("[CasperFFG] Received vote from node " + casperFFGVote.getVoter().nodeID +
            //         " for link: " + casperFFGLink.getToBeFinalized().getHeight() + "→" +
            //         casperFFGLink.getToBeJustified().getHeight());
            if (localBlockTree.areBlocksConnected(casperFFGLink.getToBeJustified(), casperFFGLink.getToBeFinalized())) {
                if (!votes.containsKey(casperFFGLink)) { // first vote for the link
                    votes.put(casperFFGLink, new HashMap<>());
                }
                votes.get(casperFFGLink).put(casperFFGVote.getVoter(), casperFFGVote);
                // System.out.println("[CasperFFG] Link votes for " +
                //         casperFFGLink.getToBeFinalized().getHeight() + "→" +
                //         casperFFGLink.getToBeJustified().getHeight() + ": " +
                //         votes.get(casperFFGLink).size() + " / " +
                //         (((numOfStakeholders / 3) * 2) + 1));
                if (votes.get(casperFFGLink).size() > (((numOfStakeholders / 3) * 2) + 1)) {
                    justifiedBlocks.add(casperFFGLink.getToBeJustified());
                    // System.out.println("[CasperFFG] Block justified: " +
                    //         casperFFGLink.getToBeJustified().getHeight());
                    if (!finalizedBlocks.contains(casperFFGLink.getToBeFinalized())) {
                        // System.out.println("[CasperFFG] Finalizing block: " +
                        //         casperFFGLink.getToBeFinalized().getHeight());
                        updateFinalizedBlocks(casperFFGLink.getToBeFinalized());
                        finalizedBlocks.add(casperFFGLink.getToBeFinalized());
                        if (this.originOfGhost.getHeight() < casperFFGLink.getToBeFinalized().getHeight()) {
                            this.originOfGhost = casperFFGLink.getToBeFinalized();
                        }
                    }
                }
            } else {
                // System.out.println("[CasperFFG] Blocks not connected for link: " +
                //         casperFFGLink.getToBeFinalized().getHeight() + "→" +
                //         casperFFGLink.getToBeJustified().getHeight());
            }
        } else {
            // System.out.println("[CasperFFG] Received non-CasperFFG vote: " + vote);
        }
    }

    private void updateFinalizedBlocks(B newlyFinalizedBlock) {
        if (!indirectlyFinalizedBlocks.contains(newlyFinalizedBlock)) {
            indirectlyFinalizedBlocks.add(newlyFinalizedBlock);
            if (blockFinalizationTimes != null) {
                blockFinalizationTimes.addValue(peerBlockchainNode.getSimulator().getSimulationTime() - newlyFinalizedBlock.getCreationTime());
            }
            // Emit BlockFinalizationEvent for this block
            if (this.peerBlockchainNode != null && this.peerBlockchainNode.getSimulator() != null) {
                long traffic = blockTraffic.getOrDefault(newlyFinalizedBlock, 0L);
                this.peerBlockchainNode.getSimulator().putEvent(
                    new BlockFinalizationEvent(
                        this.peerBlockchainNode.getSimulator().getSimulationTime(),
                        this.peerBlockchainNode,
                        newlyFinalizedBlock,
                        traffic
                    ),
                    0
                );
            }
        }

        HashSet<B> ancestors = localBlockTree.getAllAncestors(newlyFinalizedBlock);

        for (B block:ancestors) {
            if (!indirectlyFinalizedBlocks.contains(block)) {
                indirectlyFinalizedBlocks.add(block);
                if (blockFinalizationTimes != null) {
                    blockFinalizationTimes.addValue(peerBlockchainNode.getSimulator().getSimulationTime() - block.getCreationTime());
                }
                if (block instanceof BlockWithTx) {
                    finalizedTxs.addAll(((BlockWithTx<T>) block).getTxs());
                }
                // Emit BlockFinalizationEvent for ancestor blocks
                if (this.peerBlockchainNode != null && this.peerBlockchainNode.getSimulator() != null) {
                    long traffic = blockTraffic.getOrDefault(block, 0L);
                    this.peerBlockchainNode.getSimulator().putEvent(
                        new BlockFinalizationEvent(
                            this.peerBlockchainNode.getSimulator().getSimulationTime(),
                            this.peerBlockchainNode,
                            block,
                            traffic
                        ),
                        0
                    );
                }
            }
        }
    }

    @Override
    protected void updateChain() {
        // System.out.println("[CasperFFG] Updating chain...");
        this.confirmedBlocks = this.localBlockTree.getAllAncestors(this.currentMainChainHead);
        if ((currentMainChainHead.getHeight() - checkpointSpace) > latestCheckpoint) {
            latestCheckpoint = currentMainChainHead.getHeight() - (currentMainChainHead.getHeight() % checkpointSpace);

            B toBeFinalizedBlock = finalizedBlocks.last();
            if (localBlockTree.areBlocksConnected(currentMainChainHead, justifiedBlocks.last())) {
                if (finalizedBlocks.last().getHeight() < justifiedBlocks.last().getHeight()) {
                    toBeFinalizedBlock = justifiedBlocks.last();
                }
            } else {
                for (B jBlock:justifiedBlocks) {
                    if (localBlockTree.areBlocksConnected(currentMainChainHead, jBlock)) {
                        if (finalizedBlocks.last().getHeight() < jBlock.getHeight()) {
                            toBeFinalizedBlock = jBlock;
                        }
                    }
                }
            }

            B toBeJustifiedBlock = localBlockTree.getAncestorOfHeight(currentMainChainHead, latestCheckpoint);

            if (!localBlockTree.areBlocksConnected(toBeJustifiedBlock, toBeFinalizedBlock)) {
                System.out.print("What?\n");
            }

            CasperFFGLink<B> casperFFGLink = new CasperFFGLink<>(toBeFinalizedBlock, toBeJustifiedBlock);
            CasperFFGVote<B> casperFFGVote = new CasperFFGVote<>(this.peerBlockchainNode, casperFFGLink);
            VoteMessage voteMessage = new VoteMessage(casperFFGVote);
            Packet packet = new Packet(this.peerBlockchainNode, this.peerBlockchainNode, voteMessage);
            // System.out.println("[CasperFFG] Broadcasting vote for link: " +
            //         toBeFinalizedBlock.getHeight() + "→" + toBeJustifiedBlock.getHeight());
            this.peerBlockchainNode.processIncomingPacket(packet);
        }
    }

    public int getNumOfJustifiedBlocks() {
        return this.justifiedBlocks.size();
    }

    @Override
    public boolean isBlockFinalized(B block) {
        return this.indirectlyFinalizedBlocks.contains(block);
    }

    @Override
    public boolean isTxFinalized(T tx) {
        return this.finalizedTxs.contains(tx);
    }

    @Override
    public int getNumOfFinalizedBlocks() {
        return this.indirectlyFinalizedBlocks.size();
    }

    @Override
    public int getNumOfFinalizedTxs() {
        return this.finalizedTxs.size();
    }
}
