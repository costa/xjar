/**
 * 
 */
package x.java.net.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.healthmarketscience.rmiio.RemoteOutputStream;
import com.healthmarketscience.rmiio.RemoteOutputStreamClient;
import com.healthmarketscience.rmiio.SimpleRemoteOutputStream;

/**
 * The remote brother (using {@link com.healthmarketscience.rmiio}) of
 * {@link FileOutputStream}. Simple file streaming for the very basic needs.
 * 
 * {@todo 1.0} RMI port should be configurable.
 */
public class RemoteFileOutputStream extends OutputStream
{
    /**
     * {@todo 1.0} Think if {@link #stream(String)} is correctly parameterized
     * and/or add differently parameterized methods to the i/f
     */
    protected interface Streamer extends Remote
    {
        public static final String regName = Streamer.class.getName();
        
        RemoteOutputStream stream(String pathname) throws IOException;
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
     * Makes the specified file (or children of it - if it is a directory)
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
                public RemoteOutputStream stream(String pathname)
                        throws IOException {
                    File f = new File(pathname);
                    
                    if (local != null && !f.getCanonicalPath()
                                        .startsWith(local.getCanonicalPath()))
                        throw new IOException("Acess is denied to " + pathname);
                    
                    return new SimpleRemoteOutputStream(new FileOutputStream(f));
                }
             });
    }
    
    
    public RemoteFileOutputStream(RemoteFile rfile) throws IOException
    {
        this(rfile, false);
    }
    
    /**
     * @throws IOException 
     * 
     */
    public RemoteFileOutputStream(RemoteFile rfile, boolean append)
            throws IOException
    {
        Registry registry = LocateRegistry.getRegistry(rfile.getHost());
        try {
            Streamer fs = (Streamer)registry.lookup(Streamer.regName);
            RemoteOutputStream ros = fs.stream(rfile.getPath());
            wrapped = RemoteOutputStreamClient.wrap(ros);
        } catch (NotBoundException e) {
            throw new RemoteException
                (rfile.getHost() + "'s file stream server is unavailable", e);
        }
    }


    /**
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        wrapped.close();
    }

    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        wrapped.write(b, off, len);
    }

    /**
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException
    {
        wrapped.write(b);
    }

    /**
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException
    {
        wrapped.write(b);
    }

    /**
     * @see java.io.FileOuputStream#finalize()
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

    
    protected final OutputStream wrapped;
}
