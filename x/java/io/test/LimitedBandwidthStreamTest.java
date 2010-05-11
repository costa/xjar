/**
 * 
 */
package x.java.io.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import x.java.io.LimitedBandwidthInputStream;
import x.java.io.LimitedBandwidthOutputStream;

/**
 *
 */
public class LimitedBandwidthStreamTest
{
    @Test
    public void testInputStream() throws IOException
    {
        LimitedBandwidthInputStream lbis
            = new LimitedBandwidthInputStream
                (new InputStream() {
                    @Override public int read() throws IOException {
                        return (counter < 1000000)? ('0' + counter++ % 10): -1;
                    }
                    int counter = 0;
                }
                , 100, 1000);
        
        long start = System.currentTimeMillis();
        int counter = 0;
        int ch = lbis.read();
        while (ch != -1) {
            assertEquals(ch, '0' + counter++ % 10);
            ch = lbis.read();
        }
        lbis.close();
        assertEquals(counter, 1000000);

        long time = System.currentTimeMillis() - start;
        
        if (time < 9000 || time > 11000)
            fail("Time is " + time + "ms, should be ~10000");
    }
    
    @Test
    public void testOutputStream() throws IOException
    {
        LimitedBandwidthOutputStream lbos
            = new LimitedBandwidthOutputStream
                (new OutputStream() {
                    @Override public void write(int b) throws IOException {
                        assertEquals(b, '0' + counter++ % 10);                        
                    }
                    @Override public void close() {
                        assertEquals(counter, 1000000);
                    }
                    int counter = 0;
                }
                , 100, 1000);
        
        long start = System.currentTimeMillis();
        
        for (int counter = 0; counter < 1000000; counter++)
            lbos.write('0' + counter % 10);
        lbos.close();
        
        long time = System.currentTimeMillis() - start;
        
        if (time < 9000 || time > 11000)
            fail("Time is " + time + "ms, should be ~10000");
    }
}
