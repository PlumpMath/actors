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
package com.offbynull.peernetic.router.common;

/**
 * The expected response never arrived.
 * @author Kasra Faghihi
 */
public final class NoResponseException extends RuntimeException {

    /**
     * Construct a {@link NoResponseException}.
     */
    public NoResponseException() {
    }

    /**
     * Construct a {@link NoResponseException} with a message.
     * @param message message
     */
    public NoResponseException(String message) {
        super(message);
    }

    /**
     * Construct a {@link NoResponseException} with a message and a cause.
     * @param message message
     * @param cause cause
     */
    public NoResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a {@link NoResponseException} with a cause.
     * @param cause cause
     */
    public NoResponseException(Throwable cause) {
        super(cause);
    }
    
}