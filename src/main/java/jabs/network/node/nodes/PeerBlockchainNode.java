package jabs.network.node.nodes;

import jabs.consensus.algorithm.AbstractChainBasedConsensus;
import jabs.consensus.algorithm.Parlia;
import jabs.consensus.algorithm.CasperFFG;
import jabs.consensus.blockchain.LocalBlockTree;
import jabs.ledgerdata.*;
import jabs.network.message.*;
import jabs.network.networks.Network;
import jabs.network.p2p.AbstractP2PConnections;
import jabs.simulator.Simulator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class PeerBlockchainNode<B extends SingleParentBlock<B>, T extends Tx<T>> extends PeerDLTNode<B, T> {
    protected final AbstractChainBasedConsensus<B, T> consensusAlgorithm;

    protected final HashMap<Hash, T> alreadySeenTxs = new HashMap<>();
    protected final HashMap<Hash, B> alreadySeenBlocks = new HashMap<>();
    protected final HashSet<Vote> alreadySeenVotes = new HashSet<>();
    protected final HashSet<Query> alreadySeenQueries = new HashSet<>();
    protected final LocalBlockTree<B> localBlockTree;

    public PeerBlockchainNode(Simulator simulator, Network network, int nodeID, long downloadBandwidth,
                              long uploadBandwidth, AbstractP2PConnections routingTable,
                              AbstractChainBasedConsensus<B, T> consensusAlgorithm) {
        super(simulator, network, nodeID, downloadBandwidth, uploadBandwidth, routingTable, consensusAlgorithm);
        this.consensusAlgorithm = consensusAlgorithm;
        this.localBlockTree = consensusAlgorithm.getLocalBlockTree();
    }

    @Override
    public void processIncomingPacket(Packet packet) {
        Message message = packet.getMessage();
        if (message instanceof DataMessage) {
            // System.out.println("[PeerBlockchainNode] Received DataMessage: " + message);
            Data data = ((DataMessage) message).getData();
            if (data instanceof Block) {
                B block = (B) data;
                // Track traffic for this block (Parlia)
                if (this.consensusAlgorithm instanceof Parlia) {
                    ((Parlia<B, T>) this.consensusAlgorithm).addBlockTraffic(block, packet.getSize());
                }
                // Track traffic for this block (CasperFFG)
                if (this.consensusAlgorithm instanceof CasperFFG) {
                    ((CasperFFG<B, T>) this.consensusAlgorithm).addBlockTraffic(block, packet.getSize());
                }
                if (!localBlockTree.contains(block)){
                    localBlockTree.add(block);
                    alreadySeenBlocks.put(block.getHash(), block);
                    if (localBlockTree.getLocalBlock(block).isConnectedToGenesis) {
                        this.processNewBlock(block);
                        SortedSet<B> newBlocks = new TreeSet<>(localBlockTree.getAllSuccessors(block));
                        for (B newBlock:newBlocks){
                            this.processNewBlock(newBlock);
                        }
                    } else {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new RequestDataMessage(block.getParent().getHash())
                                )
                        );
                    }
                }
            } else if (data instanceof Tx) {
                T tx = (T) data;
                if (!alreadySeenTxs.containsValue(tx)){
                    alreadySeenTxs.put(tx.getHash(), tx);
                    this.processNewTx(tx, packet.getFrom());
                }
            }
        } else if (message instanceof InvMessage) {
            // System.out.println("[PeerBlockchainNode] Received InvMessage: " + message);
            Hash hash = ((InvMessage) message).getHash();
            if (hash.getData() instanceof Block){
                if (!alreadySeenTxs.containsKey(hash)) {
                    alreadySeenTxs.put(hash, null);
                    this.networkInterface.addToUpLinkQueue(
                            new Packet(this, packet.getFrom(),
                                    new RequestDataMessage(hash)
                            )
                    );
                }
            } else if (hash.getData() instanceof Tx) {
                if (!alreadySeenBlocks.containsKey(hash)) {
                    alreadySeenBlocks.put(hash, null);
                    this.networkInterface.addToUpLinkQueue(
                            new Packet(this, packet.getFrom(),
                                    new RequestDataMessage(hash)
                            )
                    );
                }
            }
        } else if (message instanceof RequestDataMessage) {
            // System.out.println("[PeerBlockchainNode] Received RequestDataMessage: " + message);
            Hash hash = ((RequestDataMessage) message).getHash();
            if (hash.getData() instanceof Block) {
                if (alreadySeenBlocks.containsKey(hash)) {
                    B block = alreadySeenBlocks.get(hash);
                    if (block != null) {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new DataMessage(block)
                                )
                        );
                    }
                }
            } else if (hash.getData() instanceof Tx) {
                if (alreadySeenTxs.containsKey(hash)) {
                    T tx = alreadySeenTxs.get(hash);
                    if (tx != null) {
                        this.networkInterface.addToUpLinkQueue(
                                new Packet(this, packet.getFrom(),
                                        new DataMessage(tx)
                                )
                        );
                    }
                }
            }
        } else if (message instanceof VoteMessage) {
            // System.out.println("[PeerBlockchainNode] Received VoteMessage: " + message);
            Vote vote = ((VoteMessage) message).getVote();
            if (!alreadySeenVotes.contains(vote)) {
                alreadySeenVotes.add(vote);
                this.processNewVote(vote);
            }
        }else if (message instanceof QueryMessage) {
            // System.out.println("[PeerBlockchainNode] Received QueryMessage: " + message);
            Query query = ((QueryMessage) message).getQuery();
            if (!alreadySeenQueries.contains(query)) {
                alreadySeenQueries.add(query);
                this.processNewQuery(query);
            }
        }
    }

    protected abstract void processNewBlock(B block);
    protected abstract void processNewVote(Vote vote);
    protected abstract void processNewQuery(Query query);

    public AbstractChainBasedConsensus<B, T> getConsensusAlgorithm() {
        return this.consensusAlgorithm;
    }

    public int numberOfAlreadySeenBlocks() {
        return alreadySeenBlocks.size();
    }
}
