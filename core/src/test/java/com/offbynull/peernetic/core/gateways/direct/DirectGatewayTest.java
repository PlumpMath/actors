package com.offbynull.peernetic.core.gateways.direct;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Test;

public class DirectGatewayTest {

    @Test
    public void testSomeMethod() throws InterruptedException {
        Coroutine echoerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            Address sender = ctx.getSource();
            Object msg = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(Address.fromString("actors:echoer"), sender, msg);
        };

        ActorRunner actorRunner = new ActorRunner("actors");
        DirectGateway directGateway = new DirectGateway("direct");

        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());

        actorRunner.addActor("echoer", echoerActor);
        Address echoerAddress = Address.of("actors", "echoer");

        String expected;
        String actual;

        expected = "echotest";
        directGateway.writeMessage(echoerAddress, expected);
        actual = (String) directGateway.readMessages().get(0).getMessage();
        Assert.assertEquals(expected, actual);

        actorRunner.close();
        directGateway.close();
    }

}
