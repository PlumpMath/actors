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
package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Instant;
import java.util.List;

/**
 * Context of an {@link Actor}. An actor's context is passed in to an actor each time an incoming message arrives. It contains ...
 * <ul>
 * <li>address of the actor.</li>
 * <li>time which the actor was triggered.</li>
 * <li>incoming message that caused the actor to be triggered.</li>
 * <li>outgoing messages that this actor is sending out.</li>
 * </ul>
 * 
 * @author Kasra Faghihi
 */
public interface Context {
    
    /**
     * Queue up an outgoing message.
     * @param source source address (must be child of or equal to {@link #getSelf()})
     * @param destination destination address
     * @param message outgoing message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code destination} is empty, or if {@code source} is not a child of or equal to
     * {@link #getSelf()}
     */
    void addOutgoingMessage(Address source, Address destination, Object message);
    
    /**
     * Returns an unmodifiable list of outgoing messages. This list stays in sync as more outgoing messages are added.
     * @return unmodifiable list of outgoing messages
     */
    List<BatchedOutgoingMessage> viewOutgoingMessages();

    /**
     * Get the address the incoming message was sent to.
     * @return destination address of incoming message (absolute, not relative)
     */
    Address getDestination();

    /**
     * Get the incoming message.
     * @param <T> message type
     * @return incoming message
     */
    <T> T getIncomingMessage();

    /**
     * Get the address of the actor that this context is for.
     * @return address of actor
     */
    Address getSelf();

    /**
     * Get the address the incoming message was sent from.
     * @return source address of incoming message
     */
    Address getSource();

    /**
     * Get the current time. This may not be exact, it is the time the actor was invoked.
     * @return current time
     */
    Instant getTime();
    
}
