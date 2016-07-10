package com.offbynull.peernetic.core.shuttle;

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AddressTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void mustConstructSingleElementAddressFromString() {
        Address fixture = Address.fromString("hi");
        assertEquals(Arrays.asList("hi"), fixture.getElements());
    }

    @Test
    public void mustConstructMultiElementAddressFromString() {
        Address fixture = Address.fromString("hi:to:you");
        assertEquals(Arrays.asList("hi", "to", "you"), fixture.getElements());
    }

    @Test
    public void mustConstructMultiElementAddressFromStringWithEscapeSequences() {
        Address fixture = Address.fromString("\\:h\\\\i\\::\\\\t\\:o\\\\:you");
        assertEquals(Arrays.asList(":h\\i:", "\\t:o\\", "you"), fixture.getElements());
    }

    @Test
    public void mustConstructAddressFromEmptyString() {
        Address fixture = Address.fromString("");
        assertEquals(Arrays.asList(), fixture.getElements());
    }

    @Test
    public void mustProperlyConvertAddressToAndFromString() {
        Address fixture = Address.of(":h\\i:", "\\t:o\\", "you");
        Address reconstructed = Address.fromString(fixture.toString());
        assertEquals(fixture, reconstructed);
    }

    @Test
    public void mustIdentifyAsPrefix() {
        Address parent = Address.of("one", "two");
        Address fixture = Address.of("one", "two", "three");
        assertTrue(parent.isPrefixOf(fixture));
    }

    @Test
    public void mustNotIdentifyAsPrefix() {
        Address other = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "xxx", "three");
        assertFalse(other.isPrefixOf(fixture));
    }

    @Test
    public void mustNotIdentifyAsPrefixWhenShorter() {
        Address other = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "two");
        assertFalse(other.isPrefixOf(fixture));
    }
    
    @Test
    public void mustIdentifyAsPrefixWhenEqual() {
        Address other = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "two", "three");
        assertTrue(other.isPrefixOf(fixture));
    }

    @Test
    public void mustIdentifyAsParentOrEqualWhenParent() {
        Address parent = Address.of("one", "two");
        Address fixture = Address.of("one", "two", "three");
        assertTrue(parent.isPrefixOf(fixture));
    }
    
    @Test
    public void mustIdentifyAsParentOrEqualWhenEqual() {
        Address parent = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "two", "three");
        assertTrue(parent.isPrefixOf(fixture));
    }

    @Test
    public void mustNotIdentifyAsParentOrEqual() {
        Address other = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "xxx", "three");
        assertFalse(other.isPrefixOf(fixture));
    }
    
    @Test
    public void mustRelativizeToAddress() {
        Address parent = Address.of("one", "two");
        Address fixture = Address.of("one", "two", "three");
        assertEquals(Address.of("three"), fixture.removePrefix(parent));
    }

    @Test
    public void mustRelativizeToEmpty() {
        Address parent = Address.of("one", "two");
        Address fixture = Address.of("one", "two");
        assertTrue(fixture.removePrefix(parent).isEmpty());
    }

    @Test
    public void mustFailToRemovePrefix() {
        Address other = Address.of("one", "two", "three");
        Address fixture = Address.of("one", "xxx", "three");
        exception.expect(IllegalArgumentException.class);
        fixture.removePrefix(other);
    }

    @Test
    public void mustRemoveSuffix() {
        Address fixture = Address.of("one", "xxx", "three");
        assertEquals(Address.of("one"), fixture.removeSuffix(2));
    }

    @Test
    public void mustNotRemoveSuffixIfRemoveCountIs0() {
        Address fixture = Address.of("one", "xxx", "three");
        assertEquals(fixture, fixture.removeSuffix(0));
    }

    @Test
    public void mustNotRemoveSuffixIfRemoveCountIsAll() {
        Address fixture = Address.of("one", "xxx", "three");
        assertTrue(fixture.removeSuffix(3).isEmpty());
    }

    @Test
    public void mustFailToRemoveSuffixIfRemoveCountIsOutOfBounds() {
        Address fixture = Address.of("one", "xxx", "three");
        exception.expect(IllegalArgumentException.class);
        fixture.removeSuffix(4);
    }
    
    @Test
    public void mustGetSuffix() {
        Address fixture = Address.of("one", "xxx", "three");
        assertEquals(Address.of("xxx", "three"), fixture.getSuffix(2));
    }

    @Test
    public void mustBeEmptyIfGetSuffixCountIs0() {
        Address fixture = Address.of("one", "xxx", "three");
        assertTrue(fixture.getSuffix(0).isEmpty());
    }

    @Test
    public void mustEqualIfGetSuffixCountIsAll() {
        Address fixture = Address.of("one", "xxx", "three");
        assertEquals(fixture, fixture.getSuffix(3));
    }

    @Test
    public void mustFailToGetSuffixIfCountIsOutOfBounds() {
        Address fixture = Address.of("one", "xxx", "three");
        exception.expect(IllegalArgumentException.class);
        fixture.getSuffix(4);
    }
    
    @Test
    public void mustConstructAnAddressWith0Elements() {
        Address.of(); // no crash means success
    }
    
    
}
