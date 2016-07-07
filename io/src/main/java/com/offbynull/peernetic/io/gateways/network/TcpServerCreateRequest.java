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

import java.net.InetAddress;
import org.apache.commons.lang3.Validate;

/**
 * Create a TCP server socket. Possible responses are {@link TcpServerCreateResponse} and {@link ErrorResponse}).
 * @author Kasra Faghihi
 */
public final class TcpServerCreateRequest {

    private InetAddress sourceAddress;
    private int sourcePort;

    /**
     * Constructs a {@link TcpCreateRequest} object.
     * @param sourceAddress source address of the socket to be created (address listening for connections)
     * @param sourcePort source port of the socket to be created (port listening for connections)
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 1 > destinationPort > 65535}
     */
    public TcpServerCreateRequest(InetAddress sourceAddress, int sourcePort) {
        Validate.notNull(sourceAddress);
        Validate.inclusiveBetween(1, 65535, sourcePort);

        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
    }

    /**
     * Source address of the socket to be created.
     * @return source address
     */
    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Source port of the socket to be created.
     * @return source port
     */
    public int getSourcePort() {
        return sourcePort;
    }

    @Override
    public String toString() {
        return "TcpServerCreateRequest{" + "sourceAddress=" + sourceAddress + ", sourcePort=" + sourcePort + '}';
    }

}
