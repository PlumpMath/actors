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

import com.offbynull.peernetic.io.gateways.udp.*;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This thread is basically an intermediary between NIO and the receiving shuttle. Originally intended for if decoupling deserialization
// from NIO selection thread such that deserialization wouldn't block NIO access if it was slow. Not required anymore since we're no longer
// deserializing at this stage. Should be removed at some point in the future.
final class IncomingPumpRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(IncomingPumpRunnable.class);

    private final Address selfPrefix;
    private final Address proxyPrefix;
    
    // from udp NIO thread to this pump
    private final LinkedBlockingQueue<Object> inQueue;
    private final Shuttle outShuttle;
    
    public IncomingPumpRunnable(Address selfPrefix, Address proxyPrefix, Shuttle proxyShuttle, LinkedBlockingQueue<Object> inQueue) {
        Validate.notNull(selfPrefix);
        Validate.notNull(proxyPrefix);
        Validate.notNull(proxyShuttle);
        Validate.notNull(inQueue);
        this.selfPrefix = selfPrefix;
        this.proxyPrefix = proxyPrefix; // the address we're suppose to funnel stuff in to, should be a child of proxyShuttle's address
        this.outShuttle = proxyShuttle;
        this.inQueue = inQueue;
        Validate.isTrue(Address.of(proxyShuttle.getPrefix()).isPrefixOf(proxyPrefix));
    }
    
    @Override
    public void run() {
        try {
            
            while (true) {
                List<Object> incomingObjs = new LinkedList<>();
                
                // Poll for new packets
                Object first = inQueue.take();
                incomingObjs.add(first);
                inQueue.drainTo(incomingObjs);

                List<Message> outgoingMessages = incomingObjs.stream()
                        .map(x -> new Message(selfPrefix, proxyPrefix, x)) // wrap in envelope from self to addr we're allowed to talk to
                        .collect(Collectors.toList());

                // Send messages to shuttle
                outShuttle.send(outgoingMessages);
            }
        } catch (InterruptedException ie) {
            LOG.debug("Message pump interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        }
    }
}
