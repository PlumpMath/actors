package com.offbynull.rpc.transport;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.lang3.Validate;

/**
 * A composite {@link OutgoingFilter} that allows adding/removing of inner filters.
 * @author Kasra F
 * @param <A> address type
 */
public final class ModifiableCompositeOutgoingFilter<A> implements OutgoingFilter<A> {
    private CopyOnWriteArrayList<OutgoingFilter<A>> filters;

    /**
     * Construct an empty {@link ModifiableCompositeOutgoingFilter}
     */
    public ModifiableCompositeOutgoingFilter() {
        this(Collections.<OutgoingFilter<A>>emptyList());
    }
    
    /**
     * Construct a {@link ModifiableCompositeOutgoingFilter} populated with {@code filters}.
     * @param filters initial listeners
     */
    public ModifiableCompositeOutgoingFilter(Collection<OutgoingFilter<A>> filters) {
        Validate.noNullElements(filters);
        
        this.filters = new CopyOnWriteArrayList<>(filters);
    }

    /**
     * Add filters to the start of the chain.
     * @param e filters to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addFirst(OutgoingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.addAll(0, Arrays.asList(e));
    }

    /**
     * Add filters to the end of the chain.
     * @param e filters to add
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void addLast(OutgoingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.addAll(Arrays.asList(e));
    }
    
    /**
     * Remove filters from the chain.
     * @param e filters remove
     * @throws NullPointerException if any element of {@code e} is {@code null}
     */
    public void remove(OutgoingFilter<A> ... e) {
        Validate.noNullElements(e);
        
        filters.removeAll(Arrays.asList(e));
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        ByteBuffer ret = buffer;
        for (OutgoingFilter<A> filter : filters) {
            filter.filter(from, ret);
        }
        return ret;
    }
    
}