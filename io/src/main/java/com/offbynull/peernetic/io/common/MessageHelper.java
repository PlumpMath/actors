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
package com.offbynull.peernetic.io.common;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class MessageHelper {
    private final Object sendObject;
    private final Address selfAddress;
    private final Address destinationAddress;
    private final Address logAddress;
    private final Set<Class<?>> acceptClasses;
    private final Set<Class<?>> errorClasses;
    private final Set<Object> errorObjects;

    private MessageHelper(Object sendObject, Address selfAddress, Address destinationAddress, Address logAddress,
            Set<Class<?>> acceptClasses, Set<Class<?>> errorClasses, Set<Object> errorObjects) {
//        Validate.notNull(sendObject);
        Validate.notNull(selfAddress);
        Validate.notNull(destinationAddress);
//        Validate.notNull(logAddress);
        Validate.notNull(acceptClasses);
        Validate.notNull(errorClasses);
        Validate.notNull(errorObjects);
        Validate.noNullElements(acceptClasses);
        Validate.noNullElements(errorClasses);
        Validate.noNullElements(errorObjects);
        Validate.isTrue(!acceptClasses.isEmpty());
        this.sendObject = sendObject;
        this.selfAddress = selfAddress;
        this.destinationAddress = destinationAddress;
        this.logAddress = logAddress;
        this.acceptClasses = acceptClasses;
        this.errorClasses = errorClasses;
        this.errorObjects = errorObjects;
    }
    
    public <T> T perform(Continuation cnt) {
        Context ctx = (Context) cnt.getContext();

        if (sendObject != null) {
            ctx.addOutgoingMessage(selfAddress, destinationAddress, sendObject);
        }
        
        while (true) {
            cnt.suspend();
            
            Object obj = ctx.getIncomingMessage();
            if (errorObjects.contains(obj) || errorClasses.contains(obj.getClass())) {
                if (logAddress != null) {
                    ctx.addOutgoingMessage(selfAddress, logAddress, LogMessage.error("Encountered error type {}", obj));
                }
                throw new RuntimeException("Encountered error " + obj);
            }

            if (acceptClasses.contains(obj.getClass())) {
                if (logAddress != null) {
                    ctx.addOutgoingMessage(selfAddress, logAddress, LogMessage.debug("Encountered accept type {}", obj));
                }
                return (T) obj;
            }

            ctx.addOutgoingMessage(selfAddress, logAddress, LogMessage.debug("Encountered unexpected type {}", obj));
        }
    }

    static final class Builder {
        private Object sendObject;
        private Address selfAddress;
        private Address destionationddress;
        private Address logAddress;
        private Set<Class<?>> acceptClasses;
        private Set<Class<?>> errorClasses;
        private Set<Object> errorObjects;        

        public Builder sendObject(Object sendObject) {
            this.sendObject = sendObject;
            return this;
        }

        public Builder selfAddress(Address selfAddress) {
            this.selfAddress = selfAddress;
            return this;
        }

        public Builder destinationAddress(Address destinationAddress) {
            this.destionationddress = destinationAddress;
            return this;
        }

        public Builder logAddress(Address logAddress) {
            this.logAddress = logAddress;
            return this;
        }

        public Builder addAcceptClass(Class<?> e) {
            acceptClasses.add(e);
            return this;
        }

        public Builder addErrorClass(Class<?> e) {
            errorClasses.add(e);
            return this;
        }

        public Builder addErrorObject(Object e) {
            errorObjects.add(e);
            return this;
        }
        
        public MessageHelper build() {
            return new MessageHelper(sendObject, selfAddress, destionationddress, logAddress, acceptClasses, errorClasses, errorObjects);
        }
    }
}
