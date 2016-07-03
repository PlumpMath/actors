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
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import static com.offbynull.peernetic.io.gateways.network.InternalUtils.socketAddressToHexString;
import com.offbynull.peernetic.io.gateways.network.UdpNetworkEntry.AddressedByteBuffer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NetworkRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkRunnable.class);

    private final Address selfPrefix;
    private final Address proxyPrefix;
    private final Shuttle proxyShuttle; // output shuttle where messages are supposed to go
    private final Bus bus; // bus from this gateway's shuttle
    
    private final LinkedBlockingQueue<Message> incomingQueue;
    private final LinkedBlockingQueue<Message> outgoingQueue;

    private final Selector selector;
    
    // intended only for use by thread -- no outside access
    private final Map<Address, NetworkEntry> idMap; // get by self suffix
    private final Map<Channel, NetworkEntry> channelMap;
    private final ByteBuffer buffer;
    private LinkedList<Message> localOutQueue;
    
    private final UnmodifiableMap<Class<?>, Processor> processors;

    NetworkRunnable(Address selfPrefix,
            Address proxyPrefix,
            Shuttle proxyShuttle,
            Bus bus,
            int bufferSize) {
        Validate.notNull(selfPrefix);
        Validate.notNull(proxyPrefix);
        Validate.notNull(proxyShuttle);
        Validate.notNull(bus);
        Validate.isTrue(bufferSize > 0);
        
        Map<Class<?>, Processor> processorsMap = new HashMap<>();
        processorsMap.put(TcpCreateRequest.class, this::processTcpCreateRequest);
        processorsMap.put(UdpCreateRequest.class, this::processUdpCreateRequest);
        processorsMap.put(LocalIpAddressesRequest.class, this::processLocalIpAddressesRequest);
        processorsMap.put(TcpWriteRequest.class, this::processTcpWriteRequest);
        processorsMap.put(UdpWriteRequest.class, this::processUdpWriteRequest);
        processorsMap.put(CloseRequest.class, this::processCloseRequest);
        this.processors = (UnmodifiableMap<Class<?>, Processor>) UnmodifiableMap.unmodifiableMap(processorsMap);
        
        this.selfPrefix = selfPrefix;
        this.proxyPrefix = proxyPrefix;
        this.proxyShuttle = proxyShuttle;
        this.bus = bus;
        
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.outgoingQueue = new LinkedBlockingQueue<>();
        
        idMap = new HashMap<>();
        channelMap = new HashMap<>();
        buffer = ByteBuffer.allocate(bufferSize);
        localOutQueue = new LinkedList<>();
        
        try {
            selector = Selector.open();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    public void run() {
        LOG.debug("Starting gateway");
        
        LOG.info("Creating message pump threads");
        // messages leaving us to proxy
        Runnable outRunnable = new OutgoingMessagePumpRunnable(selfPrefix, proxyPrefix, proxyShuttle, incomingQueue);
        Thread inThread = new Thread(outRunnable, "NIO Out Msg Pump - " + selfPrefix);
        inThread.setDaemon(true);
        // messages arriving to us from proxy
        Runnable inRunnable = new IncomingMessagePumpRunnable(selfPrefix, proxyPrefix, bus, outgoingQueue, selector);
        Thread outThread = new Thread(inRunnable, "NIO In Msg Pump - " + selfPrefix);
        outThread.setDaemon(true);
        

        try {
            while (true) {
                localOutQueue.clear();

                // Handle incoming network IO events
                selector.select();
                for (SelectionKey key : selector.selectedKeys()) {
                    if (!key.isValid()) {
                        continue;
                    }
                    Channel channel = (Channel) key.channel();
                    NetworkEntry entry = channelMap.get(channel);
                    if (entry == null) {
                        channel.close();
                        continue;
                    }
                    try {
                        if (channel instanceof SocketChannel) {
                            handleSelectForTcpChannel(key, (TcpNetworkEntry) entry);
                        } else if (channel instanceof DatagramChannel) {
                            handleSelectForUdpChannel(key, (UdpNetworkEntry) entry);
                        } else if (channel instanceof ServerSocketChannel) {
                            handleSelectForTcpServerChannel(key, (TcpServerNetworkEntry) entry);
                        } else {
                            throw new IllegalStateException(); // should never happen
                        }
                        updateSelectionKey(entry, (AbstractSelectableChannel) channel);
                    } catch (RuntimeException e) {
                        queueOutgoingMessage(entry, new ErrorNotification());
                        LOG.error("Exception encountered: {}", entry, e);
                    }
                }
                
                // Get incoming messages and process
                LinkedList<Message> localInQueue = new LinkedList<>();
                incomingQueue.drainTo(localInQueue);
                for (Message incomingEnvelope : localInQueue) {
                    processMessage(incomingEnvelope);
                }
                
                // Push outgoing messages
                outgoingQueue.addAll(localOutQueue);
            }
        } catch (Exception e) {
            LOG.error("Encountered unexpected exception", e);
            throw new RuntimeException(e); // rethrow exception
        } finally {
            LOG.debug("Stopping gateway");
            shutdownResources();
            LOG.debug("Shutdown of resources complete");
        }
    }

    private void handleSelectForTcpChannel(SelectionKey selectionKey, TcpNetworkEntry entry) {
        SocketChannel channel = (SocketChannel) entry.getChannel();
        if (selectionKey.isConnectable()) {
            tcpConnectReady(channel, entry);
        }
        if (selectionKey.isReadable()) {
            tcpReadReady(channel, entry);
        }
        if (selectionKey.isWritable()) {
            tcpWriteReady(channel, entry);
        }
    }

    private void tcpConnectReady(SocketChannel channel, TcpNetworkEntry entry) {
        LOG.debug("{} TCP connection", entry);
        try {
            // This block is sometimes called more than once for each connection -- we still call finishConnect but we also check to
            // see if we're already connected before sending the CreateTcpSocketNetworkResponse msg
            boolean alreadyConnected = channel.isConnected();
            boolean connected = channel.finishConnect();
            if (!alreadyConnected) {
                if (connected) {
                    entry.setConnecting(false);
                    queueOutgoingMessage(entry, new TcpCreateResponse());
                } else {
                    queueOutgoingMessage(entry, new ErrorResponse());
                }
            }
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorResponse());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }

    private void tcpReadReady(SocketChannel channel, TcpNetworkEntry entry) {
        try {
            buffer.clear();
            int readCount = channel.read(buffer);
            buffer.flip();

            LOG.debug("{} TCP read {} bytes", entry, readCount);

            if (readCount == -1) {
                // read finished, set flag to stop requesting read notifications
                entry.setReadFinished(true);
                queueOutgoingMessage(entry, new TcpReadClosedNotification());
            } else if (buffer.remaining() > 0) {
                byte[] bufferAsArray = InternalUtils.copyContentsToArray(buffer);
                queueOutgoingMessage(entry, new TcpReadNotification(bufferAsArray));
            }
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorNotification());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }   

    private void tcpWriteReady(SocketChannel channel, TcpNetworkEntry entry) {
        try {
            LinkedList<ByteBuffer> outBuffers = entry.getOutgoingBuffers();
            // if OP_WRITE was set, WriteTcpBlockNetworkRequest is pending (we should have at least 1 outgoing buffer)
            int writeCount = 0;
            if (outBuffers.isEmpty() && !entry.isNotifiedOfWritable()) {
                LOG.debug("{} TCP write empty", entry);

                // if empty but not notified yet
                entry.setNotifiedOfWritable(true);
                queueOutgoingMessage(entry, new TcpWriteEmptyNotification());
            } else {
                while (!outBuffers.isEmpty()) {
                    ByteBuffer outBuffer = outBuffers.getFirst();
                    writeCount += channel.write(outBuffer);

                    LOG.debug("{} TCP wrote {} bytes", entry, writeCount);

                    if (outBuffer.remaining() > 0) {
                        // not everything was written, which means we can't send anymore data until we get another OP_WRITE, so leave
                        break;
                    }
                    outBuffers.removeFirst();
                    queueOutgoingMessage(entry, new TcpWriteResponse(writeCount));
                }
            }
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorNotification());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }

    private void handleSelectForUdpChannel(SelectionKey selectionKey, UdpNetworkEntry entry) {
        DatagramChannel channel = (DatagramChannel) entry.getChannel();
        if (selectionKey.isReadable()) {
            udpReadReady(channel, entry);
        }
        if (selectionKey.isWritable()) {
            udpWriteReady(channel, entry);
        }
    }

    private void udpReadReady(DatagramChannel channel, UdpNetworkEntry entry) {
        try {
            buffer.clear();

            // Would directly call DatagramChannel.getLocalAddress(), but this doesn't look to be available on android. Doing this
            // on Java 7/8 performs the same function -- it probably does the same on Android as well?
            InetSocketAddress localAddress = (InetSocketAddress) channel.socket().getLocalSocketAddress();
            InetSocketAddress remoteAddress = (InetSocketAddress) channel.receive(buffer);

            LOG.debug("{} UDP read {} bytes from {} to {}", entry, buffer.position(), remoteAddress, localAddress);

            if (remoteAddress != null) {
                buffer.flip();
                byte[] bufferAsArray = InternalUtils.copyContentsToArray(buffer);
                queueOutgoingMessage(entry, new UdpReadNotification(localAddress, remoteAddress, bufferAsArray));
            }
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorNotification());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }   

    private void udpWriteReady(DatagramChannel channel, UdpNetworkEntry entry) {
        try {
            LinkedList<AddressedByteBuffer> outBuffers = entry.getOutgoingBuffers();
            if (!outBuffers.isEmpty()) {
                // if not empty
                AddressedByteBuffer outBuffer = outBuffers.removeFirst();

                ByteBuffer outgoingBuffer = outBuffer.getBuffer();

                // Would directly call DatagramChannel.getLocalAddress(), but this doesn't look to be available on android. Doing this
                // on Java 7/8 performs the same function -- it probably does the same on Android as well?
                InetSocketAddress localAddress = (InetSocketAddress) channel.socket().getLocalSocketAddress();
                InetSocketAddress remoteAddress = outBuffer.getSocketAddress();
                int totalCount = outgoingBuffer.remaining();

                int writeCount = channel.send(outgoingBuffer, remoteAddress);

                LOG.debug("{} UDP wrote {} bytes of {} from {} to {}", entry, writeCount, totalCount, localAddress, remoteAddress);

                queueOutgoingMessage(entry, new UdpWriteResponse(writeCount));
            } else if (!entry.isNotifiedOfWritable()) {
                LOG.debug("{} UDP write empty", entry);

                // if empty but not notified yet
                entry.setNotifiedOfWritable(true);
                queueOutgoingMessage(entry, new UdpWriteEmptyNotification());
            }
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorNotification());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }

    private void handleSelectForTcpServerChannel(SelectionKey selectionKey, TcpServerNetworkEntry entry) {
        ServerSocketChannel channel = (ServerSocketChannel) entry.getChannel();
        if (selectionKey.isAcceptable()) {
            tcpServerAcceptReady(channel, entry);
        }
    }

    private void tcpServerAcceptReady(ServerSocketChannel channel, TcpServerNetworkEntry entry) {
        LOG.debug("{} TCP accept", entry);
        try {
            SocketChannel socketChannel = channel.accept();
            socketChannel.configureBlocking(false);

            
            Address selfSuffix = entry.getSelfSuffix().appendSuffix(socketAddressToHexString(socketChannel));
            Address proxySuffix = entry.getProxySuffix().appendSuffix(socketAddressToHexString(socketChannel));
            TcpNetworkEntry tcpEntry = new TcpNetworkEntry(selfSuffix, proxySuffix, channel);
            tcpEntry.setConnecting(false);

            idMap.put(selfSuffix, entry);
            channelMap.put(channel, entry);
            
            updateSelectionKey(tcpEntry, channel);
            
            queueOutgoingMessage(
                    entry,
                    new TcpServerAcceptNotification(
                            ((InetSocketAddress) socketChannel.getLocalAddress()).getAddress(),
                            ((InetSocketAddress) socketChannel.getLocalAddress()).getPort(),
                            ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress(),
                            ((InetSocketAddress) socketChannel.getRemoteAddress()).getPort(),
                            selfPrefix.appendSuffix(selfSuffix),
                            proxyPrefix.appendSuffix(proxySuffix)));
        } catch (IOException ioe) {
            queueOutgoingMessage(entry, new ErrorResponse());
            LOG.error("Exception encountered: {}", entry, ioe);
        }
    }

    private void updateSelectionKey(NetworkEntry entry, AbstractSelectableChannel channel) throws ClosedChannelException {
        int newKey = 0;
        if (entry instanceof TcpNetworkEntry) {
            TcpNetworkEntry tcpNetworkEntry = (TcpNetworkEntry) entry;
            if (tcpNetworkEntry.isConnecting()) {
                // if connecting (tcp-only)
                newKey |= SelectionKey.OP_CONNECT;
            }
            
            if (!tcpNetworkEntry.isReadFinished()) {
                newKey |= SelectionKey.OP_READ;
            }
            
            if (!tcpNetworkEntry.getOutgoingBuffers().isEmpty()) {
                // if not empty
                newKey |= SelectionKey.OP_WRITE;
                tcpNetworkEntry.setNotifiedOfWritable(false);
            } else if (!tcpNetworkEntry.isNotifiedOfWritable()) {
                // if is empty but not notified yet
                newKey |= SelectionKey.OP_WRITE;
            }
        } else if (entry instanceof UdpNetworkEntry) {
            UdpNetworkEntry udpNetworkEntry = (UdpNetworkEntry) entry;
            newKey |= SelectionKey.OP_READ;
            
            if (!udpNetworkEntry.getOutgoingBuffers().isEmpty()) {
                // if not empty
                newKey |= SelectionKey.OP_WRITE;
                udpNetworkEntry.setNotifiedOfWritable(false);
            } else if (!udpNetworkEntry.isNotifiedOfWritable()) {
                // if is empty but not notified yet
                newKey |= SelectionKey.OP_WRITE;
            }
        } else if (entry instanceof TcpServerNetworkEntry) {
            newKey |= SelectionKey.OP_ACCEPT;
        }
        
        if (newKey != entry.getSelectionKey()) {
            entry.setSelectionKey(newKey);
            LOG.debug("{} Key updated to {}", entry, newKey);
            channel.register(selector, newKey); // register new key if different -- calling register may have performance issues?
        }
    }
    
    private void processMessage(Message envelope) throws IOException {
        LOG.debug("Processing message: {}", envelope);

        Object msg = envelope.getMessage();
        Address proxySuffix = envelope.getSourceAddress().removePrefix(proxyPrefix);
        Address selfSuffix = envelope.getDestinationAddress().removePrefix(selfPrefix);
        
        NetworkEntry networkEntry = idMap.get(selfSuffix);
        if (networkEntry != null) {
            Address expectedProxySuffix = networkEntry.getProxySuffix();
            Address expectedSelfSuffix = networkEntry.getSelfSuffix();
            
            if (!expectedSelfSuffix.equals(selfSuffix) || !expectedProxySuffix.equals(proxySuffix)) {
                queueOutgoingMessage(envelope, new ErrorResponse());
                LOG.error("Address suffixes don't match: {} vs {} and {} vs {}", expectedSelfSuffix, proxySuffix,
                        expectedSelfSuffix, selfSuffix);
                return;
            }
        }
        
        Processor processor = processors.get(msg.getClass());
        if (processor == null) {
            LOG.error("Unrecognized message: {}", msg);
            return;
        }
        
        processor.process(msg, proxySuffix, selfSuffix, networkEntry);
    }
    
    private void processUdpCreateRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException { 
        UdpCreateRequest req = (UdpCreateRequest) msg;
        if (networkEntry != null) {
            queueOutgoingMessage(networkEntry, new ErrorResponse());
            LOG.error("Socket already exists: {}", networkEntry);
            return;
        }

        DatagramChannel channel = null;
        UdpNetworkEntry entry = null;
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);

            // Would directly call DatagramChannel.bind(), but this doesn't look to be available on android. Doing this on Java 7/8
            // performs the same function -- it probably does the same on Android as well?
            channel.socket().bind(new InetSocketAddress(req.getSourceAddress(), 0));

            entry = new UdpNetworkEntry(selfSuffix, proxySuffix, channel);
            updateSelectionKey(entry, channel);

            idMap.put(selfSuffix, entry);
            channelMap.put(channel, entry);

            queueOutgoingMessage(entry, new UdpCreateResponse());
        } catch (RuntimeException re) {
            if (channel != null) {
                IOUtils.closeQuietly(channel);
            }

            if (entry != null) {
                idMap.remove(entry.getSelfSuffix());
                channelMap.remove(entry.getChannel());
            }

            queueOutgoingMessage(entry, new ErrorResponse());
            LOG.error("Unable to create socket: {}", entry, re);
        }
    }

    private void processTcpCreateRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
        TcpCreateRequest req = (TcpCreateRequest) msg;
        if (networkEntry != null) {
            queueOutgoingMessage(networkEntry, new ErrorResponse());
            LOG.error("Socket already exists: {}", networkEntry);
            return;
        }

        SocketChannel channel = null;
        TcpNetworkEntry entry = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            // Would directly call SocketChannel.bind(), but this doesn't look to be available on android. Doing this on Java 7/8
            // performs the same function -- it probably does the same on Android as well?
            channel.socket().bind(new InetSocketAddress(req.getSourceAddress(), 0));
            InetSocketAddress dst = new InetSocketAddress(req.getDestinationAddress(), req.getDestinationPort());
            channel.connect(dst);

            entry = new TcpNetworkEntry(selfSuffix, proxySuffix, channel);
            entry.setConnecting(true);
            updateSelectionKey(entry, channel);

            idMap.put(selfSuffix, entry);
            channelMap.put(channel, entry);
        } catch (RuntimeException re) {
            if (channel != null) {
                IOUtils.closeQuietly(channel);
            }

            if (entry != null) {
                idMap.remove(entry.getSelfSuffix());
                channelMap.remove(entry.getChannel());
            }

            queueOutgoingMessage(entry, new ErrorResponse());
            LOG.error("Unable to create socket: {}", entry, re);
        }
    }

    private void processTcpServerCreateRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
        TcpServerCreateRequest req = (TcpServerCreateRequest) msg;
        if (networkEntry != null) {
            queueOutgoingMessage(networkEntry, new ErrorResponse());
            LOG.error("Socket already exists: {}", networkEntry);
            return;
        }

        ServerSocketChannel channel = null;
        TcpServerNetworkEntry entry = null;
        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            // Would directly call SocketChannel.bind(), but this doesn't look to be available on android. Doing this on Java 7/8
            // performs the same function -- it probably does the same on Android as well?
            channel.socket().bind(new InetSocketAddress(req.getSourceAddress(), req.getSourcePort()));

            entry = new TcpServerNetworkEntry(selfSuffix, proxySuffix, channel);
            updateSelectionKey(entry, channel);

            idMap.put(selfSuffix, entry);
            channelMap.put(channel, entry);

            queueOutgoingMessage(entry, new TcpServerCreateResponse());
        } catch (RuntimeException re) {
            if (channel != null) {
                IOUtils.closeQuietly(channel);
            }

            if (entry != null) {
                idMap.remove(entry.getSelfSuffix());
                channelMap.remove(entry.getChannel());
            }

            queueOutgoingMessage(entry, new ErrorResponse());
            LOG.error("Unable to create socket: {}", entry, re);
        }
    }
    
    private void processLocalIpAddressesRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
