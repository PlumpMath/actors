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
import com.offbynull.peernetic.io.gateways.network.UdpNetworkEntry.AddressedByteBuffer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.LinkedList;

final class UdpNetworkEntry extends NetworkEntry {
    private LinkedList<AddressedByteBuffer> outgoingBuffers;
    private boolean notifiedOfWritable;

    UdpNetworkEntry(Address selfSuffix, Address initiatorAddress, AbstractSelectableChannel channel) {
        super(selfSuffix, initiatorAddress, channel);
        outgoingBuffers = new LinkedList<>();
        notifiedOfWritable = false;
    }

    LinkedList<AddressedByteBuffer> getOutgoingBuffers() {
        return outgoingBuffers;
    }

    boolean isNotifiedOfWritable() {
        return notifiedOfWritable;
    }

    void setNotifiedOfWritable(boolean notifiedOfWritable) {
        this.notifiedOfWritable = notifiedOfWritable;
    }
    
    static final class AddressedByteBuffer  {
        private ByteBuffer buffer;
        private InetSocketAddress socketAddres;

        AddressedByteBuffer(ByteBuffer buffer, InetSocketAddress socketAddres) {
            this.buffer = buffer;
            this.socketAddres = socketAddres;
        }

        ByteBuffer getBuffer() {
            return buffer;
        }

        InetSocketAddress getSocketAddress() {
            return socketAddres;
        }
        
    }
}
