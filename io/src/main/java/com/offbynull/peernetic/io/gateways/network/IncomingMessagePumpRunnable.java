/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.io.gateways.network;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// What's the point of this thread? The point is that we never want a Shuttle implementation that directly wakes up the selector, because we
// don't know how the selector performs (its dependent on platform/drivers). We never want a Shuttle to block.
//
// This thread is basically an intermediary between the Shuttle and NIO. If wakeup ever slows down or blocks it won't effect the shuttle
// pushing messages to this intermediary. Additional messages can still be added to the shuttle, it'll just be queued until this thread gets
// a chance to read.
final class IncomingMessagePumpRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(IncomingMessagePumpRunnable.class);
    
    private final Address selfPrefix;
    private final Address proxyPrefix;
    
    // from this gateway's Shuttle to this pump
    private final Bus bus;
    
    // from this pump to the NIO thread
    private final LinkedBlockingQueue<Message> outQueue;
    private final Selector outSelector; // selector is what blocks in the NIO thread

    public IncomingMessagePumpRunnable(Address selfPrefix, Address proxyPrefix, Bus bus, LinkedBlockingQueue<Message> outQueue,
            Selector outSelector) {
        Validate.notNull(selfPrefix);
        Validate.notNull(proxyPrefix);
        Validate.notNull(bus);
        Validate.notNull(outQueue);
        Validate.notNull(outSelector);
        this.selfPrefix = selfPrefix;
        this.proxyPrefix = proxyPrefix;
        this.bus = bus;
        this.outQueue = outQueue;
        this.outSelector = outSelector;
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                // Poll for new messages from this gateway's shuttle
                List<Object> incomingObjects = bus.pull();
                
                List<Message> convertedObjects = incomingObjects.stream()
                        .map(x -> (Message) x)
                        .filter(x -> selfPrefix.isPrefixOf(x.getSourceAddress())) // only msgs intended for this gateway
                        .filter(x -> proxyPrefix.isPrefixOf(x.getDestinationAddress())) // only msgs from address we're allowed to talk to
                        .collect(Collectors.toList());

                // insert and notify if not empty
                if (!convertedObjects.isEmpty()) {
                    // Notify selector
                    outQueue.addAll(convertedObjects);
                    outSelector.wakeup();
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Message pump interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }
}
