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
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.LinkedList;

final class TcpNetworkEntry extends NetworkEntry {
    private boolean connecting = true;
    private boolean readFinished;
    private LinkedList<ByteBuffer> outgoingBuffers;
    private boolean notifiedOfWritable;

    TcpNetworkEntry(Address selfSuffix, Address proxySuffix, Channel channel) {
        super(selfSuffix, proxySuffix, channel);
        outgoingBuffers = new LinkedList<>();
        notifiedOfWritable = false;
    }

    public LinkedList<ByteBuffer> getOutgoingBuffers() {
        return outgoingBuffers;
    }

    boolean isConnecting() {
        return connecting;
    }

    void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    public boolean isReadFinished() {
        return readFinished;
    }

    public void setReadFinished(boolean readFinished) {
        this.readFinished = readFinished;
    }

    boolean isNotifiedOfWritable() {
        return notifiedOfWritable;
    }

    void setNotifiedOfWritable(boolean notifiedOfWritable) {
        this.notifiedOfWritable = notifiedOfWritable;
    }
}
