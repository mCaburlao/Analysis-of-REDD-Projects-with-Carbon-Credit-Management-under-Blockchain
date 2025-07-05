package jabs.network.networks.bsc;

import jabs.consensus.algorithm.AbstractChainBasedConsensus;
import jabs.consensus.config.ChainBasedConsensusConfig;
import jabs.ledgerdata.ethereum.EthereumBlock;
import jabs.ledgerdata.ethereum.EthereumTx;
import jabs.network.networks.GlobalProofOfWorkNetwork;
import jabs.network.stats.ProofOfWorkGlobalNetworkStats;
import jabs.network.node.nodes.Node;
import jabs.network.node.nodes.MinerNode;
import jabs.network.node.nodes.ethereum.EthereumNode;
import jabs.network.node.nodes.ethereum.EthereumMinerNode;
import jabs.network.node.nodes.PeerBlockchainNode;
import jabs.simulator.randengine.RandomnessEngine;
import jabs.simulator.Simulator;
import jabs.consensus.algorithm.Parlia;
import jabs.consensus.blockchain.LocalBlockTree;
import jabs.simulator.event.ParliaBlockProposalEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static jabs.network.stats.eightysixcountries.ethereum.EthereumProofOfWorkGlobalNetworkStats86Countries.ETHEREUM_DIFFICULTY_2022;

public class ParliaGlobalBlockchainNetwork<R extends Enum<R>> extends
        GlobalProofOfWorkNetwork<EthereumNode, EthereumMinerNode, EthereumBlock, R> {

    private final int turnLength;
    private final int epochLength;

    public ParliaGlobalBlockchainNetwork(RandomnessEngine randomnessEngine,
            ProofOfWorkGlobalNetworkStats<R> networkStats,
            int turnLength,
            int epochLength) {
        super(randomnessEngine, networkStats);
        this.turnLength = turnLength;
        this.epochLength = epochLength;
    }

    @Override
    public EthereumBlock genesisBlock(double difficulty) {
        return new EthereumBlock(0, 0, 0, null, null, new HashSet<>(), difficulty, 0);
    }

    @Override
    public EthereumNode createSampleNode(Simulator simulator, int nodeID, EthereumBlock genesisBlock,
            ChainBasedConsensusConfig consensusAlgorithmConfig) {
        R region = this.sampleRegion();
        Parlia<EthereumBlock, EthereumTx> consensus = new Parlia<>(
                new LocalBlockTree<>(genesisBlock),
                turnLength, epochLength);
        // Add all validators to consensus
        for (MinerNode validator : this.getAllMiners()) {
            consensus.addValidator((EthereumMinerNode) validator);
        }

        return new EthereumNode(simulator, this, nodeID, this.sampleDownloadBandwidth(region),
                this.sampleUploadBandwidth(region),
                consensus);
    }

    @Override
    public EthereumMinerNode createSampleMiner(Simulator simulator, int nodeID, double hashPower,
            EthereumBlock genesisBlock,
            ChainBasedConsensusConfig consensusAlgorithmConfig) {
        R region = this.sampleRegion();
        Parlia<EthereumBlock, EthereumTx> consensus = new Parlia<>(
                new LocalBlockTree<>(genesisBlock),
                turnLength, epochLength);
        // Add all validators to consensus
        for (MinerNode validator : this.getAllMiners()) {
            consensus.addValidator((EthereumMinerNode) validator);
        }

        EthereumMinerNode node = new EthereumMinerNode(simulator, this, nodeID, this.sampleDownloadBandwidth(region),
                this.sampleUploadBandwidth(region), hashPower,
                consensus);
        consensus.addValidator(node);
        // Parlia is PoA, so mining is not used; just return a normal node
        return node;
    }

    public void startBlockProposal(Simulator simulator, Node validator) {
        // Schedule the first block proposal event at simulation time 0
        simulator.putEvent(
                new ParliaBlockProposalEvent(
                        validator,
                        simulator),
                0);
    }
}