//      LocalIpAddressesRequest req = (LocalIpAddressesRequest) msg;
        Set<InetAddress> ret = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        ret.add(addr);
                    }
                }
            }
            queueOutgoingMessage(selfSuffix, proxySuffix, new LocalIpAddressesResponse(ret));
        } catch (RuntimeException re) {
            LOG.error("Unable to process message", re);
            queueOutgoingMessage(selfSuffix, proxySuffix, new ErrorResponse());
        }
    }
    
    private void processTcpWriteRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
        TcpWriteRequest req = (TcpWriteRequest) msg;
        if (networkEntry == null) {
            queueOutgoingMessage(selfSuffix, proxySuffix, new ErrorResponse());
            LOG.error("Socket does not exist: {}", networkEntry);
            return;
        }
        
        try {
            TcpNetworkEntry entry = (TcpNetworkEntry) networkEntry;

            LinkedList<ByteBuffer> outBuffers = entry.getOutgoingBuffers();
            ByteBuffer writeBuffer = ByteBuffer.wrap(req.getData());
            if (writeBuffer.hasRemaining()) {
                // only add if it has content -- adding empty is worthless because this is a stream
                outBuffers.add(writeBuffer);
            }
            AbstractSelectableChannel channel = (AbstractSelectableChannel) entry.getChannel();
            updateSelectionKey(entry, channel);
        } catch (RuntimeException re) {
            queueOutgoingMessage(networkEntry, new ErrorResponse());
            LOG.error("Unable to process message: {}", networkEntry, re);
        }
    }
    
    private void processUdpWriteRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
        UdpWriteRequest req = (UdpWriteRequest) msg;
        if (networkEntry == null) {
            queueOutgoingMessage(selfSuffix, proxySuffix, new ErrorResponse());
            LOG.error("Socket does not exist: {}", networkEntry);
            return;
        }

        try {
            UdpNetworkEntry entry = (UdpNetworkEntry) networkEntry;

            LinkedList<AddressedByteBuffer> outBuffers = entry.getOutgoingBuffers();
            ByteBuffer writeBuffer = ByteBuffer.wrap(req.getData());
            InetSocketAddress writeAddress = req.getRemoteAddress();
            outBuffers.add(new AddressedByteBuffer(writeBuffer, writeAddress));
            AbstractSelectableChannel channel = (AbstractSelectableChannel) entry.getChannel();
            updateSelectionKey(entry, channel);
        } catch (RuntimeException re) {
            queueOutgoingMessage(networkEntry, new ErrorResponse());
            LOG.error("Unable to process message: {}", networkEntry, re);
        }
    }
    
    private void processCloseRequest(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry)
            throws IOException {
        if (networkEntry == null) {
            queueOutgoingMessage(selfSuffix, proxySuffix, new ErrorResponse());
            LOG.error("Socket does not exist: {}", networkEntry);
            return;
        }
        
        Channel channel = networkEntry.getChannel();
        IOUtils.closeQuietly(channel);
        
        idMap.remove(selfSuffix);
        channelMap.remove(channel);

        queueOutgoingMessage(networkEntry, new CloseResponse());
    }
    
    private interface Processor {
        void process(Object msg, Address proxySuffix, Address selfSuffix, NetworkEntry networkEntry) throws IOException;
    }

    private void queueOutgoingMessage(Message requestEnvelope, Object msg) {
        Address selfAddress = requestEnvelope.getDestinationAddress();
        Address proxyAddress = requestEnvelope.getSourceAddress();
        Message responseEnvelope = new Message(selfAddress, proxyAddress, msg);
        localOutQueue.add(responseEnvelope);
    }

    private void queueOutgoingMessage(NetworkEntry entry, Object msg) {
        Address selfAddress = selfPrefix.appendSuffix(entry.getSelfSuffix());
        Address proxyAddress = proxyPrefix.appendSuffix(entry.getProxySuffix());
        Message envelope = new Message(selfAddress, proxyAddress, msg);
        localOutQueue.add(envelope);
    }

    private void queueOutgoingMessage(Address selfSuffix, Address proxySuffix, Object msg) {
        Address selfAddress = selfPrefix.appendSuffix(selfSuffix);
        Address proxyAddress = proxyPrefix.appendSuffix(proxySuffix);
        Message envelope = new Message(selfAddress, proxyAddress, msg);
        localOutQueue.add(envelope);
    }

    private void shutdownResources() {
        LOG.debug("Shutting down all resources");
        
        for (Address selfSuffix : new HashSet<>(idMap.keySet())) { // shutdownResource removes items from idMap, so create a dupe of set
                                                                   // such that you don't encounter issues with making changes to the set
                                                                   // while you're iterating
            forcefullyShutdownResource(selfSuffix);
        }
        
        try {
            selector.close();
        } catch (Exception e) {
            LOG.error("Error shutting down selector", e);
        }
        channelMap.clear();
        idMap.clear();
    }

    private void forcefullyShutdownResource(Address selfSuffix) {
        NetworkEntry ne = idMap.remove(selfSuffix);
        
        LOG.debug("{} Attempting to shutdown", selfSuffix);
        
        Channel channel = null;
        try {
            channel = ne.getChannel();
            channelMap.remove(channel);
            
            queueOutgoingMessage(ne, new ErrorNotification());
        } catch (RuntimeException e) {
            LOG.error("{} Error shutting down resource", selfSuffix, e);
        } finally {
            IOUtils.closeQuietly(channel);
        }
    }

    public void close() {
        try {
            selector.close();
        } catch (IOException ioe) {
            LOG.warn("Failed to close", ioe);
        }
    }
}
