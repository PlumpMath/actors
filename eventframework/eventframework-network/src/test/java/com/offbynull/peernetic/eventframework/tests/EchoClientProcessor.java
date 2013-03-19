package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.event.ErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.eventframework.network.impl.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.eventframework.network.impl.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.eventframework.impl.basic.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;

public final class EchoClientProcessor implements Processor {

    private State state;
    private long trackedId;
    
    public EchoClientProcessor() {
        state = State.SEND;
    }
    
    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        if (event instanceof InitializeIncomingEvent && state == State.SEND) {
            trackedId = trackedIdGen.getNextId();
            OutgoingEvent outEvent = new SendMessageOutgoingEvent(
                    new FakeRequest("TEST 1 2 3"),
                    "localhost", 9111, trackedId);
            state = State.RECV;
            return new OngoingProcessResult(outEvent);
        } else if (event instanceof ReceiveResponseIncomingEvent && state == State.RECV) {
            state = State.DONE;
            ReceiveResponseIncomingEvent inEvent =
                    (ReceiveResponseIncomingEvent) event;
            FakeResponse fakeResponse = (FakeResponse) inEvent.getResponse();
            
            if (fakeResponse.getData().equals("TEST 1 2 3")) {
                return new FinishedProcessResult();
            } else {
                throw new IllegalStateException();
            }
        } else if (event instanceof ErrorIncomingEvent) {
            throw new IllegalStateException();
        }
        
        return new OngoingProcessResult();
    }
    
    private enum State {
        SEND,
        RECV,
        DONE
    }
}