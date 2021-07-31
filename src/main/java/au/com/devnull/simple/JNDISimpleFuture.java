package au.com.devnull.simple;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JNDISimpleFuture<T> implements Future<T>, Serializable {

    private UUID uuid;
    ArrayBlockingQueue<T> q = new ArrayBlockingQueue<T>(1);
    private boolean done;
    public static transient Logger logger = Logger.getLogger(JNDISimpleFuture.class.getSimpleName());
    public static final String JNDI_BASE = "java:";
    private StackTraceElement[] stackTrace;
    private boolean cancelled;
    private transient Thread waitingThread;

    public JNDISimpleFuture() throws NamingException {
        uuid = UUID.randomUUID();
        bindInEnvironment(uuid.toString(),this);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done)
            return false;

        stackTrace = Thread.currentThread().getStackTrace();
        cancelled = true;

        if (mayInterruptIfRunning)
            if(waitingThread != null)
                waitingThread.interrupt();

        unbindInEnvironment(uuid.toString());

        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    public T get() throws InterruptedException, ExecutionException {
        waitingThread = Thread.currentThread();
        try {
            return q.take();
        } finally {
            done = true;
            checkForCancelled();
            unbindInEnvironment(uuid.toString());
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        checkForCancelled();

        waitingThread = Thread.currentThread();

        try {
            return q.poll(timeout, unit);
        } catch (InterruptedException x) {
            logger.log(Level.WARNING, x.getMessage(), x);
            throw x;
        } finally {
            done = true;
            unbindInEnvironment(uuid.toString());
            checkForCancelled();
        }
    }

    public void add(T v) {
        try {
            q.add(v);
        } finally {
            done = true;
        }
    }

    public String getUuid() {
        return uuid.toString();
    }

    public static void bindInEnvironment(String uuid, Future future) throws NamingException {
        //JNDI or JAVASPACES
        InitialContext initialContext = new InitialContext();
        Context c = (Context) initialContext.lookup(JNDI_BASE);
        c.bind(uuid, future);
    }


    public static void unbindInEnvironment(String uuid) {
        try {
            InitialContext initialContext = new InitialContext();
            Context c = (Context) initialContext.lookup(JNDI_BASE);
            c.unbind(uuid.toString());
        } catch (NamingException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    public static Future lookupInEnvironment(String uuid) throws NamingException {
        InitialContext initialContext = new InitialContext();
        Context c = (Context) initialContext.lookup(JNDI_BASE);
        return (Future) c.lookup(uuid.toString());
    }

    private void checkForCancelled() throws ExecutionException {
        if (isCancelled()) {
            Exception ex = new Exception();
            ex.setStackTrace(stackTrace);
            throw new ExecutionException("Future was cancelled " + uuid, ex);
        }
    }

}
