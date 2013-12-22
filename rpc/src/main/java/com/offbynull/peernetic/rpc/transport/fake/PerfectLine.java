/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.rpc.transport.fake;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A line implementation without any constraints or failures. Takes no time to transmit a message.
 * @author Kasra F
 * @param <A> address type
 */
public final class PerfectLine<A> implements Line<A> {

    @Override
    public List<Message<A>> depart(A from, A to, ByteBuffer data) {
        return new ArrayList<>(Arrays.asList(new Message<>(from, to, data, 0L)));
    }

    @Override
    public void arrive(Collection<Message<A>> packets) {
    }
    
}