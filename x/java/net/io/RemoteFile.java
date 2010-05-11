/**
 * 
 */
package x.java.net.io;

import java.io.Serializable;


/**
 * The remote version of {@link java.io.File}.
 * 
 * @rem Re-generate {@link #serialVersionUID} when changing the file.
 * {@todo 1.0} Create all the relevant methods to mimic {@link java.io.File} i/f
 */
public class RemoteFile implements Serializable
{
    private static final long serialVersionUID = 7977086159165683399L;


    /**
     * 
     */
    public RemoteFile(String _host, String pathname)
    {
        host = _host;
        path = pathname;
    }
    
    
    public String getHost()
    {
        return host;
    }

    public String getPath()
    {
        return path;
    }
    

    protected final String host;
    protected final String path;
}
