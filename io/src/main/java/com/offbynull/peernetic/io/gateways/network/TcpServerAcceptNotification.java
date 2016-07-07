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

import com.offbynull.peernetic.core.shuttle.Address;
import java.net.InetAddress;
import org.apache.commons.lang3.Validate;

/**
 * Notification that a TCP server socket has accepted a new TCP connection.
 * @author Kasra Faghihi
 */
public final class TcpServerAcceptNotification {

    private final InetAddress sourceInetAddress;
    private final int sourceInetPort;
    private final InetAddress destinationInetAddress;
    private final int destinationInetPort;

    private final Address networkGatewayAddress;
    private final Address bindAddress;
    
    /**
     * Constructs a {@link TcpCreateRequest} object.
     * @param sourceInetAddress source address of the socket that was created
     * @param sourceInetPort source port of the socket that was created
     * @param destinationInetAddress destination address of the socket that was created
     * @param destinationInetPort destination port of the socket that was created
     * @param networkGatewayAddress {@link NetworkGateway} address for this new TCP socket
     * @param bindAddress actor/gateway address that this new TCP socket is bound to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 1 > destinationPort > 65535}
     */
    public TcpServerAcceptNotification(
            InetAddress sourceInetAddress,
            int sourceInetPort,
            InetAddress destinationInetAddress,
            int destinationInetPort,
            Address networkGatewayAddress,
            Address bindAddress) {
        Validate.notNull(sourceInetAddress);
        Validate.inclusiveBetween(1, 65535, sourceInetPort);
        Validate.notNull(destinationInetAddress);
        Validate.inclusiveBetween(1, 65535, destinationInetPort);

        this.sourceInetAddress = sourceInetAddress;
        this.sourceInetPort = sourceInetPort;
        this.destinationInetAddress = destinationInetAddress;
        this.destinationInetPort = destinationInetPort;
        this.networkGatewayAddress = networkGatewayAddress;
        this.bindAddress = bindAddress;
    }

    /**
     * Source address of the socket that was created.
     * @return source address
     */
    public InetAddress getSourceInetAddress() {
        return sourceInetAddress;
    }

    /**
     * Source port of the socket that was created.
     * @return source address
     */
    public int getSourceInetPort() {
        return sourceInetPort;
    }

    /**
     * Destination address of the socket that was created.
     * @return destination address
     */
    public InetAddress getDestinationInetAddress() {
        return destinationInetAddress;
    }

    /**
     * Destination port of the socket that was created.
     * @return destination port
     */
    public int getDestinationInetPort() {
        return destinationInetPort;
    }

    /**
     * {@link NetworkGateway} address for this new TCP socket.
     * @return network gateway address assigned to this socket
     */
    public Address getNetworkGatewayAddress() {
        return networkGatewayAddress;
    }

    /**
     * Actor/gateway address that this new TCP socket is bound to. This will be a child of the address that initiated the TCP server socket.
     * @return actor/gateway address bound to this socket
     */
    public Address getBindAddress() {
        return bindAddress;
    }

    @Override
    public String toString() {
        return "TcpServerAcceptNotification{" + "sourceInetAddress=" + sourceInetAddress + ", sourceInetPort=" + sourceInetPort
                + ", destinationInetAddress=" + destinationInetAddress + ", destinationInetPort=" + destinationInetPort
                + ", networkGatewayAddress=" + networkGatewayAddress + ", bindAddress=" + bindAddress + '}';
    }


}
