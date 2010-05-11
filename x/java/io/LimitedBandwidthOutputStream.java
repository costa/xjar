/**
 * 
 */
package x.java.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A simple Limited-Bandwidth Filter Output Stream.
 * It is implemented using a simple wait-then-write mechanism.
 * 
 * @note It doesn't seem to be mentioned elsewhere explicitly, but any
 * concurrent work with any stream has to be synchronized externally.
 * @note The time window for maintaining the bandwidth is from the beginning of
 * the reading until this moment. This may be a point for future enhancements.
 * @note This is a twin brother of {@link LimitedBandwidthInputStream}, meaning
 * it is much of copy-paste. Food for thought. Or not.
 */
public class LimitedBandwidthOutputStream extends FilterOutputStream
{
    /**
     * Creates a limit-specified-bandwidth output stream from the given output
     * stream (with unlimited bursts)
     * 
     * @param out
     * @param bytesPerMilli 
     */
    public LimitedBandwidthOutputStream(OutputStream out, long bytesPerMilli)
    {
        this(out, bytesPerMilli, Integer.MAX_VALUE);
    }
    
    /**
     * Creates a limit-specified-bandwidth output stream from the given output
     * stream (with the specified max burst size)
     * 
     * @param out
     * @param bytesPerMilli 
     * @param maxBurstBytes
     */
    public LimitedBandwidthOutputStream( OutputStream out
                                       , long bytesPerMilli
                                       , int maxBurstBytes)
    {
        super(out);
        
        if (bytesPerMilli > 0)
            bandwidth = bytesPerMilli;
        else
            throw new IllegalArgumentException
                ("Bandwidth is negative or zero (" + bytesPerMilli + ")!");
        
        if (maxBurstBytes > 0)
            burstSize = maxBurstBytes;
        else
            throw new IllegalArgumentException
                ("Maximum burst size is negative or zero ("+maxBurstBytes+")!");
        
        initTimeMillis = nextTimeMillis = 0;
        totalBytes = 0;
    }

    

    /**
     * @see java.io.FilterOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        assert b != null && off >= 0 && len >= 0;
        
        int wrote = 0;
        
        while (wrote < len) {
            int bytes = ((len - wrote) > burstSize) ? burstSize : (len - wrote);
            
            delay(bytes);
            
            out.write(b, off + wrote, bytes);
            
            wrote += bytes;
        }
    }

    /**
     * @see java.io.FilterOutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException
    {
        assert b != null;

        write(b, 0, b.length);
    }

    /**
     * @see java.io.FilterOutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException
    {
        delay(1);
        
        out.write(b);
    }


    protected void delay(int bytes)
    {
        assert bytes > 0;
        
        if (System.currentTimeMillis() < nextTimeMillis)
            try {
                Thread.sleep(nextTimeMillis - System.currentTimeMillis());
            } catch (InterruptedException e) {
                // Ignore it?
            }
        
        if (initTimeMillis == 0)
            initTimeMillis = System.currentTimeMillis();
            
        totalBytes += bytes;
        
        nextTimeMillis = initTimeMillis + totalBytes/bandwidth;
    }

    
    protected long bandwidth;
    protected int burstSize;
    protected long initTimeMillis;
    protected long nextTimeMillis;
    protected long totalBytes;
}
