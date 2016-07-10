package com.offbynull.peernetic.io.gateways.network;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class NetworkGatewayTest {

    private static final Address NETWORK_GATEWAY_ADDRESS = Address.of("ng1");
    private static final Address DIRECT_GATEWAY_ADDRESS = Address.of("dg1");
    private static final Address ACTORS_ADDRESS = Address.of("actors");

    private NetworkGateway networkGateway;
    private Shuttle networkGatewayShuttle;
    private DirectGateway directGateway;
    private Shuttle directGatewayShuttle;
    private ActorRunner actorRunner;
    private Shuttle actorRunnerShuttle;

    public NetworkGatewayTest() {
    }

    @Before
    public void setUp() {
        networkGateway = new NetworkGateway(NETWORK_GATEWAY_ADDRESS.getElement(0));
        networkGatewayShuttle = networkGateway.getIncomingShuttle();
        
        directGateway = new DirectGateway(DIRECT_GATEWAY_ADDRESS.getElement(0));
        directGatewayShuttle = directGateway.getIncomingShuttle();
        
        actorRunner = new ActorRunner(ACTORS_ADDRESS.getElement(0), 1);
        actorRunnerShuttle = actorRunner.getIncomingShuttle();
        
        directGateway.addOutgoingShuttle(networkGatewayShuttle);
        directGateway.addOutgoingShuttle(actorRunnerShuttle);
        
        networkGateway.addOutgoingShuttle(directGatewayShuttle);
        networkGateway.addOutgoingShuttle(actorRunnerShuttle);
        
        actorRunner.addOutgoingShuttle(networkGatewayShuttle);
        actorRunner.addOutgoingShuttle(directGatewayShuttle);
    }
    
    @After
    public void shutDown() throws Exception {
        networkGateway.close();
        directGateway.close();
        actorRunner.close();
    }
    
    // This is a simple sanity check, the contents of the messages aren't checked.
    @Test(timeout = 5000L)
    public void mustGetLocalIpAddresses() throws InterruptedException {
        List<Message> incoming;
        
        directGateway.writeMessages(new Message(DIRECT_GATEWAY_ADDRESS, NETWORK_GATEWAY_ADDRESS, new LocalIpAddressesRequest()));
        incoming = directGateway.readMessages();
        
        assertEquals(1, incoming.size());
        assertEquals(LocalIpAddressesResponse.class, incoming.get(0).getMessage().getClass());
        
        System.out.println(incoming);
    }
    
    // This is a simple sanity check, the contents of the messages aren't checked.
    @Test(timeout = 5000L)
    public void mustConnectToGoogle() throws InterruptedException, UnknownHostException {
        List<Message> incoming;
        
        directGateway.writeMessages(new Message(DIRECT_GATEWAY_ADDRESS, NETWORK_GATEWAY_ADDRESS,
                new TcpCreateRequest(
                        InetAddress.getByName("0.0.0.0"),
                        InetAddress.getByName("www.google.com"),
                        80)));
        boolean foundTcpCreated = false;
        boolean foundWriteEmpty = false;
        while (true) {
            incoming = directGateway.readMessages();
            System.out.println(incoming);
            foundTcpCreated = foundTcpCreated || incoming.stream()
                    .map(x -> x.getMessage())
                    .filter(x -> x instanceof TcpCreateResponse)
                    .count() > 0L;
            foundWriteEmpty = foundWriteEmpty || incoming.stream()
                    .map(x -> x.getMessage())
                    .filter(x -> x instanceof TcpWriteEmptyNotification)
                    .count() > 0L;
            if (foundTcpCreated && foundWriteEmpty) {
                break;
            }
        }
        
        
        directGateway.writeMessages(new Message(DIRECT_GATEWAY_ADDRESS, NETWORK_GATEWAY_ADDRESS,
                new TcpWriteRequest("GET / HTTP/1.1\r\n\r\n".getBytes(US_ASCII))));
        boolean foundTcpRead = false;
        boolean foundWriteComplete = false;
        while (true) {
            incoming = directGateway.readMessages();
            System.out.println(incoming);
            foundTcpRead = foundTcpRead || incoming.stream()
                    .map(x -> x.getMessage())
                    .filter(x -> x instanceof TcpReadNotification)
                    .count() > 0L;
            foundWriteComplete = foundWriteComplete || incoming.stream()
                    .map(x -> x.getMessage())
                    .filter(x -> x instanceof TcpWriteResponse)
                    .count() > 0L;
            if (foundTcpRead && foundWriteComplete) {
                break;
            }
        }
        
        
        System.out.println("a");
        directGateway.writeMessages(new Message(DIRECT_GATEWAY_ADDRESS, NETWORK_GATEWAY_ADDRESS,
                new CloseRequest()));
        boolean foundClosed = false;
        while (true) {
            incoming = directGateway.readMessages();
            System.out.println(incoming);
            foundClosed = foundClosed || incoming.stream()
                    .map(x -> x.getMessage())
                    .filter(x -> x instanceof CloseResponse)
                    .count() > 0L;
            if (foundClosed) {
                break;
            }
        }
    }
    
    @Test
    public void mustConnectAndExchangeTcpMessage() throws InterruptedException {
        Coroutine serverActor = (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            // create tcp server
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    NETWORK_GATEWAY_ADDRESS.append("server"),
                    new TcpServerCreateRequest(InetAddress.getLoopbackAddress(), 12345));
            cnt.suspend();
            TcpServerCreateResponse respMsg = ctx.getIncomingMessage();
            cnt.suspend();
            
            // wait for first connection
            TcpServerAcceptNotification acceptMsg = ctx.getIncomingMessage();
            Address bindSubAddress = acceptMsg.getBindAddress();
            Address networkGatewayAddress = acceptMsg.getNetworkGatewayAddress();
            
            // send response
            ctx.addOutgoingMessage(
                    bindSubAddress,
                    networkGatewayAddress,
                    new TcpWriteRequest(new byte[] { 0x01 }));
        };
        
        Coroutine clientActor = (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            // create tcp client
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    NETWORK_GATEWAY_ADDRESS.append("client"),
                    new TcpCreateRequest(InetAddress.getLoopbackAddress(), InetAddress.getLoopbackAddress(), 12345));
            cnt.suspend();

            // connected at this point
            TcpCreateResponse respMsg = ctx.getIncomingMessage();
            cnt.suspend();
            TcpWriteEmptyNotification writeEmptyMsg = ctx.getIncomingMessage();
            cnt.suspend();
            
            // msg comes in, funnel it to direct gateway
            TcpReadNotification readMsg = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    DIRECT_GATEWAY_ADDRESS,
                    readMsg);
        };
        
        actorRunner.addActor("server", serverActor, new Object());
        actorRunner.addActor("client", clientActor, new Object());
        
        // wait for clientActor to funnel in the data it read in
        List<Message> incoming = directGateway.readMessages();
        assertArrayEquals(new byte[] { 0x01 }, ((TcpReadNotification) incoming.get(0).getMessage()).getData());
    }

    @Test
    public void mustConnectAndExchangeUdpMessage() throws InterruptedException {
        Coroutine serverActor = (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            Address networkGatewayAddress = NETWORK_GATEWAY_ADDRESS.append("server");
                    
            // create udp server
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    networkGatewayAddress,
                    new UdpCreateRequest(InetAddress.getLoopbackAddress(), 12345));
            cnt.suspend();
            UdpCreateResponse respMsg = ctx.getIncomingMessage();
            cnt.suspend();
            UdpWriteEmptyNotification writeEmptyNotif = ctx.getIncomingMessage();
            cnt.suspend();
            
            // wait for first message and echo it back
            UdpReadNotification udpRead = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    networkGatewayAddress,
                    new UdpWriteRequest(udpRead.getRemoteAddress(), udpRead.getData()));
            
            // close
