package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.invoke.AsyncResultAdapter;
import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class LinkManager<A> {
    private static final int NEW_OUTGOING_LINKS_PER_CYCLE = 5;
    private static final long CYCLE_WAIT = 5000L;
    
    private Random random;
    private LinkedHashSet<A> addressCache;
    private IncomingLinkManager<A> incomingLinkManager;
    private OutgoingLinkManager<A> outgoingLinkManager;
    private Rpc<A> rpc;
    private Lock lock;
    
    public LinkManager(Rpc<A> rpc) throws NoSuchAlgorithmException {
        random = SecureRandom.getInstance("SHA1PRNG");
        addressCache = new LinkedHashSet<>();
        incomingLinkManager = new IncomingLinkManager(15, 30000L);
        outgoingLinkManager = new OutgoingLinkManager(15, 15000L, 30000L);
        this.rpc = rpc;
        lock = new ReentrantLock();
    }

    public boolean updateIncomingLink(long timestamp, A address, ByteBuffer secret) {
        lock.lock();
        try {
            return incomingLinkManager.updateLink(timestamp, address, secret);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean addIncomingLink(long timestamp, A address, ByteBuffer secret) {
        lock.lock();
        try {
            return incomingLinkManager.addLink(timestamp, address, secret);
        } finally {
            lock.unlock();
        }
    }
    
    public State<A> getState() {
        Set<A> incomingLinks;
        Set<A> outgoingLinks;
        int freeIncomingSlots;
        
        lock.lock();
        try {
            incomingLinks = incomingLinkManager.getLinks();
            outgoingLinks = outgoingLinkManager.getLinks();
            freeIncomingSlots = incomingLinkManager.getFreeSlots();
        } finally {
            lock.unlock();
        }
        
        return new State<>(incomingLinks, outgoingLinks, freeIncomingSlots == 0);
    }

    public long process(long timestamp) {
        establishNewOutgoingLinks(timestamp);
        maintainExistingOutgoingLinks(timestamp);
        
        return timestamp + CYCLE_WAIT;
    }

    private void maintainExistingOutgoingLinks(long timestamp) {
        Map<A, ByteBuffer> needsUpdateMap;
        lock.lock();
        try {
            OutgoingLinkManager.ProcessResult<A> result = outgoingLinkManager.process(timestamp);
            needsUpdateMap = result.getStaleAddresses();
        } finally {
            lock.unlock();
        }
        
        for (Entry<A, ByteBuffer> entry : needsUpdateMap.entrySet()) {
            UnstructuredServiceAsync<A> service = rpc.accessService(entry.getKey(), UnstructuredService.SERVICE_ID,
                    UnstructuredService.class, UnstructuredServiceAsync.class);  
            ByteBuffer secret = entry.getValue();
            byte[] secretData = new byte[secret.remaining()];
            secret.get(secretData);
            service.keepAlive(new AsyncResultAdapter<Boolean>(), secretData);
        }
    }
    
    private void establishNewOutgoingLinks(long timestamp) {
        int remainingInOutgoingLinkManager;
        int availableInAddressCache;
        
        lock.lock();
        try {
            availableInAddressCache = addressCache.size();
            remainingInOutgoingLinkManager = outgoingLinkManager.getRemaining();
        } finally {
            lock.unlock();
        }

        int numOfPossibleRequests = Math.min(availableInAddressCache, remainingInOutgoingLinkManager);
        int cappedNumOfPossibleRequests = Math.min(NEW_OUTGOING_LINKS_PER_CYCLE, numOfPossibleRequests);
        
        for (int i = 0; i < cappedNumOfPossibleRequests; i++) {
            A address;
            lock.lock();
            try {
                address = addressCache.iterator().next();
            } finally {
                lock.unlock();
            }
            
            UnstructuredServiceAsync<A> service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                    UnstructuredServiceAsync.class);            
            service.getState(new GetStateResultListener(address));
        }
    }
    
    private final class GetStateResultListener implements AsyncResultListener<State<A>> {
        private A address;

        public GetStateResultListener(A address) {
            this.address = address;
        }

        @Override
        public void invokationReturned(State<A> object) {
            Validate.notNull(object);
            Validate.notNull(object.getIncomingLinks());
            Validate.notNull(object.getOutgoingLinks());
            
            lock.lock();
            try {
                addressCache.addAll(object.getIncomingLinks());
                addressCache.addAll(object.getOutgoingLinks());
            } finally {
                lock.unlock();
            }
            
            if (object.isIncomingLinksFull()) {
                return;
            }

            UnstructuredServiceAsync<A> service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                    UnstructuredServiceAsync.class);
            
            byte[] secret = new byte[UnstructuredService.SECRET_SIZE];
            random.nextBytes(secret);
            service.join(new JoinResultListener(address, ByteBuffer.wrap(secret)), secret);
        }

        @Override
        public void invokationThrew(Throwable err) {
        }

        @Override
        public void invokationFailed(Object err) {
        }
    }

    private final class JoinResultListener implements AsyncResultListener<Boolean> {
        private A address;
        private ByteBuffer secret;

        public JoinResultListener(A address, ByteBuffer secret) {
            this.address = address;
            this.secret = secret;
        }

        @Override
        public void invokationReturned(Boolean object) {
            Validate.notNull(object);
            
            lock.lock();
            try {
                incomingLinkManager.addLink(System.currentTimeMillis(), address, secret);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void invokationThrew(Throwable err) {
        }

        @Override
        public void invokationFailed(Object err) {
        }
    }
}
