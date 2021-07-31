package au.com.devnull.simple;

import org.junit.jupiter.api.Test;

/**
 *
 * @author wozza
 */
public class SimpleFutureTest {

    @Test
    public void testSimpleFuture() {
        SimpleFuture simpleFuture = new SimpleFuture();
        simpleFuture.apply(new Object());
    }

}
