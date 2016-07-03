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

import com.offbynull.peernetic.core.shuttle.Address;
import java.net.InetAddress;
import org.apache.commons.lang3.Validate;

/**
 * Notification that a TCP server socket has accepted a new TCP connection.
 * @author Kasra Faghihi
 */
public final class TcpServerAcceptNotification {

    private final InetAddress sourceAddress;
    private final int sourcePort;
    private final InetAddress destinationAddress;
    private final int destinationPort;

    private final Address sendFromAddress;
    private final Address sendToAddress;
    
    /**
     * Constructs a {@link TcpCreateRequest} object.
     * @param sourceAddress source address of the socket that was created
     * @param sourcePort source port of the socket that was created
     * @param destinationAddress destination address of the socket that was created
     * @param destinationPort destination port of the socket that was created
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 1 > destinationPort > 65535}
     */
    public TcpServerAcceptNotification(
            InetAddress sourceAddress,
            int sourcePort,
            InetAddress destinationAddress,
            int destinationPort,
            Address sendFromAddress,
            Address sendToAddress) {
        Validate.notNull(sourceAddress);
        Validate.inclusiveBetween(1, 65535, sourcePort);
        Validate.notNull(destinationAddress);
        Validate.inclusiveBetween(1, 65535, destinationPort);

        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
        this.sendFromAddress = sendFromAddress;
        this.sendToAddress = sendFromAddress;
    }

    /**
     * Source address of the socket that was created.
     * @return source address
     */
    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Source port of the socket that was created.
     * @return source address
     */
    public int getSourcePort() {
        return sourcePort;
    }

    /**
     * Destination address of the socket that was created.
     * @return destination address
     */
    public InetAddress getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * Destination port of the socket that was created.
     * @return destination port
     */
    public int getDestinationPort() {
        return destinationPort;
    }

    public Address getSendFromAddress() {
        return sendFromAddress;
    }

    public Address getSendToAddress() {
        return sendToAddress;
    }

    @Override
    public String toString() {
        return "TcpServerAcceptNotification{" + "sourceAddress=" + sourceAddress + ", sourcePort=" + sourcePort + ", destinationAddress="
                + destinationAddress + ", destinationPort=" + destinationPort + ", sendFromAddress=" + sendFromAddress + ", sendToAddress="
                + sendToAddress + '}';
    }

}
