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
 * Create a UDP socket. Possible responses are {@link CreateUdpNetworkResponse} and {@link IdentifiableErrorNetworkResponse}).
 * @author Kasra Faghihi
 */
public final class CreateUdpNetworkRequest {
    private InetAddress sourceAddress;

    /**
     * Constructs a {@link CreateUdpNetworkRequest} object.
     * @param sourceAddress source address of the socket to be created
     * @throws NullPointerException if any argument is {@code null}
     */
    public CreateUdpNetworkRequest(InetAddress sourceAddress) {
        Validate.notNull(sourceAddress);
        this.sourceAddress = sourceAddress;
    }

    /**
     * Source address of the socket to be created.
     * @return source address
     */
    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    @Override
    public String toString() {
        return "CreateUdpNetworkRequest{" + "sourceAddress=" + sourceAddress + '}';
    }
}
