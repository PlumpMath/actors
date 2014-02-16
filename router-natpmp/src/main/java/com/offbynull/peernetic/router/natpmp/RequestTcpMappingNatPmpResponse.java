/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.router.natpmp;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * Represents a NAT-PMP TCP mapping response. From the RFC:
 * <pre>
 *    The NAT gateway responds with the following packet format:
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    | Vers = 0      | OP = 128 + x  | Result Code                   |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    | Seconds Since Start of Epoch                                  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    | Internal Port                 | Mapped External Port          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    | Port Mapping Lifetime in Seconds                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 *    The epoch time, ports, and lifetime are transmitted in the
 *    traditional network byte order (i.e., most significant byte first).
 * 
 *    The 'x' in the OP field MUST match what the client requested.  Some
 *    NAT gateways are incapable of creating a UDP port mapping without
 *    also creating a corresponding TCP port mapping, and vice versa, and
 *    these gateways MUST NOT implement NAT Port Mapping Protocol until
 *    this deficiency is fixed.  A NAT gateway that implements this
 *    protocol MUST be able to create TCP-only and UDP-only port mappings.
 *    If a NAT gateway silently creates a pair of mappings for a client
 *    that only requested one mapping, then it may expose that client to
 *    receiving inbound UDP packets or inbound TCP connection requests that
 *    it did not ask for and does not want.
 * 
 *    While a NAT gateway MUST NOT automatically create mappings for TCP
 *    when the client requests UDP, and vice versa, the NAT gateway MUST
 *    reserve the companion port so the same client can choose to map it in
 *    the future.  For example, if a client requests to map TCP port 80,
 *    as long as the client maintains the lease for that TCP port mapping,
 *    another client with a different internal IP address MUST NOT be able
 *    to successfully acquire the mapping for UDP port 80.
 * 
 *    The client normally requests the external port matching the internal
 *    port.  If that external port is not available, the NAT gateway MUST
 *    return an available external port if possible, or return an error
 *    code if no external ports are available.
 * 
 *    The source address of the packet MUST be used for the internal
 *    address in the mapping.  This protocol is not intended to facilitate
 *    one device behind a NAT creating mappings for other devices.  If
 *    there are legacy devices that require inbound mappings, permanent
 *    mappings can be created manually by the user through an
 *    administrative interface, just as they are today.
 * 
 *    If a mapping already exists for a given internal address and port
 *    (whether that mapping was created explicitly using NAT-PMP,
 *    implicitly as a result of an outgoing TCP SYN packet, or manually by
 *    a human administrator) and that client requests another mapping for
 *    the same internal port (possibly requesting a different external
 *    port), then the mapping request should succeed, returning the
 *    already-assigned external port.  This is necessary to handle the case
 *    where a client requests a mapping with suggested external port X, and
 *    is granted a mapping with actual external port Y, but the
 *    acknowledgment packet gets lost.  When the client retransmits its
 *    mapping request, it should get back the same positive acknowledgment
 *    as was sent (and lost) the first time.
 * 
 *    The NAT gateway MUST NOT accept mapping requests destined to the NAT
 *    gateway's external IP address or received on its external network
 *    interface.  Only packets received on the internal interface(s) with a
 *    destination address matching the internal address(es) of the NAT
 *    gateway should be allowed.
 * 
 *    The NAT gateway MUST fill in the Seconds Since Start of Epoch field
 *    with the time elapsed since its port mapping table was initialized on
 *    startup or reset for any other reason (see Section 3.6, "Seconds
 *    Since Start of Epoch").
 * 
 *    The Port Mapping Lifetime is an unsigned integer in seconds.  The NAT
 *    gateway MAY reduce the lifetime from what the client requested.  The
 *    NAT gateway SHOULD NOT offer a lease lifetime greater than that
 *    requested by the client.
 * 
 *    Upon receiving the response packet, the client MUST check the source
 *    IP address, and silently discard the packet if the address is not the
 *    address of the gateway to which the request was sent.
 * 
 *    The client SHOULD begin trying to renew the mapping halfway to expiry
 *    time, like DHCP.  The renewal packet should look exactly the same as
 *    a request packet, except that the client SHOULD set the Suggested
 *    External Port to what the NAT gateway previously mapped, not what the
 *    client originally suggested.  As described above, this enables the
 *    gateway to automatically recover its mapping state after a crash or
 *    reboot.
 * </pre>
 * @author Kasra Faghihi
 */
public final class RequestTcpMappingNatPmpResponse extends NatPmpResponse {
    private long secondsSinceStartOfEpoch;
    private int internalPort;
    private int externalPort;
    private long lifetime;

    /**
     * Constructs a {@link RequestTcpMappingNatPmpResponse} object by parsing a buffer.
     * @param buffer buffer containing PCP response data
     * @throws NullPointerException if any argument is {@code null}
     * @throws BufferUnderflowException if not enough data is available in {@code buffer}
     * @throws IllegalArgumentException if the version doesn't match the expected version (must always be {@code 0}), or if the op
     * {@code != 129}, or if external port in the buffer is {@code < 1 || > 65535}, or if there's an unsuccessful/unrecognized result code
     */
    public RequestTcpMappingNatPmpResponse(ByteBuffer buffer) {
        super(buffer);
        
        Validate.isTrue(getOp() == 130);
        
        secondsSinceStartOfEpoch = buffer.getInt() & 0xFFFFFFFFL;
        internalPort = buffer.getShort() & 0xFFFF;
        externalPort = buffer.getShort() & 0xFFFF;
        lifetime = buffer.getInt() & 0xFFFFFFFFL;
        
        Validate.inclusiveBetween(1, 65535, externalPort);
    }

    /**
     * Get the number of seconds since the start of epoch.
     * @return number of seconds since start of epoch
     */
    public long getSecondsSinceStartOfEpoch() {
        return secondsSinceStartOfEpoch;
    }

    /**
     * Get the internal port number.
     * @return internal port number
     */
    public int getInternalPort() {
        return internalPort;
    }

    /**
     * Get the external port number.
     * @return external port number
     */
    public int getExternalPort() {
        return externalPort;
    }

    /**
     * Get the lifetime for this mapping.
     * @return lifetime for this mapping
     */
    public long getLifetime() {
        return lifetime;
    }
}
