
package x.java.util;

import java.util.IllegalFormatException;


/**
 * The better {@link java.util.Properties}
 */
@SuppressWarnings("serial")
public class Properties extends java.util.Properties
{
    public Properties()
    {
        super();
    }
    
    public Properties(Properties _defaults)
    {
        super(_defaults);
    }

    
    public String getStringProperty(String _key)
    {
        String sProp_ = getProperty(_key);

        if (sProp_ == null)
            throw new UnknownPropertyException(_key);
        
        return sProp_;
    }

    public int getIntegerProperty(String _key)
    {
        String sProp_ = getProperty(_key);

        if (sProp_ == null)
            throw new UnknownPropertyException(_key);

        try {
            return Integer.decode(sProp_);
        } catch (NumberFormatException _) {
            throw new IllegalPropertyValueException(_key + "=" + sProp_, _);
        }        
    }

    public int getIntegerProperty(String _key, int _default)
    {
        String sProp_ = getProperty(_key);

        if (sProp_ == null)
            return _default;

        try {
            return Integer.decode(sProp_);
        } catch (NumberFormatException _) {
            throw new IllegalPropertyValueException(_key + "=" + sProp_, _);
        }
    }

    public String getFormatProperty(String _key, Object ... _args)
    {
        String sProp_ = getProperty(_key);

        if (sProp_ == null)
            throw new UnknownPropertyException(_key);
        
        try {
            return String.format(sProp_, _args);
        } catch (IllegalFormatException _) {
            throw new IllegalPropertyValueException(_key, _);
        }
    }

    public String getFormatProperty
            (String _key, String _default, Object ... _args)
    {
        String sProp_ = getProperty(_key);

        try {
            return String.format((sProp_ != null ? sProp_ : _default), _args);
        } catch (IllegalFormatException _) {
            throw new IllegalPropertyValueException(_key, _);
        }
    }
    
    /**
     * 
     * @param _key
     * @throws UnknownPropertyException if the defaults don't contain the _key
     */
    public synchronized void resetProperty(String _key)
    {
        /**
         * Not actually needed, just for the sake of semantics
         */
        if (_key == null || defaults == null || !defaults.containsKey(_key))
            throw new UnknownPropertyException(_key);
        
        remove(_key);
    }
}
