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
package com.offbynull.peernetic.io.gateways.network.messages;

import java.net.InetAddress;
import org.apache.commons.lang3.Validate;

/**
 * Create a TCP socket. Possible responses are {@link TcpCreateResponse} and {@link ErrorResponse}). Shortly after
 * creation, the socket will connect and a {@link TcpConnectedNotification} will be sent out to the creator.
 * @author Kasra Faghihi
 */
public final class TcpCreateRequest {

    private InetAddress sourceAddress;
    private InetAddress destinationAddress;
    private int destinationPort;

    /**
     * Constructs a {@link TcpCreateRequest} object.
     * @param sourceAddress source address of the socket to be created
     * @param destinationAddress destination address of the socket to be created
     * @param destinationPort destination port of the socket to be created
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 1 > destinationPort > 65535}
     */
    public TcpCreateRequest(InetAddress sourceAddress, InetAddress destinationAddress,
            int destinationPort) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.inclusiveBetween(1, 65535, destinationPort);

        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }

    /**
     * Source address of the socket to be created.
     * @return source address
     */
    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Destination address of the socket to be created.
     * @return destination address
     */
    public InetAddress getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * Destination port of the socket to be created.
     * @return destination port
     */
    public int getDestinationPort() {
        return destinationPort;
    }

    @Override
    public String toString() {
        return "TcpCreateRequest{" + ", sourceAddress=" + sourceAddress + ", destinationAddress="
                + destinationAddress + ", destinationPort=" + destinationPort + '}';
    }
}
