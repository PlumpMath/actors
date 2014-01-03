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
package com.offbynull.peernetic.rpc.invoke.helpers.invokationchain;

import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;

/**
 * Performs an async invokation.
 * @author Kasra Faghihi
 */
public interface InvokationChainStep {

    /**
     * Performs an async invokation.
     * @param resultListener result listener to pass in to async invokation
     */
    void doInvoke(AsyncResultListener resultListener);
    
}