package com.offbynull.rpc;

import com.offbynull.rpc.invoke.AsyncCapturer;
import com.offbynull.rpc.invoke.AsyncCapturerHandler;
import com.offbynull.rpc.invoke.AsyncCapturerHandlerCallback;
import com.offbynull.rpc.invoke.AsyncResultListener;
import com.offbynull.rpc.invoke.InvokeThreadInformation;
import com.offbynull.rpc.invoke.Invoker;
import com.offbynull.rpc.invoke.InvokerListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public final class AsyncInvokeTest {

    public AsyncInvokeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void simpleInvokeTest() throws InterruptedException, TimeoutException {
        final AtomicBoolean failFlag = new AtomicBoolean();
        final AtomicBoolean successFlag = new AtomicBoolean();
        final Exchanger<?> exchanger = new Exchanger<>();
        
        FakeObject server = Mockito.mock(FakeObject.class);
        FakeObjectAsync client = generateAsyncStub(server, failFlag, Collections.emptyMap());

        TestAsyncResultListener listener = new TestAsyncResultListener(failFlag, successFlag, exchanger);
        client.fakeMethodCall(listener);
        
        Assert.assertEquals(null, exchanger.exchange(null, 500L, TimeUnit.MILLISECONDS));
        Assert.assertFalse(failFlag.get());
        Assert.assertTrue(successFlag.get());
    }

    @Test
    public void simpleInvokeWithWrongReturnTypeTest() throws InterruptedException, TimeoutException {
        final AtomicBoolean failFlag = new AtomicBoolean();
        final AtomicBoolean successFlag = new AtomicBoolean();
        final Exchanger<?> exchanger = new Exchanger<>();
        
        IntegerReturnTypeObject server = Mockito.mock(IntegerReturnTypeObject.class);
        StringReturnTypeObjectAsync client = generateIncorrectlyWiredAsyncStub(StringReturnTypeObject.class,
                StringReturnTypeObjectAsync.class, server, failFlag, Collections.emptyMap());
        
        Mockito.when(server.fakeMethodCall(0)).thenReturn(5);

        TestAsyncResultListener listener = new TestAsyncResultListener(failFlag, successFlag, exchanger);
        client.fakeMethodCall(listener, 0);
        
        Assert.assertNotEquals(5, exchanger.exchange(null, 500L, TimeUnit.MILLISECONDS));
        Assert.assertTrue(failFlag.get());
        Assert.assertFalse(successFlag.get());
    }
    
    @Test
    public void simpleInvokeWithInfoTest() throws Throwable {
        final AtomicBoolean failFlag = new AtomicBoolean();
        final AtomicBoolean successFlag = new AtomicBoolean();
        final Exchanger<?> exchanger = new Exchanger<>();
        
        final Map<String, Object> info = new HashMap<>();
        info.put("TestKey1", "TestValue1");
        info.put("TestKey2", 2);
        
        FakeObject server = Mockito.mock(FakeObject.class);
        FakeObjectAsync client = generateAsyncStub(server, failFlag, info);
        
        Mockito.when(server.fakeMethodCall(Matchers.anyString())).thenAnswer(new Answer<Integer>() {

            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Map<String, Object> grabbedInfo = InvokeThreadInformation.getInfoMap();
                Assert.assertEquals(info, grabbedInfo);
                return 5;
            }
            
        });

        TestAsyncResultListener listener = new TestAsyncResultListener(failFlag, successFlag, exchanger);
        client.fakeMethodCall(listener, "");
        
        Assert.assertEquals(5, exchanger.exchange(null, 50000L, TimeUnit.MILLISECONDS));
        Assert.assertFalse(failFlag.get());
        Assert.assertTrue(successFlag.get());
    }
    
    @Test
    public void advancedInvokeTest() throws InterruptedException, TimeoutException {
        final AtomicBoolean failFlag = new AtomicBoolean();
        final AtomicBoolean successFlag = new AtomicBoolean();
        final Exchanger<?> exchanger = new Exchanger<>();
        
        FakeObject server = Mockito.mock(FakeObject.class);
        Mockito.when(server.fakeMethodCall("req msg", 0)).thenReturn("resp msg");
        
        FakeObjectAsync client = generateAsyncStub(server, failFlag, Collections.emptyMap());

        AsyncResultListener<String> listener = new TestAsyncResultListener<>(failFlag, successFlag, exchanger);
        client.fakeMethodCall(listener, "req msg", 0);

        Assert.assertEquals("resp msg", (String) exchanger.exchange(null, 500L, TimeUnit.MILLISECONDS));
        Assert.assertFalse(failFlag.get());
        Assert.assertTrue(successFlag.get());
    }

    @Test
    public void throwableInvokeTest() throws InterruptedException, TimeoutException {
        final AtomicBoolean failFlag = new AtomicBoolean();
        final AtomicBoolean successFlag = new AtomicBoolean();
        final Exchanger<?> exchanger = new Exchanger<>();
        
        FakeObject server = Mockito.mock(FakeObject.class);
        Mockito.when(server.fakeMethodCall("req msg", 0)).thenThrow(new IllegalArgumentException("bad!"));
        
        FakeObjectAsync client = generateAsyncStub(server, failFlag, Collections.emptyMap());

        AsyncResultListener<String> listener = new TestAsyncResultListener<>(failFlag, successFlag, exchanger);
        client.fakeMethodCall(listener, "req msg", 0);

        Assert.assertEquals(IllegalArgumentException.class, exchanger.exchange(null, 500L, TimeUnit.MILLISECONDS).getClass());
        Assert.assertTrue(failFlag.get());
        Assert.assertFalse(successFlag.get());
    }

    private class TestAsyncResultListener<T> implements AsyncResultListener<T> {
        private AtomicBoolean failFlag;
        private AtomicBoolean successFlag;
        private Exchanger finishExchanger;

        public TestAsyncResultListener(AtomicBoolean failFlag, AtomicBoolean successFlag, Exchanger finishExchanger) {
            this.failFlag = failFlag;
            this.successFlag = successFlag;
            this.finishExchanger = finishExchanger;
        }
        
        @Override
        public void invokationReturned(T object) {
            successFlag.set(true);
            try {
                finishExchanger.exchange(object);
            } catch (InterruptedException ex) {
            }
        }

        @Override
        public void invokationThrew(Throwable err) {
            failFlag.set(true);
            try {
                finishExchanger.exchange(err);
            } catch (InterruptedException ex) {
            }
        }

        @Override
        public void invokationFailed(Object err) {
            failFlag.set(true);
            try {
                finishExchanger.exchange(err);
            } catch (InterruptedException ex) {
            }
        }
    }
    
    private FakeObjectAsync generateAsyncStub(FakeObject obj, final AtomicBoolean failFlag,
            final Map<? extends Object, ? extends Object> invokeInfo) {
        final Invoker invoker = new Invoker(obj, Executors.newFixedThreadPool(1));
        AsyncCapturer<FakeObject, FakeObjectAsync> capturer = new AsyncCapturer<>(FakeObject.class, FakeObjectAsync.class);
        
        FakeObjectAsync client = capturer.createInstance(new AsyncCapturerHandler() {

            @Override
            public void invokationTriggered(final byte[] data, final AsyncCapturerHandlerCallback responseHandler) {

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        invoker.invoke(data, new InvokerListener() {

                            @Override
                            public void invokationFailed(Throwable t) {
                                responseHandler.responseFailed(t);
                            }

                            @Override
                            public void invokationFinised(byte[] outData) {
                                responseHandler.responseArrived(outData);
                            }
                        }, invokeInfo);
                    }
                };
                
                new Thread(r).start();
            }

            @Override
            public void invokationFailed(Throwable err) {
                failFlag.set(true);
            }
        });
        
        return client;
    }

    private <T, AT, ST> AT generateIncorrectlyWiredAsyncStub(Class<T> type, Class<AT> asyncType, Object server,
            final AtomicBoolean failFlag,
            final Map<? extends Object, ? extends Object> invokeInfo) {
        final Invoker invoker = new Invoker(server, Executors.newFixedThreadPool(1));
        AsyncCapturer<T, AT> capturer = new AsyncCapturer<>(type, asyncType);
        
        AT client = capturer.createInstance(new AsyncCapturerHandler() {

            @Override
            public void invokationTriggered(final byte[] data, final AsyncCapturerHandlerCallback responseHandler) {

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        invoker.invoke(data, new InvokerListener() {

                            @Override
                            public void invokationFailed(Throwable t) {
                                responseHandler.responseFailed(t);
                            }

                            @Override
                            public void invokationFinised(byte[] outData) {
                                responseHandler.responseArrived(outData);
                            }
                        }, invokeInfo);
                    }
                };
                
                new Thread(r).start();
            }

            @Override
            public void invokationFailed(Throwable err) {
                failFlag.set(true);
            }
        });
        
        return client;
    }
    
    private interface FakeObject {

        void fakeMethodCall();

        int fakeMethodCall(String arg);

        String fakeMethodCall(int arg);

        String fakeMethodCall(String arg1, int arg2);

        int fakeMethodCall2(String arg);
    }
    
    private interface FakeObjectAsync {
        void fakeMethodCall(AsyncResultListener<Void> result);

        void fakeMethodCall(AsyncResultListener<Integer> result, String arg);

        void fakeMethodCall(AsyncResultListener<String> result, int arg);

        void fakeMethodCall(AsyncResultListener<String> result, String arg1, int arg2);

        void fakeMethodCall2(AsyncResultListener<Integer> result, String arg);
    }

    private interface IntegerReturnTypeObject {

        Integer fakeMethodCall(int arg);
    }
    
    private interface StringReturnTypeObject {

        String fakeMethodCall(int arg);
    }
    
    private interface StringReturnTypeObjectAsync {
        void fakeMethodCall(AsyncResultListener<String> result, int arg);
    }

    private interface IntegerReturnTypeObjectAsync {
        void fakeMethodCall(AsyncResultListener<Integer> result, int arg);
    }
}
