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

    private final InetAddress localInetAddress;
    private final int localInetPort;
    private final InetAddress remoteInetAddress;
    private final int remoteInetPort;

    private final Address networkGatewayAddress;
    private final Address bindAddress;
    
    /**
     * Constructs a {@link TcpCreateRequest} object.
     * @param localInetAddress source address of the socket that was created
     * @param localInetPort source port of the socket that was created
     * @param remoteInetAddress destination address of the socket that was created
     * @param remoteInetPort destination port of the socket that was created
     * @param networkGatewayAddress {@link NetworkGateway} address for this new TCP socket
     * @param bindAddress actor/gateway address that this new TCP socket is bound to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code 1 < localInetPort > 65535} or {@code 1 < remoteInetPort > 65535}
     */
    public TcpServerAcceptNotification(
            InetAddress localInetAddress,
            int localInetPort,
            InetAddress remoteInetAddress,
            int remoteInetPort,
            Address networkGatewayAddress,
            Address bindAddress) {
        Validate.notNull(localInetAddress);
        Validate.inclusiveBetween(1, 65535, localInetPort);
        Validate.notNull(remoteInetAddress);
        Validate.inclusiveBetween(1, 65535, remoteInetPort);

        this.localInetAddress = localInetAddress;
        this.localInetPort = localInetPort;
        this.remoteInetAddress = remoteInetAddress;
        this.remoteInetPort = remoteInetPort;
        this.networkGatewayAddress = networkGatewayAddress;
        this.bindAddress = bindAddress;
    }

    /**
     * Local address of the socket that was created.
     * @return local address
     */
    public InetAddress getLocalInetAddress() {
        return localInetAddress;
    }

    /**
     * Local port of the socket that was created.
     * @return local address
     */
    public int getLocalInetPort() {
        return localInetPort;
    }

    /**
     * Remote address of the socket that was created.
     * @return remote address
     */
    public InetAddress getRemoteInetAddress() {
        return remoteInetAddress;
    }

    /**
     * Remote port of the socket that was created.
     * @return remote port
     */
    public int getRemoteInetPort() {
        return remoteInetPort;
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
        return "TcpServerAcceptNotification{" + "localInetAddress=" + localInetAddress + ", localInetPort=" + localInetPort
                + ", remoteInetAddress=" + remoteInetAddress + ", remoteInetPort=" + remoteInetPort
                + ", networkGatewayAddress=" + networkGatewayAddress + ", bindAddress=" + bindAddress + '}';
    }


}
