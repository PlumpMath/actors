/*
 * Copyright (c) 2016, Kasra Faghihi, All rights reserved.
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
 * Network communication gateway. 3 types of sockets are supported: TCP, TCP server, and UDP. When you create a socket using this gateway,
 * all operations on that socket must be performed from the address that originally created that socket.
 * <p>
 * <b><u>TCP sockets</u></b>
 * To create a TCP socket, use {@link TcpCreateRequest}.
 * <p>
 * Request types supported by TCP sockets...
 * <ul>
 * <li>{@link TcpWriteRequest}</li>
 * <li>{@link CloseRequest}</li>
 * </ul>
 * Response and notification types supported by TCP sockets...
 * <ul>
 * <li>{@link TcpCreateResponse}</li>
 * <li>{@link TcpWriteResponse}</li>
 * <li>{@link TcpWriteEmptyNotification}</li>
 * <li>{@link TcpReadNotification}</li>
 * <li>{@link TcpReadClosedNotification}</li>
 * <li>{@link CloseResponse}</li>
 * </ul>
 * <p>
 * <b><u>TCP server sockets</u></b>
 * To create a TCP server socket, use {@link TcpServerCreateRequest}.
 * <p>
 * Request types supported by TCP server sockets...
 * <ul>
 * <li>{@link CloseRequest}</li>
 * </ul>
 * Response and notification types supported by TCP server sockets...
 * <ul>
 * <li>{@link TcpServerCreateResponse}</li>
 * <li>{@link TcpServerAcceptNotification}</li>
 * <li>{@link CloseResponse}</li>
 * </ul>
 * NOTE: When a {@link TcpServerAcceptNotification} arrives, it means a new TCP socket is created. The TCP socket will be bound to a child
 * of the address that created the TCP server socket. Closing the TCP server socket won't close the TCP sockets it created -- you need to
 * explicitly close them yourself.
 * <p>
 * <b><u>UDP sockets</u></b>
 * To create a UDP socket, use {@link UdpCreateRequest}.
 * <p>
 * Request types supported by UDP sockets...
 * <ul>
 * <li>{@link UdpWriteRequest}</li>
 * <li>{@link CloseRequest}</li>
 * </ul>
 * Response and notification types supported by TCP server sockets...
 * <ul>
 * <li>{@link UdpCreateResponse}</li>
 * <li>{@link UdpReadNotification}</li>
 * <li>{@link UdpWriteEmptyNotification}</li>
 * <li>{@link UdpWriteResponse}</li>
 * <li>{@link CloseResponse}</li>
 * </ul>
 * @author Kasra Faghihi
 */
public final class NetworkGateway implements InputGateway, OutputGateway {

    private final SimpleShuttle srcShuttle;
    private final NetworkRunnable runnable;
    private final Thread thread;
    private final Bus bus;
    
    /**
     * Constructs a {@link NetworkGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public NetworkGateway(String prefix) {
        Validate.notNull(prefix);
        
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
        bus.add(new Shutdown());
        thread.join();
    }
}