//            cnt.suspend();
//            ctx.addOutgoingMessage(
//                    networkGatewayAddress,
//                    new CloseRequest());
        };
        
        Coroutine clientActor = (Continuation cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            Address networkGatewayAddress = NETWORK_GATEWAY_ADDRESS.append("client");
                    
            // create udp client
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    networkGatewayAddress,
                    new UdpCreateRequest(InetAddress.getLoopbackAddress(), 12346));
            cnt.suspend();
            UdpCreateResponse respMsg = ctx.getIncomingMessage();
            cnt.suspend();
            UdpWriteEmptyNotification writeEmptyNotif = ctx.getIncomingMessage();
            
            // send message
            ctx.addOutgoingMessage(
                    ctx.getSelf(),
                    networkGatewayAddress,
                    new UdpWriteRequest(
                            new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345),
                            new byte[] { 0x01 }));
            
            // read messages until response
            while (true) {
                cnt.suspend();
                Object resp = ctx.getIncomingMessage();
                if (resp instanceof UdpReadNotification) {
                    ctx.addOutgoingMessage(
                            ctx.getSelf(),
                            DIRECT_GATEWAY_ADDRESS,
                            resp);
                    break;
                }
            }
            
            // close
//            cnt.suspend();
//            ctx.addOutgoingMessage(
//                    networkGatewayAddress,
//                    new CloseRequest());
        };
        
        actorRunner.addActor("server", serverActor, new Object());
        actorRunner.addActor("client", clientActor, new Object());
        
        // wait for clientActor to funnel in the data it read in
        List<Message> incoming = directGateway.readMessages();
        assertArrayEquals(new byte[] { 0x01 }, ((UdpReadNotification) incoming.get(0).getMessage()).getData());
    }
}
