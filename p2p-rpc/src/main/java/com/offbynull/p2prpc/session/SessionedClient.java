package com.offbynull.p2prpc.session;

import com.offbynull.p2prpc.transport.IncomingData;
import com.offbynull.p2prpc.transport.OutgoingData;
import com.offbynull.p2prpc.transport.SessionedTransport;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestController;
import com.offbynull.p2prpc.transport.SessionedTransport.RequestSender;
import com.offbynull.p2prpc.transport.SessionedTransport.ResponseReceiver;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SessionedClient<A> implements Client<A> {
    private static final Object FAIL_MARKER = new Object();
    private RequestSender<A> requestSender;

    public SessionedClient(SessionedTransport<A> transport) {
        requestSender = transport.getRequestSender();
    }

    @Override
    public byte[] send(A address, byte[] data, long timeout) throws IOException, InterruptedException {
        final ArrayBlockingQueue<Object> exchanger = new ArrayBlockingQueue<>(1); // exchanger/synchronousqueue shouldn't be used here due
                                                                                  // to potential of responseReceiver getting blocked
        
        ResponseReceiver<A> responseReceiver = new ResponseReceiver<A>() {

            @Override
            public void responseArrived(IncomingData<A> data) {
                ByteBuffer recvData = data.getData();
                                    
                byte[] recvDataBytes = new byte[recvData.limit()];
                recvData.get(recvDataBytes);
                
                exchanger.add(recvDataBytes);
            }

            @Override
            public void communicationFailed() {
                exchanger.add(FAIL_MARKER);
            }
        };
        
        OutgoingData<A> outgoingData = new OutgoingData<>(address, data);
        RequestController controller = requestSender.sendRequest(outgoingData, responseReceiver);

        try {
            Object recvData = exchanger.poll(timeout, TimeUnit.MILLISECONDS);
            
            if (recvData == null || recvData == FAIL_MARKER) {
                throw new IOException("Communcation failed");
            }
            
            return (byte[]) recvData;
        } finally {
            controller.killCommunication(); // just incase
        }
    }
}
