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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Locale;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }

    public static byte[] copyContentsToArray(ByteBuffer src) {
        Validate.notNull(src);
        return copyContentsToArray(src, true);
    }
    
    public static byte[] copyContentsToArray(ByteBuffer src, boolean incrementSrc) {
        Validate.notNull(src);
        if (!incrementSrc) {
            src.mark();
        }
        
        ByteBuffer dst = ByteBuffer.allocate(src.remaining());
        dst.put(src);
        
        if (!incrementSrc) {
            src.reset();
        }
        
        return dst.array();
    }

    public static ByteBuffer copyContents(ByteBuffer src) {
        Validate.notNull(src);
        return copyContents(src, true, false);
    }
    
    public static ByteBuffer copyContents(ByteBuffer src, boolean incrementSrc, boolean incrementDst) {
        Validate.notNull(src);
        if (!incrementSrc) {
            src.mark();
        }
        
        ByteBuffer dst = ByteBuffer.allocate(src.remaining());
        dst.put(src);
        
        if (!incrementSrc) {
            src.reset();
        }
        
        if (!incrementDst) {
            dst.flip();
        }
        
        return dst;
    }
    
    public static String socketAddressToHexString(SocketChannel socketChannel) throws IOException {
        return (Hex.encodeHexString(((InetSocketAddress) socketChannel.getLocalAddress()).getAddress().getAddress())
                + Integer.toString(((InetSocketAddress) socketChannel.getLocalAddress()).getPort(), 16)
                + Hex.encodeHexString(((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress().getAddress())
                + Integer.toString(((InetSocketAddress) socketChannel.getRemoteAddress()).getPort(), 16)).toLowerCase(Locale.ENGLISH);
    }
}
