/**
 * 
 */
package x.java.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A simple Limited-Bandwidth Filter Input Stream.
 * It is implemented using a simple wait-then-read mechanism.
 * 
 * @note It doesn't seem to be mentioned elsewhere explicitly, but any
 * concurrent work with any stream has to be synchronized externally.
 * @note The time window for maintaining the bandwidth is from the beginning of
 * the reading until this moment. This may be a point for future enhancements.
 */
public class LimitedBandwidthInputStream extends FilterInputStream
{
    /**
     * Creates a limit-specified-bandwidth input stream from the given input
     * stream (with unlimited bursts)
     * 
     * @param in
     * @param bytesPerMilli 
     */
    public LimitedBandwidthInputStream(InputStream in, long bytesPerMilli)
    {
        this(in, bytesPerMilli, Integer.MAX_VALUE);
    }
    
    /**
     * Creates a limit-specified-bandwidth input stream from the given input
     * stream (with the specified max burst size)
     * 
     * @param in
     * @param bytesPerMilli 
     * @param maxBurstBytes
     */
    public LimitedBandwidthInputStream( InputStream in
                                      , long bytesPerMilli
                                      , int maxBurstBytes)
    {
        super(in);
        
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
     * @see java.io.FilterInputStream#available()
     */
    @Override
    public int available() throws IOException
    {
        if (System.currentTimeMillis() < nextTimeMillis)
            return 0;
        
        int total = in.available();
        
        return (total > burstSize) ? burstSize : total;
    }

    /**
     * @see java.io.FilterInputStream#read()
     */
    @Override
    public int read() throws IOException
    {
        delay(1);
        
        return in.read();
    }

    /**
     * @see java.io.FilterInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        assert b != null && off >= 0 && len >= 0;
        
        int togo = len;
        int lastRead = 1; // to get into the loop
        int totalRead = 0;
        
        while (togo > 0 && lastRead > 0) {
            int bytes = (togo > burstSize) ? burstSize : togo;
            
            delay(bytes);
            
            lastRead = in.read(b, off + totalRead, bytes);
            if (totalRead == 0)
                totalRead = lastRead;
            else if (lastRead > 0)
                totalRead += lastRead;
            
            togo -= bytes;
        }

        return totalRead;
    }

    /**
     * @see java.io.FilterInputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException
    {
        assert b != null;
        
        return read(b, 0, b.length);
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
