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
package com.offbynull.peernetic.network.gateways.udp;

import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

final class IncomingMessageShuttleHandler extends MessageToMessageDecoder<Object> {
    private String srcAddressPrefix;
    private Shuttle dstShuttle;
    private String dstAddress;

    IncomingMessageShuttleHandler(String srcAddressPrefix, Shuttle dstShuttle, String dstAddress) {
        Validate.notNull(srcAddressPrefix);
        Validate.notNull(dstShuttle);
        Validate.notNull(dstAddress);
        Validate.notEmpty(srcAddressPrefix);
        Validate.notEmpty(dstAddress);

        this.srcAddressPrefix = srcAddressPrefix;
        this.dstShuttle = dstShuttle;
        this.dstAddress = dstAddress;
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Object res;
        if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<? extends Object, ? extends SocketAddress> envelopeMsg =
                    (AddressedEnvelope<? extends Object, ? extends SocketAddress>) msg;
            
            SocketAddress sender = envelopeMsg.sender() == null ? ctx.channel().remoteAddress() : envelopeMsg.sender();
            SocketAddress recipient = envelopeMsg.recipient() == null ? ctx.channel().localAddress() : envelopeMsg.recipient();
            
            Object encoded = transform(recipient, sender, envelopeMsg.content());
            
            res = new DefaultAddressedEnvelope<>(encoded, envelopeMsg.recipient(), envelopeMsg.sender());
        } else {
            res = transform(ctx.channel().localAddress(), ctx.channel().remoteAddress(), msg);
        }
        
        out.add(res);
    }
    
    protected Object transform(SocketAddress localAddress, SocketAddress remoteAddress, Object obj) {
        EncapsulatedMessage em = (EncapsulatedMessage) obj;
        String srcAddress = srcAddressPrefix + SEPARATOR + toShuttleAddress(remoteAddress);
        String srcSuffix = em.getAddressSuffix();
        if (srcSuffix != null) {
            srcAddress += SEPARATOR + em.getAddressSuffix();
        }
        
        Object msgObj = em.getObject();
        
        Message message = new Message(
                srcAddress,
                dstAddress,
                msgObj);
        dstShuttle.send(Collections.singleton(message));
        
        return obj;
    }
    
    private static String toShuttleAddress(SocketAddress address) {
        byte[] addr = ((InetSocketAddress) address).getAddress().getAddress();
        int port = ((InetSocketAddress) address).getPort();
        
        return Hex.encodeHexString(addr) + '.' + port;
    }
}