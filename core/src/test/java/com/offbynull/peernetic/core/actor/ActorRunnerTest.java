package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttles.test.CaptureShuttle;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class ActorRunnerTest {

    private ActorRunner fixture1;
    private ActorRunner fixture2;

    @Before
    public void setUp() {
        fixture1 = new ActorRunner("local");
        fixture2 = new ActorRunner("local2");
    }

    @After
    public void tearDown() throws Exception {
        fixture1.close();
        fixture2.close();
    }

    @Test(timeout = 5000L)
    public void mustCommunicateBetweenActorsWithinSameActorRunner() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        fixture1.addActor(
                "echoer",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local:echoer"),
                            Address.fromString("local:sender"),
                            ctx.getIncomingMessage());
                });
        fixture1.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("local:echoer"),
                            "hi");
                    
                    cnt.suspend();
                    
                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());
        
        boolean processed = latch.await(5L, TimeUnit.SECONDS);
        assertTrue(processed);
    }

    @Test(timeout = 5000L)
    public void mustCommunicateBetweenActorsWithinDifferentActorRunners() throws Exception {
        // Wire together
        fixture1.addOutgoingShuttle(fixture2.getIncomingShuttle());
        fixture2.addOutgoingShuttle(fixture1.getIncomingShuttle());

        // Test
        CountDownLatch latch = new CountDownLatch(1);
        fixture2.addActor(
                "echoer",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local2:echoer"),
                            ctx.getSource(),
                            ctx.getIncomingMessage());
                });
        fixture1.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("local2:echoer"),
                            "hi");
                    cnt.suspend();

                    assertEquals(ctx.getIncomingMessage(), "hi");
                    latch.countDown();
                },
                new Object());

        latch.await();
    }
    
    @Test
    public void mustFailWhenAddingActorWithSameName() throws Exception {
        fixture1.addActor("actor", (Context ctx) -> false);
        fixture1.addActor("actor", (Context ctx) -> false);
        fixture1.join();
    }
    
    @Test
    public void mustFailWhenRemoveActorThatDoesntExist() throws Exception {
        fixture1.removeActor("actor");
        fixture1.join();
    }

    @Test
    public void mustFailWhenAddingOutgoingShuttleWithSameName() throws Exception {
        NullShuttle shuttle = new NullShuttle("local");
        // Queue outgoing shuttle with prefix as ourselves ("local") be added. We won't be notified of rejection right away, but the add
        // will cause ActorRunner's background thread to throw an exception once its attempted. As such, join() will return indicating that
        // the thread died.
        fixture1.addOutgoingShuttle(shuttle);
        fixture1.join();
    }

    @Test
    public void mustFailWhenAddingConflictingOutgoingShuttle() throws Exception {
        // Should get added
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture1.addOutgoingShuttle(captureShuttle);
        
        // Should cause a failure
        NullShuttle nullShuttle = new NullShuttle("fake");
        fixture1.addOutgoingShuttle(nullShuttle);
        
        
        fixture1.join();
    }
    
    @Test(timeout = 5000L)
    public void mustRemoveOutgoingShuttle() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture1.addOutgoingShuttle(captureShuttle);
        fixture1.removeOutgoingShuttle("fake");
        
        fixture1.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("fake"),
                            "1");
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("local:sender"),
                            new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        latch.await();

        assertTrue(captureShuttle.drainMessages().isEmpty());
    }
    
    @Test
    public void mustFailWhenRemovingIncomingShuttleThatDoesntExist() throws Exception {
        // Should cause a failure
        fixture1.removeOutgoingShuttle("fake");
        fixture1.join();
    }

    @Test(timeout = 5000L)
    public void mustStillRunIfOutgoingShuttleRemovedThenAddedAgain() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        CaptureShuttle captureShuttle = new CaptureShuttle("fake");
        fixture1.addOutgoingShuttle(captureShuttle);
        fixture1.removeOutgoingShuttle("fake");
        fixture1.addOutgoingShuttle(captureShuttle);
        
        fixture1.addActor(
                "sender",
                (Continuation cnt) -> {
                    Context ctx = (Context) cnt.getContext();
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("fake"),
                            "1");
                    ctx.addOutgoingMessage(
                            Address.fromString("local:sender"),
                            Address.fromString("local:sender"),
                            new Object());
        
                    // Suspend here. We'll continue when we get the msg we sent to ourselves, and at that point we can be sure that msgs to
                    // "fake" were sent
                    cnt.suspend();
                    
                    latch.countDown();
                },
                new Object());

        latch.await();

        assertEquals("1", captureShuttle.drainMessages().get(0).getMessage());
    }
}
