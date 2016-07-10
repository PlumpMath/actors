package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.mutable.MutableInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubcoroutineRouterTest {

    private static final Address ACTOR_ADDRESS = Address.fromString("local:actor");
    private static final Address ROUTER_ADDRESS = ACTOR_ADDRESS.append("router");
    private static final Address ROUTER_CHILD_ADDRESS = ACTOR_ADDRESS.append("router", "child");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Context context;
    private SubcoroutineRouter fixture;

    @Before
    public void setUp() {
        context = mock(Context.class);
        fixture = new SubcoroutineRouter(ROUTER_ADDRESS, context);
    }

    @Test
    public void mustSilentlyIgnoreForwardsToUnknownChildren() throws Exception {
        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        ForwardResult res = fixture.forward();

        assertFalse(res.isForwarded());
    }

    @Test
    public void mustForwardToChild() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> {
            mutableInt.increment();
            return null;
        };

        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD);

        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        ForwardResult res = fixture.forward();

        assertTrue(res.isForwarded());
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustAddNewChildAndPrime() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> {
            mutableInt.increment();
            return null;
        };

        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD_PRIME);

        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustSilentlyIgnoreForwardsToRemovedChild() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> {
            mutableInt.increment();
            cnt.suspend();
            return null;
        };

        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD);

        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        ForwardResult res = fixture.forward();

        assertTrue(res.isForwarded());
        assertEquals(1, mutableInt.intValue());
        
        
        fixture.getController().remove(ROUTER_CHILD_ADDRESS);
        
        res = fixture.forward();
        assertFalse(res.isForwarded());
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustAddNewChildAndPrimeWhenChildDoesNotFinishOnPrime() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> {
            mutableInt.increment();
            cnt.suspend();
            return null;
        };

        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD_PRIME_NO_FINISH);
        
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustFailAddNewChildAndPrimeWhenChildFinishesOnPrime() throws Exception {
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> null;

        when(context.getSelf()).thenReturn(ACTOR_ADDRESS);
        when(context.getDestination()).thenReturn(ROUTER_CHILD_ADDRESS);
        when(context.getIncomingMessage()).thenReturn(new Object());
        
        exception.expect(IllegalStateException.class);
        
        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD_PRIME_NO_FINISH);
    }

    @Test
    public void mustFailOnConflictingAdd() throws Exception {
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> null;
        
        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD);
        
        exception.expect(IllegalArgumentException.class);
        fixture.getController().add(ROUTER_CHILD_ADDRESS, subcoroutine, ADD);
    }

    @Test
    public void mustFailOnIncorrectAdd() throws Exception {
        Subcoroutine<Void> subcoroutine = (Continuation cnt) -> null;
        
        exception.expect(IllegalArgumentException.class);
        fixture.getController().add(Address.of("badid"), subcoroutine, ADD);
    }

    @Test
    public void mustFailOnMissingRemove() throws Exception {
        exception.expect(IllegalArgumentException.class);
        fixture.getController().remove(ROUTER_CHILD_ADDRESS);
    }

    @Test
    public void mustFailOnIncorrectRemove() throws Exception {
        exception.expect(IllegalArgumentException.class);
        fixture.getController().remove(ROUTER_ADDRESS.append("fake"));
    }
}
