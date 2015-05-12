package com.offbynull.peernetic.core.simulation;

import java.time.Duration;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
public class SimpleActorBehaviourDriverTest {
    
    private SimpleActorBehaviourDriver fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleActorBehaviourDriver();
    }

    @Test
    public void mustAlwaysReturnDurationOfZero() {
        Duration duration = fixture.calculateDuration("test", "test", "test", Duration.ofSeconds(5L));
        assertEquals(Duration.ZERO, duration);
    }
    
}