/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import io.netty.channel.socket.DatagramChannel;
import org.apache.commons.lang3.Validate;

/**
 * A simple {@link EndpointFinder} implementation that allows the user to specific which address they want to talk to over a network
 * transport.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class NetworkEndpointFinder<A> implements EndpointFinder<A> {
    private DatagramChannel channel;

    /**
     * Construct a {@link NetworkEndpoint} object.
     * @param channel channel
     * @throws NullPointerException if any arguments are {@code null}
     */
    public NetworkEndpointFinder(DatagramChannel channel) {
        Validate.notNull(channel);
        this.channel = channel;
    }
    

    @Override
    public Endpoint findEndpoint(A address) {
        Validate.notNull(address);
        return new NetworkEndpoint(channel, address);
    }
}