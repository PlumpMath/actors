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

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

/**
 * Send packet to a UDP socket. Possible responses are {@link UdpWriteResponse} and {@link ErrorResponse}).
 * @author Kasra Faghihi
 */
public final class UdpWriteRequest {
    private InetSocketAddress remoteAddress;
    private byte[] data;

    /**
     * Constructs a {@link WriteUdpNetworkRequest} object.
     * @param remoteAddress outgoing socket address
     * @param data send data
     * @throws NullPointerException if any argument is {@code null}
     */
    public UdpWriteRequest(InetSocketAddress remoteAddress, byte[] data) {
        Validate.notNull(remoteAddress);
        Validate.notNull(data);
        this.remoteAddress = remoteAddress;
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Get remote address.
     * @return remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Get send packet.
     * @return send packet
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String toString() {
        return "UdpWriteRequest{remoteAddress=" + remoteAddress + ", data=" + Arrays.toString(data)
                + '}';
    }

}
