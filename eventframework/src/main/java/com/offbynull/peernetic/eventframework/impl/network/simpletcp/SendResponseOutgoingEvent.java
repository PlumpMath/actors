package com.offbynull.peernetic.eventframework.impl.network.simpletcp;

import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;

public final class SendResponseOutgoingEvent
        extends DefaultTrackedOutgoingEvent {
    private Response response;
    private long pendingId;

    public SendResponseOutgoingEvent(Response response, long pendingId,
            long trackedId) {
        super(trackedId);
        this.response = response;
        this.pendingId = pendingId;
    }

    public Response getResponse() {
        return response;
    }

    public long getPendingId() {
        return pendingId;
    }

}