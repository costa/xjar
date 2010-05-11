/**
 * 
 */
package x.java.net.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;

/**
 * The remote brother (using {@link com.healthmarketscience.rmiio}) of
 * {@link FileInputStream}. Simple file streaming for the very basic needs.
 * 
 * {@todo 1.0} RMI port should be configurable.
 */
public class RemoteFileInputStream extends InputStream
{
    /**
     * {@todo 1.0} Think if {@link #stream(String)} is correctly parameterized
     * and/or add differently parameterized methods to the i/f
     */
    protected interface Streamer extends Remote
    {
        public static final String regName = Streamer.class.getName();
        
        RemoteInputStream stream(String pathname) throws IOException;
    }
    
    
    /**
     * Calls {@link #serve(File)} with an appropriate new {@link File} object.
     * 
     * @param path
     * @throws RemoteException
     * @throws AlreadyBoundException
     * @see #serve(File)
     */
    public static void serve(String path)
            throws RemoteException, AlreadyBoundException
    {
        serve((path == null) ? null : new File(path));
    }
    
    /**
     * Makes the specified file (or children of it if it is a directory)
     * available for remote streaming using an object of this class.
     * 
     * @warn This method is making local files available for RMI clients with
     * no restriction. Use extreme caution!
     * @note The default registry port is used.
     * @note If more than just this one RMI server may exist on this machine
     * (with the specified port) it is necessary to create an RMI registry in
     * advance and it will be used instead of creating one of its own.
     * @note Maybe a TODO: Only one server is permitted and therefore only one
     * pathname may currently be served.
     * @param local (if null, any accessible file will be available!)
     * @throws RemoteException
     * @throws AlreadyBoundException
     */
    public static void serve(final File local)
            throws RemoteException, AlreadyBoundException
    {
        Registry localReg = null;
        try {
            localReg = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            localReg = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        }
        localReg.bind
            ( Streamer.regName
            , new Streamer() {
                public RemoteInputStream stream(String pathname)
                        throws IOException {
                    File f = new File(pathname);
                    
                    if (local != null && !f.getCanonicalPath()
                                        .startsWith(local.getCanonicalPath()))
                        throw new IOException("Acess is denied to " + pathname);
                    
                    return new SimpleRemoteInputStream(new FileInputStream(f));
                }
             });
    }
    
    
    /**
     * @throws IOException 
     * 
     */
    public RemoteFileInputStream(RemoteFile rfile) throws IOException
    {
        Registry registry = LocateRegistry.getRegistry(rfile.getHost());
        try {
            Streamer fs = (Streamer)registry.lookup(Streamer.regName);
            RemoteInputStream ris = fs.stream(rfile.getPath());
            wrapped = RemoteInputStreamClient.wrap(ris);
        } catch (NotBoundException e) {
            throw new RemoteException
                (rfile.getHost() + "'s file stream server is unavailable", e);
        }
    }


    /**
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException
    {
        return wrapped.read();
    }

    /**
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException
    {
        return wrapped.available();
    }

    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        wrapped.close();
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        return wrapped.read(b, off, len);
    }

    /** 
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException
    {
        return wrapped.read(b);
    }

    /**
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException
    {
        return wrapped.skip(n);
    }


    /**
     * @see java.io.FileInputStream#finalize()
     */
    @Override
    protected void finalize() throws Throwable
    {
        try {
            wrapped.close();
        } catch (IOException e) {
            // Okaaaay
        }
    }

    
    protected final InputStream wrapped;
}
