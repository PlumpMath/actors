package com.offbynull.peernetic.io.gateways.network;

import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class NetworkGatewayTest {

    private static final Address NETWORK_GATEWAY_ADDRESS = Address.of("ng1");
    private static final Address DIRECT_GATEWAY_ADDRESS = Address.of("dg1");

    private NetworkGateway networkGateway;
    private Shuttle networkGatewayShuttle;
    private DirectGateway directGateway;
    private Shuttle directGatewayShuttle;

    public NetworkGatewayTest() {
    }

    @Before
    public void setUp() {
        networkGateway = new NetworkGateway(NETWORK_GATEWAY_ADDRESS.getElement(0));
        networkGatewayShuttle = networkGateway.getIncomingShuttle();
        
        directGateway = new DirectGateway(DIRECT_GATEWAY_ADDRESS.getElement(0));
        directGatewayShuttle = directGateway.getIncomingShuttle();
        
        directGateway.addOutgoingShuttle(networkGatewayShuttle);
        networkGateway.addOutgoingShuttle(directGatewayShuttle);
    }
    
    @After
    public void shutDown() throws Exception {
        networkGateway.close();
        directGateway.close();
    }
    
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
}
