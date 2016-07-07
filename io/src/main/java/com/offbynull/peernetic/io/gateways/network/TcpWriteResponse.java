/*
 * Copyright (c) 2016, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.io.gateways.network;

import org.apache.commons.lang3.Validate;

/**
 * Sent data through a TCP socket. Successful response to {@link TcpWriteRequest}.
 * @author Kasra Faghihi
 */
public final class TcpWriteResponse {
    private int amountWritten;

    /**
     * Constructs a {@link TcpWriteResponse} object.
     * @param amountWritten amount of data written out (in bytes)
     */
    public TcpWriteResponse(int amountWritten) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, amountWritten);
        this.amountWritten = amountWritten;
    }

    /**
     * Amount of data written out.
     * @return bytes written out
     */
    public int getAmountWritten() {
        return amountWritten;
    }

    @Override
    public String toString() {
        return "TcpWriteResponse{amountWritten=" + amountWritten + '}';
    }
    
}
