/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.simulator;

import com.offbynull.actors.core.shuttle.Address;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class MessageEvent extends Event {

    private final Address sourceAddress;
    private final Address destinationAddress;
    private final Object message;

    public MessageEvent(Address sourceAddress, Address destinationAddress, Object message, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.isTrue(!sourceAddress.isEmpty());
        Validate.isTrue(!destinationAddress.isEmpty());
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    public Address getSourceAddress() {
        return sourceAddress;
    }

    public Address getDestinationAddress() {
        return destinationAddress;
    }

    public Object getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "MessageEvent{" + super.toString() + ", sourceAddress=" + sourceAddress + ", destinationAddress=" + destinationAddress
                + ", message=" + message + '}';
    }

}
