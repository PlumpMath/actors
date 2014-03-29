/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.router.common;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 * UDP response listener.
 * @author Kasra Faghihi
 */
public interface UdpCommunicatorListener {
    /**
     * Called when a UDP packet arrives.
     * @param sourceAddress source address
     * @param channel channel
     * @param packet packet contents
     */
    void incomingPacket(InetSocketAddress sourceAddress, Channel channel, ByteBuffer packet);
}
