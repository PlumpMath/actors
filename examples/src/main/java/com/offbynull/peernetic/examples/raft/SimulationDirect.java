package com.offbynull.peernetic.examples.raft;

import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.ReplayerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.simulator.MessageSink;
import com.offbynull.peernetic.core.simulator.RecordMessageSink;
import com.offbynull.peernetic.core.simulator.Simulator;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.examples.raft.internalmessages.StartClient;
import com.offbynull.peernetic.examples.raft.internalmessages.StartServer;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public final class SimulationDirect {

    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    private static final Address EMPTY_ADDRESS = Address.of();
    
    private static final int MIN_ELECTION_TIMEOUT = 1500;
    private static final int MAX_ELECTION_TIMEOUT = 3000;
    
    private static final int NUM_NODES = 8;
    
    public static void main(String[] args) throws Exception {
        // Create simulator
        Instant time = Instant.ofEpochMilli(0L);
        Simulator simulator = new Simulator(time);

        // Instruct simulator to save messages to graph
        File tempFile = File.createTempFile(SimulationDirect.class.getSimpleName(), ".graphmsgs");
        MessageSink sink = new RecordMessageSink(BASE_GRAPH_ADDRESS_STRING, tempFile, new SimpleSerializer());
        simulator.addMessageSink(sink, time); // add sink
        simulator.addCoroutineActor(          // add fake actor for "graph" so sink above actually gets sent msgs by simulator --
                BASE_GRAPH_ADDRESS_STRING,     // simulator will not send messagesto sink if actor isn't present
                cnt -> {
                    while (true) {
                        cnt.suspend();
                    }
                },
                Duration.ZERO,
                time);

        // Instruct simulator to add timer
        simulator.addTimer(BASE_TIMER_ADDRESS_STRING, time);
        
        // Generate server ids
        Validate.isTrue(NUM_NODES > 0, "Bad size");
        Set<Integer> serverIds = new HashSet<>();
        for (int i = 0; i < NUM_NODES; i++) {
            serverIds.add(i);
        }
        
        // Instruct simulator to add nodes
        for (int i = 1; i < NUM_NODES; i++) {
            addServerNode(i, serverIds, simulator, time); // add server
        }
        addClientNode(NUM_NODES, serverIds, simulator, time); // add client

        // Run simulation  (NOTE: enabling logging slows this simulation to a crawl, use slf4j-nop for best performance)
        for (int i = 0; i < 20000 && simulator.hasMore(); i++) {
            simulator.process();
        }

        // Close sink
        sink.close();

        

        
        
        // Replay saved graph messages from simulation to a real graph
        GraphGateway.startApplication();
        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);

        ReplayerGateway replayerGateway = ReplayerGateway.replay(
                graphGateway.getIncomingShuttle(),
                BASE_GRAPH_ADDRESS,
                tempFile,
                new SimpleSerializer());

        replayerGateway.await();
        GraphGateway.awaitShutdown();
    }
    
    private static void addClientNode(int clientId, Collection<Integer> allServerIds, Simulator simulator, Instant time) {
        String idStr = Integer.toString(clientId);
        Set<String> allIdsAsStrs = allServerIds.stream().map(x -> Integer.toString(x)).collect(Collectors.toSet());
        
        simulator.addCoroutineActor(
                idStr,
                new RaftClientCoroutine(),
                Duration.ZERO,
                time,
                new StartClient(
                        new SimpleAddressTransformer(EMPTY_ADDRESS, idStr),
                        MIN_ELECTION_TIMEOUT,
                        MAX_ELECTION_TIMEOUT,
                        allIdsAsStrs,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }

    private static void addServerNode(int serverId, Collection<Integer> allServerIds, Simulator simulator, Instant time) {
        String idStr = Integer.toString(serverId);
        Set<String> allIdsAsStrs = allServerIds.stream().map(x -> Integer.toString(x)).collect(Collectors.toSet());
        
        simulator.addCoroutineActor(
                idStr,
                new RaftServerCoroutine(),
                Duration.ZERO,
                time,
                new StartServer(
                        new SimpleAddressTransformer(EMPTY_ADDRESS, idStr),
                        MIN_ELECTION_TIMEOUT,
                        MAX_ELECTION_TIMEOUT,
                        allIdsAsStrs,
                        (long) serverId,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}