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
        //AbstractScenario scenario;
/*
        // Simulate one day in the life of Bitcoin network
        // Nakamoto protocol with block every 600 seconds
        // Around 8000 nodes with 30 miners
        scenario = new BitcoinGlobalNetworkScenario("One day in the life of Bitcoin", 1,
                86400, 600, 6);
        scenario.AddNewLogger(new BlockConfirmationLogger(Paths.get("output/bitcoin-confirmations-log.csv")));
        scenario.AddNewLogger(new BlockPropagationDelayLogger(
                Paths.get("output/bitcoin-50-propagation-delay-log.csv"),0.5));
        scenario.AddNewLogger(new BlockPropagationDelayLogger(
                Paths.get("output/bitcoin-90-propagation-delay-log.csv"),0.9));
        scenario.AddNewLogger(new BlockchainReorgLogger<BitcoinBlockWithoutTx>(
                Paths.get("output/bitcoin-reorgs-log.csv")));
        scenario.run();

        // Simulate 1 hour in the life of Ethereum network
        // Ghost protocol with blocks every 14 seconds on average
        // Around 6000 nodes with 37 miners
        for (int i = 0; i < 30; i++) {
                AbstractScenario scenario;   
                scenario = new NormalEthereumNetworkScenario("One hour in the life of Ethereum", i,
                        3600, 13.3);
                scenario.AddNewLogger(new BlockPropagationDelayLogger(
                        Paths.get("output/ethereum-50-propagation-delay-log-"+i+".csv"), 0.5));
                scenario.AddNewLogger(new BlockPropagationDelayLogger(
                        Paths.get("output/ethereum-90-propagation-delay-log-"+i+".csv"), 0.9));
                scenario.AddNewLogger(new FinalUncleBlocksLogger(
                        Paths.get("output/ethereum-uncle-rate-"+i+".csv")));
                scenario.run();
        }
*/
        // Casper
        // AbstractScenario scenario;   
        // scenario = new CasperEthereumNetworkScenario("Half of one hour in the life of Casper Ethereum with avarage block interval 14 min", 1,
        //         1800, 14, 1000, 40);
        // scenario.AddNewLogger(new BlockPropagationDelayLogger(
        //         Paths.get("output/ethereum-50-propagation-delay-log.csv"), 0.5));
        // scenario.AddNewLogger(new BlockPropagationDelayLogger(
        //         Paths.get("output/ethereum-90-propagation-delay-log.csv"), 0.9));
        // scenario.AddNewLogger(new BlockGenerationLogger(
        //         Paths.get("output/ethereum-generation-log.csv")));
        // // scenario.AddNewLogger(new BlockDeliveryLogger(
        // //         Paths.get("output/ethereum-delivery-log.csv")));
        // scenario.AddNewLogger(new FinalUncleBlocksLogger(
        //         Paths.get("output/ethereum-uncle-rate.csv")));
        // scenario.run();

        for (int i = 1; i <= 10; i++) {
                int numOfMiners = 12; // Number of miners
                double averageBlockInterval = 15;
                double simulationStopTime = 3600; // 1 hour
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                        AbstractScenario scenario;
                        int numOfStakeholders = 100*i; // Number of stakeholders
                        scenario = new CasperEthereumNetworkScenario(
                                        "60 min in the life of Casper with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                        simulationStopTime, averageBlockInterval, 14, numOfMiners, numOfStakeholders);
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/casper-ethereum/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                        scenario.run();
                }
        }

        for (int i = 1; i <= 10; i++) {
                int numOfMiners = 12; // Number of miners
                double averageBlockInterval = 60;
                double simulationStopTime = 3600; // 1 hour
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                        AbstractScenario scenario;
                        int numOfStakeholders = 100*i; // Number of stakeholders
                        scenario = new CasperEthereumNetworkScenario(
                                        "60 min in the life of Casper with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                        simulationStopTime, averageBlockInterval, 14, numOfMiners, numOfStakeholders);
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/casper-ignis/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                        scenario.run();
                }
        }

        // Parlia BSC scenario
        for (int i = 1; i <= 10; i++) {
                double simulationStopTime = 3600; // 1 hour
                double averageBlockInterval = 3.0; // seconds
                int turnLength = 3;
                int epochLength = 21;
                int numOfMiners = 12; // Number of miners
                for (int randomSeed = 1; randomSeed <= 20; randomSeed++) {
                    //     double simulationStopTime = 360; // 10 minutes
                        int numOfStakeholders = (100*i); // Number of stakeholders
            
                        AbstractScenario scenario = new ParliaBSCNetworkScenario(
                                "1 hour Parlia BSC with "+numOfStakeholders+" stakeholders and seed "+randomSeed, randomSeed,
                                simulationStopTime, averageBlockInterval,
                                turnLength, epochLength, numOfMiners, numOfStakeholders
                        );
                        scenario.AddNewLogger(new BlockFinalizationLogger(Paths.get("output/parlia-bsc/finalization-log-non-miners/"+numOfStakeholders+"-stakeholders-and-seed-"+(randomSeed)+".csv")));
                    //     scenario.AddNewLogger(new BlockGenerationLogger(Paths.get("output/parlia-bsc-generation-log-"+numOfMiners+"-miners.csv")));
                        scenario.run();
                }
        }
/*
        // Simulate PBFT Lan network of 40 nodes for 1 hour
        scenario = new PBFTLANScenario("One hour of a PBFT lan Network", 1,
                40, 3600);
        scenario.AddNewLogger(new PBFTCSVLogger(Paths.get("output/pbft-simulation-log.csv")));
        scenario.run();

        // Simulate Snow LAN network of 40 nodes for 1 hour
        scenario = new SnowLANScenario("One hour of a Snow lan Network", 1, 40,
                3600);
        scenario.AddNewLogger(new SnowCSVLogger(Paths.get("output/snow-simulation-log.csv")));
        scenario.run();
*/
    }
}