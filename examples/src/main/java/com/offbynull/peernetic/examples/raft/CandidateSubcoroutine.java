package com.offbynull.peernetic.examples.raft;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.IndividualResponseAction;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.debug;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.raft.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.peernetic.examples.raft.Mode.LEADER;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteRequest;
import com.offbynull.peernetic.examples.raft.externalmessages.RequestVoteResponse;
import java.time.Duration;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

final class CandidateSubcoroutine implements Subcoroutine<Void> {

    private static final Address SUB_ADDRESS = ROUTER_HANDLER_RELATIVE_ADDRESS;  // this subcoroutine ran as part of handler

    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;
    
    public CandidateSubcoroutine(State state) {
        Validate.notNull(state);

        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Entering candidate mode"));
        
        while (true) {
            ctx.addOutgoingMessage(SUB_ADDRESS, logAddress, debug("Starting new election"));
            
            int currentTerm = state.incrementCurrentTerm();

            UnmodifiableSet<String> otherLinkIds = state.getOtherNodeLinkIds();

            int requiredSuccessfulCount = state.getMajorityCount();
            MutableInt successfulCount = new MutableInt(1); // start with 1 because we're voting for ourself first
            
            int lastLogIndex;
            int lastLogTerm;
            if (state.isLogEmpty()) {
                lastLogIndex = -1;
                lastLogTerm = -1;                
            } else {
                lastLogIndex = state.getLastLogIndex();
                lastLogTerm = state.getLastLogEntry().getTerm();
            }
            
            Object req = new RequestVoteRequest(currentTerm, lastLogIndex, lastLogTerm);
            int totalWaitTime = state.nextElectionTimeout();
            int attempts = 5;
            int waitTimePerAttempt = totalWaitTime / attempts; // divide by n attempts
            String multiReqId = state.nextRandomId();
            MultiRequestSubcoroutine.Builder<RequestVoteResponse> builder = new MultiRequestSubcoroutine.Builder<RequestVoteResponse>()
                    .timerAddressPrefix(timerAddress)
                    .attemptInterval(Duration.ofMillis(waitTimePerAttempt))
                    .maxAttempts(5)
                    .request(req)
                    .addExpectedResponseType(RequestVoteResponse.class)
                    .individualResponseListener(x -> {
                        // This IndividualResponseListener will stop the MultiRequestSubcoroutine once more than half of the responses come
                        // back as "successful"
                        RequestVoteResponse response = x.getResponse();
                        if (response.isVoteGranted()) {
                            successfulCount.increment();
                        }
                        
                        if (successfulCount.getValue() >= requiredSuccessfulCount) {
                            return new IndividualResponseAction(true, true);
                        } else {
                            return new IndividualResponseAction(true, false);
                        }
                    })
                    .address(SUB_ADDRESS.appendSuffix(multiReqId));

            AddressTransformer addressTransformer = state.getAddressTransformer();
            for (String linkId : otherLinkIds) {
                String msgId = state.nextRandomId();
                Address dstAddr = addressTransformer.linkIdToRemoteAddress(linkId).appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS);
                builder.addDestination(msgId, dstAddr);
            }

            MultiRequestSubcoroutine<RequestVoteResponse> multiReq = builder.build();
            multiReq.run(cnt);

            if (successfulCount.getValue() >= requiredSuccessfulCount) {
                // Majority of votes have come in for this node. Set mode to leader.
                state.setMode(LEADER);
                return null;
            } else {
                ctx.addOutgoingMessage(SUB_ADDRESS, logAddress,
                        debug("Not enough votes... Got: {} Required: {}", successfulCount.getValue(), requiredSuccessfulCount));
            }
        }
    }
    
    @Override
    public Address getAddress() {
        return SUB_ADDRESS;
    }
}