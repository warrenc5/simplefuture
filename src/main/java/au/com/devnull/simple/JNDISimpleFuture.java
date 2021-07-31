package au.com.devnull.simple;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

class SimpleFuture<T> implements Future<T>, Serializable {

    private UUID uuid;
    ArrayBlockingQueue<T> q = new ArrayBlockingQueue<T>(1);
    private boolean done;
    public transient Logger logger = Logger.getLogger(SimpleFuture.class.getSimpleName());
    public static final String JNDI_BASE = "java:";
    private StackTraceElement[] stackTrace;
    private boolean cancelled;
    private transient Thread waitingThread;

    public SimpleFuture() throws NamingException {
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

    public boolean isCancelled() {
        return cancelled;
    }

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

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        checkForCancelled();

        waitingThread = Thread.currentThread();

        try {
            return q.poll(timeout, unit);
        } catch (InterruptedException x) {
            if (logger.isEnabledFor(Level.WARN))
                logger.log(Level.WARN, x.getMessage(), x);
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

    public static void bindInEnvironment(String uuid,SimpleFuture future) throws NamingException {
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
            Logger.getLogger(SimpleFuture.class.getSimpleName()).log(Level.WARN, null, ex);
        }
    }

    public static SimpleFuture lookupInEnvironment(String uuid) throws NamingException {
        InitialContext initialContext = new InitialContext();
        Context c = (Context) initialContext.lookup(JNDI_BASE);
        return (SimpleFuture) c.lookup(uuid.toString());
    }

    private void checkForCancelled() throws ExecutionException {
        if (isCancelled()) {
            Exception ex = new Exception();
            ex.setStackTrace(stackTrace);
            throw new ExecutionException("Future was cancelled " + uuid, ex);
        }
    }

}
