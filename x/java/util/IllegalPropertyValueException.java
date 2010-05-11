
package x.java.util;


@SuppressWarnings("serial")
public class IllegalPropertyValueException extends IllegalArgumentException
{
    public IllegalPropertyValueException()
    {
    }

    public IllegalPropertyValueException(String _s)
    {
        super(_s);
    }

    public IllegalPropertyValueException(Throwable _cause)
    {
        super(_cause);
    }

    public IllegalPropertyValueException(String _message, Throwable _cause)
    {
        super(_message, _cause);
    }

}
