/*
 * Copyright (c) 2013-2016, Kasra Faghihi, All rights reserved.
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

import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import org.apache.commons.lang3.Validate;

/**
 * Network communication gateway.
 * @author Kasra Faghihi
 */
public final class NetworkGateway implements InputGateway {

    private final SimpleShuttle srcShuttle;
    private NetworkRunnable runnable;
    private Thread thread;

    /**
     * Creates a {@link NetworkGateway} object.
     * @return new {@link NetworkGateway}
     */
    public static NetworkGateway create() {
        NetworkGateway ng = new NetworkGateway();
        
        ng.runnable = new NetworkRunnable();
        ng.thread = new Thread(ng.runnable);
        ng.thread.setDaemon(true);
        ng.thread.setName("Network IO");
        
        ng.thread.start();
        
        return ng;
    }
    
    public NetworkGateway(
            String prefix,
            Shuttle outgoingShuttle,
            Address outgoingAddress) {
        // Validate outgoingAddress is for outgoignShuttle
        Address outgoingPrefix = Address.of(outgoingShuttle.getPrefix());
        Validate.isTrue(outgoingPrefix.isPrefixOf(outgoingAddress));
        
        Bus bus = new Bus();
        srcShuttle = new SimpleShuttle(prefix, bus);
        Address selfPrefix = Address.of(prefix);
        
        NetworkGateway ng = new NetworkGateway(selfPrefix, outgoingAddress, outgoingShuttle);
        
        ng.runnable = new NetworkRunnable();
        ng.thread = new Thread(ng.runnable);
        ng.thread.setDaemon(true);
        ng.thread.setName("Network IO");
        
        ng.thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return srcShuttle;
    }

    @Override
    public void close() throws Exception {
        runnable.close();
        thread.join();
    }
}
