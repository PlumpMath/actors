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
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.util.stream.Collectors.groupingBy;

// This thread is basically an intermediary between NIO and the receiving shuttle. Originally intended for if decoupling deserialization
// from NIO selection thread such that deserialization wouldn't block NIO access if it was slow. Not required anymore since we're no longer
// deserializing at this stage. Should be removed at some point in the future.
final class OutgoingMessagePumpRunnable implements Runnable {
    
    static final Object DEATH_MARKER = new Object();
    private static final Logger LOG = LoggerFactory.getLogger(OutgoingMessagePumpRunnable.class);

    // from NIO thread to this pump
    private final LinkedBlockingQueue<Object> inQueue;
    
    public OutgoingMessagePumpRunnable(Address selfPrefix, LinkedBlockingQueue<Object> inQueue) {
        Validate.notNull(selfPrefix);
        Validate.notNull(inQueue);
        this.inQueue = inQueue;
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
                
                LOG.debug("Pulling: {} items", incomingObjs.size());

                // if there's a death marker, ignore anything after it
                int deathIdx;
                if ((deathIdx = incomingObjs.indexOf(DEATH_MARKER)) != -1) {
                    LOG.debug("Death marker found at idx {}, ignoring everything after", deathIdx);
                    incomingObjs = incomingObjs.subList(0, deathIdx);
                }

                // Send messages to shuttle
                incomingObjs.stream()
                        .map(x -> (MessageWithShuttle) x)
                        .collect(groupingBy(x -> x.getOutShuttle(), mapping(x -> x.getMessage(), toList()))) // map of shuttle -> messages
                        .forEach((x, y) -> {
                            LOG.debug("Sending {} messages to {}", y.size(), x.getPrefix());
                            x.send(y);
                        });
                
                // if there's a death marker, we should die
                if (deathIdx != -1) {
                    return;
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Message pump interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        }
    }
    
    public static class MessageWithShuttle {
        private final Shuttle outShuttle;
        private final Message message;

        public MessageWithShuttle(Shuttle outShuttle, Message message) {
            Validate.notNull(outShuttle);
            Validate.notNull(message);
            this.outShuttle = outShuttle;
            this.message = message;
        }

        public Shuttle getOutShuttle() {
            return outShuttle;
        }

        public Message getMessage() {
            return message;
        }
        
    }
}
