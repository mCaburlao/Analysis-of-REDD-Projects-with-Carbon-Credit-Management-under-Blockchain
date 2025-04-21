package jabs.scenario;

// import jabs.consensus.config.GhostProtocolConfig;
import jabs.consensus.config.CasperFFGConfig;
// import jabs.ledgerdata.ethereum.EthereumBlock;
import jabs.ledgerdata.ethereum.EthereumBlock;
// import jabs.network.networks.ethereum.EthereumGlobalProofOfWorkNetwork;
import jabs.network.networks.ethereum.CasperFFGGlobalBlockchainNetwork;
import jabs.network.stats.sixglobalregions.ethereum.EthereumProofOfWorkGlobalNetworkStats6Regions;
// import jabs.network.stats.sixglobalregions.ethereum.EthereumNodeGlobalNetworkStats6Regions;

import static jabs.network.stats.eightysixcountries.ethereum.EthereumProofOfWorkGlobalNetworkStats86Countries.ETHEREUM_DIFFICULTY_2022;

public class CasperEthereumNetworkScenario extends AbstractScenario {
    private final double simulationStopTime;
    private final double averageBlockInterval;
    private final int checkpointSpace;
    private final int numOfStakeholders;

    /**
     * @param name
     * @param seed
     * @param simulationStopTime
     * @param averageBlockInterval
     */
    public CasperEthereumNetworkScenario(String name, long seed,
                                         double simulationStopTime, double averageBlockInterval, 
                                         int checkpointSpace, int numOfStakeholders) {
        super(name, seed);
        this.simulationStopTime = simulationStopTime;
        this.averageBlockInterval = averageBlockInterval;
        this.checkpointSpace = checkpointSpace;
        this.numOfStakeholders = numOfStakeholders;
    }

    @Override
    public void createNetwork() {
        CasperFFGGlobalBlockchainNetwork<?> ethereumNetwork = new CasperFFGGlobalBlockchainNetwork<>(randomnessEngine, this.checkpointSpace,
                new EthereumProofOfWorkGlobalNetworkStats6Regions(randomnessEngine));
        this.network = ethereumNetwork;
        ethereumNetwork.populateNetwork(simulator,
                new CasperFFGConfig(EthereumBlock.generateGenesisBlock(ETHEREUM_DIFFICULTY_2022),
                        this.averageBlockInterval, this.checkpointSpace, this.numOfStakeholders));
    }

    @Override
    protected void insertInitialEvents() {
        ((CasperFFGGlobalBlockchainNetwork<?>) network).startAllMiningProcesses();
    }

    @Override
    public boolean simulationStopCondition() {
        return (simulator.getSimulationTime() > simulationStopTime);
    }
}
