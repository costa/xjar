
package x.java.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;


/**
 * Fixed-property-set static configuration class.
 * Optimized for retrieval (vs storage) of variously (primitively) typed
 * properties. Actually, it is not a high-performaence class in general.
 * 
 * The class is abstract only to emphasize its intended purpose. The properties
 * should be created (and their references stored) at a subclass initialization.
 * 
 * @version 0.1
 * @note This {@link Properties} subclass is made to keep its superclass'
 * contract. However, because of its fixed-property-set nature, insertion and
 * removal operations are hooked and precoditioned as appropriate. 
 * @note We are keeping the internal contract of defaults in the read-only way!
 * Should the full contract will be needed in a subclass, the code must undergo
 * certain changes.
 * @note There is an open question about out-of-the-set properties. They are
 * totally restricted at the moment. This enforces the intended usage for this
 * class. However, this led to introducing a special "forgiving" method for
 * reading a file  ({@link #loadAny(InputStream)}.
 * @note And yes, this class is thought to be thread-safe, of course.
 * 
 * {@todo 2.0} Create a(n external?) utility for static configuration browsing
 * or storage, or even one with some dynamic capabilities.
 * {@todo 2.0} Some static configuration (subclass?) with more straightforward
 * persistence capabilities may be useful.
 */
public abstract class Configuration extends Properties
{
    /**
     * Prints a given Configuration's default values (compatible with
     * {@link #load(InputStream)} and stuff
     * 
     * @param args
     */
    public static void main(String args[])
    {
        if (args.length != 1) {
            System.err.println("A Configuration subclass name needed!");
            return;
        }
        try {
            ((Configuration)Class.forName(args[0]).newInstance())
                                                    .store(System.out, null);
        } catch (ClassNotFoundException e) {
            System.err.println("It seems there is no such class around here!");            
        } catch (InstantiationException e) {
            System.err.println("I cannot instantiate the class for some reason!");            
        } catch (IllegalAccessException e) {
            System.err.println("I cannot instantiate the class for some reason!");            
        } catch (ClassCastException e) {
            System.err.println("The class does not seem to be a Configuration subclass!");
        } catch (IOException e) {
            System.err.println("I cannot believe this is happening!");
        }
    }
    
    public abstract class Property
    {
        protected Property(String _key, String _sDescription)
        {
            assert _key != null;
            
            mStringToProperty.put(_key, this);
            key = _key;
            description = _sDescription;
        }

        
        public void reset()
        {
            resetProperty(key);
        }

        protected abstract void cache();
        
        
        protected final String key;
        protected final String description;
    }

    
    public class StringProperty extends Property
    {
        public StringProperty(String _key, String _default, String _description)
        {
            super(_key, _description);
            
            assert _default != null;
            
            defaults.setProperty(key, _default);
            cache();
        }

        
        public String get()
        {
            return sValue;
        }
            
        public void set(String _sValue)
        {
            setProperty(key, _sValue);
        }

        protected void cache()
        {
            sValue = getStringProperty(key);
        }

        
        protected String sValue;
    }
    
    
    public class IntegerProperty extends Property
    {
        public IntegerProperty(String _key, int _default, String _description)
        {
            super(_key, _description);
            
            defaults.setProperty(_key, Integer.toString(_default));
            cache();
        }

        public int get()
        {
            return value;
        }
        
        public void set(int _value)
        {
            setProperty(key, Integer.toString(_value));
        }


        protected void cache()
        {
            value = getIntegerProperty(key);
        }
        
        
        protected int value;
    }

    
    public class FormatProperty extends Property
    {
        public FormatProperty(String _key, String _default, String _description)
        {
            super(_key, _description);
            
            assert _default != null;
            
            defaults.setProperty(key, _default);
            cache();
        }

        
        public String get(Object ... _args)
        {
            return String.format(sValue, _args);
        }
            
        public void set(String _sValue)
        {
            setProperty(key, _sValue);
        }

        protected void cache()
        {
            sValue = getStringProperty(key);
        }

        
        protected String sValue;
    }
        

    /**
     * Builds a {@link Configuration} object which prefixes its properties with
     * the enclosing class name (if any) upon storage and removes the prefix
     * upon loading.<br/>
     * The properties are sorted in the creation order for output.
     */
    protected Configuration()
    {
        super(new Properties());
        
        mStringToProperty
           = Collections.synchronizedMap(new LinkedHashMap<String, Property>());

        Class<?> class_ = getClass().getEnclosingClass();
        if (class_ != null)
            propertyPrefix = class_.getName() + ".";
        else
            propertyPrefix = "";
    }

