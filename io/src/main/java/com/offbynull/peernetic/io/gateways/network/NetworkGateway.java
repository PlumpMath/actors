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
import com.offbynull.peernetic.core.gateway.OutputGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import org.apache.commons.lang3.Validate;

/**
 * Network communication gateway.
 * @author Kasra Faghihi
 */
public final class NetworkGateway implements InputGateway, OutputGateway {

    private final SimpleShuttle srcShuttle;
    private final NetworkRunnable runnable;
    private final Thread thread;
    private final Bus bus;
    
    public NetworkGateway(
            String prefix,
            Shuttle proxyShuttle,
            Address proxyAddress) {
        Validate.notNull(prefix);
        Validate.notNull(proxyShuttle);
        Validate.notNull(proxyAddress);

        // Validate outgoingAddress is for outgoignShuttle
        Address outgoingPrefix = Address.of(proxyShuttle.getPrefix());
        Validate.isTrue(outgoingPrefix.isPrefixOf(proxyAddress));
        
        bus = new Bus();
        srcShuttle = new SimpleShuttle(prefix, bus);
        Address selfPrefix = Address.of(prefix);
        
        runnable = new NetworkRunnable(selfPrefix, bus, 65535);
        thread = new Thread(runnable, "Network IO - " + selfPrefix);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return srcShuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        bus.add(new RemoveShuttle(shuttlePrefix));
    }

    @Override
    public void close() throws Exception {
        runnable.close();
        thread.join();
    }
}
