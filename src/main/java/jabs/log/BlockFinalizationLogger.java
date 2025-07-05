package jabs.log;

import jabs.ledgerdata.Block;
import jabs.network.node.nodes.Node;
import jabs.simulator.event.AbstractLogEvent;
import jabs.simulator.event.Event;
import jabs.simulator.event.BlockFinalizationEvent;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;

/**
 * Logs block finalization events (when a block is finalized by a node).
 */
public class BlockFinalizationLogger extends AbstractCSVLogger {
    /**
     * creates an abstract CSV logger
     * @param writer this is output CSV of the logger
     */
    public BlockFinalizationLogger(Writer writer) {
        super(writer);
    }

    /**
     * creates an abstract CSV logger
     * @param path this is output path of CSV file
     */
    public BlockFinalizationLogger(Path path) throws IOException {
        super(path);
    }

    @Override
    protected String csvStartingComment() {
        return String.format("Simulation name: %s      Number of nodes: %d      Network type: %s", scenario.getName(),
                this.scenario.getNetwork().getAllNodes().size(), this.scenario.getNetwork().getClass().getSimpleName());
    }

    @Override
    protected boolean csvOutputConditionBeforeEvent(Event event) {
        return false;
    }

    @Override
    protected boolean csvOutputConditionAfterEvent(Event event) {
        return event instanceof BlockFinalizationEvent;
    }

    @Override
    protected boolean csvOutputConditionFinalPerNode() {
        return false;
    }

    @Override
    protected String[] csvHeaderOutput() {
        return new String[]{"Time", "NodeID", "BlockHeight", "BlockHashCode", "BlockSize", "BlockCreationTime", "BlockCreator", "BLockFinalizationTime", "TrafficUntilFinalization"};
    }

    @Override
    protected String[] csvEventOutput(Event event) {
        BlockFinalizationEvent finalizationEvent = (BlockFinalizationEvent) event;
        Node node = finalizationEvent.getNode();
        Block block = finalizationEvent.getBlock();

        return new String[]{
                Double.toString(this.scenario.getSimulator().getSimulationTime()),
                Integer.toString(node.nodeID),
                Integer.toString(block.getHeight()),
                Integer.toString(block.hashCode()),
                Integer.toString(block.getSize()),
                Double.toString(block.getCreationTime()),
                Integer.toString(block.getCreator().nodeID),
                Double.toString(this.scenario.getSimulator().getSimulationTime() - block.getCreationTime()),
                Long.toString(finalizationEvent.getTrafficUntilFinalization())
        };
    }
}