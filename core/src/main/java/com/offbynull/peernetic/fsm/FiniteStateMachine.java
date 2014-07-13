package com.offbynull.peernetic.fsm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public final class FiniteStateMachine<P> {
    private Object object;
    private String currentState;
    private UnmodifiableMap<StateKey, Method> stateHandlerMap;
    private UnmodifiableMap<TransitionKey, Method> transitionHandlerMap;
    
    public FiniteStateMachine(Object object, String currentState, Class<P> paramType) {
        Validate.notNull(object);
        Validate.notNull(currentState);
        Validate.notNull(paramType);
        this.object = object;
        this.currentState = currentState;
        
        Class<?> cls = object.getClass();
        Method[] methods = cls.getDeclaredMethods();
        
        Map<StateKey, Method> stateHandlerMap = new HashMap<>();
        for (Method method : methods) {
            StateHandler[] annotations = method.getDeclaredAnnotationsByType(StateHandler.class);
            if (annotations.length == 0) {
                continue;
            }
            
            Validate.isTrue(annotations.length == 1, "Method %s can only have 1 %s annotation",
                    method.getName(), StateHandler.class.getSimpleName());
            
            StateHandler stateHandler = annotations[0];
            
            Class<?>[] methodParams = method.getParameterTypes();
            Validate.isTrue(methodParams.length == 5
                    && ClassUtils.isAssignable(methodParams[0], String.class) // state
                    && ClassUtils.isAssignable(methodParams[1], FiniteStateMachine.class) // this
                    && ClassUtils.isAssignable(methodParams[2], Instant.class) // time
                    && ClassUtils.isAssignable(methodParams[3], Object.class) // msg
                    && ClassUtils.isAssignable(methodParams[4], paramType), // params
                    "Method %s with %s has incorrect arguments",
                    method.getName(), StateHandler.class.getSimpleName());
            method.setAccessible(true);
            
            String[] states = stateHandler.value();
            for (String state : states) {
                StateKey key = new StateKey(state, methodParams[3]);
                Method existingMethod = stateHandlerMap.put(key, method);
                
                Validate.isTrue(existingMethod == null, "Duplicate %s found: %s",
                        StateHandler.class.getSimpleName(), method.getName());
            }
        }
        
        this.stateHandlerMap = (UnmodifiableMap<StateKey, Method>) UnmodifiableMap.unmodifiableMap(stateHandlerMap);
        
        
        
        Map<TransitionKey, Method> transitionHandlerMap = new HashMap<>();
        for (Method method : methods) {
            TransitionHandler[] annotations = method.getDeclaredAnnotationsByType(TransitionHandler.class);
            if (annotations.length == 0) {
                continue;
            }
            
            Validate.isTrue(annotations.length == 1, "Method %s can only have 1 %s annotation",
                    method.getName(), TransitionHandler.class.getSimpleName());
            
            TransitionHandler transitionHandler = annotations[0];
            
            Class<?>[] methodParams = method.getParameterTypes();
            Validate.isTrue(methodParams.length == 3
                    && ClassUtils.isAssignable(methodParams[0], String.class)
                    && ClassUtils.isAssignable(methodParams[1], String.class)
                    && ClassUtils.isAssignable(methodParams[2], FiniteStateMachine.class),
                    "Method %s with %s has incorrect arguments",
                    method.getName(), StateHandler.class.getSimpleName());
            method.setAccessible(true);
            
            Transition[] transitions = transitionHandler.value();
            Validate.isTrue(transitions.length > 0, "Need atleast 1 transition for method %s", method.getName());
            
            for (Transition transition : transitions) {
                TransitionKey key = new TransitionKey(transition.from(), transition.to());
                Method existingMethod = transitionHandlerMap.put(key, method);
                
                Validate.isTrue(existingMethod == null, "Duplicate %s found: %s",
                        TransitionHandler.class.getSimpleName(), method.getName());
            }
        }
        
        this.transitionHandlerMap = (UnmodifiableMap<TransitionKey, Method>) UnmodifiableMap.unmodifiableMap(transitionHandlerMap);
    }
    
    public void process(Instant instant, Object message, P params) {
        Validate.notNull(instant);
        Validate.notNull(message);
        
        Method method = null;
        for (Class<?> cls : ClassUtils.hierarchy(message.getClass(), ClassUtils.Interfaces.INCLUDE)) {
            StateKey key = new StateKey(currentState, cls);
            method = stateHandlerMap.get(key);
            
            if (method != null) {
                break;
            }
        }
        
        Validate.validState(method != null, "No handler for %s during state %s", message.getClass(), currentState);
        
        try {
            method.invoke(object, currentState, this, instant, message, params);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void switchStateAndProcess(String state, Instant instant, Object message, P params) {
        setState(state);
        process(instant, message, params);
    }
    
    public void setState(String state) {
        TransitionKey key = new TransitionKey(currentState, state);
        Method method = transitionHandlerMap.get(key);
        if (method != null) {
            try {
                method.invoke(object, currentState, state, this);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        
        currentState = state;
    }
    
    public String getState() {
        return currentState;
    }
    
    private static final class StateKey {
        private final String state;
        private final Class<?> type;

        public StateKey(String state, Class<?> type) {
            this.state = state;
            this.type = type;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.state);
            hash = 17 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StateKey other = (StateKey) obj;
            if (!Objects.equals(this.state, other.state)) {
                return false;
            }
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            return true;
        }
        
    }
    
    private static final class TransitionKey {
        private String from;
        private String to;

        public TransitionKey(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + Objects.hashCode(this.from);
            hash = 67 * hash + Objects.hashCode(this.to);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TransitionKey other = (TransitionKey) obj;
            if (!Objects.equals(this.from, other.from)) {
                return false;
            }
            if (!Objects.equals(this.to, other.to)) {
                return false;
            }
            return true;
        }
        
    }
}