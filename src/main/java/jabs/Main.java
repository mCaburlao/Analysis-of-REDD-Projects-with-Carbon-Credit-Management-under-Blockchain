package jabs;

import jabs.ledgerdata.bitcoin.BitcoinBlockWithoutTx;
import jabs.log.*;
import jabs.scenario.*;
import jabs.network.node.nodes.ethereum.EthereumMinerNode;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.nio.file.Paths;

/**
 *
 */
public class Main {
    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // MCO2 scenario
        for (int i = 1; i <= 10; i++) {
                int numOfMiners = 12;
                double averageBlockInterval = 15;
                double simulationStopTime = 3600;
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                        AbstractScenario scenario;
                        int numOfStakeholders = 100*i;
                        scenario = new CasperEthereumNetworkScenario(
                                        "60 min in the life of Casper with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                        simulationStopTime, averageBlockInterval, 14, numOfMiners, numOfStakeholders);
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/casper-ethereum/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                        scenario.run();
                }
        }
        // TreeCycle scenario
        for (int i = 1; i <= 10; i++) {
                int numOfMiners = 12;
                double averageBlockInterval = 60;
                double simulationStopTime = 3600;
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                        AbstractScenario scenario;
                        int numOfStakeholders = 100*i;
                        scenario = new CasperEthereumNetworkScenario(
                                        "60 min in the life of Casper with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                        simulationStopTime, averageBlockInterval, 14, numOfMiners, numOfStakeholders);
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/casper-ignis/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                        scenario.run();
                }
        }

        // Ambify scenario
        for (int i = 1; i <= 10; i++) {
                double simulationStopTime = 3600;
                double averageBlockInterval = 3.0;
                int turnLength = 3;
                int epochLength = 21;
                int numOfMiners = 12;
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                        int numOfStakeholders = (100*i);
            
                        AbstractScenario scenario = new ParliaBSCNetworkScenario(
                                "1 hour Parlia BSC with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                simulationStopTime, averageBlockInterval,
                                turnLength, epochLength, numOfMiners, numOfStakeholders
                        );
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/parlia-bsc/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                        scenario.run();
                }
        }
    }
}