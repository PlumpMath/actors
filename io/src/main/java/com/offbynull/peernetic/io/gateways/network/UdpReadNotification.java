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

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

/**
 * UDP socket has received data.
 * @author Kasra Faghihi
 */
public final class UdpReadNotification {
    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;
    private byte[] data;

    /**
     * Constructs a {@link UdpReadNotification} object.
     * @param localAddress address sent to
     * @param remoteAddress address sent from
     * @param data received data
     * @throws NullPointerException if any argument is {@code null}
     */
    public UdpReadNotification(InetSocketAddress localAddress, InetSocketAddress remoteAddress, byte[] data) {
        Validate.notNull(localAddress);
        Validate.notNull(remoteAddress);
        Validate.notNull(data);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * Get local address.
     * @return local address
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Get remote address.
     * @return remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Get data.
     * @return data
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String toString() {
        return "UdpReadNotification{localAddress=" + localAddress + ", remoteAddress=" + remoteAddress
                + ", data=" + Arrays.toString(data) + '}';
    }

}
