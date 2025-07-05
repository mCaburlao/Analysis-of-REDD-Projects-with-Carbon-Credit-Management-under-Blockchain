package jabs.simulator.event;

import jabs.ledgerdata.Block;
import jabs.network.node.nodes.Node;

/**
 * Event for block finalization logging.
 */
public class BlockFinalizationEvent extends AbstractLogEvent {
    private final Node node;
    private final Block block;
    private final long trafficUntilFinalization; // in bytes

    public BlockFinalizationEvent(double time, Node node, Block block, long trafficUntilFinalization) {
        super(time);
        this.node = node;
        this.block = block;
        this.trafficUntilFinalization = trafficUntilFinalization;
    }

    public Node getNode() {
        return node;
    }

    public Block getBlock() {
        return block;
    }

    public long getTrafficUntilFinalization() {
        return trafficUntilFinalization;
    }
}