    /**
     * Builds a {@link Configuration} object which prefixes its properties with
     * the specified string (may be empty) upon storage and removes the prefix
     * upon loading.<br/>
     * The properties are sorted in the creation order for output.
     */
    protected Configuration(String _prefix)
    {
        super(new Properties());
        
        mStringToProperty
           = Collections.synchronizedMap(new LinkedHashMap<String, Property>());

        assert(_prefix != null);
        propertyPrefix = _prefix;
    }

    
    /**
     * @see Hashtable#put(Object, Object)
     */
    @Override
    public synchronized Object put(Object _key, Object _value)
    {
        Object tmp_ = super.put(_key, _value);
        
        property((String)_key).cache();
        
        return tmp_;
    }

    
    /**
     * Unprefixing load method
     * 
     * @see Properties#load(InputStream)
     */
    @Override
    public synchronized void load(InputStream _is) throws IOException
    {
        Properties tmp_ = new Properties();
        tmp_.load(_is);

        for (Object key_ : tmp_.keySet())
            setProperty(unprefix(key_), tmp_.getProperty((String)key_));
    }

    /**
     * Unprefixing load method
     * 
     * @see java.util.Properties#loadFromXML(InputStream)
     */
    @Override
    public synchronized void loadFromXML(InputStream _is)
            throws IOException, InvalidPropertiesFormatException
    {
        Properties tmp_ = new Properties();
        tmp_.loadFromXML(_is);

        for (Object key_ : tmp_.keySet())
            setProperty(unprefix(key_), tmp_.getProperty((String)key_));
    }

    /**
     * A "forgiving" {@link #load(InputStream)} method.
     * 
     * @return A set of known properties that had illegal values up that stream
     * or <code>null</code> if there were none as such.
     * @throws IOException just like its original restricted version
     */
    public synchronized Set<String> loadAny(InputStream _is) throws IOException
    {
        TreeSet<String> sts_ = null;
        
        Properties tmp_ = new Properties();
        tmp_.load(_is);
        
        for (Object key_ : tmp_.keySet())
            try {
                setProperty(unprefix(key_), tmp_.getProperty((String)key_));
            } catch (UnknownPropertyException _) {
                // We do nothing, dude. That's the whole idea!
            } catch (IllegalPropertyValueException _) {
                if (sts_ == null)
                    sts_ = new TreeSet<String>();
                sts_.add((String)key_);
            }
        
        return sts_;
    }
    
    @Override @Deprecated
    public synchronized void save(OutputStream _out, String _comments)
    {
        // Just lazy for overriding a deprecated method..
        throw new UnsupportedOperationException
            ("java.util.Properties#save and the overrides are deprecated!");
    }

    /**
     * An appropriate upgrade of the superclass method: it will store the
     * descriptions (as comments), the default values (commented out)
     * and the actual values (if set) in a hopefully nice form.
     * The properties will be prefixed as specified.
     * 
     * @see java.util.Properties#store(OutputStream, String)
     * @note This is an ugly hack, of course. I wish there were a standard
     * method for Properties file encoding
     * (see the original {@link java.util.Properties#store(OutputStream, String))
     * @note The initial buffer size seem okay, but one may want to use
     * a constant for it or even compute a more precise value (Naah).
     */
    @Override
    public synchronized void store(OutputStream _out, String _comments)
            throws IOException
    {
        String newLine = System.getProperty("line.separator");
        BufferedWriter bw_
            = new BufferedWriter(new OutputStreamWriter(_out, "8859_1"));
        
        Properties tmp_ = new Properties();
        ByteArrayOutputStream baos_ = new ByteArrayOutputStream(256);
        
        char lineCommentChar = 0;
        

        for (Property prop_ : mStringToProperty.values()) {
            String tmpKey_ = prefix(prop_.key);
            
            tmp_.setProperty(tmpKey_, defaults.getProperty(prop_.key));
            
            // First, let's print it via the original method
            tmp_.store(baos_, null);
            
            String sProp_ = baos_.toString("8859_1");

            if (lineCommentChar == 0) {
                lineCommentChar = sProp_.charAt(0);
                
                // Safe-print the comments..
                if (_comments != null) {
                    bw_.write(lineCommentChar);
                    bw_.write(' ');
                    bw_.write(_comments.replace( newLine
                                               , newLine+lineCommentChar+' '));
                    bw_.write(newLine);
                }
                // ..and the date
                bw_.write(lineCommentChar);
                bw_.write(new Date().toString());
                bw_.write(newLine);
            }
            
            bw_.write(newLine);

            if (prop_.description != null) {
                bw_.write(lineCommentChar);
                bw_.write(' ');
                bw_.write(prop_.description.replace
                        (newLine, newLine + lineCommentChar + ' '));
                bw_.write(newLine);
            }
            
            // Then, let's get rid of the date comment the original method prints
            sProp_ = sProp_.substring(sProp_.indexOf(newLine)+newLine.length());
            
            bw_.write(lineCommentChar);
            bw_.write(sProp_);
            
            if (containsKey(prop_.key)) {
                baos_.reset();

                tmp_.setProperty(tmpKey_, getProperty(prop_.key));
                
                // ..and again for the actual value..
                tmp_.store(baos_, null);
                
                sProp_ = baos_.toString("8859_1");
                sProp_
                    = sProp_.substring(sProp_.indexOf(newLine)+newLine.length());
                
                bw_.write(sProp_);
            }
            
            tmp_.remove(tmpKey_);
            baos_.reset();
        }
        
        bw_.flush();
    }

