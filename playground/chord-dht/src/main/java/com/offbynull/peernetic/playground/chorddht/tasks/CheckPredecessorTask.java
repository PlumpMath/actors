package com.offbynull.peernetic.playground.chorddht.tasks;

import com.offbynull.peernetic.CoroutineActor;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.skeleton.SimpleJavaflowTask;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.shared.ChordHelper;
import com.offbynull.peernetic.playground.chorddht.shared.ChordOperationException;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class CheckPredecessorTask<A> extends SimpleJavaflowTask<A, byte[]> {

    private final ChordHelper<A, byte[]> helper;
    
    public static <A> CheckPredecessorTask<A> create(Instant time, ChordContext<A> context) throws Exception {
        CheckPredecessorTask<A> task = new CheckPredecessorTask<>(context);
        CoroutineActor actor = new CoroutineActor(task);
        task.initialize(time, actor);

        return task;
    }

    
    private CheckPredecessorTask(ChordContext<A> context) {
        super(context.getRouter(), context.getSelfEndpoint(), context.getEndpointScheduler(), context.getNonceAccessor());
        
        Validate.notNull(context);
        this.helper = new ChordHelper<>(getState(), getFlowControl(), context);
    }

    @Override
    public void execute() throws Exception {
        while (true) {
            helper.sleep(1L);
                        
            ExternalPointer<A> predecessor = helper.getPredecessor();
            if (predecessor == null) {
                // we don't have a predecessor to check
                continue;
            }
            
            // ask for our predecessor's id
            GetIdResponse gir;
            try {
                gir = helper.sendGetIdRequest(predecessor.getAddress());
            } catch (ChordOperationException coe) {
                // predecessor didn't respond -- clear our predecessor
                helper.clearPredecessor();
                continue;
            }
            
            Id id = helper.toId(gir.getId());
            // TODO: Is it worth checking to see if this new id is between the old id and the us? if it is, set it as the new pred???
            if (!id.equals(predecessor.getId())) {
                // predecessor responded with unexpected id -- clear our predecessor
                helper.clearPredecessor();
            }
        }
    }

    @Override
    protected boolean requiresPriming() {
        return true;
    }
}