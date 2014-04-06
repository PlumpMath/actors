package com.offbynull.peernetic.network.handlers.readwrite;

import com.offbynull.peernetic.network.handlers.common.AbstractTransformArrivingHandler;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.lang3.Validate;

/**
 * A Netty handler that writes data that comes in to a {@link BlockingQueue}.
 * @author Kasra Faghihi
 */
public final class ReadToQueueHandler extends AbstractTransformArrivingHandler {

    private BlockingQueue<Message> queue;

    /**
     * Constructs a {@link ReadToQueueHandler} object.
     * @param queue queue to write to
     * @throws NullPointerException if any argument is {@code null}
     */
    public ReadToQueueHandler(BlockingQueue<Message> queue) {
        Validate.notNull(queue);
        this.queue = queue;
    }
    
    @Override
    protected Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj) {
        Validate.notNull(obj);
        queue.add(new Message(localAddress, remoteAddress, obj));
        return obj;
    }
}