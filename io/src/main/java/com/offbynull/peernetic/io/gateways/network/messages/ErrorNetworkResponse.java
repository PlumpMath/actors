/*
 * Copyright (c) 2013-2016, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.io.gateways.network.messages;

/**
 * Non-identifiable error response. Error response to {@link CreateTcpNetworkRequest}, {@link CreateUdpNetworkRequest}, or
 * {@link GetLocalIpAddressesNetworkRequest}.
 * @author Kasra Faghihi
 */
public final class ErrorNetworkResponse {

    @Override
    public String toString() {
        return "ErrorNetworkResponse{" + '}';
    }

}
