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
package com.offbynull.peernetic.io.common;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import java.util.Arrays;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }
    
    public static <T> T readUntilOrTimeout(Continuation cnt, Class<T> expectedClass, Object timeoutObject, Class<?> ... errorClasses) {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            cnt.suspend();
            
            Object obj = ctx.getIncomingMessage();
            if (obj == timeoutObject) {
                throw new RuntimeException("Timed out");
            }

            if (obj.getClass() == expectedClass) {
                return (T) obj;
            }
            
            if (Arrays.stream(errorClasses).anyMatch(x -> x == obj.getClass())) {
                throw new RuntimeException("Encountered error " + obj);
            }
        }
    }
}
