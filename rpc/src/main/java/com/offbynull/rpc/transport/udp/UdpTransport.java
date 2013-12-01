package com.offbynull.rpc.transport.udp;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.IncomingMessage;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.IncomingResponse;
import com.offbynull.rpc.transport.OutgoingFilter;
import com.offbynull.rpc.transport.OutgoingMessage;
import com.offbynull.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.rpc.transport.OutgoingResponse;
import com.offbynull.rpc.transport.Transport;
import com.offbynull.rpc.transport.udp.TimeoutManager.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A UDP transport implementation.
 * @author Kasra F
 */
public final class UdpTransport implements Transport<InetSocketAddress> {
    
    private InetSocketAddress listenAddress;
    private Selector selector;
    
    private int bufferSize;
    private int cacheSize;
    private long timeout;
    
    private LinkedBlockingQueue<Command> commandQueue;
    
    private EventLoop eventLoop;
    private Lock accessLock;

    /**
     * Constructs a {@link UdpTransport} object.
     * @param port port to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param timeout timeout duration
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     */
    public UdpTransport(int port, int bufferSize, int cacheSize, long timeout) throws IOException {
        this(new InetSocketAddress(port), bufferSize, cacheSize, timeout);
    }

    /**
     * Constructs a {@link UdpTransport} object.
     * @param listenAddress address to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param timeout timeout duration
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
    public UdpTransport(InetSocketAddress listenAddress, int bufferSize, int cacheSize, long timeout) throws IOException {
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bufferSize);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);

        this.listenAddress = listenAddress;
        this.selector = Selector.open();
        
        this.bufferSize = bufferSize;
        this.cacheSize = cacheSize;
        this.timeout = timeout;
        
        this.commandQueue = new LinkedBlockingQueue<>();
        
        accessLock = new ReentrantLock();
    }
    
    @Override
    public void start(IncomingFilter<InetSocketAddress> incomingFilter, IncomingMessageListener<InetSocketAddress> incomingMessageListener,
            OutgoingFilter<InetSocketAddress> outgoingFilter) throws IOException {
        accessLock.lock();
        try {
            if (eventLoop != null) {
                throw new IllegalStateException();
            }
            
            Validate.notNull(incomingFilter);
            Validate.notNull(outgoingFilter);
            Validate.notNull(incomingMessageListener);

            eventLoop = new EventLoop(incomingFilter, incomingMessageListener, outgoingFilter);
            eventLoop.startAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void stop() throws IOException {
        accessLock.lock();
        try {
            if (eventLoop == null || !eventLoop.isRunning()) {
                throw new IllegalStateException();
            }

            eventLoop.stopAndWait();
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void sendMessage(OutgoingMessage message, OutgoingMessageResponseListener listener) {
        Validate.notNull(message);
        Validate.notNull(listener);
        Validate.validState(eventLoop != null && eventLoop.isRunning());
        
        commandQueue.add(new CommandSendRequest(message, listener));
        selector.wakeup();
    }
    
    private final class EventLoop extends AbstractExecutionThreadService {

        private DatagramChannel channel;
        private AtomicBoolean stop;
        
        private MessageIdGenerator idGenerator;
        private MessageIdCache idCache;
        private TimeoutManager timeoutManager;
        
        private IncomingFilter<InetSocketAddress> inFilter;
        private IncomingMessageListener<InetSocketAddress> handler;
        private OutgoingFilter<InetSocketAddress> outFilter;

        public EventLoop(IncomingFilter<InetSocketAddress> incomingFilter,
                IncomingMessageListener<InetSocketAddress> incomingMessageListener,
                OutgoingFilter<InetSocketAddress> outgoingFilter) throws IOException {
            this.inFilter = incomingFilter;
            this.handler = incomingMessageListener;
            this.outFilter = outgoingFilter;
            idGenerator = new MessageIdGenerator();
            idCache = new MessageIdCache(cacheSize);
            timeoutManager = new TimeoutManager(timeout);
            
            try {
                channel = DatagramChannel.open();
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
        }
        
        @Override
        protected void startUp() throws IOException {
            stop = new AtomicBoolean(false);
            try {
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                channel.socket().bind(listenAddress);
            } catch (RuntimeException | IOException e) {
                IOUtils.closeQuietly(selector);
                IOUtils.closeQuietly(channel);
                throw e;
            }
        }

        @Override
        protected void run() {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

            int selectionKey = SelectionKey.OP_READ;
            LinkedList<Event> internalEventQueue = new LinkedList<>();
            LinkedList<Command> dumpedCommandQueue = new LinkedList<>();
            while (true) {
                // get current time
                long currentTime = System.currentTimeMillis();
                
                // get outgoing data
                commandQueue.drainTo(dumpedCommandQueue);
                
                // set selection key based on if there's commands available -- this works because the only commands available are send req
                // and send resp
                int newSelectionKey = SelectionKey.OP_READ;
                if (!dumpedCommandQueue.isEmpty()) {
                    newSelectionKey |= SelectionKey.OP_WRITE;
                }
                
                if (newSelectionKey != selectionKey) {
                    selectionKey = newSelectionKey;
                    try {
                        channel.register(selector, selectionKey);
                    } catch (ClosedChannelException cce) {
                        throw new RuntimeException(cce);
                    }
                }
                
                // get timed out channels + max amount of time to wait till next timeout
                Result timeoutRes = timeoutManager.evaluate(currentTime);
                long waitDuration = timeoutRes.getWaitDuration();

                // go through receivers requestManager has removed for timing out and add timeout events for each of them
                boolean timeoutEventAdded = false;
                for (OutgoingMessageResponseListener<InetSocketAddress> receiver : timeoutRes.getTimedOutReceivers()) {
                    internalEventQueue.add(new EventResponseTimedOut(receiver));
                    
                    timeoutEventAdded = true;
                }
                
                // select
                try {
                    // if timeout event was added then don't wait, because we need to process those events
                    if (timeoutEventAdded) {
                        selector.selectNow();
                    } else {
                        selector.select(waitDuration);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


                
                // stop if signalled
                if (stop.get()) {
                    return;
                }
                
                // update current time
                currentTime = System.currentTimeMillis();
                
                // go through selected keys
                Iterator keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) { // incoming data available
                        try {
                            buffer.clear();
                            
                            InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);
                            buffer.flip();

                            ByteBuffer tempBuffer = inFilter.filter(from, buffer);
                            
                            if (MessageMarker.isRequest(tempBuffer)) {
                                MessageMarker.skipOver(tempBuffer);

                                MessageId id = MessageId.extractPrependedId(tempBuffer);
                                MessageId.skipOver(tempBuffer);
                                
                                if (!idCache.add(from, id, PacketType.REQUEST)) {
                                    throw new RuntimeException("Duplicate messageid encountered");
                                }

                                IncomingMessage<InetSocketAddress> request = new IncomingMessage<>(from, tempBuffer, currentTime);

                                EventRequestArrived eventRa = new EventRequestArrived(request, selector, id);
                                internalEventQueue.add(eventRa);
                            } else if (MessageMarker.isResponse(tempBuffer)) {
                                MessageMarker.skipOver(tempBuffer);

                                MessageId id = MessageId.extractPrependedId(tempBuffer);
                                MessageId.skipOver(tempBuffer);
                                
                                if (!idCache.add(from, id, PacketType.RESPONSE)) {
                                    throw new RuntimeException("Duplicate messageid encountered");
                                }

                                OutgoingMessageResponseListener<InetSocketAddress> receiver = timeoutManager.getReceiver(from, id);

                                if (receiver != null) { // timeout may have lapsed already, don't do anything if it did
                                    IncomingResponse<InetSocketAddress> response = new IncomingResponse<>(from, tempBuffer, currentTime);

                                    EventResponseArrived eventRa = new EventResponseArrived(response, receiver);
                                    internalEventQueue.add(eventRa);
                                }
                            } else {
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                        }
                    } else if (key.isWritable()) { // ready for outgoing data
                        try {
                            Command command = dumpedCommandQueue.poll();

                            if (command instanceof CommandSendRequest) {
                                CommandSendRequest commandSr = (CommandSendRequest) command;

                                OutgoingMessage<InetSocketAddress> request = commandSr.getMessage();
                                OutgoingMessageResponseListener<InetSocketAddress> receiver = commandSr.getResponseListener();

                                MessageId id = idGenerator.generate();

                                buffer.clear();

                                MessageMarker.writeRequestMarker(buffer);
                                id.writeId(buffer);
                                buffer.put(request.getData());
                                
                                buffer.flip();

                                InetSocketAddress to = request.getTo();

                                timeoutManager.addRequestId(to, id, receiver, currentTime);
                                
                                ByteBuffer tempBuffer = outFilter.filter(to, buffer);
                                
                                channel.send(tempBuffer, to);
                            } else if (command instanceof CommandSendResponse) {
                                CommandSendResponse commandSr = (CommandSendResponse) command;

                                OutgoingResponse response = commandSr.getResponse();
                                InetSocketAddress to = commandSr.getAddress();
                                MessageId id = commandSr.getMessageId();

                                buffer.clear();

                                MessageMarker.writeResponseMarker(buffer);
                                id.writeId(buffer);
                                buffer.put(response.getData());
                                
                                buffer.flip();
                                
                                ByteBuffer tempBuffer = outFilter.filter(to, buffer);
                                
                                channel.send(tempBuffer, to);                        
                            } else {
                                throw new IllegalStateException();
                            }
                        } catch (RuntimeException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                processEvents(internalEventQueue);
                internalEventQueue.clear();
            }
        }

        private void processEvents(LinkedList<Event> internalEventQueue) {
            for (Event event : internalEventQueue) {
                if (event instanceof EventRequestArrived) {
                    EventRequestArrived request = (EventRequestArrived) event;
                    
                    IncomingMessage<InetSocketAddress> data = request.getRequest();
                    Selector selector = request.getSelector();
                    MessageId id = request.getId();
                    UdpIncomingMessageResponseHandler responseSender = new UdpIncomingMessageResponseHandler(commandQueue, selector, id,
                            data.getFrom());

                    try {
                        handler.messageArrived(data, responseSender);
                    } catch (RuntimeException re) {
                        // don't bother notifying the others
                        break;
                    }
                } else if (event instanceof EventResponseArrived) {
                    EventResponseArrived response = (EventResponseArrived) event;
                    IncomingResponse<InetSocketAddress> data = response.getResponse();

                    try {
                        response.getReceiver().responseArrived(data);
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                } else if (event instanceof EventResponseTimedOut) {
                    EventResponseTimedOut response = (EventResponseTimedOut) event;

                    try {
                        response.getReceiver().timedOut();
                    } catch (RuntimeException re) {
                        // do nothing
                    }
                }
            }
        }
        
        @Override
        protected void shutDown() throws Exception {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
            
            for (OutgoingMessageResponseListener receiver : timeoutManager.pending().getTimedOutReceivers()) {
                receiver.internalErrorOccurred(null);
            }
        }

        @Override
        protected String serviceName() {
            return UdpTransport.class.getSimpleName() + " Event Loop (" + listenAddress + ")";
        }

        @Override
        protected void triggerShutdown() {
            stop.set(true);
            selector.wakeup();
        }
    }
}