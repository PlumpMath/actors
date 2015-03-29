package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.*;
import io.netty.channel.Channel;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalShuttle implements Shuttle {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalShuttle.class);
    
    private String prefix;
    private Channel channel;

    InternalShuttle(String prefix, Channel channel) {
        Validate.notNull(prefix);
        Validate.notEmpty(prefix);
        Validate.notNull(channel);

        this.prefix = prefix;
        this.channel = channel;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void send(Collection<Message> messages) {
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        messages.forEach(x -> {
            try {
                String dst = x.getDestinationAddress();
                String dstPrefix = ActorUtils.getPrefix(dst);
                Validate.isTrue(dstPrefix.equals(prefix));

                InetSocketAddress dstAddr = fromShuttleAddress(dst);

                DefaultAddressedEnvelope datagramPacket = new DefaultAddressedEnvelope(x.getMessage(), dstAddr);
                channel.writeAndFlush(datagramPacket);
            } catch (Exception e) {
                LOGGER.error("Error pushing out message to UDP channel: " + x, e);
            }
        });
    }

    private static InetSocketAddress fromShuttleAddress(String address) throws UnknownHostException, DecoderException {
        int splitIdx = address.indexOf('.');
        Validate.isTrue(splitIdx != -1);

        String addrStr = address.substring(0, splitIdx);
        String portStr = address.substring(splitIdx + 1);

        InetAddress addr = InetAddress.getByAddress(Hex.decodeHex(addrStr.toCharArray()));
        int port = Integer.parseInt(portStr);

        return new InetSocketAddress(addr, port);
    }
}