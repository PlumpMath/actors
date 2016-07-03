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
import java.nio.channels.Channel;
import org.apache.commons.lang3.Validate;

abstract class NetworkEntry {
    private final Address selfSuffix;
    private final Address proxySuffix;
    private final Channel channel;
    private int selectionKey;
    
    NetworkEntry(Address selfSuffix, Address proxySuffix, Channel channel) {
        Validate.notNull(channel);

        this.selfSuffix = selfSuffix;
        this.proxySuffix = proxySuffix;
        this.channel = channel;
        this.selectionKey = 0;
    }

    Address getSelfSuffix() {
        return selfSuffix;
    }

    Address getProxySuffix() {
        return proxySuffix;
    }

    Channel getChannel() {
        return channel;
    }

    int getSelectionKey() {
        return selectionKey;
    }

    void setSelectionKey(int selectionKey) {
        this.selectionKey = selectionKey;
    }
}