    /**
     * The properties will be prefixed as specified.
     * 
     * @see java.util.Properties#storeToXML(OutputStream, String, String)
     * @note The method does its job, but with no fancy comments of the
     * {@link #store(OutputStream, String)} 
     */
    @Override
    public synchronized void storeToXML
        (OutputStream _os, String _comment, String _encoding) throws IOException
    {
        Properties tmp_ = new Properties();
        
        for (Object key_ : keySet())
            tmp_.setProperty(prefix(key_), getProperty(key_.toString()));

        tmp_.storeToXML(_os, _comment, _encoding);
    }

    /**
     * The properties will be prefixed as specified.
     */
    @Override
    public synchronized void storeToXML(OutputStream _os, String _comment) throws IOException
    {
        storeToXML(_os, _comment, "UTF-8");
    }


    protected synchronized Property property(String _key)
    {
        Property prop_ = mStringToProperty.get(_key);
        
        if (prop_ == null)
            throw new UnknownPropertyException((String)_key);

        return prop_;
    }
    
    protected String prefix(Object _key)
    {
        return propertyPrefix + _key.toString();
    }

    protected String unprefix(Object _key)
    {
        String tmp_ = _key.toString();
        
        if (!tmp_.startsWith(propertyPrefix))
            throw new UnknownPropertyException(tmp_);
        
        return tmp_.substring(propertyPrefix.length());
    }

    
    protected final Map<String, Property> mStringToProperty;
    protected final String propertyPrefix;
    
    
    ///////////////////////////////////////////////////////////////////////////
    // BEGINNING of the Java/JDK/Collections greatness display
    
    protected class ObjectCollectionIterator implements Iterator<Object>
    {
        public ObjectCollectionIterator(Iterator<Object> _i)
        {
            i = _i;
            curr = null;
        }


        public boolean hasNext()
        {
            return i.hasNext();
        }

        public Object next()
        {
            return curr = i.next();
        }

        public synchronized void remove()
        {
            i.remove();
            if (curr != null)
                property((String)curr).cache();
        }

        
        protected final Iterator<Object> i;
        protected Object curr;
    }


    protected class ObjectSet extends AbstractSet<Object>
    {
        public ObjectSet(Set<Object> _set)
        {
            set = _set;
        }
        

        public Iterator<Object> iterator()
        {
            return new ObjectCollectionIterator(set.iterator());
        }

        public int size()
        {
            return set.size();
        }

        
        protected final Set<Object> set;
    }


    protected class ObjectCollection extends AbstractCollection<Object>
    {
        public ObjectCollection(Collection<Object> _collection)
        {
            collection = _collection;
        }
        
        
        public Iterator<Object> iterator()
        {
            return new ObjectCollectionIterator(collection.iterator());
        }

        public int size()
        {
            return collection.size();
        }

        
        protected final Collection<Object> collection;
    }


    protected class EntrySetIterator
            implements Iterator<Map.Entry<Object, Object>>
    {
        public EntrySetIterator(Iterator<Map.Entry<Object, Object>> _i)
        {
            i = _i;
            curr = null;
        }


        public boolean hasNext()
        {
            return i.hasNext();
        }

        public Map.Entry<Object, Object> next()
        {
            return curr = i.next();
        }

        public synchronized void remove()
        {
            i.remove();
            if (curr != null)
                property((String)curr.getKey()).cache();
        }

        
        protected final Iterator<Map.Entry<Object, Object>> i;
        protected Map.Entry<Object, Object> curr;
    }

    protected class EntrySet extends AbstractSet<Map.Entry<Object, Object>>
    {
        public EntrySet(Set<Map.Entry<Object, Object>> _set)
        {
            set = _set;
        }
        
        
        public Iterator<Map.Entry<Object, Object>> iterator()
        {
            return new EntrySetIterator(set.iterator());
        }

        public int size()
        {
            return set.size();
        }

        
        protected final Set<Entry<Object,Object>> set;
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet()
    {
        return new EntrySet(super.entrySet());
    }

    @Override
    public Set<Object> keySet()
    {
        return new ObjectSet(super.keySet());
    }

    @Override
    public Collection<Object> values()
    {
        return new ObjectCollection(super.values());
    }
    
    @Override
    public synchronized Object remove(Object _key)
    {
        Object tmp_ = super.remove(_key);
        
        property((String)_key).cache();
        
        return tmp_;
    }
    
    // END of Java/JDK/Collections greatness display!
    // All of the above was written solely for the purpose of hooking on remove
    ///////////////////////////////////////////////////////////////////////////
}
