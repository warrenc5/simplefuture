package au.com.devnull.simple;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 *
 * @author wozza
 */
public class SimpleFuture<T, P> implements Future<T> {

    T value;
    java.util.EventListener listener;
    boolean completed = false;
    boolean cancelled = false;
    Function<P, T> call;

    public SimpleFuture(Function<P, T> call) {
        this.call = call;
    }

    public SimpleFuture() {
    }

    public void reset() {
        this.completed = false;
        this.cancelled = false;
        this.value = null;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            if (mayInterruptIfRunning)
                synchronized (this) {
                    notifyAll();
                }
            cancelled = true;
            return !completed;
        } finally {
            completed = true;
        }
    //    if(listener!=null)
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isDone() {
        return completed;
    }

    public synchronized void set(T value) {
        this.value = value;
        notifyAll();
        completed = true;
    }

    public synchronized T get() throws InterruptedException, ExecutionException {
        if (!completed)
            this.wait();
        return value;
    }

    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!completed) {
            wait(unit.toMillis(timeout));
            if (value == null)
                throw new TimeoutException("timeout");
        }
        return value;
    }

    public T apply(P r) {
        if (call == null) {
            return;
        }

        T t = this.call.apply(r);
        this.set(t);
        return t;
    }
}
