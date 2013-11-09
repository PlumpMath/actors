package com.offbynull.p2prpc;

import com.offbynull.p2prpc.session.PacketIdGenerator;
import com.offbynull.p2prpc.session.ServerMessageCallback;
import com.offbynull.p2prpc.session.ServerResponseCallback;
import com.offbynull.p2prpc.transport.udp.UdpTransport;
import com.offbynull.p2prpc.session.UdpClient;
import com.offbynull.p2prpc.session.UdpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class UdpTest {
    
    public UdpTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void selfUdpTest() throws Throwable {
        UdpTransport base = new UdpTransport(12345);
        base.start();
        
        UdpServer server = new UdpServer(base);
        UdpClient client = new UdpClient(base, new PacketIdGenerator());
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("localhost", 12345), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.eq("HIEVERYBODY! :)".getBytes()),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }
    
    @Test(expected = IOException.class)
    public void noResponseUdpTest() throws Throwable {
        UdpTransport base = new UdpTransport(12345);
        base.start();
        
        UdpServer server = new UdpServer(base);
        UdpClient client = new UdpClient(base, new PacketIdGenerator());
        
        ServerMessageCallback<InetSocketAddress> mockedCallback = Mockito.mock(ServerMessageCallback.class);
        server.start(mockedCallback);
        
        client.send(new InetSocketAddress("asdawfaawacwavew", 12345), "HIEVERYBODY! :)".getBytes(), 500L);
        
        Mockito.verify(mockedCallback).messageArrived(
                Matchers.any(InetSocketAddress.class),
                Matchers.any(byte[].class),
                Matchers.any(ServerResponseCallback.class));
        
        server.stop();
        base.stop();
    }
}
