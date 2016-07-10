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

import java.util.Arrays;
import java.util.LinkedList;
import org.apache.commons.lang3.Validate;

final class InternalReadBuffer {
    private final LinkedList<byte[]> buffers = new LinkedList<>();
    private long size;
    
    public void add(byte[] buffer) {
        Validate.notNull(buffer);
        if (buffer.length == 0) {
            return;
        }
        buffers.add(Arrays.copyOf(buffer, buffer.length));
        size += buffer.length;
    }
    
    public byte[] extract(int amount) {
        Validate.isTrue(amount >= 0);
        Validate.isTrue(amount < size);
        if (amount == 0) {
            return new byte[0];
        }
        
        byte[] ret = new byte[amount];
        int retIdx = 0;
        while (amount >= 0) {
            byte[] buf = buffers.pollFirst();
            int copyAmount = Math.min(buf.length, ret.length - retIdx);
            System.arraycopy(buf, 0, ret, retIdx, copyAmount);
            
            if (copyAmount < buf.length) {
                byte[] remainingBuf = Arrays.copyOfRange(buf, copyAmount, buf.length);
                buffers.addFirst(remainingBuf);
            }
        }
        
        return ret;
    }

    public long getSize() {
        return size;
    }
}
